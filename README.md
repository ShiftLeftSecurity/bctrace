# io.shiftleft:bctrace

An extensible framework for creating production-ready java agents aimed at tracing and changing the behaviour of java applications without changing their source code.

`bctrace` offers a simple event-driven programming model, built around the `Hook` abstraction, and 
saves the developer from the complexity of dealing with bytecode manipulation.

It offers a set of high level primitives that allow:

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
  
 Generic API vs Direct API:
 
 https://github.com/opentracing/opentracing-java
 
 https://github.com/census-instrumentation/opencensus-java

 https://github.com/opentracing-contrib/java-specialagent
 
 ### Features
 
 - Battle tested and production-ready
 - Automatic packaging of dependencies
 - Supports filtering based on class hierarchy
 - Custom off-heap classloader, that ensures no side effects in the target application
 - Ensures no recursive event notification are triggered from listener code
 - Ensures no exceptions raised by listeners reach the application
 - JMX metrics
 - Extensible
   - Logging
   - Help menu
 
 ### Generic vs Direct APIs:




 aimed at instrumenting programs running on the JVM (modifying their original bytecode both at class-loading-time, and at run-time), with the purpose of capturing method invocation events (start, finish, errors ...) and notifying custom listeners.

> This project is a candidate to be released as OSS in the future, so its scope should be kept as generic as possible without including any ShiftLeft core feature.

**Table of Contents**
- [io.shiftleft:bctrace](#ioshiftleftctrace)
  - [How it works](#how-it-works)
  - [Usage](#usage)
  - [Registering hooks](#registering-hooks)
  - [API](#api)
  - [Maven dependency](#maven-dependency)
  - [Authors](#authors)
  - [License](#license)
	
## How it works
The [java instrumentation package](http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html) introduced in Java version 1.5, provides a simple way to transform java-class definition at loading time, consisting basically in a `byte[]` to `byte[]` transformation, by the so called "java agents".

Since Java version 1.6 these agents can perform also dynamic instrumentation; that is, retransforming the bytecode of classes already loaded. 

This library provides an configurable agent ([io.shiftleft.btrace.Init](src/main/java/io/shiftleft/bctrace/Init.java)) (to be used as an external dependency by extending agent implementations) aimed at injecting custom [hooks](src/main/java/io/shiftleft/bctrace/spi/Hook.java) into the code of the specified methods of the target application.


From a simplified point of view, the dynamic transformation turns a method like this: 
```java
public Object foo(Object bar){
    return new Object();
}
```

into that:
```java
public Object foo(Object bar){
    hook1.getListener().onStart(bar);
    ...
    hookn.getListener().onStart(bar);
    try {
        Object ret = new Object();
	hook1.getListener().onFinishedReturn(ret);
	...
	hookn.getListener().onFinishedReturn(ret);
        return ret;
    } catch(Throwable th) {
    	hook1.getListener().onFinishedThrowable(th);
	...
	hookn.getListener().onFinishedThrowable(th);
        throw th; // at bytecode level this is legal
    }
}
```
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


