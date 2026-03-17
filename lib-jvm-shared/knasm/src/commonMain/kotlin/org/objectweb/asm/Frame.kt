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

import kotlin.math.*

/**
 * The input and output stack map frames of a basic block.
 *
 *
 * Stack map frames are computed in two steps:
 *
 *
 *  * During the visit of each instruction in MethodWriter, the state of the frame at the end of
 * the current basic block is updated by simulating the action of the instruction on the
 * previous state of this so called "output frame".
 *  * After all instructions have been visited, a fix point algorithm is used in MethodWriter to
 * compute the "input frame" of each basic block (i.e. the stack map frame at the beginning of
 * the basic block). See [MethodWriter.computeAllFrames].
 *
 *
 *
 * Output stack map frames are computed relatively to the input frame of the basic block, which
 * is not yet known when output frames are computed. It is therefore necessary to be able to
 * represent abstract types such as "the type at position x in the input frame locals" or "the type
 * at position x from the top of the input frame stack" or even "the type at position x in the input
 * frame, with y more (or less) array dimensions". This explains the rather complicated type format
 * used in this class, explained below.
 *
 *
 * The local variables and the operand stack of input and output frames contain values called
 * "abstract types" hereafter. An abstract type is represented with 4 fields named DIM, KIND, FLAGS
 * and VALUE, packed in a single int value for better performance and memory efficiency:
 *
 * <pre>
 * =====================================
 * |...DIM|KIND|.F|...............VALUE|
 * =====================================
</pre> *
 *
 *
 *  * the DIM field, stored in the 6 most significant bits, is a signed number of array
 * dimensions (from -32 to 31, included). It can be retrieved with [.DIM_MASK] and a
 * right shift of [.DIM_SHIFT].
 *  * the KIND field, stored in 4 bits, indicates the kind of VALUE used. These 4 bits can be
 * retrieved with [.KIND_MASK] and, without any shift, must be equal to [       ][.CONSTANT_KIND], [.REFERENCE_KIND], [.UNINITIALIZED_KIND], [.LOCAL_KIND]
 * or [.STACK_KIND].
 *  * the FLAGS field, stored in 2 bits, contains up to 2 boolean flags. Currently only one flag
 * is defined, namely [.TOP_IF_LONG_OR_DOUBLE_FLAG].
 *  * the VALUE field, stored in the remaining 20 bits, contains either
 *
 *  * one of the constants [.ITEM_TOP], [.ITEM_ASM_BOOLEAN], [             ][.ITEM_ASM_BYTE], [.ITEM_ASM_CHAR] or [.ITEM_ASM_SHORT], [             ][.ITEM_INTEGER], [.ITEM_FLOAT], [.ITEM_LONG], [.ITEM_DOUBLE], [             ][.ITEM_NULL] or [.ITEM_UNINITIALIZED_THIS], if KIND is equal to [             ][.CONSTANT_KIND].
 *  * the index of a [Symbol.TYPE_TAG] [Symbol] in the type table of a [             ], if KIND is equal to [.REFERENCE_KIND].
 *  * the index of an [Symbol.UNINITIALIZED_TYPE_TAG] [Symbol] in the type
 * table of a SymbolTable, if KIND is equal to [.UNINITIALIZED_KIND].
 *  * the index of a local variable in the input stack frame, if KIND is equal to [             ][.LOCAL_KIND].
 *  * a position relatively to the top of the stack of the input stack frame, if KIND is
 * equal to [.STACK_KIND],
 *
 *
 *
 *
 * Output frames can contain abstract types of any kind and with a positive or negative array
 * dimension (and even unassigned types, represented by 0 - which does not correspond to any valid
 * abstract type value). Input frames can only contain CONSTANT_KIND, REFERENCE_KIND or
 * UNINITIALIZED_KIND abstract types of positive or null array dimension. In all cases
 * the type table contains only internal type names (array type descriptors are forbidden - array
 * dimensions must be represented through the DIM field).
 *
 *
 * The LONG and DOUBLE types are always represented by using two slots (LONG + TOP or DOUBLE +
 * TOP), for local variables as well as in the operand stack. This is necessary to be able to
 * simulate DUPx_y instructions, whose effect would be dependent on the concrete types represented
 * by the abstract types in the stack (which are not always known).
 *
 * @author Eric Bruneton
 */
open class Frame(owner: Label?) {
    // -----------------------------------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------------------------------
    /** The basic block to which these input and output stack map frames correspond.  */
    var owner: Label?

    /** The input stack map frame locals. This is an array of abstract types.  */
    private var inputLocals: IntArray? = null

    /** The input stack map frame stack. This is an array of abstract types.  */
    private var inputStack: IntArray? = null

    /** The output stack map frame locals. This is an array of abstract types.  */
    private var outputLocals: IntArray? = null

    /** The output stack map frame stack. This is an array of abstract types.  */
    private var outputStack: IntArray? = null

    /**
     * The start of the output stack, relatively to the input stack. This offset is always negative or
     * null. A null offset means that the output stack must be appended to the input stack. A -n
     * offset means that the first n output stack elements must replace the top n input stack
     * elements, and that the other elements must be appended to the input stack.
     */
    private var outputStackStart: Short = 0

    /** The index of the top stack element in [.outputStack].  */
    private var outputStackTop: Short = 0

    /** The number of types that are initialized in the basic block. See [.initializations].  */
    private var initializationCount = 0

    /**
     * The abstract types that are initialized in the basic block. A constructor invocation on an
     * UNINITIALIZED or UNINITIALIZED_THIS abstract type must replace *every occurrence* of this
     * type in the local variables and in the operand stack. This cannot be done during the first step
     * of the algorithm since, during this step, the local variables and the operand stack types are
     * still abstract. It is therefore necessary to store the abstract types of the constructors which
     * are invoked in the basic block, in order to do this replacement during the second step of the
     * algorithm, where the frames are fully computed. Note that this array can contain abstract types
     * that are relative to the input locals or to the input stack.
     */
    private var initializations: IntArray? = null

