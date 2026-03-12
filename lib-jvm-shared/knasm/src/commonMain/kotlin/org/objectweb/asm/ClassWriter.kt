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
 * A [ClassVisitor] that generates a corresponding ClassFile structure, as defined in the Java
 * Virtual Machine Specification (JVMS). It can be used alone, to generate a Java class "from
 * scratch", or with one or more [ClassReader] and adapter [ClassVisitor] to generate a
 * modified class from one or more existing Java classes.
 *
 * @see [JVMS 4](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html)
 *
 * @author Eric Bruneton
 */
class ClassWriter(classReader: ClassReader?, flags: Int) : ClassVisitor( /* latest api = */Opcodes.ASM9) {
    // Note: fields are ordered as in the ClassFile structure, and those related to attributes are
    // ordered as in Section 4.7 of the JVMS.
    /**
     * The minor_version and major_version fields of the JVMS ClassFile structure. minor_version is
     * stored in the 16 most significant bits, and major_version in the 16 least significant bits.
     */
    private var version = 0

    /** The symbol table for this class (contains the constant_pool and the BootstrapMethods).  */
    private val symbolTable: SymbolTable

    /**
     * The access_flags field of the JVMS ClassFile structure. This field can contain ASM specific
     * access flags, such as [Opcodes.ACC_DEPRECATED] or [Opcodes.ACC_RECORD], which are
     * removed when generating the ClassFile structure.
     */
    private var accessFlags = 0

    /** The this_class field of the JVMS ClassFile structure.  */
    private var thisClass = 0

    /** The super_class field of the JVMS ClassFile structure.  */
    private var superClass = 0

    /** The interface_count field of the JVMS ClassFile structure.  */
    private var interfaceCount = 0

    /** The 'interfaces' array of the JVMS ClassFile structure.  */
    lateinit var interfaces: IntArray

    /**
     * The fields of this class, stored in a linked list of [FieldWriter] linked via their
     * [FieldWriter.fv] field. This field stores the first element of this list.
     */
    private var firstField: FieldWriter? = null

    /**
     * The fields of this class, stored in a linked list of [FieldWriter] linked via their
     * [FieldWriter.fv] field. This field stores the last element of this list.
     */
    private var lastField: FieldWriter? = null

    /**
     * The methods of this class, stored in a linked list of [MethodWriter] linked via their
     * [MethodWriter.mv] field. This field stores the first element of this list.
     */
    private var firstMethod: MethodWriter? = null

    /**
     * The methods of this class, stored in a linked list of [MethodWriter] linked via their
     * [MethodWriter.mv] field. This field stores the last element of this list.
     */
    private var lastMethod: MethodWriter? = null

    /** The number_of_classes field of the InnerClasses attribute, or 0.  */
    private var numberOfInnerClasses = 0

    /** The 'classes' array of the InnerClasses attribute, or null.  */
    private var innerClasses: ByteVector? = null

    /** The class_index field of the EnclosingMethod attribute, or 0.  */
    private var enclosingClassIndex = 0

    /** The method_index field of the EnclosingMethod attribute.  */
    private var enclosingMethodIndex = 0

    /** The signature_index field of the Signature attribute, or 0.  */
    private var signatureIndex = 0

    /** The source_file_index field of the SourceFile attribute, or 0.  */
    private var sourceFileIndex = 0

    /** The debug_extension field of the SourceDebugExtension attribute, or null.  */
    private var debugExtension: ByteVector? = null

    /**
     * The last runtime visible annotation of this class. The previous ones can be accessed with the
     * [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeVisibleAnnotation: AnnotationWriter? = null

    /**
     * The last runtime invisible annotation of this class. The previous ones can be accessed with the
     * [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeInvisibleAnnotation: AnnotationWriter? = null

    /**
     * The last runtime visible type annotation of this class. The previous ones can be accessed with
     * the [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeVisibleTypeAnnotation: AnnotationWriter? = null

    /**
     * The last runtime invisible type annotation of this class. The previous ones can be accessed
     * with the [AnnotationWriter.previousAnnotation] field. May be null.
     */
    private var lastRuntimeInvisibleTypeAnnotation: AnnotationWriter? = null

    /** The Module attribute of this class, or null.  */
    private var moduleWriter: ModuleWriter? = null

    /** The host_class_index field of the NestHost attribute, or 0.  */
    private var nestHostClassIndex = 0

    /** The number_of_classes field of the NestMembers attribute, or 0.  */
    private var numberOfNestMemberClasses = 0

