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
package org.objectweb.asm.signature

/**
 * A parser for signature literals, as defined in the Java Virtual Machine Specification (JVMS), to
 * visit them with a SignatureVisitor.
 *
 * @see [JVMS
 * 4.7.9.1](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.9.1)
 *
 * @author Thomas Hallgren
 * @author Eric Bruneton
 */
class SignatureReader
/**
 * Constructs a [SignatureReader] for the given signature.
 *
 * @param signature A *JavaTypeSignature*, *ClassSignature* or *MethodSignature*.
 */(
    /** The JVMS signature to be read.  */
    private val signatureValue: String
) {
    /**
     * Makes the given visitor visit the signature of this [SignatureReader]. This signature is
     * the one specified in the constructor (see [.SignatureReader]). This method is intended to
     * be called on a [SignatureReader] that was created using a *ClassSignature* (such as
     * the `signature` parameter of the [org.objectweb.asm.ClassVisitor.visit]
     * method) or a *MethodSignature* (such as the `signature` parameter of the [ ][org.objectweb.asm.ClassVisitor.visitMethod] method).
     *
     * @param signatureVistor the visitor that must visit this signature.
     */
    fun accept(signatureVistor: SignatureVisitor) {
        val signature = signatureValue
        val length = signature.length
        var offset: Int // Current offset in the parsed signature (parsed from left to right).
        var currentChar: Char // The signature character at 'offset', or just before.

        // If the signature starts with '<', it starts with TypeParameters, i.e. a formal type parameter
        // identifier, followed by one or more pair ':',ReferenceTypeSignature (for its class bound and
        // interface bounds).
        if (signature[0] == '<') {
            // Invariant: offset points to the second character of a formal type parameter name at the
            // beginning of each iteration of the loop below.
            offset = 2
            do {
                // The formal type parameter name is everything between offset - 1 and the first ':'.
                val classBoundStartOffset = signature.indexOf(':', offset)
                signatureVistor.visitFormalTypeParameter(
                    signature.substring(offset - 1, classBoundStartOffset))

                // If the character after the ':' class bound marker is not the start of a
                // ReferenceTypeSignature, it means the class bound is empty (which is a valid case).
                offset = classBoundStartOffset + 1
                currentChar = signature[offset]
                if (currentChar == 'L' || currentChar == '[' || currentChar == 'T') {
                    offset = parseType(signature, offset, signatureVistor.visitClassBound())
                }

                // While the character after the class bound or after the last parsed interface bound
                // is ':', we need to parse another interface bound.
                while (signature[offset++].also { currentChar = it } == ':') {
                    offset = parseType(signature, offset, signatureVistor.visitInterfaceBound())
                }

                // At this point a TypeParameter has been fully parsed, and we need to parse the next one
                // (note that currentChar is now the first character of the next TypeParameter, and that
                // offset points to the second character), unless the character just after this
                // TypeParameter signals the end of the TypeParameters.
            } while (currentChar != '>')
        } else {
            offset = 0
        }

        // If the (optional) TypeParameters is followed by '(' this means we are parsing a
        // MethodSignature, which has JavaTypeSignature type inside parentheses, followed by a Result
        // type and optional ThrowsSignature types.
        if (signature[offset] == '(') {
            offset++
            while (signature[offset] != ')') {
                offset = parseType(signature, offset, signatureVistor.visitParameterType())
            }
            // Use offset + 1 to skip ')'.
            offset = parseType(signature, offset + 1, signatureVistor.visitReturnType())
            while (offset < length) {
                // Use offset + 1 to skip the first character of a ThrowsSignature, i.e. '^'.
                offset = parseType(signature, offset + 1, signatureVistor.visitExceptionType())
            }
        } else {
            // Otherwise we are parsing a ClassSignature (by hypothesis on the method input), which has
            // one or more ClassTypeSignature for the super class and the implemented interfaces.
            offset = parseType(signature, offset, signatureVistor.visitSuperclass())
            while (offset < length) {
                offset = parseType(signature, offset, signatureVistor.visitInterface())
            }
        }
    }

    /**
     * Makes the given visitor visit the signature of this [SignatureReader]. This signature is
     * the one specified in the constructor (see [.SignatureReader]). This method is intended to
     * be called on a [SignatureReader] that was created using a *JavaTypeSignature*, such
     * as the `signature` parameter of the [ ][org.objectweb.asm.ClassVisitor.visitField] or [ ][org.objectweb.asm.MethodVisitor.visitLocalVariable] methods.
     *
     * @param signatureVisitor the visitor that must visit this signature.
     */
    fun acceptType(signatureVisitor: SignatureVisitor) {
        parseType(signatureValue, 0, signatureVisitor)
    }

    companion object {
        /**
         * Parses a JavaTypeSignature and makes the given visitor visit it.
         *
         * @param signature a string containing the signature that must be parsed.
         * @param startOffset index of the first character of the signature to parsed.
         * @param signatureVisitor the visitor that must visit this signature.
         * @return the index of the first character after the parsed signature.
         */
        private fun parseType(
            signature: String, startOffset: Int, signatureVisitor: SignatureVisitor
        ): Int {
            var offset = startOffset // Current offset in the parsed signature.
            var currentChar = signature[offset++] // The signature character at 'offset'.
            return when (currentChar) {
                'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D', 'V' -> {
                    // Case of a BaseType or a VoidDescriptor.
                    signatureVisitor.visitBaseType(currentChar)
                    offset
                }
                '[' ->         // Case of an ArrayTypeSignature, a '[' followed by a JavaTypeSignature.
                    parseType(signature, offset, signatureVisitor.visitArrayType())
                'T' -> {
                    // Case of TypeVariableSignature, an identifier between 'T' and ';'.
                    val endOffset = signature.indexOf(';', offset)
                    signatureVisitor.visitTypeVariable(signature.substring(offset, endOffset))
                    endOffset + 1
                }
                'L' -> {
                    // Case of a ClassTypeSignature, which ends with ';'.
                    // These signatures have a main class type followed by zero or more inner class types
                    // (separated by '.'). Each can have type arguments, inside '<' and '>'.
                    var start = offset // The start offset of the currently parsed main or inner class name.
                    var visited = false // Whether the currently parsed class name has been visited.
                    var inner = false // Whether we are currently parsing an inner class type.
                    // Parses the signature, one character at a time.
                    while (true) {
                        currentChar = signature[offset++]
                        if (currentChar == '.' || currentChar == ';') {
                            // If a '.' or ';' is encountered, this means we have fully parsed the main class name
                            // or an inner class name. This name may already have been visited it is was followed by
                            // type arguments between '<' and '>'. If not, we need to visit it here.
                            if (!visited) {
                                val name = signature.substring(start, offset - 1)
                                if (inner) {
                                    signatureVisitor.visitInnerClassType(name)
                                } else {
                                    signatureVisitor.visitClassType(name)
                                }
                            }
                            // If we reached the end of the ClassTypeSignature return, otherwise start the parsing
                            // of a new class name, which is necessarily an inner class name.
                            if (currentChar == ';') {
                                signatureVisitor.visitEnd()
                                break
                            }
                            start = offset
                            visited = false
                            inner = true
                        } else if (currentChar == '<') {
                            // If a '<' is encountered, this means we have fully parsed the main class name or an
                            // inner class name, and that we now need to parse TypeArguments. First, we need to
                            // visit the parsed class name.
                            val name = signature.substring(start, offset - 1)
                            if (inner) {
                                signatureVisitor.visitInnerClassType(name)
                            } else {
                                signatureVisitor.visitClassType(name)
                            }
                            visited = true
                            // Now, parse the TypeArgument(s), one at a time.
                            while (signature[offset].also { currentChar = it } != '>') {
                                when (currentChar) {
                                    '*' -> {
                                        // Unbounded TypeArgument.
                                        ++offset
                                        signatureVisitor.visitTypeArgument()
                                    }
                                    '+', '-' ->                   // Extends or Super TypeArgument. Use offset + 1 to skip the '+' or '-'.
                                        offset = parseType(
                                            signature, offset + 1, signatureVisitor.visitTypeArgument(currentChar))
                                    else ->                   // Instanceof TypeArgument. The '=' is implicit.
                                        offset = parseType(signature,
                                            offset,
                                            signatureVisitor.visitTypeArgument('='))
                                }
                            }
                        }
                    }
                    offset
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
