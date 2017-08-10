# io.shiftleft:bctrace

An extensible java agent framework aimed at instrumenting programs running on the JVM (modifying their original bytecode both at class-loading-time, and at run-time), with the purpose of capturing method invocation events (start, finish, errors ...) and notifying custom listeners.

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

Since Java version 1.6 these agents can perform also perform dynamic instrumentation, that is retransforming the bytecode of classes already loaded. 

This library provides an configurable agent ([io.shiftleft.btrace.Init](src/main/java/io/shiftleft/bctrace/Init.java)) aimed at injecting custom [hooks](src/main/java/o/shiftleft/bctrace/spi/Hook.java) into the code of the specified methods of the target application.


From a simplified point of view, the dynamic transformation turns a method like this: 
```java
public Object foo(Object bar){
    return new Object();
}
```

into that:
```java
public Object foo(Object bar){
    onStart(bar);
    try{
        Object ret = new Object();
        onFinished(ret);
        return ret;
    } catch(Throwable th){
        onThrowable(th);
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
[`MethodRegistry`](src/main/java/io/shiftleft/bctrace/runtime/MethodRegistry.java) offers a singleton instance that provides O(1) id (`int`) <> method ([`MethodInfo`](src/main/java/io/shiftleft/bctrace/runtime/MethodInfo.java)) translations.

### FrameData
[`FrameData`](src/main/java/io/shiftleft/bctrace/runtime/FrameData.java) objects contain all the information about a execution frame, method, arguments and target object. This object are passed by the framework to the listeners for every execution event.

## Maven dependency 

```xml
<dependency>
    <groupId>org.brutusin</groupId>
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


