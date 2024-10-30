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
package com.epam.drill.test2code.classparsing

import com.epam.drill.jacoco.DrillClassProbesAdapter
import com.epam.drill.plugins.test2code.common.api.AstMethod
import org.jacoco.core.internal.flow.ClassProbesVisitor
import org.jacoco.core.internal.flow.IFrame
import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.jacoco.core.internal.instr.InstrSupport
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.AnnotationNode

open class ProbeCounter : ClassProbesVisitor() {
    var count = 0
        private set

    override fun visitMethod(
        access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?
    ): MethodProbesVisitor? {
        return null
    }

    override fun visitTotalProbeCount(count: Int) {
        this.count = count
    }
}

class ClassProbeCounter(
    private val classname: String,
    val methods: List<AstMethod> = ArrayList()
) : ProbeCounter() {
    private val annotations: MutableMap<String, List<String>> = mutableMapOf()

    override fun visitMethod(
        access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?
    ): MethodProbesVisitor {
        return MethodProbeCounter(classname = classname, methods = methods as MutableList<AstMethod>)
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        val annotationValues = mutableListOf<String>()
        val annotationVisitor = object : AnnotationVisitor(api) {
            override fun visit(name: String?, value: Any?) {
                annotationValues.add(value.toString())
            }
        }
        annotations[Type.getType(desc).className] = annotationValues
        return annotationVisitor
    }

    fun getAnnotations(): MutableMap<String, List<String>>? {
        return annotations.takeIf { it.isNotEmpty() }
    }
}


class MethodProbeCounter(
    private val classname: String,
    private val methods: MutableList<AstMethod>
) : MethodProbesVisitor() {

    private val probes = ArrayList<Int>()
    private lateinit var methodNode: MethodNode


    override fun visitEnd() {
        super.visitEnd()
        val method = AstMethod(
            classname = classname,
            name = methodNode.name,
            params = getParams(methodNode).joinToString(separator = ","),
            returnType = getReturnType(methodNode),
            bodyChecksum = "",
            probesStartPos = if (probes.size > 0) probes[0] else 0,
            probesCount = probes.size,
            annotations = getAnnotations(methodNode)
        )
        methods.add(method)
    }

    override fun accept(methodNode: MethodNode?, methodVisitor: MethodVisitor?) {
        this.methodNode = methodNode!!
        super.accept(methodNode, methodVisitor)
    }

    override fun visitProbe(probeId: Int) {
        super.visitProbe(probeId)
        probes += probeId
    }

    override fun visitInsnWithProbe(opcode: Int, probeId: Int) {
        super.visitInsnWithProbe(opcode, probeId)
        probes += probeId
    }

    override fun visitJumpInsnWithProbe(opcode: Int, label: Label?, probeId: Int, frame: IFrame?) {
        super.visitJumpInsnWithProbe(opcode, label, probeId, frame)
        probes += probeId
    }
}

fun parseAstClass(className: String, classBytes: ByteArray): List<AstMethod> {
    val classReader = InstrSupport.classReaderFor(classBytes)
    val counter = ClassProbeCounter(className)
    classReader.accept(DrillClassProbesAdapter(counter, false), 0)
    val astMethodsWithChecksum = calculateMethodsChecksums(classBytes, className)
    val classAnnotations = counter.getAnnotations()
    return counter.methods.map {
        it.copy(
            bodyChecksum = astMethodsWithChecksum[it.classSignature()] ?: "",
            classAnnotations = classAnnotations
        )
    }
}

private fun AstMethod.classSignature() =
    "${name}/${params}/${returnType}"


private fun getReturnType(methodNode: MethodNode): String {
    val returnTypeDesc: String = Type.getReturnType(methodNode.desc).descriptor
    return Type.getType(returnTypeDesc).className
}

private fun getParams(methodNode: MethodNode): List<String> = Type
    .getArgumentTypes(methodNode.desc)
    .map { it.className }

private fun getAnnotations(methodNode: MethodNode): Map<String, List<String>>? {
    // visibleAnnotations - set in code
    // invisibleAnnotations - produced during build (e.g. Lombok-generated methods)
    val annotations = (methodNode.visibleAnnotations.orEmpty() + methodNode.invisibleAnnotations.orEmpty())
        .associateBy({ it.desc }, { getValuesOfAnnotation(it) })
    return annotations.takeIf { it.isNotEmpty() }
}

private fun getValuesOfAnnotation(annotationNode: AnnotationNode): List<String> {
    return annotationNode.values
        .orEmpty()
        .flatMap { annotationValue -> parseAnnotationValueToString(annotationValue) }
}

private fun parseAnnotationValueToString(annotationValue: Any?): List<String> {
    return when (annotationValue) {
        is List<*> -> annotationValue.flatMap { parseAnnotationValueToString(it) }
        is AnnotationNode -> getValuesOfAnnotation(annotationValue)
        else -> listOf(annotationValue.toString())
    }
}