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
 * The path to a type argument, wildcard bound, array element type, or static inner type within an
 * enclosing type.
 *
 * @author Eric Bruneton
 */
class TypePath
/**
 * Constructs a new TypePath.
 *
 * @param typePathContainer a byte array containing a type_path JVMS structure.
 * @param typePathOffset the offset of the first byte of the type_path structure in
 * typePathContainer.
 */ internal constructor(
    /**
     * The byte array where the 'type_path' structure - as defined in the Java Virtual Machine
     * Specification (JVMS) - corresponding to this TypePath is stored. The first byte of the
     * structure in this array is given by [.typePathOffset].
     *
     * @see [JVMS
     * 4.7.20.2](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.20.2)
     */
    private val typePathContainer: ByteArray,
    /** The offset of the first byte of the type_path JVMS structure in [.typePathContainer].  */
    private val typePathOffset: Int// path_length is stored in the first byte of a type_path.
) {
    /**
     * Returns the length of this path, i.e. its number of steps.
     *
     * @return the length of this path.
     */
    val length: Int
        get() =// path_length is stored in the first byte of a type_path.
            typePathContainer[typePathOffset].toInt()

    /**
     * Returns the value of the given step of this path.
     *
     * @param index an index between 0 and [.getLength], exclusive.
     * @return one of [.ARRAY_ELEMENT], [.INNER_TYPE], [.WILDCARD_BOUND], or [     ][.TYPE_ARGUMENT].
     */
    fun getStep(index: Int): Int {
        // Returns the type_path_kind of the path element of the given index.
        return typePathContainer[typePathOffset + 2 * index + 1].toInt()
    }

    /**
     * Returns the index of the type argument that the given step is stepping into. This method should
     * only be used for steps whose value is [.TYPE_ARGUMENT].
     *
     * @param index an index between 0 and [.getLength], exclusive.
     * @return the index of the type argument that the given step is stepping into.
     */
    fun getStepArgument(index: Int): Int {
        // Returns the type_argument_index of the path element of the given index.
        return typePathContainer[typePathOffset + 2 * index + 2].toInt()
    }

    /**
     * Returns a string representation of this type path. [.ARRAY_ELEMENT] steps are represented
     * with '[', [.INNER_TYPE] steps with '.', [.WILDCARD_BOUND] steps with '*' and [ ][.TYPE_ARGUMENT] steps with their type argument index in decimal form followed by ';'.
     */
    override fun toString(): String {
        val length = length
        val result = StringBuilder(length * 2)
        for (i in 0 until length) {
            when (getStep(i)) {
                ARRAY_ELEMENT -> result.append('[')
                INNER_TYPE -> result.append('.')
                WILDCARD_BOUND -> result.append('*')
                TYPE_ARGUMENT -> result.append(getStepArgument(i)).append(';')
                else -> throw AssertionError()
            }
        }
        return result.toString()
    }

    companion object {
        /** A type path step that steps into the element type of an array type. See [.getStep].  */
        const val ARRAY_ELEMENT = 0

        /** A type path step that steps into the nested type of a class type. See [.getStep].  */
        const val INNER_TYPE = 1

        /** A type path step that steps into the bound of a wildcard type. See [.getStep].  */
        const val WILDCARD_BOUND = 2

        /** A type path step that steps into a type argument of a generic type. See [.getStep].  */
        const val TYPE_ARGUMENT = 3

        /**
         * Converts a type path in string form, in the format used by [.toString], into a TypePath
         * object.
         *
         * @param typePath a type path in string form, in the format used by [.toString]. May be
         * null or empty.
         * @return the corresponding TypePath object, or null if the path is empty.
         */
        fun fromString(typePath: String?): TypePath? {
            if (typePath == null || typePath.length == 0) {
                return null
            }
            val typePathLength = typePath.length
            val output = ByteVector(typePathLength)
            output.putByte(0)
            var typePathIndex = 0
            while (typePathIndex < typePathLength) {
                var c = typePath[typePathIndex++]
                if (c == '[') {
                    output.put11(ARRAY_ELEMENT, 0)
                } else if (c == '.') {
                    output.put11(INNER_TYPE, 0)
                } else if (c == '*') {
                    output.put11(WILDCARD_BOUND, 0)
                } else if (c >= '0' && c <= '9') {
                    var typeArg = c - '0'
                    while (typePathIndex < typePathLength) {
                        c = typePath[typePathIndex++]
                        typeArg = if (c >= '0' && c <= '9') {
                            typeArg * 10 + c.toInt() - '0'.toInt()
                        } else if (c == ';') {
                            break
                        } else {
                            throw IllegalArgumentException()
                        }
                    }
                    output.put11(TYPE_ARGUMENT, typeArg)
                } else {
                    throw IllegalArgumentException()
                }
            }
            output.data[0] = (output.length / 2).toByte()
            return TypePath(output.data, 0)
        }

        /**
         * Puts the type_path JVMS structure corresponding to the given TypePath into the given
         * ByteVector.
         *
         * @param typePath a TypePath instance, or null for empty paths.
         * @param output where the type path must be put.
         */
        fun put(typePath: TypePath?, output: ByteVector) {
            if (typePath == null) {
                output.putByte(0)
            } else {
                val length = typePath.typePathContainer[typePath.typePathOffset] * 2 + 1
                output.putByteArray(typePath.typePathContainer, typePath.typePathOffset, length)
            }
        }
    }
}