    /**
     * Sets this frame to the value of the given frame.
     *
     *
     * WARNING: after this method is called the two frames share the same data structures. It is
     * recommended to discard the given frame to avoid unexpected side effects.
     *
     * @param frame The new frame value.
     */
    open fun copyFrom(frame: Frame) {
        inputLocals = frame.inputLocals
        inputStack = frame.inputStack
        outputStackStart = 0
        outputLocals = frame.outputLocals
        outputStack = frame.outputStack
        outputStackTop = frame.outputStackTop
        initializationCount = frame.initializationCount
        initializations = frame.initializations
    }
    // -----------------------------------------------------------------------------------------------
    // Methods related to the input frame
    // -----------------------------------------------------------------------------------------------
    /**
     * Sets the input frame from the given method description. This method is used to initialize the
     * first frame of a method, which is implicit (i.e. not stored explicitly in the StackMapTable
     * attribute).
     *
     * @param symbolTable the type table to use to lookup and store type [Symbol].
     * @param access the method's access flags.
     * @param descriptor the method descriptor.
     * @param maxLocals the maximum number of local variables of the method.
     */
    open fun setInputFrameFromDescriptor(
        symbolTable: SymbolTable,
        access: Int,
        descriptor: String?,
        maxLocals: Int,
    ) {
        inputLocals = IntArray(maxLocals)
        inputStack = IntArray(0)
        var inputLocalIndex = 0
        if (access and Opcodes.ACC_STATIC === 0) {
            if (access and Constants.ACC_CONSTRUCTOR === 0) {
                inputLocals!![inputLocalIndex++] = REFERENCE_KIND or symbolTable.addType(symbolTable.getClassName())
            } else {
                inputLocals!![inputLocalIndex++] = UNINITIALIZED_THIS
            }
        }
        for (argumentType in Type.getArgumentTypes(descriptor)) {
            val abstractType = getAbstractTypeFromDescriptor(symbolTable, argumentType!!.descriptor, 0)
            inputLocals!![inputLocalIndex++] = abstractType
            if (abstractType == LONG || abstractType == DOUBLE) {
                inputLocals!![inputLocalIndex++] = TOP
            }
        }
        while (inputLocalIndex < maxLocals) {
            inputLocals!![inputLocalIndex++] = TOP
        }
    }

    /**
     * Sets the input frame from the given public API frame description.
     *
     * @param symbolTable the type table to use to lookup and store type [Symbol].
     * @param numLocal the number of local variables.
     * @param local the local variable types, described using the same format as in [     ][MethodVisitor.visitFrame].
     * @param numStack the number of operand stack elements.
     * @param stack the operand stack types, described using the same format as in [     ][MethodVisitor.visitFrame].
     */
    open fun setInputFrameFromApiFormat(
        symbolTable: SymbolTable,
        numLocal: Int,
        local: Array<Any?>?,
        numStack: Int,
        stack: Array<Any?>?,
    ) {
        var inputLocalIndex = 0
        for (i in 0 until numLocal) {
            inputLocals!![inputLocalIndex++] = getAbstractTypeFromApiFormat(symbolTable, local!![i])
            if (local[i] == Opcodes.LONG || local[i] == Opcodes.DOUBLE) {
                inputLocals!![inputLocalIndex++] = TOP
            }
        }
        while (inputLocalIndex < inputLocals!!.size) {
            inputLocals!![inputLocalIndex++] = TOP
        }
        var numStackTop = 0
        for (i in 0 until numStack) {
            if (stack!![i] === Opcodes.LONG || stack!![i] === Opcodes.DOUBLE) {
                ++numStackTop
            }
        }
        inputStack = IntArray(numStack + numStackTop)
        var inputStackIndex = 0
        for (i in 0 until numStack) {
            inputStack!![inputStackIndex++] = getAbstractTypeFromApiFormat(symbolTable, stack!![i])
            if (stack!![i] === Opcodes.LONG || stack!![i] === Opcodes.DOUBLE) {
                inputStack!![inputStackIndex++] = TOP
            }
        }
        outputStackTop = 0
        initializationCount = 0
    }

    open fun getInputStackSize(): Int {
        return inputStack!!.size
    }
    // -----------------------------------------------------------------------------------------------
    // Methods related to the output frame
    // -----------------------------------------------------------------------------------------------
    /**
     * Returns the abstract type stored at the given local variable index in the output frame.
     *
     * @param localIndex the index of the local variable whose value must be returned.
     * @return the abstract type stored at the given local variable index in the output frame.
     */
    private fun getLocal(localIndex: Int): Int {
        return if (outputLocals == null || localIndex >= outputLocals!!.size) {
            // If this local has never been assigned in this basic block, it is still equal to its value
            // in the input frame.
            LOCAL_KIND or localIndex
        } else {
            var abstractType = outputLocals!![localIndex]
            if (abstractType == 0) {
                // If this local has never been assigned in this basic block, so it is still equal to its
                // value in the input frame.
                outputLocals!![localIndex] = LOCAL_KIND or localIndex
                abstractType = outputLocals!![localIndex]
            }
            abstractType
        }
    }

    /**
     * Replaces the abstract type stored at the given local variable index in the output frame.
     *
     * @param localIndex the index of the output frame local variable that must be set.
     * @param abstractType the value that must be set.
     */
    private fun setLocal(localIndex: Int, abstractType: Int) {
        // Create and/or resize the output local variables array if necessary.
        if (outputLocals == null) {
            outputLocals = IntArray(10)
        }
        val outputLocalsLength = outputLocals!!.size
        if (localIndex >= outputLocalsLength) {
            val newOutputLocals = IntArray(max(localIndex + 1, 2 * outputLocalsLength))
            System.arraycopy(outputLocals, 0, newOutputLocals, 0, outputLocalsLength)
            outputLocals = newOutputLocals
        }
        // Set the local variable.
        outputLocals!![localIndex] = abstractType
    }

    /**
     * Pushes the given abstract type on the output frame stack.
     *
     * @param abstractType an abstract type.
     */
    private fun push(abstractType: Int) {
        // Create and/or resize the output stack array if necessary.
        if (outputStack == null) {
            outputStack = IntArray(10)
        }
        val outputStackLength = outputStack!!.size
        if (outputStackTop >= outputStackLength) {
            val newOutputStack = IntArray(max(outputStackTop + 1, 2 * outputStackLength))
            System.arraycopy(outputStack, 0, newOutputStack, 0, outputStackLength)
            outputStack = newOutputStack
        }
        // Pushes the abstract type on the output stack.
        outputStack!![outputStackTop++.toInt()] = abstractType
        // Updates the maximum size reached by the output stack, if needed (note that this size is
        // relative to the input stack size, which is not known yet).
        val outputStackSize = (outputStackStart + outputStackTop).toShort()
        if (outputStackSize > owner!!.outputStackMax) {
            owner!!.outputStackMax = outputStackSize
        }
    }

