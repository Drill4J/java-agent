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
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * `visit` [ `visitSource` ] [ `visitModule` ][ `visitNestHost` ][ `visitOuterClass` ] ( `visitAnnotation` | `visitTypeAnnotation` | `visitAttribute` )* ( `visitNestMember` | [ `* visitPermittedSubclass` ] | `visitInnerClass` | `visitRecordComponent` | `visitField` | `visitMethod` )*
 * `visitEnd`.
 *
 * @author Eric Bruneton
 */
abstract class ClassVisitor @JvmOverloads constructor(api: Int, classVisitor: ClassVisitor? = null) {
    /**
     * The ASM API version implemented by this visitor. The value of this field must be one of the
     * `ASM`*x* values in [Opcodes].
     */
    protected val api: Int

    /** The class visitor to which this visitor must delegate method calls. May be null.  */
    protected var cv: ClassVisitor?

    /**
     * Visits the header of the class.
     *
     * @param version the class version. The minor version is stored in the 16 most significant bits,
     * and the major version in the 16 least significant bits.
     * @param access the class's access flags (see [Opcodes]). This parameter also indicates if
     * the class is deprecated [Opcodes.ACC_DEPRECATED] or a record [     ][Opcodes.ACC_RECORD].
     * @param name the internal name of the class (see [Type.getInternalName]).
     * @param signature the signature of this class. May be null if the class is not a
     * generic one, and does not extend or implement generic classes or interfaces.
     * @param superName the internal of name of the super class (see [Type.getInternalName]).
     * For interfaces, the super class is [Object]. May be null, but only for the
     * [Object] class.
     * @param interfaces the internal names of the class's interfaces (see [     ][Type.getInternalName]). May be null.
     */
    open fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<String?>?
    ) {
        if (api < Opcodes.ASM8 && access and Opcodes.ACC_RECORD !== 0) {
            throw UnsupportedOperationException("Records requires ASM8")
        }
        if (cv != null) {
            cv!!.visit(version, access, name, signature, superName, interfaces)
        }
    }

    /**
     * Visits the source of the class.
     *
     * @param source the name of the source file from which the class was compiled. May be null.
     * @param debug additional debug information to compute the correspondence between source and
     * compiled elements of the class. May be null.
     */
    open fun visitSource(source: String?, debug: String?) {
        if (cv != null) {
            cv!!.visitSource(source, debug)
        }
    }

    /**
     * Visit the module corresponding to the class.
     *
     * @param name the fully qualified name (using dots) of the module.
     * @param access the module access flags, among `ACC_OPEN`, `ACC_SYNTHETIC` and `ACC_MANDATED`.
     * @param version the module version, or null.
     * @return a visitor to visit the module values, or null if this visitor is not
     * interested in visiting this module.
     */
    open fun visitModule(name: String?, access: Int, version: String?): ModuleVisitor? {
        if (api < Opcodes.ASM6) {
            throw UnsupportedOperationException("Module requires ASM6")
        }
        return if (cv != null) {
            cv!!.visitModule(name, access, version)
        } else null
    }

    /**
     * Visits the nest host class of the class. A nest is a set of classes of the same package that
     * share access to their private members. One of these classes, called the host, lists the other
     * members of the nest, which in turn should link to the host of their nest. This method must be
     * called only once and only if the visited class is a non-host member of a nest. A class is
     * implicitly its own nest, so it's invalid to call this method with the visited class name as
     * argument.
     *
     * @param nestHost the internal name of the host class of the nest.
     */
    open fun visitNestHost(nestHost: String?) {
        if (api < Opcodes.ASM7) {
            throw UnsupportedOperationException("NestHost requires ASM7")
        }
        if (cv != null) {
            cv!!.visitNestHost(nestHost)
        }
    }

    /**
     * Visits the enclosing class of the class. This method must be called only if the class has an
     * enclosing class.
     *
     * @param owner internal name of the enclosing class of the class.
     * @param name the name of the method that contains the class, or null if the class is
     * not enclosed in a method of its enclosing class.
     * @param descriptor the descriptor of the method that contains the class, or null if
     * the class is not enclosed in a method of its enclosing class.
     */
    open fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {
        if (cv != null) {
            cv!!.visitOuterClass(owner, name, descriptor)
        }
    }

    /**
     * Visits an annotation of the class.
     *
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not
     * interested in visiting this annotation.
     */
    open fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return if (cv != null) {
            cv!!.visitAnnotation(descriptor, visible)
        } else null
    }

    /**
     * Visits an annotation on a type in the class signature.
     *
     * @param typeRef a reference to the annotated type. The sort of this type reference must be
     * [TypeReference.CLASS_TYPE_PARAMETER], [     ][TypeReference.CLASS_TYPE_PARAMETER_BOUND] or [TypeReference.CLASS_EXTENDS]. See
     * [TypeReference].
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
            throw UnsupportedOperationException("TypeAnnotation requires ASM5")
        }
        return if (cv != null) {
            cv!!.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
        } else null
    }

    /**
     * Visits a non standard attribute of the class.
     *
     * @param attribute an attribute.
     */
    open fun visitAttribute(attribute: Attribute?) {
        if (cv != null) {
            cv!!.visitAttribute(attribute)
        }
    }

    /**
     * Visits a member of the nest. A nest is a set of classes of the same package that share access
     * to their private members. One of these classes, called the host, lists the other members of the
     * nest, which in turn should link to the host of their nest. This method must be called only if
     * the visited class is the host of a nest. A nest host is implicitly a member of its own nest, so
     * it's invalid to call this method with the visited class name as argument.
     *
     * @param nestMember the internal name of a nest member.
     */
    open fun visitNestMember(nestMember: String?) {
        if (api < Opcodes.ASM7) {
            throw UnsupportedOperationException("NestMember requires ASM7")
        }
        if (cv != null) {
            cv!!.visitNestMember(nestMember)
        }
    }

    /**
     * Visits a permitted subclasses. A permitted subclass is one of the allowed subclasses of the
     * current class.
     *
     * @param permittedSubclass the internal name of a permitted subclass.
     */
    open fun visitPermittedSubclass(permittedSubclass: String?) {
        if (api < Opcodes.ASM9) {
            throw UnsupportedOperationException("PermittedSubclasses requires ASM9")
        }
        if (cv != null) {
            cv!!.visitPermittedSubclass(permittedSubclass)
        }
    }

    /**
     * Visits information about an inner class. This inner class is not necessarily a member of the
     * class being visited.
     *
     * @param name the internal name of an inner class (see [Type.getInternalName]).
     * @param outerName the internal name of the class to which the inner class belongs (see [     ][Type.getInternalName]). May be null for not member classes.
     * @param innerName the (simple) name of the inner class inside its enclosing class. May be
     * null for anonymous inner classes.
     * @param access the access flags of the inner class as originally declared in the enclosing
     * class.
     */
    open fun visitInnerClass(
        name: String?, outerName: String?, innerName: String?, access: Int
    ) {
        if (cv != null) {
            cv!!.visitInnerClass(name, outerName, innerName, access)
        }
    }

    /**
     * Visits a record component of the class.
     *
     * @param name the record component name.
     * @param descriptor the record component descriptor (see [Type]).
     * @param signature the record component signature. May be null if the record component
     * type does not use generic types.
     * @return a visitor to visit this record component annotations and attributes, or null
     * if this class visitor is not interested in visiting these annotations and attributes.
     */
    open fun visitRecordComponent(
        name: String?, descriptor: String?, signature: String?
    ): RecordComponentVisitor? {
        if (api < Opcodes.ASM8) {
            throw UnsupportedOperationException("Record requires ASM8")
        }
        return if (cv != null) {
            cv!!.visitRecordComponent(name, descriptor, signature)
        } else null
    }

    /**
     * Visits a field of the class.
     *
     * @param access the field's access flags (see [Opcodes]). This parameter also indicates if
     * the field is synthetic and/or deprecated.
     * @param name the field's name.
     * @param descriptor the field's descriptor (see [Type]).
     * @param signature the field's signature. May be null if the field's type does not use
     * generic types.
     * @param value the field's initial value. This parameter, which may be null if the
     * field does not have an initial value, must be an [Integer], a [Float], a [     ], a [Double] or a [String] (for `int`, `float`, `long`
     * or `String` fields respectively). *This parameter is only used for static
     * fields*. Its value is ignored for non static fields, which must be initialized through
     * bytecode instructions in constructors or methods.
     * @return a visitor to visit field annotations and attributes, or null if this class
     * visitor is not interested in visiting these annotations and attributes.
     */
    open fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        return if (cv != null) {
            cv!!.visitField(access, name, descriptor, signature, value)
        } else null
    }

    /**
     * Visits a method of the class. This method *must* return a new [MethodVisitor]
     * instance (or null) each time it is called, i.e., it should not return a previously
     * returned visitor.
     *
     * @param access the method's access flags (see [Opcodes]). This parameter also indicates if
     * the method is synthetic and/or deprecated.
     * @param name the method's name.
     * @param descriptor the method's descriptor (see [Type]).
     * @param signature the method's signature. May be null if the method parameters,
     * return type and exceptions do not use generic types.
     * @param exceptions the internal names of the method's exception classes (see [     ][Type.getInternalName]). May be null.
     * @return an object to visit the byte code of the method, or null if this class
     * visitor is not interested in visiting the code of this method.
     */
    open fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor? {
        return if (cv != null) {
            cv!!.visitMethod(access, name, descriptor, signature, exceptions)
        } else null
    }

    /**
     * Visits the end of the class. This method, which is the last one to be called, is used to inform
     * the visitor that all the fields and methods of the class have been visited.
     */
    open fun visitEnd() {
        if (cv != null) {
            cv!!.visitEnd()
        }
    }
    /**
     * Constructs a new [ClassVisitor].
     *
     * @param api the ASM API version implemented by this visitor. Must be one of the `ASM`*x* values in [Opcodes].
     * @param classVisitor the class visitor to which this visitor must delegate method calls. May be
     * null.
     */
    /**
     * Constructs a new [ClassVisitor].
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
        cv = classVisitor
    }
}
