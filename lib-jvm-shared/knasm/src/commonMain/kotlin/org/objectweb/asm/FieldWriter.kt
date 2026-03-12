// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm

/**
 * A [FieldVisitor] that generates a corresponding 'field_info' structure, as defined in the
 * Java Virtual Machine Specification (JVMS).
 *
 * @see [JVMS
 * 4.5](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.5)
 *
 * @author Eric Bruneton
 */
internal class FieldWriter(
    symbolTable: SymbolTable,
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    constantValue: Any?
) : FieldVisitor( /* latest api = */Opcodes.ASM9) {
    /** Where the constants used in this FieldWriter must be stored.  */
    private val symbolTable: SymbolTable
    // Note: fields are ordered as in the field_info structure, and those related to attributes are
    // ordered as in Section 4.7 of the JVMS.
    /**
     * The access_flags field of the field_info JVMS structure. This field can contain ASM specific
     * access flags, such as [Opcodes.ACC_DEPRECATED], which are removed when generating the
     * ClassFile structure.
     */
    private val accessFlags: Int

    /** The name_index field of the field_info JVMS structure.  */
    private val nameIndex: Int

    /** The descriptor_index field of the field_info JVMS structure.  */
    private val descriptorIndex: Int

    /**
     * The signature_index field of the Signature attribute of this field_info, or 0 if there is no
     * Signature attribute.
     */
    private var signatureIndex = 0

    /**
     * The constantvalue_index field of the ConstantValue attribute of this field_info, or 0 if there
     * is no ConstantValue attribute.
     */
    private var constantValueIndex = 0

    /**
     * The last runtime visible annotation of this field. The previous ones can be accessed with the
     * [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeVisibleAnnotation: AnnotationWriter? = null

    /**
     * The last runtime invisible annotation of this field. The previous ones can be accessed with the
     * [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeInvisibleAnnotation: AnnotationWriter? = null

    /**
     * The last runtime visible type annotation of this field. The previous ones can be accessed with
     * the [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeVisibleTypeAnnotation: AnnotationWriter? = null

    /**
     * The last runtime invisible type annotation of this field. The previous ones can be accessed
     * with the [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeInvisibleTypeAnnotation: AnnotationWriter? = null

    /**
     * The first non standard attribute of this field. The next ones can be accessed with the [ ][Attribute.nextAttribute] field. May be null.
     *
     *
     * **WARNING**: this list stores the attributes in the *reverse* order of their visit.
     * firstAttribute is actually the last attribute visited in [.visitAttribute]. The [ ][.putFieldInfo] method writes the attributes in the order defined by this list, i.e. in the
     * reverse order specified by the user.
     */
    private var firstAttribute: Attribute? = null

    // -----------------------------------------------------------------------------------------------
    // Implementation of the FieldVisitor abstract class
    // -----------------------------------------------------------------------------------------------
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        return if (visible) {
            AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation)
                .also { lastRuntimeVisibleAnnotation = it }
        } else {
            AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation)
                .also { lastRuntimeInvisibleAnnotation = it }
        }
    }

    override fun visitTypeAnnotation(
        typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean
    ): AnnotationVisitor {
        return if (visible) {
            AnnotationWriter.create(
                symbolTable, typeRef, typePath, descriptor, lastRuntimeVisibleTypeAnnotation)
                .also { lastRuntimeVisibleTypeAnnotation = it }
        } else {
            AnnotationWriter.create(
                symbolTable, typeRef, typePath, descriptor, lastRuntimeInvisibleTypeAnnotation)
                .also { lastRuntimeInvisibleTypeAnnotation = it }
        }
    }

    override fun visitAttribute(attribute: Attribute) {
        // Store the attributes in the <i>reverse</i> order of their visit by this method.
        attribute.nextAttribute = firstAttribute
        firstAttribute = attribute
    }

    override fun visitEnd() {
        // Nothing to do.
    }
    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the size of the field_info JVMS structure generated by this FieldWriter. Also adds the
     * names of the attributes of this field in the constant pool.
     *
     * @return the size in bytes of the field_info JVMS structure.
     */
    fun computeFieldInfoSize(): Int {
        // The access_flags, name_index, descriptor_index and attributes_count fields use 8 bytes.
        var size = 8
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        if (constantValueIndex != 0) {
            // ConstantValue attributes always use 8 bytes.
            symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE)
            size += 8
        }
        size += Attribute.computeAttributesSize(symbolTable, accessFlags, signatureIndex)
        size += AnnotationWriter.computeAnnotationsSize(
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation)
        if (firstAttribute != null) {
            size += firstAttribute!!.computeAttributesSize(symbolTable)
        }
        return size
    }

    /**
     * Puts the content of the field_info JVMS structure generated by this FieldWriter into the given
     * ByteVector.
     *
     * @param output where the field_info structure must be put.
     */
    fun putFieldInfo(output: ByteVector) {
        val useSyntheticAttribute: Boolean = symbolTable.getMajorVersion() < Opcodes.V1_5
        // Put the access_flags, name_index and descriptor_index fields.
        val mask = if (useSyntheticAttribute) Opcodes.ACC_SYNTHETIC else 0
        output.putShort(accessFlags and mask.inv()).putShort(nameIndex).putShort(descriptorIndex)
        // Compute and put the attributes_count field.
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        var attributesCount = 0
        if (constantValueIndex != 0) {
            ++attributesCount
        }
        if (accessFlags and Opcodes.ACC_SYNTHETIC !== 0 && useSyntheticAttribute) {
            ++attributesCount
        }
        if (signatureIndex != 0) {
            ++attributesCount
        }
        if (accessFlags and Opcodes.ACC_DEPRECATED !== 0) {
            ++attributesCount
        }
        if (lastRuntimeVisibleAnnotation != null) {
            ++attributesCount
        }
        if (lastRuntimeInvisibleAnnotation != null) {
            ++attributesCount
        }
        if (lastRuntimeVisibleTypeAnnotation != null) {
            ++attributesCount
        }
        if (lastRuntimeInvisibleTypeAnnotation != null) {
            ++attributesCount
        }
        if (firstAttribute != null) {
            attributesCount += firstAttribute!!.attributeCount
        }
        output.putShort(attributesCount)
        // Put the field_info attributes.
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        if (constantValueIndex != 0) {
            output
                .putShort(symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE))
                .putInt(2)
                .putShort(constantValueIndex)
        }
        Attribute.putAttributes(symbolTable, accessFlags, signatureIndex, output)
        AnnotationWriter.putAnnotations(
            symbolTable,
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation,
            output)
        if (firstAttribute != null) {
            firstAttribute!!.putAttributes(symbolTable, output)
        }
    }

    /**
     * Collects the attributes of this field into the given set of attribute prototypes.
     *
     * @param attributePrototypes a set of attribute prototypes.
     */
    fun collectAttributePrototypes(attributePrototypes: Attribute.Set) {
        attributePrototypes.addAttributes(firstAttribute)
    }
    // -----------------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------------
    /**
     * Constructs a new [FieldWriter].
     *
     * @param symbolTable where the constants used in this FieldWriter must be stored.
     * @param access the field's access flags (see [Opcodes]).
     * @param name the field's name.
     * @param descriptor the field's descriptor (see [Type]).
     * @param signature the field's signature. May be null.
     * @param constantValue the field's constant value. May be null.
     */
    init {
        this.symbolTable = symbolTable
        accessFlags = access
        nameIndex = symbolTable.addConstantUtf8(name)
        descriptorIndex = symbolTable.addConstantUtf8(descriptor)
        if (signature != null) {
            signatureIndex = symbolTable.addConstantUtf8(signature)
        }
        if (constantValue != null) {
            constantValueIndex = symbolTable.addConstant(constantValue).index
        }
    }
}
