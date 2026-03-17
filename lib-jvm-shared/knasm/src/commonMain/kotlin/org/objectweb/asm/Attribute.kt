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
 * A non standard class, field, method or Code attribute, as defined in the Java Virtual Machine
 * Specification (JVMS).
 *
 * @see [JVMS
 * 4.7](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7)
 *
 * @see [JVMS
 * 4.7.3](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.3)
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
open class Attribute
/**
 * Constructs a new empty attribute.
 *
 * @param type the type of the attribute.
 */(
    /** The type of this attribute, also called its name in the JVMS.  */
    val type: String?
) {
    /**
     * The raw content of this attribute, only used for unknown attributes (see [.isUnknown]).
     * The 6 header bytes of the attribute (attribute_name_index and attribute_length) are *not*
     * included.
     */
    private lateinit var content: ByteArray

    /**
     * The next attribute in this attribute list (Attribute instances can be linked via this field to
     * store a list of class, field, method or Code attributes). May be null.
     */
    var nextAttribute: Attribute? = null

    /**
     * Returns true if this type of attribute is unknown. This means that the attribute
     * content can't be parsed to extract constant pool references, labels, etc. Instead, the
     * attribute content is read as an opaque byte array, and written back as is. This can lead to
     * invalid attributes, if the content actually contains constant pool references, labels, or other
     * symbolic references that need to be updated when there are changes to the constant pool, the
     * method bytecode, etc. The default implementation of this method always returns true.
     *
     * @return true if this type of attribute is unknown.
     */
    val isUnknown: Boolean
        get() = true

    /**
     * Returns true if this type of attribute is a Code attribute.
     *
     * @return true if this type of attribute is a Code attribute.
     */
    val isCodeAttribute: Boolean
        get() = false

    /**
     * Returns the labels corresponding to this attribute.
     *
     * @return the labels corresponding to this attribute, or null if this attribute is not
     * a Code attribute that contains labels.
     */
    protected val labels: Array<org.objectweb.asm.Label?>
        protected get() = arrayOfNulls<Label>(0)

    /**
     * Reads a [.type] attribute. This method must return a *new* [Attribute] object,
     * of type [.type], corresponding to the 'length' bytes starting at 'offset', in the given
     * ClassReader.
     *
     * @param classReader the class that contains the attribute to be read.
     * @param offset index of the first byte of the attribute's content in [ClassReader]. The 6
     * attribute header bytes (attribute_name_index and attribute_length) are not taken into
     * account here.
     * @param length the length of the attribute's content (excluding the 6 attribute header bytes).
     * @param charBuffer the buffer to be used to call the ClassReader methods requiring a
     * 'charBuffer' parameter.
     * @param codeAttributeOffset index of the first byte of content of the enclosing Code attribute
     * in [ClassReader], or -1 if the attribute to be read is not a Code attribute. The 6
     * attribute header bytes (attribute_name_index and attribute_length) are not taken into
     * account here.
     * @param labels the labels of the method's code, or null if the attribute to be read
     * is not a Code attribute.
     * @return a *new* [Attribute] object corresponding to the specified bytes.
     */
    fun read(
        classReader: ClassReader,
        offset: Int,
        length: Int,
        charBuffer: CharArray?,
        codeAttributeOffset: Int,
        labels: Array<Label?>?
    ): Attribute {
        val attribute = Attribute(type)
        attribute.content = ByteArray(length)
        System.arraycopy(classReader.classFileBuffer, offset, attribute.content, 0, length)
        return attribute
    }

    /**
     * Returns the byte array form of the content of this attribute. The 6 header bytes
     * (attribute_name_index and attribute_length) must *not* be added in the returned
     * ByteVector.
     *
     * @param classWriter the class to which this attribute must be added. This parameter can be used
     * to add the items that corresponds to this attribute to the constant pool of this class.
     * @param code the bytecode of the method corresponding to this Code attribute, or null
     * if this attribute is not a Code attribute. Corresponds to the 'code' field of the Code
     * attribute.
     * @param codeLength the length of the bytecode of the method corresponding to this code
     * attribute, or 0 if this attribute is not a Code attribute. Corresponds to the 'code_length'
     * field of the Code attribute.
     * @param maxStack the maximum stack size of the method corresponding to this Code attribute, or
     * -1 if this attribute is not a Code attribute.
     * @param maxLocals the maximum number of local variables of the method corresponding to this code
     * attribute, or -1 if this attribute is not a Code attribute.
     * @return the byte array form of this attribute.
     */
    protected fun write(
        classWriter: ClassWriter?,
        code: ByteArray?,
        codeLength: Int,
        maxStack: Int,
        maxLocals: Int
    ): ByteVector {
        return ByteVector(content)
    }

    /**
     * Returns the number of attributes of the attribute list that begins with this attribute.
     *
     * @return the number of attributes of the attribute list that begins with this attribute.
     */
    val attributeCount: Int
        get() {
            var count = 0
            var attribute: Attribute? = this
            while (attribute != null) {
                count += 1
                attribute = attribute.nextAttribute
            }
            return count
        }

    /**
     * Returns the total size in bytes of all the attributes in the attribute list that begins with
     * this attribute. This size includes the 6 header bytes (attribute_name_index and
     * attribute_length) per attribute. Also adds the attribute type names to the constant pool.
     *
     * @param symbolTable where the constants used in the attributes must be stored.
     * @return the size of all the attributes in this attribute list. This size includes the size of
     * the attribute headers.
     */
    fun computeAttributesSize(symbolTable: SymbolTable): Int {
        val code: ByteArray? = null
        val codeLength = 0
        val maxStack = -1
        val maxLocals = -1
        return computeAttributesSize(symbolTable, code, codeLength, maxStack, maxLocals)
    }

    /**
     * Returns the total size in bytes of all the attributes in the attribute list that begins with
     * this attribute. This size includes the 6 header bytes (attribute_name_index and
     * attribute_length) per attribute. Also adds the attribute type names to the constant pool.
     *
     * @param symbolTable where the constants used in the attributes must be stored.
     * @param code the bytecode of the method corresponding to these Code attributes, or null if they are not Code attributes. Corresponds to the 'code' field of the Code
     * attribute.
     * @param codeLength the length of the bytecode of the method corresponding to these code
     * attributes, or 0 if they are not Code attributes. Corresponds to the 'code_length' field of
     * the Code attribute.
     * @param maxStack the maximum stack size of the method corresponding to these Code attributes, or
     * -1 if they are not Code attributes.
     * @param maxLocals the maximum number of local variables of the method corresponding to these
     * Code attributes, or -1 if they are not Code attribute.
     * @return the size of all the attributes in this attribute list. This size includes the size of
     * the attribute headers.
     */
    fun computeAttributesSize(
        symbolTable: SymbolTable,
        code: ByteArray?,
        codeLength: Int,
        maxStack: Int,
        maxLocals: Int
    ): Int {
        val classWriter: ClassWriter = symbolTable.classWriter
        var size = 0
        var attribute: Attribute? = this
        while (attribute != null) {
            symbolTable.addConstantUtf8(attribute.type)
            size += 6 + attribute.write(classWriter, code, codeLength, maxStack, maxLocals).length
            attribute = attribute.nextAttribute
        }
        return size
    }

    /**
     * Puts all the attributes of the attribute list that begins with this attribute, in the given
     * byte vector. This includes the 6 header bytes (attribute_name_index and attribute_length) per
     * attribute.
     *
     * @param symbolTable where the constants used in the attributes must be stored.
     * @param output where the attributes must be written.
     */
    fun putAttributes(symbolTable: SymbolTable, output: ByteVector) {
        val code: ByteArray? = null
        val codeLength = 0
        val maxStack = -1
        val maxLocals = -1
        putAttributes(symbolTable, code, codeLength, maxStack, maxLocals, output)
    }

    /**
     * Puts all the attributes of the attribute list that begins with this attribute, in the given
     * byte vector. This includes the 6 header bytes (attribute_name_index and attribute_length) per
     * attribute.
     *
     * @param symbolTable where the constants used in the attributes must be stored.
     * @param code the bytecode of the method corresponding to these Code attributes, or null if they are not Code attributes. Corresponds to the 'code' field of the Code
     * attribute.
     * @param codeLength the length of the bytecode of the method corresponding to these code
     * attributes, or 0 if they are not Code attributes. Corresponds to the 'code_length' field of
     * the Code attribute.
     * @param maxStack the maximum stack size of the method corresponding to these Code attributes, or
     * -1 if they are not Code attributes.
     * @param maxLocals the maximum number of local variables of the method corresponding to these
     * Code attributes, or -1 if they are not Code attribute.
     * @param output where the attributes must be written.
     */
    fun putAttributes(
        symbolTable: SymbolTable,
        code: ByteArray?,
        codeLength: Int,
        maxStack: Int,
        maxLocals: Int,
        output: ByteVector
    ) {
        val classWriter: ClassWriter = symbolTable.classWriter
        var attribute: Attribute? = this
        while (attribute != null) {
            val attributeContent: ByteVector = attribute.write(classWriter, code, codeLength, maxStack, maxLocals)
            // Put attribute_name_index and attribute_length.
            output.putShort(symbolTable.addConstantUtf8(attribute.type)).putInt(attributeContent.length)
            output.putByteArray(attributeContent.data, 0, attributeContent.length)
            attribute = attribute.nextAttribute
        }
    }

    /** A set of attribute prototypes (attributes with the same type are considered equal).  */
    class Set {
        private var size = 0
        private var data = arrayOfNulls<Attribute>(SIZE_INCREMENT)
        fun addAttributes(attributeList: Attribute?) {
            var attribute = attributeList
            while (attribute != null) {
                if (!contains(attribute)) {
                    add(attribute)
                }
                attribute = attribute.nextAttribute
            }
        }

        fun toArray(): Array<Attribute?> {
            val result = arrayOfNulls<Attribute>(size)
            System.arraycopy(data, 0, result, 0, size)
            return result
        }

        private operator fun contains(attribute: Attribute): Boolean {
            for (i in 0 until size) {
                if (data[i]!!.type == attribute.type) {
                    return true
                }
            }
            return false
        }

        private fun add(attribute: Attribute) {
            if (size >= data.size) {
                val newData = arrayOfNulls<Attribute>(data.size + SIZE_INCREMENT)
                System.arraycopy(data, 0, newData, 0, size)
                data = newData
            }
            data[size++] = attribute
        }

        companion object {
            private const val SIZE_INCREMENT = 6
        }
    }

    companion object {
        /**
         * Returns the total size in bytes of all the attributes that correspond to the given field,
         * method or class access flags and signature. This size includes the 6 header bytes
         * (attribute_name_index and attribute_length) per attribute. Also adds the attribute type names
         * to the constant pool.
         *
         * @param symbolTable where the constants used in the attributes must be stored.
         * @param accessFlags some field, method or class access flags.
         * @param signatureIndex the constant pool index of a field, method of class signature.
         * @return the size of all the attributes in bytes. This size includes the size of the attribute
         * headers.
         */
        fun computeAttributesSize(
            symbolTable: SymbolTable, accessFlags: Int, signatureIndex: Int
        ): Int {
            var size = 0
            // Before Java 1.5, synthetic fields are represented with a Synthetic attribute.
            if (accessFlags and Opcodes.ACC_SYNTHETIC !== 0
                && symbolTable.getMajorVersion() < Opcodes.V1_5
            ) {
                // Synthetic attributes always use 6 bytes.
                symbolTable.addConstantUtf8(Constants.SYNTHETIC)
                size += 6
            }
            if (signatureIndex != 0) {
                // Signature attributes always use 8 bytes.
                symbolTable.addConstantUtf8(Constants.SIGNATURE)
                size += 8
            }
            // ACC_DEPRECATED is ASM specific, the ClassFile format uses a Deprecated attribute instead.
            if (accessFlags and Opcodes.ACC_DEPRECATED !== 0) {
                // Deprecated attributes always use 6 bytes.
                symbolTable.addConstantUtf8(Constants.DEPRECATED)
                size += 6
            }
            return size
        }

        /**
         * Puts all the attributes that correspond to the given field, method or class access flags and
         * signature, in the given byte vector. This includes the 6 header bytes (attribute_name_index and
         * attribute_length) per attribute.
         *
         * @param symbolTable where the constants used in the attributes must be stored.
         * @param accessFlags some field, method or class access flags.
         * @param signatureIndex the constant pool index of a field, method of class signature.
         * @param output where the attributes must be written.
         */
        fun putAttributes(
            symbolTable: SymbolTable,
            accessFlags: Int,
            signatureIndex: Int,
            output: ByteVector
        ) {
            // Before Java 1.5, synthetic fields are represented with a Synthetic attribute.
            if (accessFlags and Opcodes.ACC_SYNTHETIC !== 0
                && symbolTable.getMajorVersion() < Opcodes.V1_5
            ) {
                output.putShort(symbolTable.addConstantUtf8(Constants.SYNTHETIC)).putInt(0)
            }
            if (signatureIndex != 0) {
                output
                    .putShort(symbolTable.addConstantUtf8(Constants.SIGNATURE))
                    .putInt(2)
                    .putShort(signatureIndex)
            }
            if (accessFlags and Opcodes.ACC_DEPRECATED !== 0) {
                output.putShort(symbolTable.addConstantUtf8(Constants.DEPRECATED)).putInt(0)
            }
        }
    }
}
