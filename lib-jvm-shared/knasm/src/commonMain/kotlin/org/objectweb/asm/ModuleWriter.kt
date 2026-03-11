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
 * A [ModuleVisitor] that generates the corresponding Module, ModulePackages and
 * ModuleMainClass attributes, as defined in the Java Virtual Machine Specification (JVMS).
 *
 * @see [JVMS
 * 4.7.25](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.25)
 *
 * @see [JVMS
 * 4.7.26](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.26)
 *
 * @see [JVMS
 * 4.7.27](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.27)
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
internal class ModuleWriter(symbolTable: SymbolTable, name: Int, access: Int, version: Int) :
    ModuleVisitor( /* latest api = */Opcodes.ASM9) {
    /** Where the constants used in this AnnotationWriter must be stored.  */
    private val symbolTable: SymbolTable

    /** The module_name_index field of the JVMS Module attribute.  */
    private val moduleNameIndex: Int

    /** The module_flags field of the JVMS Module attribute.  */
    private val moduleFlags: Int

    /** The module_version_index field of the JVMS Module attribute.  */
    private val moduleVersionIndex: Int

    /** The requires_count field of the JVMS Module attribute.  */
    private var requiresCount = 0

    /** The binary content of the 'requires' array of the JVMS Module attribute.  */
    private val requires: ByteVector

    /** The exports_count field of the JVMS Module attribute.  */
    private var exportsCount = 0

    /** The binary content of the 'exports' array of the JVMS Module attribute.  */
    private val exports: ByteVector

    /** The opens_count field of the JVMS Module attribute.  */
    private var opensCount = 0

    /** The binary content of the 'opens' array of the JVMS Module attribute.  */
    private val opens: ByteVector

    /** The uses_count field of the JVMS Module attribute.  */
    private var usesCount = 0

    /** The binary content of the 'uses_index' array of the JVMS Module attribute.  */
    private val usesIndex: ByteVector

    /** The provides_count field of the JVMS Module attribute.  */
    private var providesCount = 0

    /** The binary content of the 'provides' array of the JVMS Module attribute.  */
    private val provides: ByteVector

    /** The provides_count field of the JVMS ModulePackages attribute.  */
    private var packageCount = 0

    /** The binary content of the 'package_index' array of the JVMS ModulePackages attribute.  */
    private val packageIndex: ByteVector

    /** The main_class_index field of the JVMS ModuleMainClass attribute, or 0.  */
    private var mainClassIndex = 0
    override fun visitMainClass(mainClass: String?) {
        mainClassIndex = symbolTable.addConstantClass(mainClass).index
    }

    override fun visitPackage(packaze: String?) {
        packageIndex.putShort(symbolTable.addConstantPackage(packaze).index)
        packageCount++
    }

    override fun visitRequire(module: String?, access: Int, version: String?) {
        requires
            .putShort(symbolTable.addConstantModule(module).index)
            .putShort(access)
            .putShort(if (version == null) 0 else symbolTable.addConstantUtf8(version))
        requiresCount++
    }

    override fun visitExport(packaze: String?, access: Int, vararg modules: String?) {
        exports.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access)
        if (modules == null) {
            exports.putShort(0)
        } else {
            exports.putShort(modules.size)
            for (module in modules) {
                exports.putShort(symbolTable.addConstantModule(module).index)
            }
        }
        exportsCount++
    }

    override fun visitOpen(packaze: String?, access: Int, vararg modules: String?) {
        opens.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access)
        if (modules == null) {
            opens.putShort(0)
        } else {
            opens.putShort(modules.size)
            for (module in modules) {
                opens.putShort(symbolTable.addConstantModule(module).index)
            }
        }
        opensCount++
    }

    override fun visitUse(service: String?) {
        usesIndex.putShort(symbolTable.addConstantClass(service).index)
        usesCount++
    }

    override fun visitProvide(service: String?, vararg providers: String?) {
        provides.putShort(symbolTable.addConstantClass(service).index)
        provides.putShort(providers.size)
        for (provider in providers) {
            provides.putShort(symbolTable.addConstantClass(provider).index)
        }
        providesCount++
    }

    override fun visitEnd() {
        // Nothing to do.
    }

    /**
     * Returns the number of Module, ModulePackages and ModuleMainClass attributes generated by this
     * ModuleWriter.
     *
     * @return the number of Module, ModulePackages and ModuleMainClass attributes (between 1 and 3).
     */
    val attributeCount: Int
        get() = 1 + (if (packageCount > 0) 1 else 0) + if (mainClassIndex > 0) 1 else 0

    /**
     * Returns the size of the Module, ModulePackages and ModuleMainClass attributes generated by this
     * ModuleWriter. Also add the names of these attributes in the constant pool.
     *
     * @return the size in bytes of the Module, ModulePackages and ModuleMainClass attributes.
     */
    fun computeAttributesSize(): Int {
        symbolTable.addConstantUtf8(Constants.MODULE)
        // 6 attribute header bytes, 6 bytes for name, flags and version, and 5 * 2 bytes for counts.
        var size: Int = 22 + requires.length + exports.length + opens.length + usesIndex.length + provides.length
        if (packageCount > 0) {
            symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES)
            // 6 attribute header bytes, and 2 bytes for package_count.
            size += 8 + packageIndex.length
        }
        if (mainClassIndex > 0) {
            symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS)
            // 6 attribute header bytes, and 2 bytes for main_class_index.
            size += 8
        }
        return size
    }

    /**
     * Puts the Module, ModulePackages and ModuleMainClass attributes generated by this ModuleWriter
     * in the given ByteVector.
     *
     * @param output where the attributes must be put.
     */
    fun putAttributes(output: ByteVector) {
        // 6 bytes for name, flags and version, and 5 * 2 bytes for counts.
        val moduleAttributeLength: Int =
            16 + requires.length + exports.length + opens.length + usesIndex.length + provides.length
        output
            .putShort(symbolTable.addConstantUtf8(Constants.MODULE))
            .putInt(moduleAttributeLength)
            .putShort(moduleNameIndex)
            .putShort(moduleFlags)
            .putShort(moduleVersionIndex)
            .putShort(requiresCount)
            .putByteArray(requires.data, 0, requires.length)
            .putShort(exportsCount)
            .putByteArray(exports.data, 0, exports.length)
            .putShort(opensCount)
            .putByteArray(opens.data, 0, opens.length)
            .putShort(usesCount)
            .putByteArray(usesIndex.data, 0, usesIndex.length)
            .putShort(providesCount)
            .putByteArray(provides.data, 0, provides.length)
        if (packageCount > 0) {
            output
                .putShort(symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES))
                .putInt(2 + packageIndex.length)
                .putShort(packageCount)
                .putByteArray(packageIndex.data, 0, packageIndex.length)
        }
        if (mainClassIndex > 0) {
            output
                .putShort(symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS))
                .putInt(2)
                .putShort(mainClassIndex)
        }
    }

    init {
        this.symbolTable = symbolTable
        moduleNameIndex = name
        moduleFlags = access
        moduleVersionIndex = version
        requires = ByteVector()
        exports = ByteVector()
        opens = ByteVector()
        usesIndex = ByteVector()
        provides = ByteVector()
        packageIndex = ByteVector()
    }
}
