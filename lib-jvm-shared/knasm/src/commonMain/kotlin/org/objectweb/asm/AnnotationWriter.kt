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
 * An [AnnotationVisitor] that generates a corresponding 'annotation' or 'type_annotation'
 * structure, as defined in the Java Virtual Machine Specification (JVMS). AnnotationWriter
 * instances can be chained in a doubly linked list, from which Runtime[In]Visible[Type]Annotations
 * attributes can be generated with the [.putAnnotations] method. Similarly, arrays of such
 * lists can be used to generate Runtime[In]VisibleParameterAnnotations attributes.
 *
 * @see [JVMS
 * 4.7.16](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.16)
 *
 * @see [JVMS
 * 4.7.20](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.20)
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
internal class AnnotationWriter(
    symbolTable: SymbolTable,
    useNamedValues: Boolean,
    annotation: ByteVector,
    previousAnnotation: AnnotationWriter?
) : AnnotationVisitor( /* latest api = */Opcodes.ASM9) {
    /** Where the constants used in this AnnotationWriter must be stored.  */
    private val symbolTable: SymbolTable

    /**
     * Whether values are named or not. AnnotationWriter instances used for annotation default and
     * annotation arrays use unnamed values (i.e. they generate an 'element_value' structure for each
     * value, instead of an element_name_index followed by an element_value).
     */
    private val useNamedValues: Boolean

    /**
     * The 'annotation' or 'type_annotation' JVMS structure corresponding to the annotation values
     * visited so far. All the fields of these structures, except the last one - the
     * element_value_pairs array, must be set before this ByteVector is passed to the constructor
     * (num_element_value_pairs can be set to 0, it is reset to the correct value in [ ][.visitEnd]). The element_value_pairs array is filled incrementally in the various visit()
     * methods.
     *
     *
     * Note: as an exception to the above rules, for AnnotationDefault attributes (which contain a
     * single element_value by definition), this ByteVector is initially empty when passed to the
     * constructor, and [.numElementValuePairsOffset] is set to -1.
     */
    private val annotation: ByteVector

    /**
     * The offset in [.annotation] where [.numElementValuePairs] must be stored (or -1 for
     * the case of AnnotationDefault attributes).
     */
    private val numElementValuePairsOffset: Int

    /** The number of element value pairs visited so far.  */
    private var numElementValuePairs = 0

    /**
     * The previous AnnotationWriter. This field is used to store the list of annotations of a
     * Runtime[In]Visible[Type]Annotations attribute. It is unused for nested or array annotations
     * (annotation values of annotation type), or for AnnotationDefault attributes.
     */
    private val previousAnnotation: AnnotationWriter?

    /**
     * The next AnnotationWriter. This field is used to store the list of annotations of a
     * Runtime[In]Visible[Type]Annotations attribute. It is unused for nested or array annotations
     * (annotation values of annotation type), or for AnnotationDefault attributes.
     */
    private var nextAnnotation: AnnotationWriter? = null

    // -----------------------------------------------------------------------------------------------
    // Implementation of the AnnotationVisitor abstract class
    // -----------------------------------------------------------------------------------------------
    override fun visit(name: String?, value: Any?) {
        // Case of an element_value with a const_value_index, class_info_index or array_index field.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
        ++numElementValuePairs
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name))
        }
        if (value is String) {
            annotation.put12('s'.toInt(), symbolTable.addConstantUtf8(value))
        } else if (value is Byte) {
            annotation.put12('B'.toInt(), symbolTable.addConstantInteger(value.toByte().toInt()).index)
        } else if (value is Boolean) {
            val booleanValue = if (value) 1 else 0
            annotation.put12('Z'.toInt(), symbolTable.addConstantInteger(booleanValue).index)
        } else if (value is Char) {
            annotation.put12('C'.toInt(), symbolTable.addConstantInteger(value.toChar().toInt()).index)
        } else if (value is Short) {
            annotation.put12('S'.toInt(), symbolTable.addConstantInteger(value.toShort().toInt()).index)
        } else if (value is Type) {
            annotation.put12('c'.toInt(), symbolTable.addConstantUtf8(value.descriptor))
        } else if (value is ByteArray) {
            val byteArray = value
            annotation.put12('['.toInt(), byteArray.size)
            for (byteValue in byteArray) {
                annotation.put12('B'.toInt(), symbolTable.addConstantInteger(byteValue.toInt()).index)
            }
        } else if (value is BooleanArray) {
            val booleanArray = value
            annotation.put12('['.toInt(), booleanArray.size)
            for (booleanValue in booleanArray) {
                annotation.put12('Z'.toInt(), symbolTable.addConstantInteger(if (booleanValue) 1 else 0).index)
            }
        } else if (value is ShortArray) {
            val shortArray = value
            annotation.put12('['.toInt(), shortArray.size)
            for (shortValue in shortArray) {
                annotation.put12('S'.toInt(), symbolTable.addConstantInteger(shortValue.toInt()).index)
            }
        } else if (value is CharArray) {
            val charArray = value
            annotation.put12('['.toInt(), charArray.size)
            for (charValue in charArray) {
                annotation.put12('C'.toInt(), symbolTable.addConstantInteger(charValue.toInt()).index)
            }
        } else if (value is IntArray) {
            val intArray = value
            annotation.put12('['.toInt(), intArray.size)
            for (intValue in intArray) {
                annotation.put12('I'.toInt(), symbolTable.addConstantInteger(intValue).index)
            }
        } else if (value is LongArray) {
            val longArray = value
            annotation.put12('['.toInt(), longArray.size)
            for (longValue in longArray) {
                annotation.put12('J'.toInt(), symbolTable.addConstantLong(longValue).index)
            }
        } else if (value is FloatArray) {
            val floatArray = value
            annotation.put12('['.toInt(), floatArray.size)
            for (floatValue in floatArray) {
                annotation.put12('F'.toInt(), symbolTable.addConstantFloat(floatValue).index)
            }
        } else if (value is DoubleArray) {
            val doubleArray = value
            annotation.put12('['.toInt(), doubleArray.size)
            for (doubleValue in doubleArray) {
                annotation.put12('D'.toInt(), symbolTable.addConstantDouble(doubleValue).index)
            }
        } else {
            val symbol: Symbol = symbolTable.addConstant(value)
            annotation.put12(".s.IFJDCS"[symbol.tag].toInt(), symbol.index)
        }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        // Case of an element_value with an enum_const_value field.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
        ++numElementValuePairs
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name))
        }
        annotation
            .put12('e'.toInt(), symbolTable.addConstantUtf8(descriptor))
            .putShort(symbolTable.addConstantUtf8(value))
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor {
        // Case of an element_value with an annotation_value field.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
        ++numElementValuePairs
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name))
        }
        // Write tag and type_index, and reserve 2 bytes for num_element_value_pairs.
        annotation.put12('@'.toInt(), symbolTable.addConstantUtf8(descriptor)).putShort(0)
        return AnnotationWriter(symbolTable,  /* useNamedValues = */true, annotation, null)
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        // Case of an element_value with an array_value field.
        // https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1
        ++numElementValuePairs
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name))
        }
        // Write tag, and reserve 2 bytes for num_values. Here we take advantage of the fact that the
        // end of an element_value of array type is similar to the end of an 'annotation' structure: an
        // unsigned short num_values followed by num_values element_value, versus an unsigned short
        // num_element_value_pairs, followed by num_element_value_pairs { element_name_index,
        // element_value } tuples. This allows us to use an AnnotationWriter with unnamed values to
        // visit the array elements. Its num_element_value_pairs will correspond to the number of array
        // elements and will be stored in what is in fact num_values.
        annotation.put12('['.toInt(), 0)
        return AnnotationWriter(symbolTable,  /* useNamedValues = */false, annotation, null)
    }

    override fun visitEnd() {
        if (numElementValuePairsOffset != -1) {
            val data: ByteArray = annotation.data
            data[numElementValuePairsOffset] = (numElementValuePairs ushr 8).toByte()
            data[numElementValuePairsOffset + 1] = numElementValuePairs.toByte()
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the size of a Runtime[In]Visible[Type]Annotations attribute containing this annotation
     * and all its *predecessors* (see [.previousAnnotation]. Also adds the attribute name
     * to the constant pool of the class (if not null).
     *
     * @param attributeName one of "Runtime[In]Visible[Type]Annotations", or null.
     * @return the size in bytes of a Runtime[In]Visible[Type]Annotations attribute containing this
     * annotation and all its predecessors. This includes the size of the attribute_name_index and
     * attribute_length fields.
     */
    fun computeAnnotationsSize(attributeName: String?): Int {
        if (attributeName != null) {
            symbolTable.addConstantUtf8(attributeName)
        }
        // The attribute_name_index, attribute_length and num_annotations fields use 8 bytes.
        var attributeSize = 8
        var annotationWriter:AnnotationWriter? = this
        while (annotationWriter != null) {
            attributeSize += annotationWriter.annotation.length
            annotationWriter = annotationWriter.previousAnnotation
        }
        return attributeSize
    }

    /**
     * Puts a Runtime[In]Visible[Type]Annotations attribute containing this annotations and all its
     * *predecessors* (see [.previousAnnotation] in the given ByteVector. Annotations are
     * put in the same order they have been visited.
     *
     * @param attributeNameIndex the constant pool index of the attribute name (one of
     * "Runtime[In]Visible[Type]Annotations").
     * @param output where the attribute must be put.
     */
    fun putAnnotations(attributeNameIndex: Int, output: ByteVector) {
        var attributeLength = 2 // For num_annotations.
        var numAnnotations = 0
        var annotationWriter: AnnotationWriter? = this
        var firstAnnotation: AnnotationWriter? = null
        while (annotationWriter != null) {
            // In case the user forgot to call visitEnd().
            annotationWriter.visitEnd()
            attributeLength += annotationWriter.annotation.length
            numAnnotations++
            firstAnnotation = annotationWriter
            annotationWriter = annotationWriter.previousAnnotation
        }
        output.putShort(attributeNameIndex)
        output.putInt(attributeLength)
        output.putShort(numAnnotations)
        annotationWriter = firstAnnotation
        while (annotationWriter != null) {
            output.putByteArray(annotationWriter.annotation.data, 0, annotationWriter.annotation.length)
            annotationWriter = annotationWriter.nextAnnotation
        }
    }

    companion object {
        /**
         * Creates a new [AnnotationWriter] using named values.
         *
         * @param symbolTable where the constants used in this AnnotationWriter must be stored.
         * @param descriptor the class descriptor of the annotation class.
         * @param previousAnnotation the previously visited annotation of the
         * Runtime[In]Visible[Type]Annotations attribute to which this annotation belongs, or
         * null in other cases (e.g. nested or array annotations).
         * @return a new [AnnotationWriter] for the given annotation descriptor.
         */
        fun create(
            symbolTable: SymbolTable,
            descriptor: String?,
            previousAnnotation: AnnotationWriter?
        ): AnnotationWriter {
            // Create a ByteVector to hold an 'annotation' JVMS structure.
            // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.
            val annotation = ByteVector()
            // Write type_index and reserve space for num_element_value_pairs.
            annotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0)
            return AnnotationWriter(
                symbolTable,  /* useNamedValues = */true, annotation, previousAnnotation!!)
        }

        /**
         * Creates a new [AnnotationWriter] using named values.
         *
         * @param symbolTable where the constants used in this AnnotationWriter must be stored.
         * @param typeRef a reference to the annotated type. The sort of this type reference must be
         * [TypeReference.CLASS_TYPE_PARAMETER], [     ][TypeReference.CLASS_TYPE_PARAMETER_BOUND] or [TypeReference.CLASS_EXTENDS]. See
         * [TypeReference].
         * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
         * static inner type within 'typeRef'. May be null if the annotation targets
         * 'typeRef' as a whole.
         * @param descriptor the class descriptor of the annotation class.
         * @param previousAnnotation the previously visited annotation of the
         * Runtime[In]Visible[Type]Annotations attribute to which this annotation belongs, or
         * null in other cases (e.g. nested or array annotations).
         * @return a new [AnnotationWriter] for the given type annotation reference and descriptor.
         */
        fun create(
            symbolTable: SymbolTable,
            typeRef: Int,
            typePath: TypePath?,
            descriptor: String?,
            previousAnnotation: AnnotationWriter?
        ): AnnotationWriter {
            // Create a ByteVector to hold a 'type_annotation' JVMS structure.
            // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.
            val typeAnnotation = ByteVector()
            // Write target_type, target_info, and target_path.
            TypeReference.putTarget(typeRef, typeAnnotation)
            TypePath.put(typePath, typeAnnotation)
            // Write type_index and reserve space for num_element_value_pairs.
            typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0)
            return AnnotationWriter(
                symbolTable,  /* useNamedValues = */true, typeAnnotation, previousAnnotation!!)
        }

        /**
         * Returns the size of the Runtime[In]Visible[Type]Annotations attributes containing the given
         * annotations and all their *predecessors* (see [.previousAnnotation]. Also adds the
         * attribute names to the constant pool of the class (if not null).
         *
         * @param lastRuntimeVisibleAnnotation The last runtime visible annotation of a field, method or
         * class. The previous ones can be accessed with the [.previousAnnotation] field. May be
         * null.
         * @param lastRuntimeInvisibleAnnotation The last runtime invisible annotation of this a field,
         * method or class. The previous ones can be accessed with the [.previousAnnotation]
         * field. May be null.
         * @param lastRuntimeVisibleTypeAnnotation The last runtime visible type annotation of this a
         * field, method or class. The previous ones can be accessed with the [     ][.previousAnnotation] field. May be null.
         * @param lastRuntimeInvisibleTypeAnnotation The last runtime invisible type annotation of a
         * field, method or class field. The previous ones can be accessed with the [     ][.previousAnnotation] field. May be null.
         * @return the size in bytes of a Runtime[In]Visible[Type]Annotations attribute containing the
         * given annotations and all their predecessors. This includes the size of the
         * attribute_name_index and attribute_length fields.
         */
        fun computeAnnotationsSize(
            lastRuntimeVisibleAnnotation: AnnotationWriter?,
            lastRuntimeInvisibleAnnotation: AnnotationWriter?,
            lastRuntimeVisibleTypeAnnotation: AnnotationWriter?,
            lastRuntimeInvisibleTypeAnnotation: AnnotationWriter?
        ): Int {
            var size = 0
            if (lastRuntimeVisibleAnnotation != null) {
                size += lastRuntimeVisibleAnnotation.computeAnnotationsSize(
                    Constants.RUNTIME_VISIBLE_ANNOTATIONS)
            }
            if (lastRuntimeInvisibleAnnotation != null) {
                size += lastRuntimeInvisibleAnnotation.computeAnnotationsSize(
                    Constants.RUNTIME_INVISIBLE_ANNOTATIONS)
            }
            if (lastRuntimeVisibleTypeAnnotation != null) {
                size += lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
                    Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)
            }
            if (lastRuntimeInvisibleTypeAnnotation != null) {
                size += lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
                    Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)
            }
            return size
        }

        /**
         * Puts the Runtime[In]Visible[Type]Annotations attributes containing the given annotations and
         * all their *predecessors* (see [.previousAnnotation] in the given ByteVector.
         * Annotations are put in the same order they have been visited.
         *
         * @param symbolTable where the constants used in the AnnotationWriter instances are stored.
         * @param lastRuntimeVisibleAnnotation The last runtime visible annotation of a field, method or
         * class. The previous ones can be accessed with the [.previousAnnotation] field. May be
         * null.
         * @param lastRuntimeInvisibleAnnotation The last runtime invisible annotation of this a field,
         * method or class. The previous ones can be accessed with the [.previousAnnotation]
         * field. May be null.
         * @param lastRuntimeVisibleTypeAnnotation The last runtime visible type annotation of this a
         * field, method or class. The previous ones can be accessed with the [     ][.previousAnnotation] field. May be null.
         * @param lastRuntimeInvisibleTypeAnnotation The last runtime invisible type annotation of a
         * field, method or class field. The previous ones can be accessed with the [     ][.previousAnnotation] field. May be null.
         * @param output where the attributes must be put.
         */
        fun putAnnotations(
            symbolTable: SymbolTable,
            lastRuntimeVisibleAnnotation: AnnotationWriter?,
            lastRuntimeInvisibleAnnotation: AnnotationWriter?,
            lastRuntimeVisibleTypeAnnotation: AnnotationWriter?,
            lastRuntimeInvisibleTypeAnnotation: AnnotationWriter?,
            output: ByteVector
        ) {
            lastRuntimeVisibleAnnotation?.putAnnotations(
                symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_ANNOTATIONS), output)
            lastRuntimeInvisibleAnnotation?.putAnnotations(
                symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_ANNOTATIONS), output)
            lastRuntimeVisibleTypeAnnotation?.putAnnotations(
                symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output)
            lastRuntimeInvisibleTypeAnnotation?.putAnnotations(
                symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output)
        }

        /**
         * Returns the size of a Runtime[In]VisibleParameterAnnotations attribute containing all the
         * annotation lists from the given AnnotationWriter sub-array. Also adds the attribute name to the
         * constant pool of the class.
         *
         * @param attributeName one of "Runtime[In]VisibleParameterAnnotations".
         * @param annotationWriters an array of AnnotationWriter lists (designated by their *last*
         * element).
         * @param annotableParameterCount the number of elements in annotationWriters to take into account
         * (elements [0..annotableParameterCount[ are taken into account).
         * @return the size in bytes of a Runtime[In]VisibleParameterAnnotations attribute corresponding
         * to the given sub-array of AnnotationWriter lists. This includes the size of the
         * attribute_name_index and attribute_length fields.
         */
        fun computeParameterAnnotationsSize(
            attributeName: String?,
            annotationWriters: Array<AnnotationWriter?>,
            annotableParameterCount: Int
        ): Int {
            // Note: attributeName is added to the constant pool by the call to computeAnnotationsSize
            // below. This assumes that there is at least one non-null element in the annotationWriters
            // sub-array (which is ensured by the lazy instantiation of this array in MethodWriter).
            // The attribute_name_index, attribute_length and num_parameters fields use 7 bytes, and each
            // element of the parameter_annotations array uses 2 bytes for its num_annotations field.
            var attributeSize = 7 + 2 * annotableParameterCount
            for (i in 0 until annotableParameterCount) {
                val annotationWriter = annotationWriters[i]
                attributeSize += if (annotationWriter == null) 0 else annotationWriter.computeAnnotationsSize(
                    attributeName) - 8
            }
            return attributeSize
        }

        /**
         * Puts a Runtime[In]VisibleParameterAnnotations attribute containing all the annotation lists
         * from the given AnnotationWriter sub-array in the given ByteVector.
         *
         * @param attributeNameIndex constant pool index of the attribute name (one of
         * Runtime[In]VisibleParameterAnnotations).
         * @param annotationWriters an array of AnnotationWriter lists (designated by their *last*
         * element).
         * @param annotableParameterCount the number of elements in annotationWriters to put (elements
         * [0..annotableParameterCount[ are put).
         * @param output where the attribute must be put.
         */
        fun putParameterAnnotations(
            attributeNameIndex: Int,
            annotationWriters: Array<AnnotationWriter?>,
            annotableParameterCount: Int,
            output: ByteVector
        ) {
            // The num_parameters field uses 1 byte, and each element of the parameter_annotations array
            // uses 2 bytes for its num_annotations field.
            var attributeLength = 1 + 2 * annotableParameterCount
            for (i in 0 until annotableParameterCount) {
                val annotationWriter = annotationWriters[i]
                attributeLength += if (annotationWriter == null) 0 else annotationWriter.computeAnnotationsSize(null) - 8
            }
            output.putShort(attributeNameIndex)
            output.putInt(attributeLength)
            output.putByte(annotableParameterCount)
            for (i in 0 until annotableParameterCount) {
                var annotationWriter = annotationWriters[i]
                var firstAnnotation: AnnotationWriter? = null
                var numAnnotations = 0
                while (annotationWriter != null) {
                    // In case user the forgot to call visitEnd().
                    annotationWriter.visitEnd()
                    numAnnotations++
                    firstAnnotation = annotationWriter
                    annotationWriter = annotationWriter.previousAnnotation
                }
                output.putShort(numAnnotations)
                annotationWriter = firstAnnotation
                while (annotationWriter != null) {
                    output.putByteArray(
                        annotationWriter.annotation.data, 0, annotationWriter.annotation.length)
                    annotationWriter = annotationWriter.nextAnnotation
                }
            }
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Constructors and factories
    // -----------------------------------------------------------------------------------------------
    /**
     * Constructs a new [AnnotationWriter].
     *
     * @param symbolTable where the constants used in this AnnotationWriter must be stored.
     * @param useNamedValues whether values are named or not. AnnotationDefault and annotation arrays
     * use unnamed values.
     * @param annotation where the 'annotation' or 'type_annotation' JVMS structure corresponding to
     * the visited content must be stored. This ByteVector must already contain all the fields of
     * the structure except the last one (the element_value_pairs array).
     * @param previousAnnotation the previously visited annotation of the
     * Runtime[In]Visible[Type]Annotations attribute to which this annotation belongs, or
     * null in other cases (e.g. nested or array annotations).
     */
    init {
        this.symbolTable = symbolTable
        this.useNamedValues = useNamedValues
        this.annotation = annotation
        // By hypothesis, num_element_value_pairs is stored in the last unsigned short of 'annotation'.
        numElementValuePairsOffset = if (annotation.length === 0) -1 else annotation.length - 2
        this.previousAnnotation = previousAnnotation
        if (previousAnnotation != null) {
            previousAnnotation.nextAnnotation = this
        }
    }
}
