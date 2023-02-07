/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused")

package com.epam.drill.api

import com.epam.drill.core.messanger.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.*
import kotlinx.cinterop.*

/**
 * we should duplicate all of this methods signature with "external" keyword and "@SymbolName" annotation, without body,
 * in order to allow use this API for native plugins
 */

@CName("JNIFun")
fun JNIFun(): JNI {
    return jni
}

@CName("JNIEn")
fun JNIEn(): JNI {
    return jni
}

@CName("sendToSocket")
fun sendToSocket(pluginId: String, message: String) {
    sendMessage(pluginId, message)
}


@CName("currentThread")
fun currentThread() = memScoped {
    val threadAllocation = alloc<jthreadVar>()
    GetCurrentThread(threadAllocation.ptr)
    threadAllocation.value
}

@CName("drillRequest")
fun drillRequest() = drillCRequest()?.get()


fun drillCRequest(thread: jthread? = currentThread()) = memScoped {
    val drillReq = alloc<COpaquePointerVar>()
    GetThreadLocalStorage(thread, drillReq.ptr)
    drillReq.value?.asStableRef<DrillRequest>()
}


@CName("jvmtix")
fun jvmti(): CPointer<jvmtiEnvVar>? {
    return jvmti.value
}

@CName("SetEventCallbacksP")
fun jvmti(
    callbacks: CValuesRef<jvmtiEventCallbacks>?,
    size_of_callbacks: jint /* = kotlin.Int */
) {
    SetEventCallbacks(callbacks, size_of_callbacks)
}

@CName("enableJvmtiEventBreakpoint")
fun enableJvmtiEventBreakpoint(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, thread)
    
}

@CName("enableJvmtiEventClassFileLoadHook")
fun enableJvmtiEventClassFileLoadHook(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread)
    
}

@CName("enableJvmtiEventClassLoad")
fun enableJvmtiEventClassLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, thread)
    
}

@CName("enableJvmtiEventClassPrepare")
fun enableJvmtiEventClassPrepare(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, thread)
    
}

@CName("enableJvmtiEventCompiledMethodLoad")
fun enableJvmtiEventCompiledMethodLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, thread)
    
}

@CName("enableJvmtiEventCompiledMethodUnload")
fun enableJvmtiEventCompiledMethodUnload(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, thread)
    
}

@CName("enableJvmtiEventDataDumpRequest")
fun enableJvmtiEventDataDumpRequest(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, thread)
    
}

@CName("enableJvmtiEventDynamicCodeGenerated")
fun enableJvmtiEventDynamicCodeGenerated(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, thread)
    
}

@CName("enableJvmtiEventException")
fun enableJvmtiEventException(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, thread)
    
}

@CName("enableJvmtiEventExceptionCatch")
fun enableJvmtiEventExceptionCatch(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION_CATCH, thread)
    
}

@CName("enableJvmtiEventFieldAccess")
fun enableJvmtiEventFieldAccess(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, thread)
    
}

@CName("enableJvmtiEventFieldModification")
fun enableJvmtiEventFieldModification(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_MODIFICATION, thread)
    
}

@CName("enableJvmtiEventFramePop")
fun enableJvmtiEventFramePop(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FRAME_POP, thread)
    
}

@CName("enableJvmtiEventGarbageCollectionFinish")
fun enableJvmtiEventGarbageCollectionFinish(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, thread)
    
}

@CName("enableJvmtiEventGarbageCollectionStart")
fun enableJvmtiEventGarbageCollectionStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, thread)
    
}

@CName("enableJvmtiEventMethodEntry")
fun enableJvmtiEventMethodEntry(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, thread)
    
}

@CName("enableJvmtiEventMethodExit")
fun enableJvmtiEventMethodExit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, thread)
    
}

@CName("enableJvmtiEventMonitorContendedEnter")
fun enableJvmtiEventMonitorContendedEnter(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, thread)
    
}

@CName("enableJvmtiEventMonitorContendedEntered")
fun enableJvmtiEventMonitorContendedEntered(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, thread)
    
}

@CName("enableJvmtiEventMonitorWait")
fun enableJvmtiEventMonitorWait(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, thread)
    
}

@CName("enableJvmtiEventMonitorWaited")
fun enableJvmtiEventMonitorWaited(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, thread)
    
}

@CName("enableJvmtiEventNativeMethodBind")
fun enableJvmtiEventNativeMethodBind(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, thread)
    
}

@CName("enableJvmtiEventObjectFree")
fun enableJvmtiEventObjectFree(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, thread)
    
}

@CName("enableJvmtiEventResourceExhausted")
fun enableJvmtiEventResourceExhausted(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_RESOURCE_EXHAUSTED, thread)
    
}

@CName("enableJvmtiEventSingleStep")
fun enableJvmtiEventSingleStep(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, thread)
    
}

