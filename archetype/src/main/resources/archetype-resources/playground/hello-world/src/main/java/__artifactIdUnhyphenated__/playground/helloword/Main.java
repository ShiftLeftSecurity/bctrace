package ${package}.${artifactIdUnhyphenated}.playground.helloworld;

public class Main {

  public static void main(String[] args) {
    System.out.println("Hello " + getName(args) + "!");
  }

  private static String getName(String[] args) {
    if (args.length > 0) {
      return args[0];
    } else {
      return "world";
    }
  }
}