    /**
     * Pushes the abstract type corresponding to the given descriptor on the output frame stack.
     *
     * @param symbolTable the type table to use to lookup and store type [Symbol].
     * @param descriptor a type or method descriptor (in which case its return type is pushed).
     */
    private fun push(symbolTable: SymbolTable?, descriptor: String?) {
        val typeDescriptorOffset = if (descriptor!![0] == '(') Type.getReturnTypeOffset(descriptor) else 0
        val abstractType = getAbstractTypeFromDescriptor(symbolTable, descriptor, typeDescriptorOffset)
        if (abstractType != 0) {
            push(abstractType)
            if (abstractType == LONG || abstractType == DOUBLE) {
                push(TOP)
            }
        }
    }

    /**
     * Pops an abstract type from the output frame stack and returns its value.
     *
     * @return the abstract type that has been popped from the output frame stack.
     */
    private fun pop(): Int {
        return if (outputStackTop > 0) {
            val sh = --outputStackTop
            outputStack!![sh.toInt()]
        } else {
            // If the output frame stack is empty, pop from the input stack.
            STACK_KIND or -(--outputStackStart)
        }
    }

    /**
     * Pops the given number of abstract types from the output frame stack.
     *
     * @param elements the number of abstract types that must be popped.
     */
    private fun pop(elements: Int) {
        if (outputStackTop >= elements) {
            outputStackTop.minus(elements.toShort())
        } else {
            // If the number of elements to be popped is greater than the number of elements in the output
            // stack, clear it, and pop the remaining elements from the input stack.
            (outputStackStart.minus(elements - outputStackTop)).toShort()
            outputStackTop = 0
        }
    }

