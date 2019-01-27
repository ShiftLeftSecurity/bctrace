package io.shiftleft.bctrace.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A jars-in-jar class loader that stores entries in direct memory.
 *
 * Main jar is required to contain a "libraries.txt" descriptor in the root namespace, where each
 * line contains the relative path of the managed inner jars
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class BctraceClassLoader extends ClassLoader {

  private static final String URL_PROTOCOL = "bctrace-agent";

  private final URLStreamHandler handler = new AgentURLStreamHandler();
  private final Map<String, Map<URL, ByteBuffer>> resourceMap = new HashMap<String, Map<URL, ByteBuffer>>();

  private final ProtectionDomain agentProtectionDomain;

  public BctraceClassLoader() {
    super(null);
    try {
      Permissions permissions = new Permissions();
      permissions.add(new AllPermission());
      this.agentProtectionDomain = new ProtectionDomain(new CodeSource(
          new URL(URL_PROTOCOL, null, -1, "jar", handler),
          (Certificate[]) null),
          permissions,
          this,
          null);
      Scanner scanner = new Scanner(ClassLoader.getSystemResourceAsStream("libraries.txt"));
      while (scanner.hasNextLine()) {
        String jar = scanner.nextLine();
        URL url = ClassLoader.getSystemResource(jar);
        Manifest mf = getManifest(new ZipInputStream(url.openStream()));
        if (mf == null) {
          throw new Error(
              "Library jars used by agent require a manifest. Error found in jar " + jar);
        }
        ZipInputStream zis = new ZipInputStream(url.openStream());
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
          String name = zipEntry.getName();
          URL entryURL = new URL(URL_PROTOCOL, null, -1, jar + "!" + name, handler);
          if (name.endsWith("/")) {
            if (!name.startsWith("META-INF/")) {
              try {
                definePackage(name, mf, entryURL);
              } catch (IllegalArgumentException iae) {
                // Race conditions due to concurrent definition
                if (getPackage(name) == null) {
                  throw new AssertionError(
                      "Package " + name + " has already been defined but it could not be found");
                }
              }
            }
          } else {
            byte[] bytes = read(zis);
            Map<URL, ByteBuffer> entryMap = resourceMap.get(name);
            if (entryMap == null) {
              entryMap = new HashMap<URL, ByteBuffer>();
              resourceMap.put(name, entryMap);
            }
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            entryMap.put(entryURL, byteBuffer);
          }
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static Manifest getManifest(ZipInputStream zis) throws IOException {
    ZipEntry zipEntry;
    while ((zipEntry = zis.getNextEntry()) != null) {
      if (zipEntry.getName().equals(JarFile.MANIFEST_NAME)) {
        return new Manifest(zis);
      }
    }
    return null;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    Map<URL, ByteBuffer> entryMap = resourceMap.get(name.replace('.', '/') + ".class");
    if (entryMap == null) {
      throw new ClassNotFoundException(name);
    }
    ByteBuffer byteBuffer = entryMap.values().iterator().next();
    byte[] bytes = new byte[byteBuffer.limit()];
    synchronized (byteBuffer) {
      byteBuffer.position(0);
      byteBuffer.get(bytes);
    }
    return super.defineClass(name, bytes, 0, bytes.length, this.agentProtectionDomain);
  }

  @Override
  protected Enumeration<URL> findResources(String name) {
    name = removeLeadingSlash(name);
    Map<URL, ByteBuffer> entryMap = resourceMap.get(name);
    if (entryMap == null) {
      return null;
    }
    return Collections.enumeration(entryMap.keySet());
  }

  @Override
  protected URL findResource(String name) {
    name = removeLeadingSlash(name);
    Enumeration<URL> urls = findResources(name);
    if (urls == null) {
      return null;
    }
    return urls.nextElement();
  }

  @Override
  protected Package getPackage(String name) {
    return super.getPackage(getNormalizedPackageName(name));
  }


  private void definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
    if (getPackage(name) != null) {
      return;
    }
    String path = name.replace('.', '/').concat("/");
    String specTitle = null, specVersion = null, specVendor = null;
    String implTitle = null, implVersion = null, implVendor = null;
    String sealed = null;
    URL sealBase = null;

    Attributes attr = man.getAttributes(path);
    if (attr != null) {
      specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
      specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
      specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
      implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
      implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
      implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
      sealed = attr.getValue(Attributes.Name.SEALED);
    }
    attr = man.getMainAttributes();
    if (attr != null) {
      if (specTitle == null) {
        specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
      }
      if (specVersion == null) {
        specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
      }
      if (specVendor == null) {
        specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
      }
      if (implTitle == null) {
        implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
      }
      if (implVersion == null) {
        implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
      }
      if (implVendor == null) {
        implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
      }
      if (sealed == null) {
        sealed = attr.getValue(Attributes.Name.SEALED);
      }
    }
    if ("true".equalsIgnoreCase(sealed)) {
      sealBase = url;
    }

    definePackage(getNormalizedPackageName(name), specTitle, specVersion, specVendor,
        implTitle, implVersion, implVendor, sealBase);
  }

  private static String getNormalizedPackageName(String name) {
    return removeTrailingSlash(name).replace('/', '.');
  }

  private static String removeTrailingSlash(String name) {
    return name.replaceAll("/+$", "");
  }

  private static String removeLeadingSlash(String name) {
    return name.replaceAll("^/+", "");
  }

  private byte[] read(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }

  private class AgentURLConnection extends URLConnection {

    protected AgentURLConnection(URL url) {
      super(url);
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public InputStream getInputStream() throws IOException {
      String name = url.getFile().substring(url.getFile().lastIndexOf("!") + 1);
      Map<URL, ByteBuffer> entryMap = resourceMap.get(name);
      if (entryMap == null) {
        return null;
      }
      ByteBuffer byteBuffer = entryMap.get(url);
      if (byteBuffer == null) {
        return null;
      }
      byte[] bytes = new byte[byteBuffer.limit()];
      synchronized (byteBuffer) {
        byteBuffer.position(0);
        byteBuffer.get(bytes);
      }
      return new ByteArrayInputStream(bytes);
    }
  }

  private class AgentURLStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      return new AgentURLConnection(url);
    }
  }
}
