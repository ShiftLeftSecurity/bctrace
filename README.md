# io.shiftleft:bctrace

An extensible framework for creating production-ready **java agents**.


`bctrace` exposes a simple event-driven programming model, built around the `Hook` abstraction, and 
saves the developer from the complexity of dealing with bytecode manipulation.

 
## Instrumentation primitives
- Notifying events to hook listeners in the case of:
  - Method started
  - Method about to return
  - Method about to rise a `Throwable`
  - Call site about to be invoked
  - Call site just returned
  - Call site just raised a `Throwable`
- Changing runtime data: 
  - Method/call-site passed argument values
  - Method/call-site value to be returned
  - Method/call-site `Throwable` to be raised
  
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
 
## Usage
Agent projects making use of this library must create a **fat-jar** including all their dependencies. 
Agent jars must contain at least this entry in its manifest:
```
Premain-Class: io.shiftleft.bctrace.Init
```
This fat-jar is the agent jar that will be passed as an argument to the java command:

```
-javaagent:thefat.jar
```

## Registering hooks
On agent bootstrap, a resource called `.bctrace` (if any) is read by the agent classloader (root namespace), where the initial (before class-loading) hook implementation class names are declared.

The agent also offers an API for registering hooks dynamically.

## API
These are the main types to consider:

### BcTrace
[`BcTrace`](src/main/java/o/shiftleft/bctrace/Bctrace.java) class offers a singleton instance that allows to register/unregister hooks dinamically from code.

### Hook
[`Hook`](src/main/java/io/shiftleft/bctrace/spi/Hook.java) class represents the main abstraction that client projects has to implement. Hooks are registered programatically using the previous API, or statically from the descriptor file (see ["registering hooks"](#registering-hooks)).

Hooks offer two main functionalities: 
- Filtering information (what methods to instrument)  
- Event callback (what actions to perform under the execution events ocurred in the intrumented methods)

### Instrumentation
On hook initialization, the framework passes a unique instance of [`Instrumentation`](src/main/java/io/shiftleft/bctrace/spi/Instrumentation.java) to the hook instances, to provide them retransformation capabilities, as well as accounting of all the classes they are instrumenting.

### MethodRegistry
[`MethodRegistry`](src/main/java/io/shiftleft/bctrace/runtime/MethodRegistry.java) offers a singleton instance that provides O(1) mappings: id ([`int:FrameData.methodId`](https://github.com/ShiftLeftSecurity/bctrace/blob/master/src/main/java/io/shiftleft/bctrace/runtime/FrameData.java)) <> method ([`MethodInfo`](src/main/java/io/shiftleft/bctrace/runtime/MethodInfo.java)).

## System properties
- `-Dbctrace.dump.path`: Dump instrumented class byte code to the specified folder
- `-Dbctrace.debug.server`: Track call statistics and start debug http server. Value in the form `hostname:port` 

## Maven dependency 

```xml
<dependency>
    <groupId>io.shiftleft</groupId>
    <artifactId>bctrace</artifactId>
</dependency>
```

## Main stack
This module could not be possible without:
* [org.ow2.asm:asm-all](http://asm.ow2.org/)

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

## License
Based on [brutusin:instrumentation](https://github.com/brutusin/instrumentation) by Ignacio del Valle Alles distributed under [Apache v2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

ShiftLeft license to TBD (Hopefully OSS is a future)


