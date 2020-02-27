# io.shiftleft:bctrace

An extensible framework for creating production-ready **java agents**.


`bctrace` exposes a simple event-driven programming model, built around the [`Hook`](core/src/main/java/io/shiftleft/bctrace/hook/Hook.java) abstraction, and 
saves the developer from the complexity of dealing with bytecode manipulation.

 
## Instrumentation primitives
- Notifying events to hook listeners in the case of
  - Method just started
  - Method about to return
  - Method about to rise a `Throwable`
  - Call site about to be invoked
  - Call site just returned
  - Call site just raised a `Throwable`
- Changing runtime data: 
  - Method/call-site (about to be) passed arguments
  - Method/call-site (about to be) returned value
  - Method/call-site (about to be) thrown `Throwable`
  
## Features
 - Battle tested and production-ready
 - Generic vs direct APIs (this last suited for instrumenting hot spot methods)
 - Automatic packaging of dependencies
 - Supports filtering based on class hierarchy
 - Off-heap classloading, that ensures no side effects in the target application
 - Ensures no recursive event notifications are triggered from listener code
 - Ensures no exceptions raised by listeners reach the application
 - JMX metrics
 - Extensible
   - Logging
   - Help menu
 
## Getting started

### Bootstraping a new agent project
Set the coordinates for you new agent project
```bash
export ORG_ID=org.myorganization
export ARTIFACT_ID=test-agent
export VERSION=0.0.0-SNAPSHOT
```
Generate the project into a new folder of the current directory by using the provided archetype:
```bash
mvn archetype:generate -B \
-DarchetypeGroupId=io.shiftleft \
-DarchetypeArtifactId=bctrace-archetype \
-DarchetypeVersion=0.0.0-SNAPSHOT \
-DgroupId=$ORG_ID \
-DartifactId=$ARTIFACT_ID \
-Dversion=$VERSION
```
Move to the new agent project root folder
```bash
cd $ARTIFACT_ID
```
And build it 
```bash
mvn clean package

```
### Project structure
The newly created agent project is a multi-module Maven project, than contains 
- `/agent`: The agent module. 
- `/playground`: Several target applications to test instrumentation.

