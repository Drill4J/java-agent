[![Check](https://github.com/Drill4J/java-agent/actions/workflows/check.yml/badge.svg)](https://github.com/Drill4J/java-agent/actions/workflows/check.yml)
[![Release](https://github.com/Drill4J/java-agent/actions/workflows/release.yml/badge.svg)](https://github.com/Drill4J/java-agent/actions/workflows/release.yml)
[![License](https://img.shields.io/github/license/Drill4J/java-agent)](LICENSE)
[![Visit the website at drill4j.github.io](https://img.shields.io/badge/visit-website-green.svg?logo=firefox)](https://drill4j.github.io/)
[![Telegram Chat](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/drill4j)  
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Drill4J/java-agent)
![Docker Pulls](https://img.shields.io/docker/pulls/drill4j/java-agent)
![GitHub contributors](https://img.shields.io/github/contributors/Drill4J/java-agent)
![Lines of code](https://img.shields.io/tokei/lines/github/Drill4J/java-agent)
![YouTube Channel Views](https://img.shields.io/youtube/channel/views/UCJtegUnUHr0bO6icF1CYjKw?style=social)

# Drill agent component

This module contains the native agent used to profile a java application and report the results to Drill4J admin services.

## Modules

- **bootstrap**: See description below
- **java-agent**: Java-agent itself
- **pt-runner**: Module with scripts to run demo application

## Classes structure

- **commonMain**: consist of empty `expected` classes (transformers, plugin related and request-processing related classes) and few utility classes (serialization functions and `ClassSource` models)
- **jvmMain**: classes for initial class scanning and transformation
  - `com.epam.drill.agent.classloading`: initial class scanning without transformation
  - `com.epam.drill.agent.instrument`: java-classes instrumentation for http-headers processing
    - `com.epam.drill.agent.instrument.Transformer`: `ServletContextListeneer` instrumentation for web-app classes initial scanning
  - `com.epam.drill.request`: http-request headers processing (mainly related with `drill-session-id`)
- **nativeMain**: 
  - `com.epam.drill.agent`: agent configuration and state classes
  - `com.epam.drill.agent.classloading`: stubs to call jvm-part of the same package (using KNI)
  - `com.epam.drill.agent.instrument`: stubs to call jvm-part of the same package (using KNI)
  - `com.epam.drill.core`:
    - `Starter.kt`: JVMTI entrypoint, call agent configuration classes and registers hooks on JVM initialization and class-loading
    - `CallbackRegister.kt`: Internal callback initialization
    - `SymbolsRegister.kt`: Native functions to call from plugins using `com.epam.drill.plugin.api.Native` class
  - `com.epam.drill.core.callbacks.vminit`: Agent initialization callbacks (connects to admin, reads package settings)
  - `com.epam.drill.core.callbacks.classloading`: Classloading callback (applies default transformers from agent-jvm-part and transformers from loaded plugins)
  - `com.epam.drill.core.plugin.loader`: Plugin load and initialization related classes

## Run demo
To run the demo application (Petclinic) with a built agent following command may be used:
```
./gradlew :pt-runner:run 
```
The Petclinic application will be started on port 8080, and agent will be configured to connect to the admin back-end on port 8090.

## Module bootstrap

Problems before:
- Need to update version agent, but restart of app with agent takes a lot of time, and it breaks CI/CD process.
  We can find the moment when app will be restarted, but it is not convenient.
- On Windows, we can't remove files(agent) while process is running

Bootstrap solve these problems, now we can change dirs to java agent. After restart app we will use new java agent. 
