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
 * An entry of the constant pool, of the BootstrapMethods attribute, or of the (ASM specific) type
 * table of a class.
 *
 * @see [JVMS
 * 4.4](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.4)
 *
 * @see [JVMS
 * 4.7.23](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.23)
 *
 * @author Eric Bruneton
 */
abstract class Symbol
/**
 * Constructs a new Symbol. This constructor can't be used directly because the Symbol class is
 * abstract. Instead, use the factory methods of the [SymbolTable] class.
 *
 * @param index the symbol index in the constant pool, in the BootstrapMethods attribute, or in
 * the (ASM specific) type table of a class (depending on 'tag').
 * @param tag the symbol type. Must be one of the static tag values defined in this class.
 * @param owner The internal name of the symbol's owner class. Maybe null.
 * @param name The name of the symbol's corresponding class field or method. Maybe null.
 * @param value The string value of this symbol. Maybe null.
 * @param data The numeric value of this symbol.
 */(
    /**
     * The index of this symbol in the constant pool, in the BootstrapMethods attribute, or in the
     * (ASM specific) type table of a class (depending on the [.tag] value).
     */
    val index: Int,
    /**
     * A tag indicating the type of this symbol. Must be one of the static tag values defined in this
     * class.
     */
    val tag: Int,
    /**
     * The internal name of the owner class of this symbol. Only used for [ ][.CONSTANT_FIELDREF_TAG], [.CONSTANT_METHODREF_TAG], [ ][.CONSTANT_INTERFACE_METHODREF_TAG], and [.CONSTANT_METHOD_HANDLE_TAG] symbols.
     */
    val owner: String?,
    /**
     * The name of the class field or method corresponding to this symbol. Only used for [ ][.CONSTANT_FIELDREF_TAG], [.CONSTANT_METHODREF_TAG], [ ][.CONSTANT_INTERFACE_METHODREF_TAG], [.CONSTANT_NAME_AND_TYPE_TAG], [ ][.CONSTANT_METHOD_HANDLE_TAG], [.CONSTANT_DYNAMIC_TAG] and [ ][.CONSTANT_INVOKE_DYNAMIC_TAG] symbols.
     */
    val name: String?,
    /**
     * The string value of this symbol. This is:
     *
     *
     *  * a field or method descriptor for [.CONSTANT_FIELDREF_TAG], [       ][.CONSTANT_METHODREF_TAG], [.CONSTANT_INTERFACE_METHODREF_TAG], [       ][.CONSTANT_NAME_AND_TYPE_TAG], [.CONSTANT_METHOD_HANDLE_TAG], [       ][.CONSTANT_METHOD_TYPE_TAG], [.CONSTANT_DYNAMIC_TAG] and [       ][.CONSTANT_INVOKE_DYNAMIC_TAG] symbols,
     *  * an arbitrary string for [.CONSTANT_UTF8_TAG] and [.CONSTANT_STRING_TAG]
     * symbols,
     *  * an internal class name for [.CONSTANT_CLASS_TAG], [.TYPE_TAG] and [       ][.UNINITIALIZED_TYPE_TAG] symbols,
     *  * null for the other types of symbol.
     *
     */
    val value: String?,
    /**
     * The numeric value of this symbol. This is:
     *
     *
     *  * the symbol's value for [.CONSTANT_INTEGER_TAG],[.CONSTANT_FLOAT_TAG], [       ][.CONSTANT_LONG_TAG], [.CONSTANT_DOUBLE_TAG],
     *  * the CONSTANT_MethodHandle_info reference_kind field value for [       ][.CONSTANT_METHOD_HANDLE_TAG] symbols,
     *  * the CONSTANT_InvokeDynamic_info bootstrap_method_attr_index field value for [       ][.CONSTANT_INVOKE_DYNAMIC_TAG] symbols,
     *  * the offset of a bootstrap method in the BootstrapMethods boostrap_methods array, for
     * [.CONSTANT_DYNAMIC_TAG] or [.BOOTSTRAP_METHOD_TAG] symbols,
     *  * the bytecode offset of the NEW instruction that created an [       ][Frame.ITEM_UNINITIALIZED] type for [.UNINITIALIZED_TYPE_TAG] symbols,
     *  * the indices (in the class' type table) of two [.TYPE_TAG] source types for [       ][.MERGED_TYPE_TAG] symbols,
     *  * 0 for the other types of symbol.
     *
     */
    val data: Long
) {
    // Instance fields.
    /**
     * Additional information about this symbol, generally computed lazily. *Warning: the value of
     * this field is ignored when comparing Symbol instances* (to avoid duplicate entries in a
     * SymbolTable). Therefore, this field should only contain data that can be computed from the
     * other fields of this class. It contains:
     *
     *
     *  * the [Type.getArgumentsAndReturnSizes] of the symbol's method descriptor for [       ][.CONSTANT_METHODREF_TAG], [.CONSTANT_INTERFACE_METHODREF_TAG] and [       ][.CONSTANT_INVOKE_DYNAMIC_TAG] symbols,
     *  * the index in the InnerClasses_attribute 'classes' array (plus one) corresponding to this
     * class, for [.CONSTANT_CLASS_TAG] symbols,
     *  * the index (in the class' type table) of the merged type of the two source types for
     * [.MERGED_TYPE_TAG] symbols,
     *  * 0 for the other types of symbol, or if this field has not been computed yet.
     *
     */
    var info = 0

    /**
     * Returns the result [Type.getArgumentsAndReturnSizes] on [.value].
     *
     * @return the result [Type.getArgumentsAndReturnSizes] on [.value] (memoized in
     * [.info] for efficiency). This should only be used for [     ][.CONSTANT_METHODREF_TAG], [.CONSTANT_INTERFACE_METHODREF_TAG] and [     ][.CONSTANT_INVOKE_DYNAMIC_TAG] symbols.
     */
    fun getArgumentsAndReturnSizes(): Int {
        if (info == 0) {
            info = Type.getArgumentsAndReturnSizes(value)
        }
        return info
    }

    companion object {
        // Tag values for the constant pool entries (using the same order as in the JVMS).
        /** The tag value of CONSTANT_Class_info JVMS structures.  */
        const val CONSTANT_CLASS_TAG = 7

        /** The tag value of CONSTANT_Fieldref_info JVMS structures.  */
        const val CONSTANT_FIELDREF_TAG = 9

        /** The tag value of CONSTANT_Methodref_info JVMS structures.  */
        const val CONSTANT_METHODREF_TAG = 10

        /** The tag value of CONSTANT_InterfaceMethodref_info JVMS structures.  */
        const val CONSTANT_INTERFACE_METHODREF_TAG = 11

        /** The tag value of CONSTANT_String_info JVMS structures.  */
        const val CONSTANT_STRING_TAG = 8

        /** The tag value of CONSTANT_Integer_info JVMS structures.  */
        const val CONSTANT_INTEGER_TAG = 3

        /** The tag value of CONSTANT_Float_info JVMS structures.  */
        const val CONSTANT_FLOAT_TAG = 4

        /** The tag value of CONSTANT_Long_info JVMS structures.  */
        const val CONSTANT_LONG_TAG = 5

        /** The tag value of CONSTANT_Double_info JVMS structures.  */
        const val CONSTANT_DOUBLE_TAG = 6

        /** The tag value of CONSTANT_NameAndType_info JVMS structures.  */
        const val CONSTANT_NAME_AND_TYPE_TAG = 12

        /** The tag value of CONSTANT_Utf8_info JVMS structures.  */
        const val CONSTANT_UTF8_TAG = 1

        /** The tag value of CONSTANT_MethodHandle_info JVMS structures.  */
        const val CONSTANT_METHOD_HANDLE_TAG = 15

        /** The tag value of CONSTANT_MethodType_info JVMS structures.  */
        const val CONSTANT_METHOD_TYPE_TAG = 16

        /** The tag value of CONSTANT_Dynamic_info JVMS structures.  */
        const val CONSTANT_DYNAMIC_TAG = 17

        /** The tag value of CONSTANT_InvokeDynamic_info JVMS structures.  */
        const val CONSTANT_INVOKE_DYNAMIC_TAG = 18

        /** The tag value of CONSTANT_Module_info JVMS structures.  */
        const val CONSTANT_MODULE_TAG = 19

        /** The tag value of CONSTANT_Package_info JVMS structures.  */
        const val CONSTANT_PACKAGE_TAG = 20
        // Tag values for the BootstrapMethods attribute entries (ASM specific tag).
        /** The tag value of the BootstrapMethods attribute entries.  */
        const val BOOTSTRAP_METHOD_TAG = 64
        // Tag values for the type table entries (ASM specific tags).
        /** The tag value of a normal type entry in the (ASM specific) type table of a class.  */
        const val TYPE_TAG = 128

        /**
         * The tag value of an [Frame.ITEM_UNINITIALIZED] type entry in the type table of a class.
         */
        const val UNINITIALIZED_TYPE_TAG = 129

        /** The tag value of a merged type entry in the (ASM specific) type table of a class.  */
        const val MERGED_TYPE_TAG = 130
    }
}