    /** The 'classes' array of the NestMembers attribute, or null.  */
    private var nestMemberClasses: ByteVector? = null

    /** The number_of_classes field of the PermittedSubclasses attribute, or 0.  */
    private var numberOfPermittedSubclasses = 0

    /** The 'classes' array of the PermittedSubclasses attribute, or null.  */
    private var permittedSubclasses: ByteVector? = null

    /**
     * The record components of this class, stored in a linked list of [RecordComponentWriter]
     * linked via their [RecordComponentWriter.delegate] field. This field stores the first
     * element of this list.
     */
    private var firstRecordComponent: RecordComponentWriter? = null

    /**
     * The record components of this class, stored in a linked list of [RecordComponentWriter]
     * linked via their [RecordComponentWriter.delegate] field. This field stores the last
     * element of this list.
     */
    private var lastRecordComponent: RecordComponentWriter? = null

    /**
     * The first non standard attribute of this class. The next ones can be accessed with the [ ][Attribute.nextAttribute] field. May be null.
     *
     *
     * **WARNING**: this list stores the attributes in the *reverse* order of their visit.
     * firstAttribute is actually the last attribute visited in [.visitAttribute]. The [ ][.toByteArray] method writes the attributes in the order defined by this list, i.e. in the
     * reverse order specified by the user.
     */
    private var firstAttribute: Attribute? = null

    /**
     * Indicates what must be automatically computed in [MethodWriter]. Must be one of [ ][MethodWriter.COMPUTE_NOTHING], [MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL], [ ][MethodWriter.COMPUTE_INSERTED_FRAMES], or [MethodWriter.COMPUTE_ALL_FRAMES].
     */
    private var compute = 0
    // -----------------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------------
    /**
     * Constructs a new [ClassWriter] object.
     *
     * @param flags option flags that can be used to modify the default behavior of this class. Must
     * be zero or more of [.COMPUTE_MAXS] and [.COMPUTE_FRAMES].
     */
    constructor(flags: Int) : this(null, flags) {}