    /**
     * Pops as many abstract types from the output frame stack as described by the given descriptor.
     *
     * @param descriptor a type or method descriptor (in which case its argument types are popped).
     */
    private fun pop(descriptor: String?) {
        val firstDescriptorChar = descriptor!![0]
        if (firstDescriptorChar == '(') {
            pop((Type.getArgumentsAndReturnSizes(descriptor) shr 2) - 1)
        } else if (firstDescriptorChar == 'J' || firstDescriptorChar == 'D') {
            pop(2)
        } else {
            pop(1)
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to handle uninitialized types
    // -----------------------------------------------------------------------------------------------
    /**
     * Adds an abstract type to the list of types on which a constructor is invoked in the basic
     * block.
     *
     * @param abstractType an abstract type on a which a constructor is invoked.
     */
    private fun addInitializedType(abstractType: Int) {
        // Create and/or resize the initializations array if necessary.
        if (initializations == null) {
            initializations = IntArray(2)
        }
        val initializationsLength = initializations!!.size
        if (initializationCount >= initializationsLength) {
            val newInitializations = IntArray(max(initializationCount + 1, 2 * initializationsLength))
            System.arraycopy(initializations, 0, newInitializations, 0, initializationsLength)
            initializations = newInitializations
        }
        // Store the abstract type.
        initializations!![initializationCount++] = abstractType
    }

    /**
     * Returns the "initialized" abstract type corresponding to the given abstract type.
     *
     * @param symbolTable the type table to use to lookup and store type [Symbol].
     * @param abstractType an abstract type.
     * @return the REFERENCE_KIND abstract type corresponding to abstractType if it is
     * UNINITIALIZED_THIS or an UNINITIALIZED_KIND abstract type for one of the types on which a
     * constructor is invoked in the basic block. Otherwise returns abstractType.
     */
    private fun getInitializedType(symbolTable: SymbolTable, abstractType: Int): Int {
        if (abstractType == UNINITIALIZED_THIS
            || abstractType and (DIM_MASK or KIND_MASK) == UNINITIALIZED_KIND
        ) {
            for (i in 0 until initializationCount) {
                var initializedType = initializations!![i]
                val dim = initializedType and DIM_MASK
                val kind = initializedType and KIND_MASK
                val value = initializedType and VALUE_MASK
                if (kind == LOCAL_KIND) {
                    initializedType = dim + inputLocals!![value]
                } else if (kind == STACK_KIND) {
                    initializedType = dim + inputStack!![inputStack!!.size - value]
                }
                if (abstractType == initializedType) {
                    return if (abstractType == UNINITIALIZED_THIS) {
                        REFERENCE_KIND or symbolTable.addType(symbolTable.getClassName())
                    } else {
                        (REFERENCE_KIND
                                or symbolTable.addType(symbolTable.getType(abstractType and VALUE_MASK)!!.value))
                    }
                }
            }
        }
        return abstractType
    }
    // -----------------------------------------------------------------------------------------------
    // Main method, to simulate the execution of each instruction on the output frame
    // -----------------------------------------------------------------------------------------------
    /**
     * Simulates the action of the given instruction on the output stack frame.
     *
     * @param opcode the opcode of the instruction.
     * @param arg the numeric operand of the instruction, if any.
     * @param argSymbol the Symbol operand of the instruction, if any.
     * @param symbolTable the type table to use to lookup and store type [Symbol].
     */
    open fun execute(
        opcode: Int, arg: Int, argSymbol: Symbol?, symbolTable: SymbolTable?,
    ) {
        // Abstract types popped from the stack or read from local variables.
        val abstractType1: Int
        val abstractType2: Int
        val abstractType3: Int
        val abstractType4: Int
        when (opcode) {
            Opcodes.NOP, Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S, Opcodes.GOTO, Opcodes.RETURN -> {
            }
            Opcodes.ACONST_NULL -> push(NULL)
            Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.ILOAD -> push(
                INTEGER)
            Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.LLOAD -> {
                push(LONG)
                push(TOP)
            }
            Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.FLOAD -> push(FLOAT)
            Opcodes.DCONST_0, Opcodes.DCONST_1, Opcodes.DLOAD -> {
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.LDC -> when (argSymbol!!.tag) {
                Symbol.CONSTANT_INTEGER_TAG -> push(INTEGER)
                Symbol.CONSTANT_LONG_TAG -> {
                    push(LONG)
                    push(TOP)
                }
                Symbol.CONSTANT_FLOAT_TAG -> push(FLOAT)
                Symbol.CONSTANT_DOUBLE_TAG -> {
                    push(DOUBLE)
                    push(TOP)
                }
                Symbol.CONSTANT_CLASS_TAG -> push(REFERENCE_KIND or symbolTable!!.addType("java/lang/Class"))
                Symbol.CONSTANT_STRING_TAG -> push(REFERENCE_KIND or symbolTable!!.addType("java/lang/String"))
                Symbol.CONSTANT_METHOD_TYPE_TAG -> push(REFERENCE_KIND or symbolTable!!.addType("java/lang/invoke/MethodType"))
                Symbol.CONSTANT_METHOD_HANDLE_TAG -> push(REFERENCE_KIND or symbolTable!!.addType("java/lang/invoke/MethodHandle"))
                Symbol.CONSTANT_DYNAMIC_TAG -> push(symbolTable, argSymbol!!.value)
                else -> throw AssertionError()
            }
            Opcodes.ALOAD -> push(getLocal(arg))
            Opcodes.LALOAD, Opcodes.D2L -> {
                pop(2)
                push(LONG)
                push(TOP)
            }
            Opcodes.DALOAD, Opcodes.L2D -> {
                pop(2)
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.AALOAD -> {
                pop(1)
                abstractType1 = pop()
                push(if (abstractType1 == NULL) abstractType1 else ELEMENT_OF + abstractType1)
            }
            Opcodes.ISTORE, Opcodes.FSTORE, Opcodes.ASTORE -> {
                abstractType1 = pop()
                setLocal(arg, abstractType1)
                if (arg > 0) {
                    val previousLocalType = getLocal(arg - 1)
                    if (previousLocalType == LONG || previousLocalType == DOUBLE) {
                        setLocal(arg - 1, TOP)
                    } else if (previousLocalType and KIND_MASK == LOCAL_KIND
                        || previousLocalType and KIND_MASK == STACK_KIND
                    ) {
                        // The type of the previous local variable is not known yet, but if it later appears
                        // to be LONG or DOUBLE, we should then use TOP instead.
                        setLocal(arg - 1, previousLocalType or TOP_IF_LONG_OR_DOUBLE_FLAG)
                    }
                }
            }
            Opcodes.LSTORE, Opcodes.DSTORE -> {
                pop(1)
                abstractType1 = pop()
                setLocal(arg, abstractType1)
                setLocal(arg + 1, TOP)
                if (arg > 0) {
                    val previousLocalType = getLocal(arg - 1)
                    if (previousLocalType == LONG || previousLocalType == DOUBLE) {
                        setLocal(arg - 1, TOP)
                    } else if (previousLocalType and KIND_MASK == LOCAL_KIND
                        || previousLocalType and KIND_MASK == STACK_KIND
                    ) {
                        // The type of the previous local variable is not known yet, but if it later appears
                        // to be LONG or DOUBLE, we should then use TOP instead.
                        setLocal(arg - 1, previousLocalType or TOP_IF_LONG_OR_DOUBLE_FLAG)
                    }
                }
            }
            Opcodes.IASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE, Opcodes.FASTORE, Opcodes.AASTORE -> pop(
                3)
            Opcodes.LASTORE, Opcodes.DASTORE -> pop(4)
            Opcodes.POP, Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IRETURN, Opcodes.FRETURN, Opcodes.ARETURN, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH, Opcodes.ATHROW, Opcodes.MONITORENTER, Opcodes.MONITOREXIT, Opcodes.IFNULL, Opcodes.IFNONNULL -> pop(
                1)
            Opcodes.POP2, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.LRETURN, Opcodes.DRETURN -> pop(
                2)
            Opcodes.DUP -> {
                abstractType1 = pop()
                push(abstractType1)
                push(abstractType1)
            }
            Opcodes.DUP_X1 -> {
                abstractType1 = pop()
                abstractType2 = pop()
                push(abstractType1)
                push(abstractType2)
                push(abstractType1)
            }
            Opcodes.DUP_X2 -> {
                abstractType1 = pop()
                abstractType2 = pop()
                abstractType3 = pop()
                push(abstractType1)
                push(abstractType3)
                push(abstractType2)
                push(abstractType1)
            }
            Opcodes.DUP2 -> {
                abstractType1 = pop()
                abstractType2 = pop()
                push(abstractType2)
                push(abstractType1)
                push(abstractType2)
                push(abstractType1)
            }
            Opcodes.DUP2_X1 -> {
                abstractType1 = pop()
                abstractType2 = pop()
                abstractType3 = pop()
                push(abstractType2)
                push(abstractType1)
                push(abstractType3)
                push(abstractType2)
                push(abstractType1)
            }
            Opcodes.DUP2_X2 -> {
                abstractType1 = pop()
                abstractType2 = pop()
                abstractType3 = pop()
                abstractType4 = pop()
                push(abstractType2)
                push(abstractType1)
                push(abstractType4)
                push(abstractType3)
                push(abstractType2)
                push(abstractType1)
            }
            Opcodes.SWAP -> {
                abstractType1 = pop()
                abstractType2 = pop()
                push(abstractType1)
                push(abstractType2)
            }
            Opcodes.IALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM, Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR, Opcodes.L2I, Opcodes.D2I, Opcodes.FCMPL, Opcodes.FCMPG -> {
                pop(2)
                push(INTEGER)
            }
            Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR -> {
                pop(4)
                push(LONG)
                push(TOP)
            }
            Opcodes.FALOAD, Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM, Opcodes.L2F, Opcodes.D2F -> {
                pop(2)
                push(FLOAT)
            }
            Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM -> {
                pop(4)
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR -> {
                pop(3)
                push(LONG)
                push(TOP)
            }
            Opcodes.IINC -> setLocal(arg, INTEGER)
            Opcodes.I2L, Opcodes.F2L -> {
                pop(1)
                push(LONG)
                push(TOP)
            }
            Opcodes.I2F -> {
                pop(1)
                push(FLOAT)
            }
            Opcodes.I2D, Opcodes.F2D -> {
                pop(1)
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.F2I, Opcodes.ARRAYLENGTH, Opcodes.INSTANCEOF -> {
                pop(1)
                push(INTEGER)
            }
            Opcodes.LCMP, Opcodes.DCMPL, Opcodes.DCMPG -> {
                pop(4)
                push(INTEGER)
            }
            Opcodes.JSR, Opcodes.RET -> throw IllegalArgumentException("JSR/RET are not supported with computeFrames option")
            Opcodes.GETSTATIC -> push(symbolTable, argSymbol!!.value)
            Opcodes.PUTSTATIC -> pop(argSymbol!!.value)
            Opcodes.GETFIELD -> {
                pop(1)
                push(symbolTable, argSymbol!!.value)
            }
            Opcodes.PUTFIELD -> {
                pop(argSymbol!!.value)
                pop()
            }
            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE -> {
                pop(argSymbol!!.value)
                if (opcode != Opcodes.INVOKESTATIC) {
                    abstractType1 = pop()
                    if (opcode == Opcodes.INVOKESPECIAL && argSymbol.name!![0] == '<') {
                        addInitializedType(abstractType1)
                    }
                }
                push(symbolTable, argSymbol!!.value)
            }
            Opcodes.INVOKEDYNAMIC -> {
                pop(argSymbol!!.value)
                push(symbolTable, argSymbol!!.value)
            }
            Opcodes.NEW -> push(UNINITIALIZED_KIND or symbolTable!!.addUninitializedType(
                argSymbol!!.value, arg))
            Opcodes.NEWARRAY -> {
                pop()
                when (arg) {
                    Opcodes.T_BOOLEAN -> push(ARRAY_OF or BOOLEAN)
                    Opcodes.T_CHAR -> push(ARRAY_OF or CHAR)
                    Opcodes.T_BYTE -> push(ARRAY_OF or BYTE)
                    Opcodes.T_SHORT -> push(ARRAY_OF or SHORT)
                    Opcodes.T_INT -> push(ARRAY_OF or INTEGER)
                    Opcodes.T_FLOAT -> push(ARRAY_OF or FLOAT)
                    Opcodes.T_DOUBLE -> push(ARRAY_OF or DOUBLE)
                    Opcodes.T_LONG -> push(ARRAY_OF or LONG)
                    else -> throw IllegalArgumentException()
                }
            }
            Opcodes.ANEWARRAY -> {
                val arrayElementType = argSymbol!!.value
                pop()
                if (arrayElementType!![0] == '[') {
                    push(symbolTable, "[$arrayElementType")
                } else {
                    push(ARRAY_OF or REFERENCE_KIND or symbolTable!!.addType(arrayElementType))
                }
            }
            Opcodes.CHECKCAST -> {
                val castType = argSymbol!!.value
                pop()
                if (castType!![0] == '[') {
                    push(symbolTable, castType)
                } else {
                    push(REFERENCE_KIND or symbolTable!!.addType(castType))
                }
            }
            Opcodes.MULTIANEWARRAY -> {
                pop(arg)
                push(symbolTable, argSymbol!!.value)
            }
            else -> throw IllegalArgumentException()
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Frame merging methods, used in the second step of the stack map frame computation algorithm
    // -----------------------------------------------------------------------------------------------
    /**
     * Computes the concrete output type corresponding to a given abstract output type.
     *
     * @param abstractOutputType an abstract output type.
     * @param numStack the size of the input stack, used to resolve abstract output types of
     * STACK_KIND kind.
     * @return the concrete output type corresponding to 'abstractOutputType'.
     */
    private fun getConcreteOutputType(abstractOutputType: Int, numStack: Int): Int {
        val dim = abstractOutputType and DIM_MASK
        val kind = abstractOutputType and KIND_MASK
        return if (kind == LOCAL_KIND) {
            // By definition, a LOCAL_KIND type designates the concrete type of a local variable at
            // the beginning of the basic block corresponding to this frame (which is known when
            // this method is called, but was not when the abstract type was computed).
            var concreteOutputType =
                dim + inputLocals!![abstractOutputType and VALUE_MASK]
            if (abstractOutputType and TOP_IF_LONG_OR_DOUBLE_FLAG != 0
                && (concreteOutputType == LONG || concreteOutputType == DOUBLE)
            ) {
                concreteOutputType = TOP
            }
            concreteOutputType
        } else if (kind == STACK_KIND) {
            // By definition, a STACK_KIND type designates the concrete type of a local variable at
            // the beginning of the basic block corresponding to this frame (which is known when
            // this method is called, but was not when the abstract type was computed).
            var concreteOutputType =
                dim + inputStack!![numStack - (abstractOutputType and VALUE_MASK)]
            if (abstractOutputType and TOP_IF_LONG_OR_DOUBLE_FLAG != 0
                && (concreteOutputType == LONG || concreteOutputType == DOUBLE)
            ) {
                concreteOutputType = TOP
            }
            concreteOutputType
        } else {
            abstractOutputType
        }
    }

    /**
     * Merges the input frame of the given [Frame] with the input and output frames of this
     * [Frame]. Returns true if the given frame has been changed by this operation
     * (the input and output frames of this [Frame] are never changed).
     *
     * @param symbolTable the type table to use to lookup and store type [Symbol].
     * @param dstFrame the [Frame] whose input frame must be updated. This should be the frame
     * of a successor, in the control flow graph, of the basic block corresponding to this frame.
     * @param catchTypeIndex if 'frame' corresponds to an exception handler basic block, the type
     * table index of the caught exception type, otherwise 0.
     * @return true if the input frame of 'frame' has been changed by this operation.
     */
    open fun merge(
        symbolTable: SymbolTable, dstFrame: Frame?, catchTypeIndex: Int,
    ): Boolean {
        var frameChanged = false

        // Compute the concrete types of the local variables at the end of the basic block corresponding
        // to this frame, by resolving its abstract output types, and merge these concrete types with
        // those of the local variables in the input frame of dstFrame.
        val numLocal = inputLocals!!.size
        val numStack = inputStack!!.size
        if (dstFrame!!.inputLocals == null) {
            dstFrame.inputLocals = IntArray(numLocal)
            frameChanged = true
        }
        for (i in 0 until numLocal) {
            var concreteOutputType: Int
            concreteOutputType = if (outputLocals != null && i < outputLocals!!.size) {
                val abstractOutputType = outputLocals!![i]
                if (abstractOutputType == 0) {
                    // If the local variable has never been assigned in this basic block, it is equal to its
                    // value at the beginning of the block.
                    inputLocals!![i]
                } else {
                    getConcreteOutputType(abstractOutputType, numStack)
                }
            } else {
                // If the local variable has never been assigned in this basic block, it is equal to its
                // value at the beginning of the block.
                inputLocals!![i]
            }
            // concreteOutputType might be an uninitialized type from the input locals or from the input
            // stack. However, if a constructor has been called for this class type in the basic block,
            // then this type is no longer uninitialized at the end of basic block.
            if (initializations != null) {
                concreteOutputType = getInitializedType(symbolTable, concreteOutputType)
            }
            frameChanged = frameChanged or merge(symbolTable, concreteOutputType, dstFrame.inputLocals, i)
        }

        // If dstFrame is an exception handler block, it can be reached from any instruction of the
        // basic block corresponding to this frame, in particular from the first one. Therefore, the
        // input locals of dstFrame should be compatible (i.e. merged) with the input locals of this
        // frame (and the input stack of dstFrame should be compatible, i.e. merged, with a one
        // element stack containing the caught exception type).
        if (catchTypeIndex > 0) {
            for (i in 0 until numLocal) {
                frameChanged = frameChanged or merge(symbolTable, inputLocals!![i], dstFrame.inputLocals, i)
            }
            if (dstFrame.inputStack == null) {
                dstFrame.inputStack = IntArray(1)
                frameChanged = true
            }
            frameChanged = frameChanged or merge(symbolTable, catchTypeIndex, dstFrame.inputStack, 0)
            return frameChanged
        }

        // Compute the concrete types of the stack operands at the end of the basic block corresponding
        // to this frame, by resolving its abstract output types, and merge these concrete types with
        // those of the stack operands in the input frame of dstFrame.
        val numInputStack = inputStack!!.size + outputStackStart
        if (dstFrame.inputStack == null) {
            dstFrame.inputStack = IntArray(numInputStack + outputStackTop)
            frameChanged = true
        }
        // First, do this for the stack operands that have not been popped in the basic block
        // corresponding to this frame, and which are therefore equal to their value in the input
        // frame (except for uninitialized types, which may have been initialized).
        for (i in 0 until numInputStack) {
            var concreteOutputType = inputStack!![i]
            if (initializations != null) {
                concreteOutputType = getInitializedType(symbolTable, concreteOutputType)
            }
            frameChanged = frameChanged or merge(symbolTable, concreteOutputType, dstFrame.inputStack, i)
        }
        // Then, do this for the stack operands that have pushed in the basic block (this code is the
        // same as the one above for local variables).
        for (i in 0 until outputStackTop) {
            val abstractOutputType = outputStack!![i]
            var concreteOutputType = getConcreteOutputType(abstractOutputType, numStack)
            if (initializations != null) {
                concreteOutputType = getInitializedType(symbolTable, concreteOutputType)
            }
            frameChanged =
                frameChanged or merge(symbolTable, concreteOutputType, dstFrame.inputStack, numInputStack + i)
        }
        return frameChanged
    }
    // -----------------------------------------------------------------------------------------------
    // Frame output methods, to generate StackMapFrame attributes
    // -----------------------------------------------------------------------------------------------
    /**
     * Makes the given [MethodWriter] visit the input frame of this [Frame]. The visit is
     * done with the [MethodWriter.visitFrameStart], [MethodWriter.visitAbstractType] and
     * [MethodWriter.visitFrameEnd] methods.
     *
     * @param methodWriter the [MethodWriter] that should visit the input frame of this [     ].
     */
    open fun accept(methodWriter: MethodWriter) {
        // Compute the number of locals, ignoring TOP types that are just after a LONG or a DOUBLE, and
        // all trailing TOP types.
        val localTypes = inputLocals
        var numLocal = 0
        var numTrailingTop = 0
        var i = 0
        while (i < localTypes!!.size) {
            val localType = localTypes[i]
            i += if (localType == LONG || localType == DOUBLE) 2 else 1
            if (localType == TOP) {
                numTrailingTop++
            } else {
                numLocal += numTrailingTop + 1
                numTrailingTop = 0
            }
        }
        // Compute the stack size, ignoring TOP types that are just after a LONG or a DOUBLE.
        val stackTypes = inputStack
        var numStack = 0
        i = 0
        while (i < stackTypes!!.size) {
            val stackType = stackTypes[i]
            i += if (stackType == LONG || stackType == DOUBLE) 2 else 1
            numStack++
        }
        // Visit the frame and its content.
        var frameIndex: Int = methodWriter.visitFrameStart(owner!!.bytecodeOffset, numLocal, numStack)
        i = 0
        while (numLocal-- > 0) {
            val localType = localTypes[i]
            i += if (localType == LONG || localType == DOUBLE) 2 else 1
            methodWriter.visitAbstractType(frameIndex++, localType)
        }
        i = 0
        while (numStack-- > 0) {
            val stackType = stackTypes[i]
            i += if (stackType == LONG || stackType == DOUBLE) 2 else 1
            methodWriter.visitAbstractType(frameIndex++, stackType)
        }
        methodWriter.visitFrameEnd()
    }

    companion object {
        // Constants used in the StackMapTable attribute.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4.
        const val SAME_FRAME = 0
        const val SAME_LOCALS_1_STACK_ITEM_FRAME = 64
        const val RESERVED = 128
        const val SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247
        const val CHOP_FRAME = 248
        const val SAME_FRAME_EXTENDED = 251
        const val APPEND_FRAME = 252
        const val FULL_FRAME = 255
        const val ITEM_TOP = 0
        const val ITEM_INTEGER = 1
        const val ITEM_FLOAT = 2
        const val ITEM_DOUBLE = 3
        const val ITEM_LONG = 4
        const val ITEM_NULL = 5
        const val ITEM_UNINITIALIZED_THIS = 6
        const val ITEM_OBJECT = 7
        const val ITEM_UNINITIALIZED = 8

        // Additional, ASM specific constants used in abstract types below.
        private const val ITEM_ASM_BOOLEAN = 9
        private const val ITEM_ASM_BYTE = 10
        private const val ITEM_ASM_CHAR = 11
        private const val ITEM_ASM_SHORT = 12

        // The size and offset in bits of each field of an abstract type.
        private const val DIM_SIZE = 6
        private const val KIND_SIZE = 4
        private const val FLAGS_SIZE = 2
        private const val VALUE_SIZE = 32 - DIM_SIZE - KIND_SIZE - FLAGS_SIZE
        private const val DIM_SHIFT = KIND_SIZE + FLAGS_SIZE + VALUE_SIZE
        private const val KIND_SHIFT = FLAGS_SIZE + VALUE_SIZE
        private const val FLAGS_SHIFT = VALUE_SIZE

        // Bitmasks to get each field of an abstract type.
        private const val DIM_MASK = (1 shl DIM_SIZE) - 1 shl DIM_SHIFT
        private const val KIND_MASK = (1 shl KIND_SIZE) - 1 shl KIND_SHIFT
        private const val VALUE_MASK = (1 shl VALUE_SIZE) - 1
        // Constants to manipulate the DIM field of an abstract type.
        /** The constant to be added to an abstract type to get one with one more array dimension.  */
        private const val ARRAY_OF = +1 shl DIM_SHIFT

        /** The constant to be added to an abstract type to get one with one less array dimension.  */
        private const val ELEMENT_OF = -1 shl DIM_SHIFT

        // Possible values for the KIND field of an abstract type.
        private const val CONSTANT_KIND = 1 shl KIND_SHIFT
        private const val REFERENCE_KIND = 2 shl KIND_SHIFT
        private const val UNINITIALIZED_KIND = 3 shl KIND_SHIFT
        private const val LOCAL_KIND = 4 shl KIND_SHIFT
        private const val STACK_KIND = 5 shl KIND_SHIFT
        // Possible flags for the FLAGS field of an abstract type.
        /**
         * A flag used for LOCAL_KIND and STACK_KIND abstract types, indicating that if the resolved,
         * concrete type is LONG or DOUBLE, TOP should be used instead (because the value has been
         * partially overridden with an xSTORE instruction).
         */
        private const val TOP_IF_LONG_OR_DOUBLE_FLAG = 1 shl FLAGS_SHIFT

        // Useful predefined abstract types (all the possible CONSTANT_KIND types).
        private const val TOP = CONSTANT_KIND or ITEM_TOP
        private const val BOOLEAN = CONSTANT_KIND or ITEM_ASM_BOOLEAN
        private const val BYTE = CONSTANT_KIND or ITEM_ASM_BYTE
        private const val CHAR = CONSTANT_KIND or ITEM_ASM_CHAR
        private const val SHORT = CONSTANT_KIND or ITEM_ASM_SHORT
        private const val INTEGER = CONSTANT_KIND or ITEM_INTEGER
        private const val FLOAT = CONSTANT_KIND or ITEM_FLOAT
        private const val LONG = CONSTANT_KIND or ITEM_LONG
        private const val DOUBLE = CONSTANT_KIND or ITEM_DOUBLE
        private const val NULL = CONSTANT_KIND or ITEM_NULL
        private const val UNINITIALIZED_THIS = CONSTANT_KIND or ITEM_UNINITIALIZED_THIS
        // -----------------------------------------------------------------------------------------------
        // Static methods to get abstract types from other type formats
        // -----------------------------------------------------------------------------------------------
        /**
         * Returns the abstract type corresponding to the given public API frame element type.
         *
         * @param symbolTable the type table to use to lookup and store type [Symbol].
         * @param type a frame element type described using the same format as in [     ][MethodVisitor.visitFrame], i.e. either [Opcodes.TOP], [Opcodes.INTEGER], [     ][Opcodes.FLOAT], [Opcodes.LONG], [Opcodes.DOUBLE], [Opcodes.NULL], or
         * [Opcodes.UNINITIALIZED_THIS], or the internal name of a class, or a Label designating
         * a NEW instruction (for uninitialized types).
         * @return the abstract type corresponding to the given frame element type.
         */
        open fun getAbstractTypeFromApiFormat(symbolTable: SymbolTable, type: Any?): Int {
            return if (type is Int) {
                CONSTANT_KIND or type.toInt()
            } else if (type is String) {
                val descriptor: String = Type.getObjectType(type).descriptor
                getAbstractTypeFromDescriptor(symbolTable, descriptor, 0)
            } else {
                (UNINITIALIZED_KIND
                        or symbolTable.addUninitializedType("", (type as Label).bytecodeOffset))
            }
        }

        /**
         * Returns the abstract type corresponding to the internal name of a class.
         *
         * @param symbolTable the type table to use to lookup and store type [Symbol].
         * @param internalName the internal name of a class. This must *not* be an array type
         * descriptor.
         * @return the abstract type value corresponding to the given internal name.
         */
        open fun getAbstractTypeFromInternalName(
            symbolTable: SymbolTable, internalName: String?,
        ): Int {
            return REFERENCE_KIND or symbolTable.addType(internalName)
        }

        /**
         * Returns the abstract type corresponding to the given type descriptor.
         *
         * @param symbolTable the type table to use to lookup and store type [Symbol].
         * @param buffer a string ending with a type descriptor.
         * @param offset the start offset of the type descriptor in buffer.
         * @return the abstract type corresponding to the given type descriptor.
         */
        private fun getAbstractTypeFromDescriptor(
            symbolTable: SymbolTable?, buffer: String?, offset: Int,
        ): Int {
            val internalName: String
            return when (buffer!![offset]) {
                'V' -> 0
                'Z', 'C', 'B', 'S', 'I' -> INTEGER
                'F' -> FLOAT
                'J' -> LONG
                'D' -> DOUBLE
                'L' -> {
                    internalName = buffer.substring(offset + 1, buffer.length - 1)
                    REFERENCE_KIND or symbolTable!!.addType(internalName)
                }
                '[' -> {
                    var elementDescriptorOffset = offset + 1
                    while (buffer[elementDescriptorOffset] == '[') {
                        ++elementDescriptorOffset
                    }
                    val typeValue: Int
                    when (buffer[elementDescriptorOffset]) {
                        'Z' -> typeValue = BOOLEAN
                        'C' -> typeValue = CHAR
                        'B' -> typeValue = BYTE
                        'S' -> typeValue = SHORT
                        'I' -> typeValue = INTEGER
                        'F' -> typeValue = FLOAT
                        'J' -> typeValue = LONG
                        'D' -> typeValue = DOUBLE
                        'L' -> {
                            internalName = buffer.substring(elementDescriptorOffset + 1, buffer.length - 1)
                            typeValue = REFERENCE_KIND or symbolTable!!.addType(internalName)
                        }
                        else -> throw IllegalArgumentException(
                            "Invalid descriptor fragment: " + buffer.substring(elementDescriptorOffset)
                        )
                    }
                    elementDescriptorOffset - offset shl DIM_SHIFT or typeValue
                }
                else -> throw IllegalArgumentException("Invalid descriptor: " + buffer.substring(offset))
            }
        }

        /**
         * Merges the type at the given index in the given abstract type array with the given type.
         * Returns true if the type array has been modified by this operation.
         *
         * @param symbolTable the type table to use to lookup and store type [Symbol].
         * @param sourceType the abstract type with which the abstract type array element must be merged.
         * This type should be of [.CONSTANT_KIND], [.REFERENCE_KIND] or [     ][.UNINITIALIZED_KIND] kind, with positive or null array dimensions.
         * @param dstTypes an array of abstract types. These types should be of [.CONSTANT_KIND],
         * [.REFERENCE_KIND] or [.UNINITIALIZED_KIND] kind, with positive or null array dimensions.
         * @param dstIndex the index of the type that must be merged in dstTypes.
         * @return true if the type array has been modified by this operation.
         */
        open fun merge(
            symbolTable: SymbolTable,
            sourceType: Int,
            dstTypes: IntArray?,
            dstIndex: Int,
        ): Boolean {
            val dstType = dstTypes!![dstIndex]
            if (dstType == sourceType) {
                // If the types are equal, merge(sourceType, dstType) = dstType, so there is no change.
                return false
            }
            var srcType = sourceType
            if (sourceType and DIM_MASK.inv() == NULL) {
                if (dstType == NULL) {
                    return false
                }
                srcType = NULL
            }
            if (dstType == 0) {
                // If dstTypes[dstIndex] has never been assigned, merge(srcType, dstType) = srcType.
                dstTypes[dstIndex] = srcType
                return true
            }
            val mergedType: Int
            if (dstType and DIM_MASK != 0 || dstType and KIND_MASK == REFERENCE_KIND) {
                // If dstType is a reference type of any array dimension.
                if (srcType == NULL) {
                    // If srcType is the NULL type, merge(srcType, dstType) = dstType, so there is no change.
                    return false
                } else if (srcType and (DIM_MASK or KIND_MASK) == dstType and (DIM_MASK or KIND_MASK)) {
                    // If srcType has the same array dimension and the same kind as dstType.
                    mergedType =
                        if (dstType and KIND_MASK == REFERENCE_KIND) {
                            // If srcType and dstType are reference types with the same array dimension,
                            // merge(srcType, dstType) = dim(srcType) | common super class of srcType and dstType.
                            (srcType and DIM_MASK
                                    or REFERENCE_KIND
                                    or symbolTable.addMergedType(srcType and VALUE_MASK,
                                dstType and VALUE_MASK))
                        } else {
                            // If srcType and dstType are array types of equal dimension but different element types,
                            // merge(srcType, dstType) = dim(srcType) - 1 | java/lang/Object.
                            val mergedDim =
                                ELEMENT_OF + (srcType and DIM_MASK)
                            mergedDim or REFERENCE_KIND or symbolTable.addType("java/lang/Object")
                        }
                } else if (srcType and DIM_MASK != 0 || srcType and KIND_MASK == REFERENCE_KIND) {
                    // If srcType is any other reference or array type,
                    // merge(srcType, dstType) = min(srcDdim, dstDim) | java/lang/Object
                    // where srcDim is the array dimension of srcType, minus 1 if srcType is an array type
                    // with a non reference element type (and similarly for dstDim).
                    var srcDim = srcType and DIM_MASK
                    if (srcDim != 0 && srcType and KIND_MASK != REFERENCE_KIND) {
                        srcDim = ELEMENT_OF + srcDim
                    }
                    var dstDim = dstType and DIM_MASK
                    if (dstDim != 0 && dstType and KIND_MASK != REFERENCE_KIND) {
                        dstDim = ELEMENT_OF + dstDim
                    }
                    mergedType = min(srcDim, dstDim) or REFERENCE_KIND or symbolTable.addType("java/lang/Object")
                } else {
                    // If srcType is any other type, merge(srcType, dstType) = TOP.
                    mergedType = TOP
                }
            } else if (dstType == NULL) {
                // If dstType is the NULL type, merge(srcType, dstType) = srcType, or TOP if srcType is not a
                // an array type or a reference type.
                mergedType = if (srcType and DIM_MASK != 0 || srcType and KIND_MASK == REFERENCE_KIND) srcType else TOP
            } else {
                // If dstType is any other type, merge(srcType, dstType) = TOP whatever srcType.
                mergedType = TOP
            }
            if (mergedType != dstType) {
                dstTypes[dstIndex] = mergedType
                return true
            }
            return false
        }

        /**
         * Put the given abstract type in the given ByteVector, using the JVMS verification_type_info
         * format used in StackMapTable attributes.
         *
         * @param symbolTable the type table to use to lookup and store type [Symbol].
         * @param abstractType an abstract type, restricted to [Frame.CONSTANT_KIND], [     ][Frame.REFERENCE_KIND] or [Frame.UNINITIALIZED_KIND] types.
         * @param output where the abstract type must be put.
         * @see [JVMS
         * 4.7.4](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.4)
         */
        open fun putAbstractType(
            symbolTable: SymbolTable, abstractType: Int, output: ByteVector?,
        ) {
            var arrayDimensions = abstractType and DIM_MASK shr DIM_SHIFT
            if (arrayDimensions == 0) {
                val typeValue = abstractType and VALUE_MASK
                when (abstractType and KIND_MASK) {
                    CONSTANT_KIND -> output!!.putByte(typeValue)
                    REFERENCE_KIND -> output
                        ?.putByte(ITEM_OBJECT)
                        ?.putShort(symbolTable.addConstantClass(symbolTable.getType(typeValue)!!.value).index)
                    UNINITIALIZED_KIND -> output!!.putByte(ITEM_UNINITIALIZED).putShort(
                        symbolTable.getType(typeValue)!!.data as Int)
                    else -> throw AssertionError()
                }
            } else {
                // Case of an array type, we need to build its descriptor first.
                val typeDescriptor = StringBuilder()
                while (arrayDimensions-- > 0) {
                    typeDescriptor.append('[')
                }
                if (abstractType and KIND_MASK == REFERENCE_KIND) {
                    typeDescriptor
                        .append('L')
                        .append(symbolTable.getType(abstractType and VALUE_MASK)!!.value)
                        .append(';')
                } else {
                    when (abstractType and VALUE_MASK) {
                        ITEM_ASM_BOOLEAN -> typeDescriptor.append('Z')
                        ITEM_ASM_BYTE -> typeDescriptor.append('B')
                        ITEM_ASM_CHAR -> typeDescriptor.append('C')
                        ITEM_ASM_SHORT -> typeDescriptor.append('S')
                        ITEM_INTEGER -> typeDescriptor.append('I')
                        ITEM_FLOAT -> typeDescriptor.append('F')
                        ITEM_LONG -> typeDescriptor.append('J')
                        ITEM_DOUBLE -> typeDescriptor.append('D')
                        else -> throw AssertionError()
                    }
                }
                output?.putByte(ITEM_OBJECT)?.putShort(symbolTable.addConstantClass(typeDescriptor.toString()).index)
            }
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------------
    /**
     * Constructs a new Frame.
     *
     * @param owner the basic block to which these input and output stack map frames correspond.
     */
    init {
        this.owner = owner
    }
}
