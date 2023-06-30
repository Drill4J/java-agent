# Removed obsolete features

## Support for additional native-libraries load on vm init

* Revision: 0db87d2c20d973ea89dc378b0fe17f4bd5db4778
* Author: epamrsa <epamrsa@gmail.com>
* Date: Tuesday, 20 June, 2023 15:53:19
* Message:
```
feat: Change java agent to transfer only class metadata instead of class bytes to the admin side

Removed obsolete support for additional native-libraries load on vm init

Refs: EPMDJ-10603
```
* Changes:
```
Deleted: java-agent/src/commonMain/kotlin/com/epam/drill/agent/NativeRegistry.kt
Deleted: java-agent/src/jvmMain/kotlin/com/epam/drill/agent/NativeRegistry.kt
Modified: java-agent/src/nativeMain/kotlin/com/epam/drill/agent/Config.kt
Modified: java-agent/src/nativeMain/kotlin/com/epam/drill/agent/Configuration.kt
Deleted: java-agent/src/nativeMain/kotlin/com/epam/drill/agent/NativeRegistry.kt
Modified: java-agent/src/nativeMain/kotlin/com/epam/drill/core/callbacks/vminit/VmInitEvent.kt
```

## Native plugins support

* Revision: 0081846ff882f276123f2df9f547bdeb5c6af35b
* Author: epamrsa <epamrsa@gmail.com>
* Date: Friday, 30 June, 2023 16:16:47
* Message:
```
feat: Change java agent to transfer only class metadata instead of class bytes to the admin side

Remove obsolete support for native plugins

Refs: EPMDJ-10603
```
* Changes:
```
Modified: java-agent/src/nativeMain/kotlin/com/epam/drill/core/CallbackRegister.kt
```