### Running the agent 
Run the `/playground/hello-word` test application:
```bash
$ java -jar playground/hello-world/target/$ARTIFACT_ID-playground-hello-world-$VERSION.jar

Hello world!
```  
By default, the generated agent defines two hooks: 
- [`StringConstructorHook`](https://github.com/ShiftLeftSecurity/bctrace/blob/master/archetype/src/main/resources/archetype-resources/agent/src/main/java/__artifactIdUnhyphenated__/hooks/StringConstructorHook.java), that logs `String` constructors invocations.
- [`StringBuilderAppendHook`](https://github.com/ShiftLeftSecurity/bctrace/blob/master/archetype/src/main/resources/archetype-resources/agent/src/main/java/__artifactIdUnhyphenated__/hooks/StringBuilderAppendHook.java) that logs `StringBuilder.append()` invocations.

Now, run it again, attaching the agent, and compare the results:
```bash
$ java -javaagent:agent/target/$ARTIFACT_ID-$VERSION.jar -jar playground/hello-world/target/$ARTIFACT_ID-playground-hello-world-$VERSION.jar

INFO 1554349422236 Starting bctrace agent ...
INFO 1554349422267 Created String instance: "playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar"
INFO 1554349422267 Created String instance: "playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar"
INFO 1554349422267 Created String instance: "META-INF/MANIFEST.MF"
INFO 1554349422268 Created String instance: "Manifest-Version"
INFO 1554349422268 Created String instance: "1.0"
INFO 1554349422268 Created String instance: "Archiver-Version"
INFO 1554349422268 Created String instance: "Plexus Archiver"
INFO 1554349422268 Created String instance: "Built-By"
INFO 1554349422268 Created String instance: "nacho"
INFO 1554349422268 Created String instance: "Created-By"
INFO 1554349422268 Created String instance: "Apache Maven 3.5.4"
INFO 1554349422269 Created String instance: "Build-Jdk"
INFO 1554349422269 Created String instance: "1.8.0_191"
INFO 1554349422269 Created String instance: "Main-Class"
INFO 1554349422269 Created String instance: "org.myorganization.testagent.playground.helloworld.Main"
INFO 1554349422269 Created String instance: "org/myorganization/testagent/playground/helloworld/Main"
INFO 1554349422269 Created String instance: "org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422269 Created String instance: "org/myorganization/testagent/playground/helloworld/Main"
INFO 1554349422269 Created String instance: "org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422269 Created String instance: "org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422270 Created String instance: "org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422270 Created String instance: "org/"
INFO 1554349422270 Appending "" + "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!/"
INFO 1554349422270 Appending "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!/" + "org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422270 Created String instance: "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!/org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422270 Created String instance: "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!"
INFO 1554349422270 Created String instance: "/org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422270 Appending "" + "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!"
INFO 1554349422271 Appending "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!" + "/org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422271 Created String instance: "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar!/org/myorganization/testagent/playground/helloworld/Main.class"
INFO 1554349422271 Created String instance: "org.myorganization.testagent.playground.helloworld"
INFO 1554349422271 Created String instance: "META-INF/MAVEN/"
INFO 1554349422271 Created String instance: "META-INF/MAVEN/ORG.MYORGANIZATION/"
INFO 1554349422271 Created String instance: "META-INF/MAVEN/ORG.MYORGANIZATION/TEST-AGENT-PLAYGROUND-HELLO-WORLD/"
INFO 1554349422271 Created String instance: "META-INF/MAVEN/ORG.MYORGANIZATION/TEST-AGENT-PLAYGROUND-HELLO-WORLD/POM.XML"
INFO 1554349422271 Created String instance: "META-INF/MAVEN/ORG.MYORGANIZATION/TEST-AGENT-PLAYGROUND-HELLO-WORLD/POM.PROPERTIES"
INFO 1554349422272 Created String instance: "Manifest-Version"
INFO 1554349422272 Created String instance: "1.0"
INFO 1554349422272 Created String instance: "Archiver-Version"
INFO 1554349422272 Created String instance: "Plexus Archiver"
INFO 1554349422272 Created String instance: "Built-By"
INFO 1554349422272 Created String instance: "nacho"
INFO 1554349422272 Created String instance: "Created-By"
INFO 1554349422272 Created String instance: "Apache Maven 3.5.4"
INFO 1554349422272 Created String instance: "Build-Jdk"
INFO 1554349422272 Created String instance: "1.8.0_191"
INFO 1554349422272 Created String instance: "Main-Class"
INFO 1554349422272 Created String instance: "org.myorganization.testagent.playground.helloworld.Main"
INFO 1554349422272 Created String instance: "org/myorganization/testagent/playground/helloworld"
INFO 1554349422272 Created String instance: "org/myorganization/testagent/playground/helloworld/"
INFO 1554349422273 Created String instance: "org/myorganization/testagent/playground/helloworld"
INFO 1554349422273 Created String instance: "org/myorganization/testagent/playground/helloworld/"
INFO 1554349422273 Created String instance: "org/myorganization/testagent/playground/helloworld"
INFO 1554349422273 Created String instance: "org/myorganization/testagent/playground/helloworld/"
INFO 1554349422273 Created String instance: "org.myorganization.testagent.playground.helloworld"
INFO 1554349422273 Created String instance: "file:/tmp/test-agent/playground/hello-world/target/test-agent-playground-hello-world-0.0.0-SNAPSHOT.jar"
INFO 1554349422274 Appending "" + "Hello "
INFO 1554349422274 Appending "Hello " + "world"
INFO 1554349422274 Appending "Hello world" + "!"
INFO 1554349422274 Created String instance: "Hello world!"
Hello world!
INFO 1554349422274 Appending "" + ""
INFO 1554349422274 Appending "" + ".level"
INFO 1554349422275 Created String instance: ".level"  
```  
Congratulations, your `bctrace` agent is up and running (and ready to grow)!

### Next steps
Explore the agent documentation

## Main stack
This module could not be possible without:
* [org.ow2.asm:asm-all](http://asm.ow2.org/)

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

## License
Based on [brutusin:instrumentation](https://github.com/brutusin/instrumentation) by Ignacio del Valle Alles distributed under [Apache v2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

## Creating a release
```bash
VERSION="0.0.1"
mvn versions:set -DnewVersion=$VERSION
mvn versions:commit
git add pom.xml */pom.xml
git commit -m "updating version to $VERSION"
git tag v$VERSION
mvn clean test javadoc:jar deploy
git push origin $VERSION
```
