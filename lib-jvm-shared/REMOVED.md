# Removed obsolete features

## Native plugins support

* Revision: f88002dd2de8b33296ca837b27b465952dd2ffd6
* Author: epamrsa <epamrsa@gmail.com>
* Date: Friday, 30 June, 2023 16:17:26
* Message:
```
feat: Change java agent to transfer only class metadata instead of class bytes to the admin side

Remove obsolete support for native plugins

Refs: EPMDJ-10603
```
* Changes:
```
Deleted: agent/src/commonMain/kotlin/com/epam/drill/DynamicLoader.kt
Modified: agent/src/jvmMain/kotlin/com/epam/drill/ActualStubs.kt
Deleted: agent/src/mingwX64Main/kotlin/com/epam/drill/DynamicLoader.kt
Modified: agent/src/nativeMain/kotlin/com/epam/drill/core/AgentApi.kt
Modified: agent/src/nativeMain/kotlin/com/epam/drill/core/ws/WsRouter.kt
Deleted: agent/src/nativeMain/kotlin/com/epam/drill/plugin/api/processing/NativePart.kt
Deleted: agent/src/posixMain/kotlin/com/epam/drill/DynamicLoader.kt
Modified: common/src/commonMain/kotlin/com/epam/drill/common/PluginMetadata.kt
```

## Removed modules: KNI (obsolete), interceptor-http2 (obsolete), agent-runner-gradle (moved to autotest-agent repo), agent-runner-common (moved to autotest-agent repo)

* Revision: 3f711a163660737089c33bf7632ab86bbf1be058
* Author: epamrsa <epamrsa@gmail.com>
* Date: Thursday, 9 November, 2023 16:53:03
* Message:
```
feat: Remove obsolete modules in lib-jvm-shared and java-agent

Removed modules:
- KNI (obsolete)
- interceptor-http2 (obsolete)
- agent-runner-gradle (moved to autotest-agent repo)
- agent-runner-common (moved to autotest-agent repo)

Refs: EPMDJ-10708
```
* Changes:
```
Deleted: agent-runner-common/LICENSE
Deleted: agent-runner-common/README.md
Deleted: agent-runner-common/build.gradle.kts
Deleted: agent-runner-common/src/main/kotlin/com/epam/drill/agent/runner/AgentConfiguration.kt
Deleted: agent-runner-common/src/main/kotlin/com/epam/drill/agent/runner/AgentLoader.kt
Deleted: agent-runner-common/src/main/kotlin/com/epam/drill/agent/runner/AppAgentConfiguration.kt
Deleted: agent-runner-common/src/main/kotlin/com/epam/drill/agent/runner/Configuration.kt
Deleted: agent-runner-common/src/main/kotlin/com/epam/drill/agent/runner/LogLevels.kt
Deleted: agent-runner-common/src/main/kotlin/com/epam/drill/agent/runner/OS.kt
Deleted: agent-runner-gradle/LICENSE
Deleted: agent-runner-gradle/README.md
Deleted: agent-runner-gradle/build.gradle.kts
Deleted: agent-runner-gradle/src/main/kotlin/com/epam/drill/autotest/gradle/Agent.kt
Deleted: agent-runner-gradle/src/main/kotlin/com/epam/drill/autotest/gradle/AppAgent.kt
Deleted: agent-runner-gradle/src/main/kotlin/com/epam/drill/autotest/gradle/AutoTestAgent.kt
Modified: gradle.properties
Deleted: interceptor-http2-test-grpc/LICENSE
Deleted: interceptor-http2-test-grpc/build.gradle.kts
Deleted: interceptor-http2-test-grpc/src/main/proto/helloworld/helloworld.proto
Deleted: interceptor-http2-test-grpc/src/test/kotlin/GrpcTest.kt
Deleted: interceptor-http2-test-grpc/src/test/kotlin/Xs.kt
Deleted: interceptor-http2-test/LICENSE
Deleted: interceptor-http2-test/build.gradle.kts
Deleted: interceptor-http2-test/src/commonMain/kotlin/CommonDataForTest.kt
Deleted: interceptor-http2-test/src/jvmJettyServerTest/kotlin/io/ktor/samples/http2push/Http2PushApplication.kt
Deleted: interceptor-http2-test/src/jvmJettyServerTest/resources/application.conf
Deleted: interceptor-http2-test/src/jvmJettyServerTest/resources/index.html
Deleted: interceptor-http2-test/src/jvmJettyServerTest/resources/keystore
Deleted: interceptor-http2-test/src/jvmJettyServerTest/resources/temporary.jks
Deleted: interceptor-http2-test/src/jvmMain/kotlin/bindings/Bindings.kt
Deleted: interceptor-http2-test/src/nativeAgentMain/kotlin/Main.kt
Deleted: interceptor-http2/LICENSE
Deleted: interceptor-http2/README.md
Deleted: interceptor-http2/build.gradle.kts
Deleted: interceptor-http2/src/commonMain/kotlin/com/epam/drill/interceptor/api.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/AutoCloseable.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/ByteArrayInputStream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/ByteArrayOutputStream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/Closeable.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/Decoder.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/DynamicTable.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/EOFException.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/Encoder.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/Flushable.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/HeaderField.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/HeaderListener.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/HpackUtil.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/Huffman.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/HuffmanDecoder.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/HuffmanEncoder.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/IOException.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/InputStream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/OutputStream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/StaticTable.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/hpack/System.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/Arrays.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/Http2.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/Http2Parser.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/ReadStream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/Stream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/WriteStream.kt
Deleted: interceptor-http2/src/commonNative/kotlin/com/epam/drill/interceptor/haffman/Util.kt
Deleted: interceptor-http2/src/linuxX64Main/kotlin/com/epam/drill/interceptor/Http2.kt
Deleted: interceptor-http2/src/macosX64Main/kotlin/com/epam/drill/interceptor/Http2.kt
Deleted: interceptor-http2/src/macosX64Test/kotlin/Arrays.kt
Deleted: interceptor-http2/src/mingwX64Main/kotlin/com/epam/drill/interceptor/Http2.kt
Deleted: kni-plugin/LICENSE
Deleted: kni-plugin/README.md
Deleted: kni-plugin/build.gradle.kts
Deleted: kni-plugin/src/main/kotlin/com/epam/drill/kni/gradle/Generator.kt
Deleted: kni-plugin/src/main/kotlin/com/epam/drill/kni/gradle/JvmtiGenerator.kt
Deleted: kni-plugin/src/main/kotlin/com/epam/drill/kni/gradle/Kni.kt
Deleted: kni-plugin/src/main/kotlin/com/epam/drill/kni/gradle/Mappings.kt
Deleted: kni-runtime/LICENSE
Deleted: kni-runtime/README.md
Deleted: kni-runtime/build.gradle.kts
Deleted: kni-runtime/src/commonMain/kotlin/com/epam/drill/kni/Annotation.kt
Deleted: kni-runtime/src/commonMain/kotlin/com/epam/drill/kni/JvmtiAgent.kt
Modified: settings.gradle.kts
```
