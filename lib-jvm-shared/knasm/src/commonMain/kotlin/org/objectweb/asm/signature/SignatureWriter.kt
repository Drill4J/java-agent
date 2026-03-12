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

import org.objectweb.asm.*

/**
 * A SignatureVisitor that generates signature literals, as defined in the Java Virtual Machine
 * Specification (JVMS).
 *
 * @see [JVMS
 * 4.7.9.1](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.9.1)
 *
 * @author Thomas Hallgren
 * @author Eric Bruneton
 */
class SignatureWriter
/** Constructs a new [SignatureWriter].  */
    : SignatureVisitor {
    /** The builder used to construct the visited signature.  */
    private val stringBuilder: StringBuilder

    /** Whether the visited signature contains formal type parameters.  */
    private var hasFormals = false

    /** Whether the visited signature contains method parameter types.  */
    private var hasParameters = false

    /** Constructs a new {@link SignatureWriter}. */
    constructor() : this(StringBuilder())

    private constructor(stringBuilder: StringBuilder) : super( /* latest api =*/Opcodes.ASM9) {
        this.stringBuilder = stringBuilder
    }

    /**
     * The stack used to keep track of class types that have arguments. Each element of this stack is
     * a boolean encoded in one bit. The top of the stack is the least significant bit. Pushing false
     * = *2, pushing true = *2+1, popping = /2.
     *
     *
     * Class type arguments must be surrounded with '&lt;' and '&gt;' and, because
     *
     *
     *  1. class types can be nested (because type arguments can themselves be class types),
     *  1. SignatureWriter always returns 'this' in each visit* method (to avoid allocating new
     * SignatureWriter instances),
     *
     *
     *
     * we need a stack to properly balance these 'parentheses'. A new element is pushed on this
     * stack for each new visited type, and popped when the visit of this type ends (either is
     * visitEnd, or because visitInnerClassType is called).
     */
    private var argumentStack = 1

    // -----------------------------------------------------------------------------------------------
    // Implementation of the SignatureVisitor interface
    // -----------------------------------------------------------------------------------------------
    override fun visitFormalTypeParameter(name: String?) {
        if (!hasFormals) {
            hasFormals = true
            stringBuilder.append('<')
        }
        stringBuilder.append(name)
        stringBuilder.append(':')
    }

    override fun visitClassBound(): SignatureVisitor {
        return this
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        stringBuilder.append(':')
        return this
    }

    override fun visitSuperclass(): SignatureVisitor {
        endFormals()
        return this
    }

    override fun visitInterface(): SignatureVisitor {
        return this
    }

    override fun visitParameterType(): SignatureVisitor {
        endFormals()
        if (!hasParameters) {
            hasParameters = true
            stringBuilder.append('(')
        }
        return this
    }

    override fun visitReturnType(): SignatureVisitor {
        endFormals()
        if (!hasParameters) {
            stringBuilder.append('(')
        }
        stringBuilder.append(')')
        return this
    }

    override fun visitExceptionType(): SignatureVisitor {
        stringBuilder.append('^')
        return this
    }

    override fun visitBaseType(descriptor: Char) {
        stringBuilder.append(descriptor)
    }

    override fun visitTypeVariable(name: String?) {
        stringBuilder.append('T')
        stringBuilder.append(name)
        stringBuilder.append(';')
    }

    override fun visitArrayType(): SignatureVisitor {
        stringBuilder.append('[')
        return this
    }

    override fun visitClassType(name: String?) {
        stringBuilder.append('L')
        stringBuilder.append(name)
        // Pushes 'false' on the stack, meaning that this type does not have type arguments (as far as
        // we can tell at this point).
        argumentStack = argumentStack shl 1
    }

    override fun visitInnerClassType(name: String?) {
        endArguments()
        stringBuilder.append('.')
        stringBuilder.append(name)
        // Pushes 'false' on the stack, meaning that this type does not have type arguments (as far as
        // we can tell at this point).
        argumentStack = argumentStack shl 1
    }

    override fun visitTypeArgument() {
        // If the top of the stack is 'false', this means we are visiting the first type argument of the
        // currently visited type. We therefore need to append a '<', and to replace the top stack
        // element with 'true' (meaning that the current type does have type arguments).
        if ((argumentStack and 1) == 0) {
            argumentStack = argumentStack or 1
            stringBuilder.append('<')
        }
        stringBuilder.append('*')
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        // If the top of the stack is 'false', this means we are visiting the first type argument of the
        // currently visited type. We therefore need to append a '<', and to replace the top stack
        // element with 'true' (meaning that the current type does have type arguments).
        if (argumentStack and 1 == 0) {
            argumentStack = argumentStack or 1
            stringBuilder.append('<')
        }
        if (wildcard != '=') {
            stringBuilder.append(wildcard)
        }
        // If the stack is full, start a nested one by returning a new SignatureWriter.
        return if (argumentStack and (1 shl 31) == 0) this else SignatureWriter(stringBuilder)
    }

    override fun visitEnd() {
        endArguments()
        stringBuilder.append(';')
    }

    /**
     * Returns the signature that was built by this signature writer.
     *
     * @return the signature that was built by this signature writer.
     */
    override fun toString(): String {
        return stringBuilder.toString()
    }
    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------
    /** Ends the formal type parameters section of the signature.  */
    private fun endFormals() {
        if (hasFormals) {
            hasFormals = false
            stringBuilder.append('>')
        }
    }

    /** Ends the type arguments of a class or inner class type.  */
    private fun endArguments() {
        // If the top of the stack is 'true', this means that some type arguments have been visited for
        // the type whose visit is now ending. We therefore need to append a '>', and to pop one element
        // from the stack.
        if (argumentStack and 1 == 1) {
            stringBuilder.append('>')
        }
        argumentStack = argumentStack ushr 1
    }
}