    // -----------------------------------------------------------------------------------------------
    // Implementation of the ClassVisitor abstract class
    // -----------------------------------------------------------------------------------------------
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<String?>?
    ) {
        this.version = version
        accessFlags = access
        thisClass = symbolTable.setMajorVersionAndClassName(version and 0xFFFF, name)
        if (signature != null) {
            signatureIndex = symbolTable.addConstantUtf8(signature)
        }
        superClass = if (superName == null) 0 else symbolTable.addConstantClass(superName).index
        if (interfaces != null && interfaces.size > 0) {
            interfaceCount = interfaces.size
            this.interfaces = IntArray(interfaceCount)
            for (i in 0 until interfaceCount) {
                this.interfaces[i] = symbolTable.addConstantClass(interfaces[i]).index
            }
        }
        if (compute == MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL && version and 0xFFFF >= Opcodes.V1_7) {
            compute = MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES
        }
    }

    override fun visitSource(file: String?, debug: String?) {
        if (file != null) {
            sourceFileIndex = symbolTable.addConstantUtf8(file)
        }
        if (debug != null) {
            debugExtension = ByteVector().encodeUtf8(debug, 0, Int.MAX_VALUE)
        }
    }

    override fun visitModule(
        name: String?, access: Int, version: String?
    ): ModuleVisitor {
        return ModuleWriter(
            symbolTable,
            symbolTable.addConstantModule(name).index,
            access,
            if (version == null) 0 else symbolTable.addConstantUtf8(version)).also { moduleWriter = it }
    }

    override fun visitNestHost(nestHost: String?) {
        nestHostClassIndex = symbolTable.addConstantClass(nestHost).index
    }

    override fun visitOuterClass(
        owner: String?, name: String?, descriptor: String?
    ) {
        enclosingClassIndex = symbolTable.addConstantClass(owner).index
        if (name != null && descriptor != null) {
            enclosingMethodIndex = symbolTable.addConstantNameAndType(name, descriptor)
        }
    }

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

    override fun visitAttribute(attribute: Attribute?) {
        // Store the attributes in the <i>reverse</i> order of their visit by this method.
        attribute!!.nextAttribute = firstAttribute
        firstAttribute = attribute
    }

    override fun visitNestMember(nestMember: String?) {
        if (nestMemberClasses == null) {
            nestMemberClasses = ByteVector()
        }
        ++numberOfNestMemberClasses
        nestMemberClasses!!.putShort(symbolTable.addConstantClass(nestMember).index)
    }

    override fun visitPermittedSubclass(permittedSubclass: String?) {
        if (permittedSubclasses == null) {
            permittedSubclasses = ByteVector()
        }
        ++numberOfPermittedSubclasses
        permittedSubclasses!!.putShort(symbolTable.addConstantClass(permittedSubclass).index)
    }

    override fun visitInnerClass(
        name: String?, outerName: String?, innerName: String?, access: Int
    ) {
        if (innerClasses == null) {
            innerClasses = ByteVector()
        }
        // Section 4.7.6 of the JVMS states "Every CONSTANT_Class_info entry in the constant_pool table
        // which represents a class or interface C that is not a package member must have exactly one
        // corresponding entry in the classes array". To avoid duplicates we keep track in the info
        // field of the Symbol of each CONSTANT_Class_info entry C whether an inner class entry has
        // already been added for C. If so, we store the index of this inner class entry (plus one) in
        // the info field. This trick allows duplicate detection in O(1) time.
        val nameSymbol: Symbol = symbolTable.addConstantClass(name)
        if (nameSymbol.info === 0) {
            ++numberOfInnerClasses
            innerClasses!!.putShort(nameSymbol.index)
            innerClasses!!.putShort(if (outerName == null) 0 else symbolTable.addConstantClass(outerName).index)
            innerClasses!!.putShort(if (innerName == null) 0 else symbolTable.addConstantUtf8(innerName))
            innerClasses!!.putShort(access)
            nameSymbol.info = numberOfInnerClasses
        }
        // Else, compare the inner classes entry nameSymbol.info - 1 with the arguments of this method
        // and throw an exception if there is a difference?
    }

    override fun visitRecordComponent(
        name: String?, descriptor: String?, signature: String?
    ): RecordComponentVisitor {
        val recordComponentWriter = RecordComponentWriter(symbolTable, name, descriptor, signature)
        if (firstRecordComponent == null) {
            firstRecordComponent = recordComponentWriter
        } else {
            lastRecordComponent!!.delegate = recordComponentWriter
        }
        return recordComponentWriter.also { lastRecordComponent = it }
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        val fieldWriter = FieldWriter(symbolTable, access, name, descriptor, signature, value)
        if (firstField == null) {
            firstField = fieldWriter
        } else {
            lastField!!.fv = fieldWriter
        }
        return fieldWriter.also { lastField = it }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor {
        val methodWriter = MethodWriter(symbolTable, access, name, descriptor, signature, exceptions, compute)
        if (firstMethod == null) {
            firstMethod = methodWriter
        } else {
            lastMethod!!.mv = methodWriter
        }
        return methodWriter.also { lastMethod = it }
    }

    override fun visitEnd() {
        // Nothing to do.
    }
    // -----------------------------------------------------------------------------------------------
    // Other public methods
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the content of the class file that was built by this ClassWriter.
     *
     * @return the binary content of the JVMS ClassFile structure that was built by this ClassWriter.
     * @throws ClassTooLargeException if the constant pool of the class is too large.
     * @throws MethodTooLargeException if the Code attribute of a method is too large.
     */
    fun toByteArray(): ByteArray {
        // First step: compute the size in bytes of the ClassFile structure.
        // The magic field uses 4 bytes, 10 mandatory fields (minor_version, major_version,
        // constant_pool_count, access_flags, this_class, super_class, interfaces_count, fields_count,
        // methods_count and attributes_count) use 2 bytes each, and each interface uses 2 bytes too.
        var size = 24 + 2 * interfaceCount
        var fieldsCount = 0
        var fieldWriter: FieldWriter? = firstField
        while (fieldWriter != null) {
            ++fieldsCount
            size += fieldWriter.computeFieldInfoSize()
            fieldWriter = fieldWriter.fv as FieldWriter
        }
        var methodsCount = 0
        var methodWriter: MethodWriter? = firstMethod
        while (methodWriter != null) {
            ++methodsCount
            size += methodWriter.computeMethodInfoSize()
            methodWriter = methodWriter.mv as MethodWriter
        }

        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        var attributesCount = 0
        if (innerClasses != null) {
            ++attributesCount
            size += 8 + innerClasses!!.length
            symbolTable.addConstantUtf8(Constants.INNER_CLASSES)
        }
        if (enclosingClassIndex != 0) {
            ++attributesCount
            size += 10
            symbolTable.addConstantUtf8(Constants.ENCLOSING_METHOD)
        }
        if (accessFlags and Opcodes.ACC_SYNTHETIC !== 0 && version and 0xFFFF < Opcodes.V1_5) {
            ++attributesCount
            size += 6
            symbolTable.addConstantUtf8(Constants.SYNTHETIC)
        }
        if (signatureIndex != 0) {
            ++attributesCount
            size += 8
            symbolTable.addConstantUtf8(Constants.SIGNATURE)
        }
        if (sourceFileIndex != 0) {
            ++attributesCount
            size += 8
            symbolTable.addConstantUtf8(Constants.SOURCE_FILE)
        }
        if (debugExtension != null) {
            ++attributesCount
            size += 6 + debugExtension!!.length
            symbolTable.addConstantUtf8(Constants.SOURCE_DEBUG_EXTENSION)
        }
        if (accessFlags and Opcodes.ACC_DEPRECATED !== 0) {
            ++attributesCount
            size += 6
            symbolTable.addConstantUtf8(Constants.DEPRECATED)
        }
        if (lastRuntimeVisibleAnnotation != null) {
            ++attributesCount
            size += lastRuntimeVisibleAnnotation!!.computeAnnotationsSize(
                Constants.RUNTIME_VISIBLE_ANNOTATIONS)
        }
        if (lastRuntimeInvisibleAnnotation != null) {
            ++attributesCount
            size += lastRuntimeInvisibleAnnotation!!.computeAnnotationsSize(
                Constants.RUNTIME_INVISIBLE_ANNOTATIONS)
        }
        if (lastRuntimeVisibleTypeAnnotation != null) {
            ++attributesCount
            size += lastRuntimeVisibleTypeAnnotation!!.computeAnnotationsSize(
                Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)
        }
        if (lastRuntimeInvisibleTypeAnnotation != null) {
            ++attributesCount
            size += lastRuntimeInvisibleTypeAnnotation!!.computeAnnotationsSize(
                Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)
        }
        if (symbolTable.computeBootstrapMethodsSize() > 0) {
            ++attributesCount
            size += symbolTable.computeBootstrapMethodsSize()
        }
        if (moduleWriter != null) {
            attributesCount += moduleWriter!!.attributeCount
            size += moduleWriter!!.computeAttributesSize()
        }
        if (nestHostClassIndex != 0) {
            ++attributesCount
            size += 8
            symbolTable.addConstantUtf8(Constants.NEST_HOST)
        }
        if (nestMemberClasses != null) {
            ++attributesCount
            size += 8 + nestMemberClasses!!.length
            symbolTable.addConstantUtf8(Constants.NEST_MEMBERS)
        }
        if (permittedSubclasses != null) {
            ++attributesCount
            size += 8 + permittedSubclasses!!.length
            symbolTable.addConstantUtf8(Constants.PERMITTED_SUBCLASSES)
        }
        var recordComponentCount = 0
        var recordSize = 0
        if (firstRecordComponent != null) {
            var recordComponentWriter = firstRecordComponent
            while (recordComponentWriter != null) {
                ++recordComponentCount
                recordSize += recordComponentWriter.computeRecordComponentInfoSize()
                recordComponentWriter = recordComponentWriter.delegate as RecordComponentWriter
            }
            ++attributesCount
            size += 8 + recordSize
            symbolTable.addConstantUtf8(Constants.RECORD)
        }
        if (firstAttribute != null) {
            attributesCount += firstAttribute!!.attributeCount
            size += firstAttribute!!.computeAttributesSize(symbolTable)
        }
        // IMPORTANT: this must be the last part of the ClassFile size computation, because the previous
        // statements can add attribute names to the constant pool, thereby changing its size!
        size += symbolTable.getConstantPoolLength()
        val constantPoolCount: Int = symbolTable.getConstantPoolCount()
        if (constantPoolCount > 0xFFFF) {
            throw ClassTooLargeException(symbolTable.getClassName(), constantPoolCount)
        }

        // Second step: allocate a ByteVector of the correct size (in order to avoid any array copy in
        // dynamic resizes) and fill it with the ClassFile content.
        val result = ByteVector(size)
        result.putInt(-0x35014542).putInt(version)
        symbolTable.putConstantPool(result)
        val mask = if (version and 0xFFFF < Opcodes.V1_5) Opcodes.ACC_SYNTHETIC else 0
        result.putShort(accessFlags and mask.inv()).putShort(thisClass).putShort(superClass)
        result.putShort(interfaceCount)
        for (i in 0 until interfaceCount) {
            result.putShort(interfaces[i])
        }
        result.putShort(fieldsCount)
        fieldWriter = firstField
        while (fieldWriter != null) {
            fieldWriter.putFieldInfo(result)
            fieldWriter = fieldWriter.fv as FieldWriter
        }
        result.putShort(methodsCount)
        var hasFrames = false
        var hasAsmInstructions = false
        methodWriter = firstMethod
        while (methodWriter != null) {
            hasFrames = hasFrames or methodWriter.hasFrames()
            hasAsmInstructions = hasAsmInstructions or methodWriter.hasAsmInstructions()
            methodWriter.putMethodInfo(result)
            methodWriter = methodWriter.mv as MethodWriter
        }
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        result.putShort(attributesCount)
        if (innerClasses != null) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.INNER_CLASSES))
                .putInt(innerClasses!!.length + 2)
                .putShort(numberOfInnerClasses)
                .putByteArray(innerClasses!!.data, 0, innerClasses!!.length)
        }
        if (enclosingClassIndex != 0) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.ENCLOSING_METHOD))
                .putInt(4)
                .putShort(enclosingClassIndex)
                .putShort(enclosingMethodIndex)
        }
        if (accessFlags and Opcodes.ACC_SYNTHETIC !== 0 && version and 0xFFFF < Opcodes.V1_5) {
            result.putShort(symbolTable.addConstantUtf8(Constants.SYNTHETIC)).putInt(0)
        }
        if (signatureIndex != 0) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.SIGNATURE))
                .putInt(2)
                .putShort(signatureIndex)
        }
        if (sourceFileIndex != 0) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.SOURCE_FILE))
                .putInt(2)
                .putShort(sourceFileIndex)
        }
        if (debugExtension != null) {
            val length: Int = debugExtension!!.length
            result
                .putShort(symbolTable.addConstantUtf8(Constants.SOURCE_DEBUG_EXTENSION))
                .putInt(length)
                .putByteArray(debugExtension!!.data, 0, length)
        }
        if (accessFlags and Opcodes.ACC_DEPRECATED !== 0) {
            result.putShort(symbolTable.addConstantUtf8(Constants.DEPRECATED)).putInt(0)
        }
        AnnotationWriter.putAnnotations(
            symbolTable,
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation,
            result)
        symbolTable.putBootstrapMethods(result)
        if (moduleWriter != null) {
            moduleWriter!!.putAttributes(result)
        }
        if (nestHostClassIndex != 0) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.NEST_HOST))
                .putInt(2)
                .putShort(nestHostClassIndex)
        }
        if (nestMemberClasses != null) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.NEST_MEMBERS))
                .putInt(nestMemberClasses!!.length + 2)
                .putShort(numberOfNestMemberClasses)
                .putByteArray(nestMemberClasses!!.data, 0, nestMemberClasses!!.length)
        }
        if (permittedSubclasses != null) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.PERMITTED_SUBCLASSES))
                .putInt(permittedSubclasses!!.length + 2)
                .putShort(numberOfPermittedSubclasses)
                .putByteArray(permittedSubclasses!!.data, 0, permittedSubclasses!!.length)
        }
        if (accessFlags and Opcodes.ACC_RECORD !== 0 || firstRecordComponent != null) {
            result
                .putShort(symbolTable.addConstantUtf8(Constants.RECORD))
                .putInt(recordSize + 2)
                .putShort(recordComponentCount)
            var recordComponentWriter: RecordComponentWriter? = firstRecordComponent
            while (recordComponentWriter != null) {
                recordComponentWriter.putRecordComponentInfo(result)
                recordComponentWriter = recordComponentWriter.delegate as RecordComponentWriter
            }
        }
        if (firstAttribute != null) {
            firstAttribute!!.putAttributes(symbolTable, result)
        }

        // Third step: replace the ASM specific instructions, if any.
        return if (hasAsmInstructions) {
            replaceAsmInstructions(result.data, hasFrames)
        } else {
            result.data
        }
    }

    /**
     * Returns the equivalent of the given class file, with the ASM specific instructions replaced
     * with standard ones. This is done with a ClassReader -&gt; ClassWriter round trip.
     *
     * @param classFile a class file containing ASM specific instructions, generated by this
     * ClassWriter.
     * @param hasFrames whether there is at least one stack map frames in 'classFile'.
     * @return an equivalent of 'classFile', with the ASM specific instructions replaced with standard
     * ones.
     */
    private fun replaceAsmInstructions(classFile: ByteArray, hasFrames: Boolean): ByteArray {
        val attributes: Array<Attribute?> = getAttributePrototypes()
        firstField = null
        lastField = null
        firstMethod = null
        lastMethod = null
        lastRuntimeVisibleAnnotation = null
        lastRuntimeInvisibleAnnotation = null
        lastRuntimeVisibleTypeAnnotation = null
        lastRuntimeInvisibleTypeAnnotation = null
        moduleWriter = null
        nestHostClassIndex = 0
        numberOfNestMemberClasses = 0
        nestMemberClasses = null
        numberOfPermittedSubclasses = 0
        permittedSubclasses = null
        firstRecordComponent = null
        lastRecordComponent = null
        firstAttribute = null
        compute = if (hasFrames) MethodWriter.COMPUTE_INSERTED_FRAMES else MethodWriter.COMPUTE_NOTHING
        ClassReader(classFile, 0,  /* checkClassVersion = */false)
            .accept(
                this,
                attributes,
                (if (hasFrames) ClassReader.EXPAND_FRAMES else 0) or ClassReader.EXPAND_ASM_INSNS)
        return toByteArray()
    }

    /**
     * Returns the prototypes of the attributes used by this class, its fields and its methods.
     *
     * @return the prototypes of the attributes used by this class, its fields and its methods.
     */
    private fun getAttributePrototypes(): Array<Attribute?> {
        val attributePrototypes: Attribute.Set = Attribute.Set()
        attributePrototypes.addAttributes(firstAttribute)
        var fieldWriter: FieldWriter? = firstField
        while (fieldWriter != null) {
            fieldWriter.collectAttributePrototypes(attributePrototypes)
            fieldWriter = fieldWriter.fv as FieldWriter
        }
        var methodWriter: MethodWriter? = firstMethod
        while (methodWriter != null) {
            methodWriter.collectAttributePrototypes(attributePrototypes)
            methodWriter = methodWriter.mv as MethodWriter
        }
        var recordComponentWriter: RecordComponentWriter? = firstRecordComponent
        while (recordComponentWriter != null) {
            recordComponentWriter.collectAttributePrototypes(attributePrototypes)
            recordComponentWriter = recordComponentWriter.delegate as RecordComponentWriter
        }
        return attributePrototypes.toArray()
    }
    // -----------------------------------------------------------------------------------------------
    // Utility methods: constant pool management for Attribute sub classes
    // -----------------------------------------------------------------------------------------------
    /**
     * Adds a number or string constant to the constant pool of the class being build. Does nothing if
     * the constant pool already contains a similar item. *This method is intended for [ ] sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param value the value of the constant to be added to the constant pool. This parameter must be
     * an [Integer], a [Float], a [Long], a [Double] or a [String].
     * @return the index of a new or already existing constant item with the given value.
     */
    fun newConst(value: Any): Int {
        return symbolTable.addConstant(value).index
    }

    /**
     * Adds an UTF8 string to the constant pool of the class being build. Does nothing if the constant
     * pool already contains a similar item. *This method is intended for [Attribute] sub
     * classes, and is normally not needed by class generators or adapters.*
     *
     * @param value the String value.
     * @return the index of a new or already existing UTF8 item.
     */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
    fun newUTF8(value: String?): Int {
        return symbolTable.addConstantUtf8(value)
    }

    /**
     * Adds a class reference to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param value the internal name of the class.
     * @return the index of a new or already existing class reference item.
     */
    fun newClass(value: String?): Int {
        return symbolTable.addConstantClass(value).index
    }

    /**
     * Adds a method type reference to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param methodDescriptor method descriptor of the method type.
     * @return the index of a new or already existing method type reference item.
     */
    fun newMethodType(methodDescriptor: String?): Int {
        return symbolTable.addConstantMethodType(methodDescriptor).index
    }

    /**
     * Adds a module reference to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param moduleName name of the module.
     * @return the index of a new or already existing module reference item.
     */
    fun newModule(moduleName: String?): Int {
        return symbolTable.addConstantModule(moduleName).index
    }

    /**
     * Adds a package reference to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param packageName name of the package in its internal form.
     * @return the index of a new or already existing module reference item.
     */
    fun newPackage(packageName: String?): Int {
        return symbolTable.addConstantPackage(packageName).index
    }

    /**
     * Adds a handle to the constant pool of the class being build. Does nothing if the constant pool
     * already contains a similar item. *This method is intended for [Attribute] sub classes,
     * and is normally not needed by class generators or adapters.*
     *
     * @param tag the kind of this handle. Must be [Opcodes.H_GETFIELD], [     ][Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD], [Opcodes.H_PUTSTATIC], [     ][Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC], [Opcodes.H_INVOKESPECIAL],
     * [Opcodes.H_NEWINVOKESPECIAL] or [Opcodes.H_INVOKEINTERFACE].
     * @param owner the internal name of the field or method owner class.
     * @param name the name of the field or method.
     * @param descriptor the descriptor of the field or method.
     * @return the index of a new or already existing method type reference item.
     */
    @Deprecated("""this method is superseded by {@link #newHandle(int, String, String, String,
   *     boolean)}.""")
    fun newHandle(
        tag: Int, owner: String?, name: String?, descriptor: String?
    ): Int {
        return newHandle(tag, owner, name, descriptor, tag == Opcodes.H_INVOKEINTERFACE)
    }

    /**
     * Adds a handle to the constant pool of the class being build. Does nothing if the constant pool
     * already contains a similar item. *This method is intended for [Attribute] sub classes,
     * and is normally not needed by class generators or adapters.*
     *
     * @param tag the kind of this handle. Must be [Opcodes.H_GETFIELD], [     ][Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD], [Opcodes.H_PUTSTATIC], [     ][Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC], [Opcodes.H_INVOKESPECIAL],
     * [Opcodes.H_NEWINVOKESPECIAL] or [Opcodes.H_INVOKEINTERFACE].
     * @param owner the internal name of the field or method owner class.
     * @param name the name of the field or method.
     * @param descriptor the descriptor of the field or method.
     * @param isInterface true if the owner is an interface.
     * @return the index of a new or already existing method type reference item.
     */
    fun newHandle(
        tag: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ): Int {
        return symbolTable.addConstantMethodHandle(tag, owner, name, descriptor, isInterface).index
    }

    /**
     * Adds a dynamic constant reference to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item. *This method is intended for [ ] sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param name name of the invoked method.
     * @param descriptor field descriptor of the constant type.
     * @param bootstrapMethodHandle the bootstrap method.
     * @param bootstrapMethodArguments the bootstrap method constant arguments.
     * @return the index of a new or already existing dynamic constant reference item.
     */
    fun newConstantDynamic(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ): Int {
        return symbolTable.addConstantDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments).index
    }

    /**
     * Adds an invokedynamic reference to the constant pool of the class being build. Does nothing if
     * the constant pool already contains a similar item. *This method is intended for [ ] sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param name name of the invoked method.
     * @param descriptor descriptor of the invoke method.
     * @param bootstrapMethodHandle the bootstrap method.
     * @param bootstrapMethodArguments the bootstrap method constant arguments.
     * @return the index of a new or already existing invokedynamic reference item.
     */
    fun newInvokeDynamic(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ): Int {
        return symbolTable.addConstantInvokeDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments).index
    }

    /**
     * Adds a field reference to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param owner the internal name of the field's owner class.
     * @param name the field's name.
     * @param descriptor the field's descriptor.
     * @return the index of a new or already existing field reference item.
     */
    fun newField(owner: String?, name: String?, descriptor: String?): Int {
        return symbolTable.addConstantFieldref(owner, name, descriptor).index
    }

    /**
     * Adds a method reference to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param owner the internal name of the method's owner class.
     * @param name the method's name.
     * @param descriptor the method's descriptor.
     * @param isInterface true if `owner` is an interface.
     * @return the index of a new or already existing method reference item.
     */
    fun newMethod(
        owner: String?, name: String?, descriptor: String?, isInterface: Boolean
    ): Int {
        return symbolTable.addConstantMethodref(owner, name, descriptor, isInterface).index
    }

    /**
     * Adds a name and type to the constant pool of the class being build. Does nothing if the
     * constant pool already contains a similar item. *This method is intended for [Attribute]
     * sub classes, and is normally not needed by class generators or adapters.*
     *
     * @param name a name.
     * @param descriptor a type descriptor.
     * @return the index of a new or already existing name and type item.
     */
    fun newNameType(name: String?, descriptor: String?): Int {
        return symbolTable.addConstantNameAndType(name, descriptor)
    }
    // -----------------------------------------------------------------------------------------------
    // Default method to compute common super classes when computing stack map frames
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the common super type of the two given types. The default implementation of this method
     * *loads* the two given classes and uses the java.lang.Class methods to find the common
     * super class. It can be overridden to compute this common super type in other ways, in
     * particular without actually loading any class, or to take into account the class that is
     * currently being generated by this ClassWriter, which can of course not be loaded since it is
     * under construction.
     *
     * @param type1 the internal name of a class.
     * @param type2 the internal name of another class.
     * @return the internal name of the common super class of the two given classes.
     */
