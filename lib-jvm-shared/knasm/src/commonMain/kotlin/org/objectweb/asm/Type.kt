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

import kotlin.math.*


/**
 * A Java field or method type. This class can be used to make it easier to manipulate type and
 * method descriptors.
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
class Type
/**
 * Constructs a reference type.
 *
 * @param sort the sort of this type, see [.sort].
 * @param valueBuffer a buffer containing the value of this field or method type.
 * @param valueBegin the beginning index, inclusive, of the value of this field or method type in
 * valueBuffer.
 * @param valueEnd the end index, exclusive, of the value of this field or method type in
 * valueBuffer.
 */ private constructor(
    /**
     * The sort of this type. Either [.VOID], [.BOOLEAN], [.CHAR], [.BYTE],
     * [.SHORT], [.INT], [.FLOAT], [.LONG], [.DOUBLE], [.ARRAY],
     * [.OBJECT], [.METHOD] or [.INTERNAL].
     */
    private val sort: Int,
    /**
     * A buffer containing the value of this field or method type. This value is an internal name for
     * [.OBJECT] and [.INTERNAL] types, and a field or method descriptor in the other
     * cases.
     *
     *
     * For [.OBJECT] types, this field also contains the descriptor: the characters in
     * [[.valueBegin],[.valueEnd]) contain the internal name, and those in [[ ][.valueBegin] - 1, [.valueEnd] + 1) contain the descriptor.
     */
    private val valueBuffer: String,
    /**
     * The beginning index, inclusive, of the value of this Java field or method type in [ ][.valueBuffer]. This value is an internal name for [.OBJECT] and [.INTERNAL] types,
     * and a field or method descriptor in the other cases.
     */
    private val valueBegin: Int,
    /**
     * The end index, exclusive, of the value of this Java field or method type in [ ][.valueBuffer]. This value is an internal name for [.OBJECT] and [.INTERNAL] types,
     * and a field or method descriptor in the other cases.
     */
    private val valueEnd: Int
) {
    // -----------------------------------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the type of the elements of this array type. This method should only be used for an
     * array type.
     *
     * @return Returns the type of the elements of this array type.
     */
    val elementType: Type
        get() {
            val numDimensions = dimensions
            return getTypeInternal(valueBuffer, valueBegin + numDimensions, valueEnd)
        }

    /**
     * Returns the argument types of methods of this type. This method should only be used for method
     * types.
     *
     * @return the argument types of methods of this type.
     */
    val argumentTypes: Array<Type?>
        get() = getArgumentTypes(descriptor)

    /**
     * Returns the return type of methods of this type. This method should only be used for method
     * types.
     *
     * @return the return type of methods of this type.
     */
    val returnType: Type
        get() = getReturnType(descriptor)
    // -----------------------------------------------------------------------------------------------
    // Methods to get class names, internal names or descriptors.
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the binary name of the class corresponding to this type. This method must not be used
     * on method types.
     *
     * @return the binary name of the class corresponding to this type.
     */
    val className: String
        get() = when (sort) {
            VOID -> "void"
            BOOLEAN -> "boolean"
            CHAR -> "char"
            BYTE -> "byte"
            SHORT -> "short"
            INT -> "int"
            FLOAT -> "float"
            LONG -> "long"
            DOUBLE -> "double"
            ARRAY -> {
                val stringBuilder: StringBuilder = StringBuilder(elementType.className)
                var i = dimensions
                while (i > 0) {
                    stringBuilder.append("[]")
                    --i
                }
                stringBuilder.toString()
            }
            OBJECT, INTERNAL -> valueBuffer.substring(
                valueBegin, valueEnd).replace('/', '.')
            else -> throw AssertionError()
        }

    /**
     * Returns the internal name of the class corresponding to this object or array type. The internal
     * name of a class is its fully qualified name (as returned by Class.getName(), where '.' are
     * replaced by '/'). This method should only be used for an object or array type.
     *
     * @return the internal name of the class corresponding to this object type.
     */
    val internalName: String
        get() = valueBuffer.substring(valueBegin, valueEnd)

    /**
     * Returns the descriptor corresponding to this type.
     *
     * @return the descriptor corresponding to this type.
     */
    val descriptor: String
        get() = if (sort == OBJECT) {
            valueBuffer.substring(valueBegin - 1, valueEnd + 1)
        } else if (sort == INTERNAL) {
            'L'.toString() + valueBuffer.substring(valueBegin, valueEnd) + ';'
        } else {
            valueBuffer.substring(valueBegin, valueEnd)
        }

    /**
     * Appends the descriptor corresponding to this type to the given string buffer.
     *
     * @param stringBuilder the string builder to which the descriptor must be appended.
     */
    private fun appendDescriptor(stringBuilder: StringBuilder) {
        if (sort == OBJECT) {
            stringBuilder.append(valueBuffer, valueBegin - 1, valueEnd + 1)
        } else if (sort == INTERNAL) {
            stringBuilder.append('L').append(valueBuffer, valueBegin, valueEnd).append(';')
        } else {
            stringBuilder.append(valueBuffer, valueBegin, valueEnd)
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to get the sort, dimension, size, and opcodes corresponding to a Type or descriptor.
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the sort of this type.
     *
     * @return [.VOID], [.BOOLEAN], [.CHAR], [.BYTE], [.SHORT], [     ][.INT], [.FLOAT], [.LONG], [.DOUBLE], [.ARRAY], [.OBJECT] or
     * [.METHOD].
     */
    fun getSort(): Int {
        return if (sort == INTERNAL) OBJECT else sort
    }

    /**
     * Returns the number of dimensions of this array type. This method should only be used for an
     * array type.
     *
     * @return the number of dimensions of this array type.
     */
    val dimensions: Int
        get() {
            var numDimensions = 1
            while (valueBuffer[valueBegin + numDimensions] == '[') {
                numDimensions++
            }
            return numDimensions
        }

    /**
     * Returns the size of values of this type. This method must not be used for method types.
     *
     * @return the size of values of this type, i.e., 2 for `long` and `double`, 0 for
     * `void` and 1 otherwise.
     */
    val size: Int
        get() = when (sort) {
            VOID -> 0
            BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT, ARRAY, OBJECT, INTERNAL -> 1
            LONG, DOUBLE -> 2
            else -> throw AssertionError()
        }

    /**
     * Returns the size of the arguments and of the return value of methods of this type. This method
     * should only be used for method types.
     *
     * @return the size of the arguments of the method (plus one for the implicit this argument),
     * argumentsSize, and the size of its return value, returnSize, packed into a single int i =
     * `(argumentsSize &lt;&lt; 2) | returnSize` (argumentsSize is therefore equal to `i &gt;&gt; 2`, and returnSize to `i &amp; 0x03`).
     */
    val argumentsAndReturnSizes: Int
        get() = getArgumentsAndReturnSizes(descriptor)

    /**
     * Returns a JVM instruction opcode adapted to this [Type]. This method must not be used for
     * method types.
     *
     * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD,
     * IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and
     * IRETURN.
     * @return an opcode that is similar to the given opcode, but adapted to this [Type]. For
     * example, if this type is `float` and `opcode` is IRETURN, this method returns
     * FRETURN.
     */
    fun getOpcode(opcode: Int): Int {
        return if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
            when (sort) {
                BOOLEAN, BYTE -> opcode + (Opcodes.BALOAD - Opcodes.IALOAD)
                CHAR -> opcode + (Opcodes.CALOAD - Opcodes.IALOAD)
                SHORT -> opcode + (Opcodes.SALOAD - Opcodes.IALOAD)
                INT -> opcode
                FLOAT -> opcode + (Opcodes.FALOAD - Opcodes.IALOAD)
                LONG -> opcode + (Opcodes.LALOAD - Opcodes.IALOAD)
                DOUBLE -> opcode + (Opcodes.DALOAD - Opcodes.IALOAD)
                ARRAY, OBJECT, INTERNAL -> opcode + (Opcodes.AALOAD - Opcodes.IALOAD)
                METHOD, VOID -> throw UnsupportedOperationException()
                else -> throw AssertionError()
            }
        } else {
            when (sort) {
                VOID -> {
                    if (opcode != Opcodes.IRETURN) {
                        throw UnsupportedOperationException()
                    }
                    Opcodes.RETURN
                }
                BOOLEAN, BYTE, CHAR, SHORT, INT -> opcode
                FLOAT -> opcode + (Opcodes.FRETURN - Opcodes.IRETURN)
                LONG -> opcode + (Opcodes.LRETURN - Opcodes.IRETURN)
                DOUBLE -> opcode + (Opcodes.DRETURN - Opcodes.IRETURN)
                ARRAY, OBJECT, INTERNAL -> {
                    if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
                        throw UnsupportedOperationException()
                    }
                    opcode + (Opcodes.ARETURN - Opcodes.IRETURN)
                }
                METHOD -> throw UnsupportedOperationException()
                else -> throw AssertionError()
            }
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Equals, hashCode and toString.
    // -----------------------------------------------------------------------------------------------
    /**
     * Tests if the given object is equal to this type.
     *
     * @param object the object to be compared to this type.
     * @return true if the given object is equal to this type.
     */
    override fun equals(`object`: Any?): Boolean {
        if (this === `object`) {
            return true
        }
        if (`object` !is Type) {
            return false
        }
        val other = `object`
        if ((if (sort == INTERNAL) OBJECT else sort) != (if (other.sort == INTERNAL) OBJECT else other.sort)) {
            return false
        }
        val begin = valueBegin
        val end = valueEnd
        val otherBegin = other.valueBegin
        val otherEnd = other.valueEnd
        // Compare the values.
        if (end - begin != otherEnd - otherBegin) {
            return false
        }
        var i = begin
        var j = otherBegin
        while (i < end) {
            if (valueBuffer[i] != other.valueBuffer[j]) {
                return false
            }
            i++
            j++
        }
        return true
    }

    /**
     * Returns a hash code value for this type.
     *
     * @return a hash code value for this type.
     */
    override fun hashCode(): Int {
        var hashCode = 13 * if (sort == INTERNAL) OBJECT else sort
        if (sort >= ARRAY) {
            var i = valueBegin
            val end = valueEnd
            while (i < end) {
                hashCode = 17 * (hashCode + valueBuffer[i].toInt())
                i++
            }
        }
        return hashCode
    }

    /**
     * Returns a string representation of this type.
     *
     * @return the descriptor of this type.
     */
    override fun toString(): String {
        return descriptor
    }

    companion object {
        /** The sort of the `void` type. See [.getSort].  */
        const val VOID = 0

        /** The sort of the `boolean` type. See [.getSort].  */
        const val BOOLEAN = 1

        /** The sort of the `char` type. See [.getSort].  */
        const val CHAR = 2

        /** The sort of the `byte` type. See [.getSort].  */
        const val BYTE = 3

        /** The sort of the `short` type. See [.getSort].  */
        const val SHORT = 4

        /** The sort of the `int` type. See [.getSort].  */
        const val INT = 5

        /** The sort of the `float` type. See [.getSort].  */
        const val FLOAT = 6

        /** The sort of the `long` type. See [.getSort].  */
        const val LONG = 7

        /** The sort of the `double` type. See [.getSort].  */
        const val DOUBLE = 8

        /** The sort of array reference types. See [.getSort].  */
        const val ARRAY = 9

        /** The sort of object reference types. See [.getSort].  */
        const val OBJECT = 10

        /** The sort of method types. See [.getSort].  */
        const val METHOD = 11

        /** The (private) sort of object reference types represented with an internal name.  */
        private const val INTERNAL = 12

        /** The descriptors of the primitive types.  */
        private const val PRIMITIVE_DESCRIPTORS = "VZCBSIFJD"

        /** The `void` type.  */
        val VOID_TYPE = Type(VOID, PRIMITIVE_DESCRIPTORS, VOID, VOID + 1)

        /** The `boolean` type.  */
        val BOOLEAN_TYPE = Type(BOOLEAN, PRIMITIVE_DESCRIPTORS, BOOLEAN, BOOLEAN + 1)

        /** The `char` type.  */
        val CHAR_TYPE = Type(CHAR, PRIMITIVE_DESCRIPTORS, CHAR, CHAR + 1)

        /** The `byte` type.  */
        val BYTE_TYPE = Type(BYTE, PRIMITIVE_DESCRIPTORS, BYTE, BYTE + 1)

        /** The `short` type.  */
        val SHORT_TYPE = Type(SHORT, PRIMITIVE_DESCRIPTORS, SHORT, SHORT + 1)

        /** The `int` type.  */
        val INT_TYPE = Type(INT, PRIMITIVE_DESCRIPTORS, INT, INT + 1)

        /** The `float` type.  */
        val FLOAT_TYPE = Type(FLOAT, PRIMITIVE_DESCRIPTORS, FLOAT, FLOAT + 1)

        /** The `long` type.  */
        val LONG_TYPE = Type(LONG, PRIMITIVE_DESCRIPTORS, LONG, LONG + 1)

        /** The `double` type.  */
        val DOUBLE_TYPE = Type(DOUBLE, PRIMITIVE_DESCRIPTORS, DOUBLE, DOUBLE + 1)
        // -----------------------------------------------------------------------------------------------
        // Methods to get Type(s) from a descriptor, a reflected Method or Constructor, other types, etc.
        // -----------------------------------------------------------------------------------------------
        /**
         * Returns the [Type] corresponding to the given type descriptor.
         *
         * @param typeDescriptor a field or method type descriptor.
         * @return the [Type] corresponding to the given type descriptor.
         */
        fun getType(typeDescriptor: String?): Type {
            return getTypeInternal(typeDescriptor, 0, typeDescriptor!!.length)
        }

        /**
         * Returns the [Type] corresponding to the given class.
         *
         * @param clazz a class.
         * @return the [Type] corresponding to the given class.
         */
//        fun getType(clazz: java.lang.Class<*>): Type {
//            return if (clazz.isPrimitive()) {
//                if (clazz == java.lang.Integer.TYPE) {
//                    INT_TYPE
//                } else if (clazz == java.lang.Void.TYPE) {
//                    VOID_TYPE
//                } else if (clazz == java.lang.Boolean.TYPE) {
//                    BOOLEAN_TYPE
//                } else if (clazz == java.lang.Byte.TYPE) {
//                    BYTE_TYPE
//                } else if (clazz == java.lang.Character.TYPE) {
//                    CHAR_TYPE
//                } else if (clazz == java.lang.Short.TYPE) {
//                    SHORT_TYPE
//                } else if (clazz == java.lang.Double.TYPE) {
//                    DOUBLE_TYPE
//                } else if (clazz == java.lang.Float.TYPE) {
//                    FLOAT_TYPE
//                } else if (clazz == java.lang.Long.TYPE) {
//                    LONG_TYPE
//                } else {
//                    throw AssertionError()
//                }
//            } else {
//                getType(getDescriptor(clazz))
//            }
//        }

        /**
         * Returns the method [Type] corresponding to the given constructor.
         *
         * @param constructor a [Constructor] object.
         * @return the method [Type] corresponding to the given constructor.
         */
//        fun getType(constructor: Constructor<*>): Type {
//            return getType(getConstructorDescriptor(constructor))
//        }

        /**
         * Returns the method [Type] corresponding to the given method.
         *
         * @param method a [Method] object.
         * @return the method [Type] corresponding to the given method.
         */
//        fun getType(method: Method): Type {
//            return getType(getMethodDescriptor(method))
//        }

        /**
         * Returns the [Type] corresponding to the given internal name.
         *
         * @param internalName an internal name.
         * @return the [Type] corresponding to the given internal name.
         */
        fun getObjectType(internalName: String?): Type {
            return Type(
                if (internalName!![0] == '[') ARRAY else INTERNAL, internalName, 0, internalName.length)
        }

        /**
         * Returns the [Type] corresponding to the given method descriptor. Equivalent to `
         * Type.getType(methodDescriptor)`.
         *
         * @param methodDescriptor a method descriptor.
         * @return the [Type] corresponding to the given method descriptor.
         */
        fun getMethodType(methodDescriptor: String?): Type {
            return Type(METHOD, methodDescriptor!!, 0, methodDescriptor.length)
        }

        /**
         * Returns the method [Type] corresponding to the given argument and return types.
         *
         * @param returnType the return type of the method.
         * @param argumentTypes the argument types of the method.
         * @return the method [Type] corresponding to the given argument and return types.
         */
        fun getMethodType(returnType: Type, vararg argumentTypes: Type?): Type {
            return getType(getMethodDescriptor(returnType, *argumentTypes))
        }

        /**
         * Returns the [Type] values corresponding to the argument types of the given method
         * descriptor.
         *
         * @param methodDescriptor a method descriptor.
         * @return the [Type] values corresponding to the argument types of the given method
         * descriptor.
         */
        fun getArgumentTypes(methodDescriptor: String?): Array<Type?> {
            // First step: compute the number of argument types in methodDescriptor.
            var numArgumentTypes = 0
            // Skip the first character, which is always a '('.
            var currentOffset = 1
            // Parse the argument types, one at a each loop iteration.
            while (methodDescriptor!![currentOffset] != ')') {
                while (methodDescriptor[currentOffset] == '[') {
                    currentOffset++
                }
                if (methodDescriptor[currentOffset++] == 'L') {
                    // Skip the argument descriptor content.
                    val semiColumnOffset = methodDescriptor.indexOf(';', currentOffset)
                    currentOffset = max(currentOffset, semiColumnOffset + 1)
                }
                ++numArgumentTypes
            }

            // Second step: create a Type instance for each argument type.
            val argumentTypes = arrayOfNulls<Type>(numArgumentTypes)
            // Skip the first character, which is always a '('.
            currentOffset = 1
            // Parse and create the argument types, one at each loop iteration.
            var currentArgumentTypeIndex = 0
            while (methodDescriptor[currentOffset] != ')') {
                val currentArgumentTypeOffset = currentOffset
                while (methodDescriptor[currentOffset] == '[') {
                    currentOffset++
                }
                if (methodDescriptor[currentOffset++] == 'L') {
                    // Skip the argument descriptor content.
                    val semiColumnOffset = methodDescriptor.indexOf(';', currentOffset)
                    currentOffset = max(currentOffset, semiColumnOffset + 1)
                }
                argumentTypes[currentArgumentTypeIndex++] =
                    getTypeInternal(methodDescriptor, currentArgumentTypeOffset, currentOffset)
            }
            return argumentTypes
        }

        /**
         * Returns the [Type] values corresponding to the argument types of the given method.
         *
         * @param method a method.
         * @return the [Type] values corresponding to the argument types of the given method.
         */
//        fun getArgumentTypes(method: java.lang.reflect.Method): Array<Type?> {
//            val classes: Array<java.lang.Class<*>> = method.getParameterTypes()
//            val types = arrayOfNulls<Type>(classes.size)
//            for (i in classes.indices.reversed()) {
//                types[i] = getType(classes[i])
//            }
//            return types
//        }

        /**
         * Returns the [Type] corresponding to the return type of the given method descriptor.
         *
         * @param methodDescriptor a method descriptor.
         * @return the [Type] corresponding to the return type of the given method descriptor.
         */
        fun getReturnType(methodDescriptor: String): Type {
            return getTypeInternal(
                methodDescriptor, getReturnTypeOffset(methodDescriptor), methodDescriptor.length)
        }

        /**
         * Returns the [Type] corresponding to the return type of the given method.
         *
         * @param method a method.
         * @return the [Type] corresponding to the return type of the given method.
         */
//        fun getReturnType(method: java.lang.reflect.Method): Type {
//            return getType(method.getReturnType())
//        }

        /**
         * Returns the start index of the return type of the given method descriptor.
         *
         * @param methodDescriptor a method descriptor.
         * @return the start index of the return type of the given method descriptor.
         */
        fun getReturnTypeOffset(methodDescriptor: String): Int {
            // Skip the first character, which is always a '('.
            var currentOffset = 1
            // Skip the argument types, one at a each loop iteration.
            while (methodDescriptor[currentOffset] != ')') {
                while (methodDescriptor[currentOffset] == '[') {
                    currentOffset++
                }
                if (methodDescriptor[currentOffset++] == 'L') {
                    // Skip the argument descriptor content.
                    val semiColumnOffset = methodDescriptor.indexOf(';', currentOffset)
                    currentOffset = max(currentOffset, semiColumnOffset + 1)
                }
            }
            return currentOffset + 1
        }

        /**
         * Returns the [Type] corresponding to the given field or method descriptor.
         *
         * @param descriptorBuffer a buffer containing the field or method descriptor.
         * @param descriptorBegin the beginning index, inclusive, of the field or method descriptor in
         * descriptorBuffer.
         * @param descriptorEnd the end index, exclusive, of the field or method descriptor in
         * descriptorBuffer.
         * @return the [Type] corresponding to the given type descriptor.
         */
        private fun getTypeInternal(
            descriptorBuffer: String?, descriptorBegin: Int, descriptorEnd: Int
        ): Type {
            return when (descriptorBuffer!![descriptorBegin]) {
                'V' -> VOID_TYPE
                'Z' -> BOOLEAN_TYPE
                'C' -> CHAR_TYPE
                'B' -> BYTE_TYPE
                'S' -> SHORT_TYPE
                'I' -> INT_TYPE
                'F' -> FLOAT_TYPE
                'J' -> LONG_TYPE
                'D' -> DOUBLE_TYPE
                '[' -> Type(ARRAY,
                    descriptorBuffer,
                    descriptorBegin,
                    descriptorEnd)
                'L' -> Type(OBJECT,
                    descriptorBuffer,
                    descriptorBegin + 1,
                    descriptorEnd - 1)
                '(' -> Type(METHOD,
                    descriptorBuffer,
                    descriptorBegin,
                    descriptorEnd)
                else -> throw IllegalArgumentException("Invalid descriptor: $descriptorBuffer")
            }
        }

        /**
         * Returns the internal name of the given class. The internal name of a class is its fully
         * qualified name, as returned by Class.getName(), where '.' are replaced by '/'.
         *
         * @param clazz an object or array class.
         * @return the internal name of the given class.
         */
//        fun getInternalName(clazz: java.lang.Class<*>): String {
//            return clazz.getName().replace('.', '/')
//        }

        /**
         * Returns the descriptor corresponding to the given class.
         *
         * @param clazz an object class, a primitive class or an array class.
         * @return the descriptor corresponding to the given class.
         */
//        fun getDescriptor(clazz: java.lang.Class<*>): String {
//            val stringBuilder: java.lang.StringBuilder = java.lang.StringBuilder()
//            appendDescriptor(clazz, stringBuilder)
//            return stringBuilder.toString()
//        }

        /**
         * Returns the descriptor corresponding to the given constructor.
         *
         * @param constructor a [Constructor] object.
         * @return the descriptor of the given constructor.
         */
//        fun getConstructorDescriptor(constructor: java.lang.reflect.Constructor<*>): String {
//            val stringBuilder: java.lang.StringBuilder = java.lang.StringBuilder()
//            stringBuilder.append('(')
//            val parameters: Array<java.lang.Class<*>> = constructor.getParameterTypes()
//            for (parameter in parameters) {
//                appendDescriptor(parameter, stringBuilder)
//            }
//            return stringBuilder.append(")V").toString()
//        }

        /**
         * Returns the descriptor corresponding to the given argument and return types.
         *
         * @param returnType the return type of the method.
         * @param argumentTypes the argument types of the method.
         * @return the descriptor corresponding to the given argument and return types.
         */
        fun getMethodDescriptor(returnType: Type, vararg argumentTypes: Type?): String {
            val stringBuilder = StringBuilder()
            stringBuilder.append('(')
            for (argumentType in argumentTypes) {
                argumentType?.appendDescriptor(stringBuilder)
            }
            stringBuilder.append(')')
            returnType.appendDescriptor(stringBuilder)
            return stringBuilder.toString()
        }

        /**
         * Returns the descriptor corresponding to the given method.
         *
         * @param method a [Method] object.
         * @return the descriptor of the given method.
         */
//        fun getMethodDescriptor(method: java.lang.reflect.Method): String {
//            val stringBuilder: java.lang.StringBuilder = java.lang.StringBuilder()
//            stringBuilder.append('(')
//            val parameters: Array<java.lang.Class<*>> = method.getParameterTypes()
//            for (parameter in parameters) {
//                appendDescriptor(parameter, stringBuilder)
//            }
//            stringBuilder.append(')')
//            appendDescriptor(method.getReturnType(), stringBuilder)
//            return stringBuilder.toString()
//        }

        /**
         * Appends the descriptor of the given class to the given string builder.
         *
         * @param clazz the class whose descriptor must be computed.
         * @param stringBuilder the string builder to which the descriptor must be appended.
         */
//        private fun appendDescriptor(clazz: java.lang.Class<*>, stringBuilder: java.lang.StringBuilder) {
//            var currentClass: java.lang.Class<*> = clazz
//            while (currentClass.isArray()) {
//                stringBuilder.append('[')
//                currentClass = currentClass.getComponentType()
//            }
//            if (currentClass.isPrimitive()) {
//                val descriptor: Char
//                descriptor = if (currentClass == java.lang.Integer.TYPE) {
//                    'I'
//                } else if (currentClass == java.lang.Void.TYPE) {
//                    'V'
//                } else if (currentClass == java.lang.Boolean.TYPE) {
//                    'Z'
//                } else if (currentClass == java.lang.Byte.TYPE) {
//                    'B'
//                } else if (currentClass == java.lang.Character.TYPE) {
//                    'C'
//                } else if (currentClass == java.lang.Short.TYPE) {
//                    'S'
//                } else if (currentClass == java.lang.Double.TYPE) {
//                    'D'
//                } else if (currentClass == java.lang.Float.TYPE) {
//                    'F'
//                } else if (currentClass == java.lang.Long.TYPE) {
//                    'J'
//                } else {
//                    throw AssertionError()
//                }
//                stringBuilder.append(descriptor)
//            } else {
//                stringBuilder.append('L').append(getInternalName(currentClass)).append(';')
//            }
//        }

        /**
         * Computes the size of the arguments and of the return value of a method.
         *
         * @param methodDescriptor a method descriptor.
         * @return the size of the arguments of the method (plus one for the implicit this argument),
         * argumentsSize, and the size of its return value, returnSize, packed into a single int i =
         * `(argumentsSize &lt;&lt; 2) | returnSize` (argumentsSize is therefore equal to `i &gt;&gt; 2`, and returnSize to `i &amp; 0x03`).
         */
        fun getArgumentsAndReturnSizes(methodDescriptor: String?): Int {
            var argumentsSize = 1
            // Skip the first character, which is always a '('.
            var currentOffset = 1
            var currentChar = methodDescriptor!![currentOffset].toInt()
            // Parse the argument types and compute their size, one at a each loop iteration.
            while (currentChar != ')'.toInt()) {
                if (currentChar == 'J'.toInt() || currentChar == 'D'.toInt()) {
                    currentOffset++
                    argumentsSize += 2
                } else {
                    while (methodDescriptor[currentOffset] == '[') {
                        currentOffset++
                    }
                    if (methodDescriptor[currentOffset++] == 'L') {
                        // Skip the argument descriptor content.
                        val semiColumnOffset = methodDescriptor.indexOf(';', currentOffset)
                        currentOffset = max(currentOffset, semiColumnOffset + 1)
                    }
                    argumentsSize += 1
                }
                currentChar = methodDescriptor[currentOffset].toInt()
            }
            currentChar = methodDescriptor[currentOffset + 1].toInt()
            return if (currentChar == 'V'.toInt()) {
                argumentsSize shl 2
            } else {
                val returnSize = if (currentChar == 'J'.toInt() || currentChar == 'D'.toInt()) 2 else 1
                argumentsSize shl 2 or returnSize
            }
        }
    }
}