@CName("enableJvmtiEventThreadEnd")
fun enableJvmtiEventThreadEnd(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, thread)
    
}

@CName("enableJvmtiEventThreadStart")
fun enableJvmtiEventThreadStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, thread)
    
}

@CName("enableJvmtiEventVmDeath")
fun enableJvmtiEventVmDeath(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, thread)
    
}

@CName("enableJvmtiEventVmInit")
fun enableJvmtiEventVmInit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, thread)
    
}

@CName("enableJvmtiEventVmObjectAlloc")
fun enableJvmtiEventVmObjectAlloc(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, thread)
    
}

@CName("enableJvmtiEventVmStart")
fun enableJvmtiEventVmStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, thread)
    
}

@CName("disableJvmtiEventBreakpoint")
fun disableJvmtiEventBreakpoint(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_BREAKPOINT, thread)
    
}

@CName("disableJvmtiEventClassFileLoadHook")
fun disableJvmtiEventClassFileLoadHook(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread)
    
}

@CName("disableJvmtiEventClassLoad")
fun disableJvmtiEventClassLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_LOAD, thread)
    
}

@CName("disableJvmtiEventClassPrepare")
fun disableJvmtiEventClassPrepare(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_PREPARE, thread)
    
}

@CName("disableJvmtiEventCompiledMethodLoad")
fun disableJvmtiEventCompiledMethodLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, thread)
    
}

@CName("disableJvmtiEventCompiledMethodUnload")
fun disableJvmtiEventCompiledMethodUnload(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, thread)
    
}

@CName("disableJvmtiEventDataDumpRequest")
fun disableJvmtiEventDataDumpRequest(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, thread)
    
}

@CName("disableJvmtiEventDynamicCodeGenerated")
fun disableJvmtiEventDynamicCodeGenerated(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, thread)
    
}

@CName("disableJvmtiEventException")
fun disableJvmtiEventException(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, thread)
    
}

@CName("disableJvmtiEventExceptionCatch")
fun disableJvmtiEventExceptionCatch(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION_CATCH, thread)
    
}

@CName("disableJvmtiEventFieldAccess")
fun disableJvmtiEventFieldAccess(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FIELD_ACCESS, thread)
    
}

@CName("disableJvmtiEventFieldModification")
fun disableJvmtiEventFieldModification(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FIELD_MODIFICATION, thread)
    
}

@CName("disableJvmtiEventFramePop")
fun disableJvmtiEventFramePop(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FRAME_POP, thread)
    
}

@CName("disableJvmtiEventGarbageCollectionFinish")
fun disableJvmtiEventGarbageCollectionFinish(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, thread)
    
}

@CName("disableJvmtiEventGarbageCollectionStart")
fun disableJvmtiEventGarbageCollectionStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, thread)
    
}

@CName("disableJvmtiEventMethodEntry")
fun disableJvmtiEventMethodEntry(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_ENTRY, thread)
    
}

@CName("disableJvmtiEventMethodExit")
fun disableJvmtiEventMethodExit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_EXIT, thread)
    
}

@CName("disableJvmtiEventMonitorContendedEnter")
fun disableJvmtiEventMonitorContendedEnter(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, thread)
    
}

@CName("disableJvmtiEventMonitorContendedEntered")
fun disableJvmtiEventMonitorContendedEntered(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, thread)
    
}

@CName("disableJvmtiEventMonitorWait")
fun disableJvmtiEventMonitorWait(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAIT, thread)
    
}

@CName("disableJvmtiEventMonitorWaited")
fun disableJvmtiEventMonitorWaited(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAITED, thread)
    
}

@CName("disableJvmtiEventNativeMethodBind")
fun disableJvmtiEventNativeMethodBind(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, thread)
    
}

@CName("disableJvmtiEventObjectFree")
fun disableJvmtiEventObjectFree(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_OBJECT_FREE, thread)
    
}

@CName("disableJvmtiEventResourceExhausted")
fun disableJvmtiEventResourceExhausted(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_RESOURCE_EXHAUSTED, thread)
    
}

@CName("disableJvmtiEventSingleStep")
fun disableJvmtiEventSingleStep(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, thread)
    
}

@CName("disableJvmtiEventThreadEnd")
fun disableJvmtiEventThreadEnd(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_END, thread)
    
}

@CName("disableJvmtiEventThreadStart")
fun disableJvmtiEventThreadStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_START, thread)
    
}

@CName("disableJvmtiEventVmDeath")
fun disableJvmtiEventVmDeath(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_DEATH, thread)
    
}

@CName("disableJvmtiEventVmInit")
fun disableJvmtiEventVmInit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_INIT, thread)
    
}

@CName("disableJvmtiEventVmObjectAlloc")
fun disableJvmtiEventVmObjectAlloc(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, thread)
    
}

@CName("disableJvmtiEventVmStart")
fun disableJvmtiEventVmStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_START, thread)
    
}