//    fun getCommonSuperClass(type1: String, type2: String): String {
//        val classLoader: java.lang.ClassLoader = getClassLoader()
//        var class1: java.lang.Class<*>
//        class1 = try {
//            java.lang.Class.forName(type1.replace('/', '.'), false, classLoader)
//        } catch (e: ClassNotFoundException) {
//            throw TypeNotPresentException(type1, e)
//        }
//        val class2: java.lang.Class<*>
//        class2 = try {
//            java.lang.Class.forName(type2.replace('/', '.'), false, classLoader)
//        } catch (e: ClassNotFoundException) {
//            throw TypeNotPresentException(type2, e)
//        }
//        if (class1.isAssignableFrom(class2)) {
//            return type1
//        }
//        if (class2.isAssignableFrom(class1)) {
//            return type2
//        }
//        return if (class1.isInterface() || class2.isInterface()) {
//            "java/lang/Object"
//        } else {
//            do {
//                class1 = class1.getSuperclass()
//            } while (!class1.isAssignableFrom(class2))
//            class1.getName().replace('.', '/')
//        }
//    }

    /**
     * Returns the [ClassLoader] to be used by the default implementation of [ ][.getCommonSuperClass], that of this [ClassWriter]'s runtime type by
     * default.
     *
     * @return ClassLoader
     */
