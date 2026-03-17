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
 * Defines additional JVM opcodes, access flags and constants which are not part of the ASM public
 * API.
 *
 * @see [JVMS 6](https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html)
 *
 * @author Eric Bruneton
 */
internal object Constants {
    // The ClassFile attribute names, in the order they are defined in
    // https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.7-300.
    const val CONSTANT_VALUE = "ConstantValue"
    const val CODE = "Code"
    const val STACK_MAP_TABLE = "StackMapTable"
    const val EXCEPTIONS = "Exceptions"
    const val INNER_CLASSES = "InnerClasses"
    const val ENCLOSING_METHOD = "EnclosingMethod"
    const val SYNTHETIC = "Synthetic"
    const val SIGNATURE = "Signature"
    const val SOURCE_FILE = "SourceFile"
    const val SOURCE_DEBUG_EXTENSION = "SourceDebugExtension"
    const val LINE_NUMBER_TABLE = "LineNumberTable"
    const val LOCAL_VARIABLE_TABLE = "LocalVariableTable"
    const val LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable"
    const val DEPRECATED = "Deprecated"
    const val RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations"
    const val RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations"
    const val RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations"
    const val RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations"
    const val RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations"
    const val RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations"
    const val ANNOTATION_DEFAULT = "AnnotationDefault"
    const val BOOTSTRAP_METHODS = "BootstrapMethods"
    const val METHOD_PARAMETERS = "MethodParameters"
    const val MODULE = "Module"
    const val MODULE_PACKAGES = "ModulePackages"
    const val MODULE_MAIN_CLASS = "ModuleMainClass"
    const val NEST_HOST = "NestHost"
    const val NEST_MEMBERS = "NestMembers"
    const val PERMITTED_SUBCLASSES = "PermittedSubclasses"
    const val RECORD = "Record"

    // ASM specific access flags.
    // WARNING: the 16 least significant bits must NOT be used, to avoid conflicts with standard
    // access flags, and also to make sure that these flags are automatically filtered out when
    // written in class files (because access flags are stored using 16 bits only).
    const val ACC_CONSTRUCTOR = 0x40000 // method access flag.
    // ASM specific stack map frame types, used in {@link ClassVisitor#visitFrame}.
    /**
     * A frame inserted between already existing frames. This internal stack map frame type (in
     * addition to the ones declared in [Opcodes]) can only be used if the frame content can be
     * computed from the previous existing frame and from the instructions between this existing frame
     * and the inserted one, without any knowledge of the type hierarchy. This kind of frame is only
     * used when an unconditional jump is inserted in a method while expanding an ASM specific
     * instruction. Keep in sync with Opcodes.java.
     */
    const val F_INSERT = 256

    // The JVM opcode values which are not part of the ASM public API.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html.
    const val LDC_W = 19
    const val LDC2_W = 20
    const val ILOAD_0 = 26
    const val ILOAD_1 = 27
    const val ILOAD_2 = 28
    const val ILOAD_3 = 29
    const val LLOAD_0 = 30
    const val LLOAD_1 = 31
    const val LLOAD_2 = 32
    const val LLOAD_3 = 33
    const val FLOAD_0 = 34
    const val FLOAD_1 = 35
    const val FLOAD_2 = 36
    const val FLOAD_3 = 37
    const val DLOAD_0 = 38
    const val DLOAD_1 = 39
    const val DLOAD_2 = 40
    const val DLOAD_3 = 41
    const val ALOAD_0 = 42
    const val ALOAD_1 = 43
    const val ALOAD_2 = 44
    const val ALOAD_3 = 45
    const val ISTORE_0 = 59
    const val ISTORE_1 = 60
    const val ISTORE_2 = 61
    const val ISTORE_3 = 62
    const val LSTORE_0 = 63
    const val LSTORE_1 = 64
    const val LSTORE_2 = 65
    const val LSTORE_3 = 66
    const val FSTORE_0 = 67
    const val FSTORE_1 = 68
    const val FSTORE_2 = 69
    const val FSTORE_3 = 70
    const val DSTORE_0 = 71
    const val DSTORE_1 = 72
    const val DSTORE_2 = 73
    const val DSTORE_3 = 74
    const val ASTORE_0 = 75
    const val ASTORE_1 = 76
    const val ASTORE_2 = 77
    const val ASTORE_3 = 78
    const val WIDE = 196
    const val GOTO_W = 200
    const val JSR_W = 201

    // Constants to convert between normal and wide jump instructions.
    // The delta between the GOTO_W and JSR_W opcodes and GOTO and JUMP.
    val WIDE_JUMP_OPCODE_DELTA: Int = GOTO_W - Opcodes.GOTO

    // Constants to convert JVM opcodes to the equivalent ASM specific opcodes, and vice versa.
    // The delta between the ASM_IFEQ, ..., ASM_IF_ACMPNE, ASM_GOTO and ASM_JSR opcodes
    // and IFEQ, ..., IF_ACMPNE, GOTO and JSR.
    const val ASM_OPCODE_DELTA = 49

    // The delta between the ASM_IFNULL and ASM_IFNONNULL opcodes and IFNULL and IFNONNULL.
    const val ASM_IFNULL_OPCODE_DELTA = 20

    // ASM specific opcodes, used for long forward jump instructions.
    val ASM_IFEQ: Int = Opcodes.IFEQ + ASM_OPCODE_DELTA
    val ASM_IFNE: Int = Opcodes.IFNE + ASM_OPCODE_DELTA
    val ASM_IFLT: Int = Opcodes.IFLT + ASM_OPCODE_DELTA
    val ASM_IFGE: Int = Opcodes.IFGE + ASM_OPCODE_DELTA
    val ASM_IFGT: Int = Opcodes.IFGT + ASM_OPCODE_DELTA
    val ASM_IFLE: Int = Opcodes.IFLE + ASM_OPCODE_DELTA
    val ASM_IF_ICMPEQ: Int = Opcodes.IF_ICMPEQ + ASM_OPCODE_DELTA
    val ASM_IF_ICMPNE: Int = Opcodes.IF_ICMPNE + ASM_OPCODE_DELTA
    val ASM_IF_ICMPLT: Int = Opcodes.IF_ICMPLT + ASM_OPCODE_DELTA
    val ASM_IF_ICMPGE: Int = Opcodes.IF_ICMPGE + ASM_OPCODE_DELTA
    val ASM_IF_ICMPGT: Int = Opcodes.IF_ICMPGT + ASM_OPCODE_DELTA
    val ASM_IF_ICMPLE: Int = Opcodes.IF_ICMPLE + ASM_OPCODE_DELTA
    val ASM_IF_ACMPEQ: Int = Opcodes.IF_ACMPEQ + ASM_OPCODE_DELTA
    val ASM_IF_ACMPNE: Int = Opcodes.IF_ACMPNE + ASM_OPCODE_DELTA
    val ASM_GOTO: Int = Opcodes.GOTO + ASM_OPCODE_DELTA
    val ASM_JSR: Int = Opcodes.JSR + ASM_OPCODE_DELTA
    val ASM_IFNULL: Int = Opcodes.IFNULL + ASM_IFNULL_OPCODE_DELTA
    val ASM_IFNONNULL: Int = Opcodes.IFNONNULL + ASM_IFNULL_OPCODE_DELTA
    const val ASM_GOTO_W = 220
    fun checkAsmExperimental(caller: Any) {
        //DO nothing because it's common part
        // TODO write actual function in jvm part
    }

}
