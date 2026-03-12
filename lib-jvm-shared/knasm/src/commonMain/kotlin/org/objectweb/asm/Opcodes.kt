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
 * The JVM opcodes, access flags and array type codes. This interface does not define all the JVM
 * opcodes because some opcodes are automatically handled. For example, the xLOAD and xSTORE opcodes
 * are automatically replaced by xLOAD_n and xSTORE_n opcodes when possible. The xLOAD_n and
 * xSTORE_n opcodes are therefore not defined in this interface. Likewise for LDC, automatically
 * replaced by LDC_W or LDC2_W when necessary, WIDE, GOTO_W and JSR_W.
 *
 * @see [JVMS 6](https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html)
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
// DontCheck(InterfaceIsType): can't be fixed (for backward binary compatibility).
interface Opcodes {
    companion object {
        // ASM API versions.
        const val ASM4 = 4 shl 16 or (0 shl 8)
        const val ASM5 = 5 shl 16 or (0 shl 8)
        const val ASM6 = 6 shl 16 or (0 shl 8)
        const val ASM7 = 7 shl 16 or (0 shl 8)
        const val ASM8 = 8 shl 16 or (0 shl 8)
        const val ASM9 = 9 shl 16 or (0 shl 8)

        /**
         * *Experimental, use at your own risk. This field will be renamed when it becomes stable, this
         * will break existing code using it. Only code compiled with --enable-preview can use this.*
         *
         */
        @Deprecated("This API is experimental.")
        val ASM10_EXPERIMENTAL = 1 shl 24 or (10 shl 16) or (0 shl 8)

        /*
   * Internal flags used to redirect calls to deprecated methods. For instance, if a visitOldStuff
   * method in API_OLD is deprecated and replaced with visitNewStuff in API_NEW, then the
   * redirection should be done as follows:
   *
   * <pre>
   * public class StuffVisitor {
   *   ...
   *
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
   *     // SOURCE_DEPRECATED means "a call from a deprecated method using the old 'api' value".
   *     visitNewStuf(arg | (api &#60; API_NEW ? SOURCE_DEPRECATED : 0), ...);
   *   }
   *
   *   public void visitNewStuff(int argAndSource, ...) {
   *     if (api &#60; API_NEW &#38;&#38; (argAndSource &#38; SOURCE_DEPRECATED) == 0) {
   *       visitOldStuff(argAndSource, ...);
   *     } else {
   *       int arg = argAndSource &#38; ~SOURCE_MASK;
   *       [ do stuff ]
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>If 'api' is equal to API_NEW, there are two cases:
   *
   * <ul>
   *   <li>call visitNewStuff: the redirection test is skipped and 'do stuff' is executed directly.
   *   <li>call visitOldSuff: the source is not set to SOURCE_DEPRECATED before calling
   *       visitNewStuff, but the redirection test is skipped anyway in visitNewStuff, which
   *       directly executes 'do stuff'.
   * </ul>
   *
   * <p>If 'api' is equal to API_OLD, there are two cases:
   *
   * <ul>
   *   <li>call visitOldSuff: the source is set to SOURCE_DEPRECATED before calling visitNewStuff.
   *       Because of this visitNewStuff does not redirect back to visitOldStuff, and instead
   *       executes 'do stuff'.
   *   <li>call visitNewStuff: the call is redirected to visitOldStuff because the source is 0.
   *       visitOldStuff now sets the source to SOURCE_DEPRECATED and calls visitNewStuff back. This
   *       time visitNewStuff does not redirect the call, and instead executes 'do stuff'.
   * </ul>
   *
   * <h1>User subclasses</h1>
   *
   * <p>If a user subclass overrides one of these methods, there are only two cases: either 'api' is
   * API_OLD and visitOldStuff is overridden (and visitNewStuff is not), or 'api' is API_NEW or
   * more, and visitNewStuff is overridden (and visitOldStuff is not). Any other case is a user
   * programming error.
   *
   * <p>If 'api' is equal to API_NEW, the class hierarchy is equivalent to
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) { visitNewStuf(arg, ...); }
   *   public void visitNewStuff(int arg, ...) { [ do stuff ] }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int arg, ...) {
   *     super.visitNewStuff(int arg, ...); // optional
   *     [ do user stuff ]
   *   }
   * }
   * </pre>
   *
   * <p>It is then obvious that whether visitNewStuff or visitOldStuff is called, 'do stuff' and 'do
   * user stuff' will be executed, in this order.
   *
   * <p>If 'api' is equal to API_OLD, the class hierarchy is equivalent to
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
   *     visitNewStuff(arg | SOURCE_DEPRECATED, ...);
   *   }
   *   public void visitNewStuff(int argAndSource...) {
   *     if ((argAndSource & SOURCE_DEPRECATED) == 0) {
   *       visitOldStuff(argAndSource, ...);
   *     } else {
   *       int arg = argAndSource &#38; ~SOURCE_MASK;
   *       [ do stuff ]
   *     }
   *   }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitOldStuff(int arg, ...) {
   *     super.visitOldStuff(int arg, ...); // optional
   *     [ do user stuff ]
   *   }
   * }
   * </pre>
   *
   * <p>and there are two cases:
   *
   * <ul>
   *   <li>call visitOldStuff: in the call to super.visitOldStuff, the source is set to
   *       SOURCE_DEPRECATED and visitNewStuff is called. Here 'do stuff' is run because the source
   *       was previously set to SOURCE_DEPRECATED, and execution eventually returns to
   *       UserStuffVisitor.visitOldStuff, where 'do user stuff' is run.
   *   <li>call visitNewStuff: the call is redirected to UserStuffVisitor.visitOldStuff because the
   *       source is 0. Execution continues as in the previous case, resulting in 'do stuff' and 'do
   *       user stuff' being executed, in this order.
   * </ul>
   *
   * <h1>ASM subclasses</h1>
   *
   * <p>In ASM packages, subclasses of StuffVisitor can typically be sub classed again by the user,
   * and can be used with API_OLD or API_NEW. Because of this, if such a subclass must override
   * visitNewStuff, it must do so in the following way (and must not override visitOldStuff):
   *
   * <pre>
   * public class AsmStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int argAndSource, ...) {
   *     if (api &#60; API_NEW &#38;&#38; (argAndSource &#38; SOURCE_DEPRECATED) == 0) {
   *       super.visitNewStuff(argAndSource, ...);
   *       return;
   *     }
   *     super.visitNewStuff(argAndSource, ...); // optional
   *     int arg = argAndSource &#38; ~SOURCE_MASK;
   *     [ do other stuff ]
   *   }
   * }
   * </pre>
   *
   * <p>If a user class extends this with 'api' equal to API_NEW, the class hierarchy is equivalent
   * to
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) { visitNewStuf(arg, ...); }
   *   public void visitNewStuff(int arg, ...) { [ do stuff ] }
   * }
   * public class AsmStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int arg, ...) {
   *     super.visitNewStuff(arg, ...);
   *     [ do other stuff ]
   *   }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int arg, ...) {
   *     super.visitNewStuff(int arg, ...);
   *     [ do user stuff ]
   *   }
   * }
   * </pre>
   *
   * <p>It is then obvious that whether visitNewStuff or visitOldStuff is called, 'do stuff', 'do
   * other stuff' and 'do user stuff' will be executed, in this order. If, on the other hand, a user
   * class extends AsmStuffVisitor with 'api' equal to API_OLD, the class hierarchy is equivalent to
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
   *     visitNewStuf(arg | SOURCE_DEPRECATED, ...);
   *   }
   *   public void visitNewStuff(int argAndSource, ...) {
   *     if ((argAndSource & SOURCE_DEPRECATED) == 0) {
   *       visitOldStuff(argAndSource, ...);
   *     } else {
   *       int arg = argAndSource &#38; ~SOURCE_MASK;
   *       [ do stuff ]
   *     }
   *   }
   * }
   * public class AsmStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int argAndSource, ...) {
   *     if ((argAndSource &#38; SOURCE_DEPRECATED) == 0) {
   *       super.visitNewStuff(argAndSource, ...);
   *       return;
   *     }
   *     super.visitNewStuff(argAndSource, ...); // optional
   *     int arg = argAndSource &#38; ~SOURCE_MASK;
   *     [ do other stuff ]
   *   }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitOldStuff(int arg, ...) {
   *     super.visitOldStuff(arg, ...);
   *     [ do user stuff ]
   *   }
   * }
   * </pre>
   *
   * <p>and, here again, whether visitNewStuff or visitOldStuff is called, 'do stuff', 'do other
   * stuff' and 'do user stuff' will be executed, in this order (exercise left to the reader).
   *
   * <h1>Notes</h1>
   *
   * <ul>
   *   <li>the SOURCE_DEPRECATED flag is set only if 'api' is API_OLD, just before calling
   *       visitNewStuff. By hypothesis, this method is not overridden by the user. Therefore, user
   *       classes can never see this flag. Only ASM subclasses must take care of extracting the
   *       actual argument value by clearing the source flags.
   *   <li>because the SOURCE_DEPRECATED flag is immediately cleared in the caller, the caller can
   *       call visitOldStuff or visitNewStuff (in 'do stuff' and 'do user stuff') on a delegate
   *       visitor without any risks (breaking the redirection logic, "leaking" the flag, etc).
   *   <li>all the scenarios discussed above are unit tested in MethodVisitorTest.
   * </ul>
   */
        const val SOURCE_DEPRECATED = 0x100
        const val SOURCE_MASK = SOURCE_DEPRECATED

        // Java ClassFile versions (the minor version is stored in the 16 most significant bits, and the
        // major version in the 16 least significant bits).
        const val V1_1 = 3 shl 16 or 45
        const val V1_2 = 0 shl 16 or 46
        const val V1_3 = 0 shl 16 or 47
        const val V1_4 = 0 shl 16 or 48
        const val V1_5 = 0 shl 16 or 49
        const val V1_6 = 0 shl 16 or 50
        const val V1_7 = 0 shl 16 or 51
        const val V1_8 = 0 shl 16 or 52
        const val V9 = 0 shl 16 or 53
        const val V10 = 0 shl 16 or 54
        const val V11 = 0 shl 16 or 55
        const val V12 = 0 shl 16 or 56
        const val V13 = 0 shl 16 or 57
        const val V14 = 0 shl 16 or 58
        const val V15 = 0 shl 16 or 59
        const val V16 = 0 shl 16 or 60
        const val V17 = 0 shl 16 or 61
        const val V18 = 0 shl 16 or 62
        const val V19 = 0 shl 16 or 63
        const val V20 = 0 shl 16 or 64
        const val V21 = 0 shl 16 or 65
        /**
         * Version flag indicating that the class is using 'preview' features.
         *
         *
         * `version & V_PREVIEW == V_PREVIEW` tests if a version is flagged with `V_PREVIEW`.
         */
        const val V_PREVIEW = -0x10000

        // Access flags values, defined in
        // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1-200-E.1
        // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5-200-A.1
        // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6-200-A.1
        // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25
        const val ACC_PUBLIC = 0x0001 // class, field, method
        const val ACC_PRIVATE = 0x0002 // class, field, method
        const val ACC_PROTECTED = 0x0004 // class, field, method
        const val ACC_STATIC = 0x0008 // field, method
        const val ACC_FINAL = 0x0010 // class, field, method, parameter
        const val ACC_SUPER = 0x0020 // class
        const val ACC_SYNCHRONIZED = 0x0020 // method
        const val ACC_OPEN = 0x0020 // module
        const val ACC_TRANSITIVE = 0x0020 // module requires
        const val ACC_VOLATILE = 0x0040 // field
        const val ACC_BRIDGE = 0x0040 // method
        const val ACC_STATIC_PHASE = 0x0040 // module requires
        const val ACC_VARARGS = 0x0080 // method
        const val ACC_TRANSIENT = 0x0080 // field
        const val ACC_NATIVE = 0x0100 // method
        const val ACC_INTERFACE = 0x0200 // class
        const val ACC_ABSTRACT = 0x0400 // class, method
        const val ACC_STRICT = 0x0800 // method
        const val ACC_SYNTHETIC = 0x1000 // class, field, method, parameter, module *
        const val ACC_ANNOTATION = 0x2000 // class
        const val ACC_ENUM = 0x4000 // class(?) field inner
        const val ACC_MANDATED = 0x8000 // field, method, parameter, module, module *
        const val ACC_MODULE = 0x8000 // class

        // ASM specific access flags.
        // WARNING: the 16 least significant bits must NOT be used, to avoid conflicts with standard
        // access flags, and also to make sure that these flags are automatically filtered out when
        // written in class files (because access flags are stored using 16 bits only).
        const val ACC_RECORD = 0x10000 // class
        const val ACC_DEPRECATED = 0x20000 // class, field, method

        // Possible values for the type operand of the NEWARRAY instruction.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5.newarray.
        const val T_BOOLEAN = 4
        const val T_CHAR = 5
        const val T_FLOAT = 6
        const val T_DOUBLE = 7
        const val T_BYTE = 8
        const val T_SHORT = 9
        const val T_INT = 10
        const val T_LONG = 11

        // Possible values for the reference_kind field of CONSTANT_MethodHandle_info structures.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.8.
        const val H_GETFIELD = 1
        const val H_GETSTATIC = 2
        const val H_PUTFIELD = 3
        const val H_PUTSTATIC = 4
        const val H_INVOKEVIRTUAL = 5
        const val H_INVOKESTATIC = 6
        const val H_INVOKESPECIAL = 7
        const val H_NEWINVOKESPECIAL = 8
        const val H_INVOKEINTERFACE = 9
        // ASM specific stack map frame types, used in {@link ClassVisitor#visitFrame}.
        /** An expanded frame. See [ClassReader.EXPAND_FRAMES].  */
        const val F_NEW = -1

        /** A compressed frame with complete frame data.  */
        const val F_FULL = 0

        /**
         * A compressed frame where locals are the same as the locals in the previous frame, except that
         * additional 1-3 locals are defined, and with an empty stack.
         */
        const val F_APPEND = 1

        /**
         * A compressed frame where locals are the same as the locals in the previous frame, except that
         * the last 1-3 locals are absent and with an empty stack.
         */
        const val F_CHOP = 2

        /**
         * A compressed frame with exactly the same locals as the previous frame and with an empty stack.
         */
        const val F_SAME = 3

        /**
         * A compressed frame with exactly the same locals as the previous frame and with a single value
         * on the stack.
         */
        const val F_SAME1 = 4

        // Standard stack map frame element types, used in {@link ClassVisitor#visitFrame}.
        val TOP: Int = Frame.ITEM_TOP
        val INTEGER: Int = Frame.ITEM_INTEGER
        val FLOAT: Int = Frame.ITEM_FLOAT
        val DOUBLE: Int = Frame.ITEM_DOUBLE
        val LONG: Int = Frame.ITEM_LONG
        val NULL: Int = Frame.ITEM_NULL
        val UNINITIALIZED_THIS: Int = Frame.ITEM_UNINITIALIZED_THIS

        // The JVM opcode values (with the MethodVisitor method name used to visit them in comment, and
        // where '-' means 'same method name as on the previous line').
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html.
        const val NOP = 0 // visitInsn
        const val ACONST_NULL = 1 // -
        const val ICONST_M1 = 2 // -
        const val ICONST_0 = 3 // -
        const val ICONST_1 = 4 // -
        const val ICONST_2 = 5 // -
        const val ICONST_3 = 6 // -
        const val ICONST_4 = 7 // -
        const val ICONST_5 = 8 // -
        const val LCONST_0 = 9 // -
        const val LCONST_1 = 10 // -
        const val FCONST_0 = 11 // -
        const val FCONST_1 = 12 // -
        const val FCONST_2 = 13 // -
        const val DCONST_0 = 14 // -
        const val DCONST_1 = 15 // -
        const val BIPUSH = 16 // visitIntInsn
        const val SIPUSH = 17 // -
        const val LDC = 18 // visitLdcInsn
        const val ILOAD = 21 // visitVarInsn
        const val LLOAD = 22 // -
        const val FLOAD = 23 // -
        const val DLOAD = 24 // -
        const val ALOAD = 25 // -
        const val IALOAD = 46 // visitInsn
        const val LALOAD = 47 // -
        const val FALOAD = 48 // -
        const val DALOAD = 49 // -
        const val AALOAD = 50 // -
        const val BALOAD = 51 // -
        const val CALOAD = 52 // -
        const val SALOAD = 53 // -
        const val ISTORE = 54 // visitVarInsn
        const val LSTORE = 55 // -
        const val FSTORE = 56 // -
        const val DSTORE = 57 // -
        const val ASTORE = 58 // -
        const val IASTORE = 79 // visitInsn
        const val LASTORE = 80 // -
        const val FASTORE = 81 // -
        const val DASTORE = 82 // -
        const val AASTORE = 83 // -
        const val BASTORE = 84 // -
        const val CASTORE = 85 // -
        const val SASTORE = 86 // -
        const val POP = 87 // -
        const val POP2 = 88 // -
        const val DUP = 89 // -
        const val DUP_X1 = 90 // -
        const val DUP_X2 = 91 // -
        const val DUP2 = 92 // -
        const val DUP2_X1 = 93 // -
        const val DUP2_X2 = 94 // -
        const val SWAP = 95 // -
        const val IADD = 96 // -
        const val LADD = 97 // -
        const val FADD = 98 // -
        const val DADD = 99 // -
        const val ISUB = 100 // -
        const val LSUB = 101 // -
        const val FSUB = 102 // -
        const val DSUB = 103 // -
        const val IMUL = 104 // -
        const val LMUL = 105 // -
        const val FMUL = 106 // -
        const val DMUL = 107 // -
        const val IDIV = 108 // -
        const val LDIV = 109 // -
        const val FDIV = 110 // -
        const val DDIV = 111 // -
        const val IREM = 112 // -
        const val LREM = 113 // -
        const val FREM = 114 // -
        const val DREM = 115 // -
        const val INEG = 116 // -
        const val LNEG = 117 // -
        const val FNEG = 118 // -
        const val DNEG = 119 // -
        const val ISHL = 120 // -
        const val LSHL = 121 // -
        const val ISHR = 122 // -
        const val LSHR = 123 // -
        const val IUSHR = 124 // -
        const val LUSHR = 125 // -
        const val IAND = 126 // -
        const val LAND = 127 // -
        const val IOR = 128 // -
        const val LOR = 129 // -
        const val IXOR = 130 // -
        const val LXOR = 131 // -
        const val IINC = 132 // visitIincInsn
        const val I2L = 133 // visitInsn
        const val I2F = 134 // -
        const val I2D = 135 // -
        const val L2I = 136 // -
        const val L2F = 137 // -
        const val L2D = 138 // -
        const val F2I = 139 // -
        const val F2L = 140 // -
        const val F2D = 141 // -
        const val D2I = 142 // -
        const val D2L = 143 // -
        const val D2F = 144 // -
        const val I2B = 145 // -
        const val I2C = 146 // -
        const val I2S = 147 // -
        const val LCMP = 148 // -
        const val FCMPL = 149 // -
        const val FCMPG = 150 // -
        const val DCMPL = 151 // -
        const val DCMPG = 152 // -
        const val IFEQ = 153 // visitJumpInsn
        const val IFNE = 154 // -
        const val IFLT = 155 // -
        const val IFGE = 156 // -
        const val IFGT = 157 // -
        const val IFLE = 158 // -
        const val IF_ICMPEQ = 159 // -
        const val IF_ICMPNE = 160 // -
        const val IF_ICMPLT = 161 // -
        const val IF_ICMPGE = 162 // -
        const val IF_ICMPGT = 163 // -
        const val IF_ICMPLE = 164 // -
        const val IF_ACMPEQ = 165 // -
        const val IF_ACMPNE = 166 // -
        const val GOTO = 167 // -
        const val JSR = 168 // -
        const val RET = 169 // visitVarInsn
        const val TABLESWITCH = 170 // visiTableSwitchInsn
        const val LOOKUPSWITCH = 171 // visitLookupSwitch
        const val IRETURN = 172 // visitInsn
        const val LRETURN = 173 // -
        const val FRETURN = 174 // -
        const val DRETURN = 175 // -
        const val ARETURN = 176 // -
        const val RETURN = 177 // -
        const val GETSTATIC = 178 // visitFieldInsn
        const val PUTSTATIC = 179 // -
        const val GETFIELD = 180 // -
        const val PUTFIELD = 181 // -
        const val INVOKEVIRTUAL = 182 // visitMethodInsn
        const val INVOKESPECIAL = 183 // -
        const val INVOKESTATIC = 184 // -
        const val INVOKEINTERFACE = 185 // -
        const val INVOKEDYNAMIC = 186 // visitInvokeDynamicInsn
        const val NEW = 187 // visitTypeInsn
        const val NEWARRAY = 188 // visitIntInsn
        const val ANEWARRAY = 189 // visitTypeInsn
        const val ARRAYLENGTH = 190 // visitInsn
        const val ATHROW = 191 // -
        const val CHECKCAST = 192 // visitTypeInsn
        const val INSTANCEOF = 193 // -
        const val MONITORENTER = 194 // visitInsn
        const val MONITOREXIT = 195 // -
        const val MULTIANEWARRAY = 197 // visitMultiANewArrayInsn
        const val IFNULL = 198 // visitJumpInsn
        const val IFNONNULL = 199 // -
    }
}