//    protected fun getClassLoader(): java.lang.ClassLoader {
//        return javaClass.getClassLoader()
//    }

    companion object {
        /**
         * A flag to automatically compute the maximum stack size and the maximum number of local
         * variables of methods. If this flag is set, then the arguments of the [ ][MethodVisitor.visitMaxs] method of the [MethodVisitor] returned by the [ ][.visitMethod] method will be ignored, and computed automatically from the signature and the
         * bytecode of each method.
         *
         *
         * **Note:** for classes whose version is [Opcodes.V1_7] of more, this option requires
         * valid stack map frames. The maximum stack size is then computed from these frames, and from the
         * bytecode instructions in between. If stack map frames are not present or must be recomputed,
         * used [.COMPUTE_FRAMES] instead.
         *
         * @see .ClassWriter
         */
        const val COMPUTE_MAXS = 1

        /**
         * A flag to automatically compute the stack map frames of methods from scratch. If this flag is
         * set, then the calls to the [MethodVisitor.visitFrame] method are ignored, and the stack
         * map frames are recomputed from the methods bytecode. The arguments of the [ ][MethodVisitor.visitMaxs] method are also ignored and recomputed from the bytecode. In other
         * words, [.COMPUTE_FRAMES] implies [.COMPUTE_MAXS].
         *
         * @see .ClassWriter
         */
        const val COMPUTE_FRAMES = 2
    }

    /**
     * Constructs a new [ClassWriter] object and enables optimizations for "mostly add" bytecode
     * transformations. These optimizations are the following:
     *
     *
     *  * The constant pool and bootstrap methods from the original class are copied as is in the
     * new class, which saves time. New constant pool entries and new bootstrap methods will be
     * added at the end if necessary, but unused constant pool entries or bootstrap methods
     * *won't be removed*.
     *  * Methods that are not transformed are copied as is in the new class, directly from the
     * original class bytecode (i.e. without emitting visit events for all the method
     * instructions), which saves a *lot* of time. Untransformed methods are detected by
     * the fact that the [ClassReader] receives [MethodVisitor] objects that come
     * from a [ClassWriter] (and not from any other [ClassVisitor] instance).
     *
     *
     * @param classReader the [ClassReader] used to read the original class. It will be used to
     * copy the entire constant pool and bootstrap methods from the original class and also to
     * copy other fragments of original bytecode where applicable.
     * @param flags option flags that can be used to modify the default behavior of this class.Must be
     * zero or more of [.COMPUTE_MAXS] and [.COMPUTE_FRAMES]. *These option flags do
     * not affect methods that are copied as is in the new class. This means that neither the
     * maximum stack size nor the stack frames will be computed for these methods*.
     */
    init {
        symbolTable = if (classReader == null) SymbolTable(this) else SymbolTable(this, classReader)
        if (flags and COMPUTE_FRAMES != 0) {
            compute = MethodWriter.COMPUTE_ALL_FRAMES
        } else if (flags and COMPUTE_MAXS != 0) {
            compute = MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL
        } else {
            compute = MethodWriter.COMPUTE_NOTHING
        }
    }
}
