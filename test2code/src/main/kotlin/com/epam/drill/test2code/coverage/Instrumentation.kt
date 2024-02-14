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
package com.epam.drill.test2code.coverage

import com.epam.drill.common.agent.module.*
import com.epam.drill.jacoco.*
import com.epam.drill.jacoco.BooleanArrayProbeInserter.*
import com.epam.drill.test2code.classparsing.ProbeCounter
import kotlinx.atomicfu.*
import org.jacoco.core.internal.data.CRC64
import org.jacoco.core.internal.flow.*
import org.jacoco.core.internal.instr.*
import org.objectweb.asm.*
import mu.KotlinLogging

private val classCounter = atomic(0)

class DrillInstrumenter(
    private val probesProxy: IProbesProxy,
    private val classDescriptorsStorage: IClassDescriptorStorage
) : Instrumenter {

    private val logger = KotlinLogging.logger {}

    override fun instrument(className: String, initialBytes: ByteArray): ByteArray? = try {
        val version = InstrSupport.getMajorVersion(initialBytes)
        val classId = CRC64.classId(initialBytes)

        //count probes before transformation
        val counter = ProbeCounter()
        val reader = InstrSupport.classReaderFor(initialBytes)
        reader.accept(DrillClassProbesAdapter(counter, false), 0)

        val genId = classCounter.incrementAndGet()
        val probeCount = counter.count
        val strategy = DrillProbeStrategy(
            probesProxy,
            className,
            classId,
            genId,
            probeCount
        )
        val writer = object : ClassWriter(reader, 0) {
            override fun getCommonSuperClass(type1: String, type2: String): String = throw IllegalStateException()
        }
        val visitor = DrillClassProbesAdapter(
            DrillClassInstrumenter(strategy, className, writer),
            InstrSupport.needsFrames(version)
        )
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)

        classDescriptorsStorage.add(
            ClassDescriptor(
                id = classId,
                name = className,
                probeCount = probeCount
            )
        )

        writer.toByteArray()
    } catch (e: Exception) {
        logger.error { "Error while instrumenting $className: ${e.message}" }
        null
    }
}


private class DrillProbeStrategy(
    private val probesProxy: IProbesProxy,
    private val className: String,
    private val classId: Long,
    private val number: Int,
    private val probeCount: Int
) : IProbeArrayStrategy {
    override fun storeInstance(mv: MethodVisitor?, clinit: Boolean, variable: Int): Int = mv!!.run {
        val drillClassName = probesProxy.javaClass.name.replace('.', '/')
        visitFieldInsn(Opcodes.GETSTATIC, drillClassName, "INSTANCE", "L$drillClassName;")
        // Stack[0]: Lcom/epam/drill/jacoco/Stuff;

        visitLdcInsn(classId)
        visitLdcInsn(number)
        visitLdcInsn(className)
        visitLdcInsn(probeCount)
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, drillClassName, "invoke", "(JILjava/lang/String;I)L$PROBE_IMPL;",
            false
        )
        visitVarInsn(Opcodes.ASTORE, variable)

        6 //stack size
    }

    override fun addMembers(cv: ClassVisitor?, probeCount: Int) {
//        createDataField(cv)
    }
}

class DrillClassInstrumenter(
    private val probeArrayStrategy: IProbeArrayStrategy,
    private val clazzName: String,
    cv: ClassVisitor
) : ClassInstrumenter(probeArrayStrategy, cv) {

    override fun visitMethod(
        access: Int,
        name: String?,
        desc: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodProbesVisitor {
        InstrSupport.assertNotInstrumented(name, clazzName)
        val mv = cv.visitMethod(
            access, name, desc, signature,
            exceptions
        )
        val frameEliminator: MethodVisitor = DrillDuplicateFrameEliminator(mv)
        val probeVariableInserter = BooleanArrayProbeInserter(
            access,
            name,
            desc,
            frameEliminator,
            this.probeArrayStrategy
        )
        return DrillMethodInstrumenter(
            probeVariableInserter,
            probeVariableInserter
        )
    }
}
