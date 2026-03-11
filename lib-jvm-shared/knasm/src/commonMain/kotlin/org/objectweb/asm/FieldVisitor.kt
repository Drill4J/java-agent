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

import kotlin.jvm.*

/**
 * A visitor to visit a Java field. The methods of this class must be called in the following order:
 * ( `visitAnnotation` | `visitTypeAnnotation` | `visitAttribute` )* `visitEnd`.
 *
 * @author Eric Bruneton
 */
abstract class FieldVisitor @JvmOverloads constructor(api: Int, fieldVisitor: FieldVisitor? = null) {
    /**
     * The ASM API version implemented by this visitor. The value of this field must be one of the
     * `ASM`*x* values in [Opcodes].
     */
    protected val api: Int

    /** The field visitor to which this visitor must delegate method calls. May be null.  */
    var fv: FieldVisitor?

    /**
     * Visits an annotation of the field.
     *
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return if (fv != null) {
            fv!!.visitAnnotation(descriptor, visible)
        } else null
    }

    /**
     * Visits an annotation on the type of the field.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     * [TypeReference.FIELD]. See [TypeReference].
     * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
     * static inner type within 'typeRef'. May be null if the annotation targets
     * 'typeRef' as a whole.
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitTypeAnnotation(
        typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean
    ): AnnotationVisitor? {
        if (api < Opcodes.ASM5) {
            throw UnsupportedOperationException("This feature requires ASM5")
        }
        return if (fv != null) {
            fv!!.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
        } else null
    }

    /**
     * Visits a non standard attribute of the field.
     *
     * @param attribute an attribute.
     */
    open fun visitAttribute(attribute: Attribute) {
        if (fv != null) {
            fv!!.visitAttribute(attribute)
        }
    }

    /**
     * Visits the end of the field. This method, which is the last one to be called, is used to inform
     * the visitor that all the annotations and attributes of the field have been visited.
     */
    open fun visitEnd() {
        if (fv != null) {
            fv!!.visitEnd()
        }
    }
    /**
     * Constructs a new [FieldVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of the `ASM`*x* values in [Opcodes].
     * @param fieldVisitor the field visitor to which this visitor must delegate method calls. May be
     * null.
     */
    /**
     * Constructs a new [FieldVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of the `ASM`*x* values in [Opcodes].
     */
    init {
        if (api != Opcodes.ASM9 && api != Opcodes.ASM8 && api != Opcodes.ASM7 && api != Opcodes.ASM6 && api != Opcodes.ASM5 && api != Opcodes.ASM4 && api != Opcodes.ASM10_EXPERIMENTAL) {
            throw IllegalArgumentException("Unsupported api $api")
        }
        if (api == Opcodes.ASM10_EXPERIMENTAL) {
            Constants.checkAsmExperimental(this)
        }
        this.api = api
        fv = fieldVisitor
    }
}
