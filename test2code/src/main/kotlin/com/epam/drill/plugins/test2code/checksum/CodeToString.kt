/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.plugins.test2code.checksum

import org.apache.bcel.*
import org.apache.bcel.classfile.*
import org.apache.bcel.util.*
import java.io.*
import java.util.concurrent.*

private val ldcInstructions = listOf(
    Const.LDC_W,
    Const.LDC2_W,
    Const.LDC_QUICK,
    Const.LDC_W_QUICK,
    Const.LDC2_W_QUICK
)


internal fun codeToString(
    code: ByteArray,
    constant_pool: ConstantPool,
    index: Int,
    length: Int,
    verbose: Boolean
): String {
    val buf = StringBuilder(code.size * 20) // Should be sufficient // CHECKSTYLE IGNORE MagicNumber
    try {
        ByteSequence(code).use { byteStream ->
            for (i in 0 until index) {
                codeToString(byteStream, constant_pool, verbose)
            }
            var i = 0
            while (byteStream.available() > 0) {
                if (length < 0 || i < length) {
                    val codeToString = codeToString(byteStream, constant_pool, verbose)
                    buf.append(codeToString)
                        .append('\n')
                }
                i++
            }
        }
    } catch (e: IOException) {
        throw ClassFormatException("Byte code error: $buf", e)
    }
    return buf.toString()
}

@Throws(IOException::class)
private fun codeToString(
    bytes: ByteSequence,
    constant_pool: ConstantPool,
    verbose: Boolean
): String {
    val opcode = bytes.readUnsignedByte().toShort()
    var default_offset = 0
    val low: Int
    val high: Int
    val npairs: Int
    val index: Int
    val vindex: Int
    val constant: Int
    val match: IntArray
    val jump_table: IntArray
    var no_pad_bytes = 0
    val offset: Int
    var wide = false
    val instruction = opcode.takeIf { it !in ldcInstructions } ?: Const.LDC
    val buf = java.lang.StringBuilder(Const.getOpcodeName(instruction.toInt()))
    /* Special case: Skip (0-3) padding bytes, i.e., the
     * following bytes are 4-byte-aligned
     */if (opcode == Const.TABLESWITCH || opcode == Const.LOOKUPSWITCH) {
        val remainder = bytes.index % 4
        no_pad_bytes = if (remainder == 0) 0 else 4 - remainder
        for (i in 0 until no_pad_bytes) {
            var b: Byte
            if (bytes.readByte().also { b = it }.toInt() != 0) {
                System.err.println(
                    "Warning: Padding byte != 0 in "
                            + Const.getOpcodeName(opcode.toInt()) + ":" + b
                )
            }
        }
        // Both cases have a field default_offset in common
        default_offset = bytes.readInt()
    }
    when (opcode) {
        Const.TABLESWITCH -> {
            low = bytes.readInt()
            high = bytes.readInt()
            offset = bytes.index - 12 - no_pad_bytes - 1
            default_offset += offset
            buf.append("\tdefault = ").append(default_offset).append(", low = ").append(low)
                .append(", high = ").append(high).append("(")
            jump_table = IntArray(high - low + 1)
            var i = 0
            while (i < jump_table.size) {
                jump_table[i] = offset + bytes.readInt()
                buf.append(jump_table[i])
                if (i < jump_table.size - 1) {
                    buf.append(", ")
                }
                i++
            }
            buf.append(")")
        }

        Const.LOOKUPSWITCH -> {
            npairs = bytes.readInt()
            offset = bytes.index - 8 - no_pad_bytes - 1
            match = IntArray(npairs)
            jump_table = IntArray(npairs)
            default_offset += offset
            buf.append("\tdefault = ").append(default_offset).append(", npairs = ").append(
                npairs
            ).append(" (")
            var i = 0
            while (i < npairs) {
                match[i] = bytes.readInt()
                jump_table[i] = offset + bytes.readInt()
                buf.append("(").append(match[i]).append(", ").append(jump_table[i]).append(")")
                if (i < npairs - 1) {
                    buf.append(", ")
                }
                i++
            }
            buf.append(")")
        }

        Const.GOTO, Const.IFEQ, Const.IFGE, Const.IFGT,
        Const.IFLE, Const.IFLT, Const.JSR, Const.IFNE,
        Const.IFNONNULL, Const.IFNULL, Const.IF_ACMPEQ,
        Const.IF_ACMPNE, Const.IF_ICMPEQ, Const.IF_ICMPGE,
        Const.IF_ICMPGT, Const.IF_ICMPLE, Const.IF_ICMPLT,
        Const.IF_ICMPNE -> bytes.readShort()

        Const.GOTO_W, Const.JSR_W -> bytes.readInt()
        Const.ALOAD, Const.ASTORE, Const.DLOAD, Const.DSTORE, Const.FLOAD, Const.FSTORE, Const.ILOAD, Const.ISTORE, Const.LLOAD, Const.LSTORE, Const.RET -> {
            if (wide) {
                vindex = bytes.readUnsignedShort()
                wide = false // Clear flag
            } else {
                vindex = bytes.readUnsignedByte()
            }
            buf.append("\t\t%").append(vindex)
        }

        Const.WIDE -> {
            wide = true
            buf.append("\t(wide)")
        }

        Const.NEWARRAY -> buf.append("\t\t<").append(Const.getTypeName(bytes.readByte().toInt())).append(">")
        Const.GETFIELD, Const.GETSTATIC, Const.PUTFIELD, Const.PUTSTATIC -> {
            index = bytes.readUnsignedShort()
            buf.append("\t\t").append(
                constant_pool.constantToString(index, Const.CONSTANT_Fieldref)
            ).append(
                if (verbose) " ($index)" else ""
            )
        }

        Const.NEW, Const.CHECKCAST -> {
            buf.append("\t")
            index = bytes.readUnsignedShort()
            buf.append("\t<").append(
                constant_pool.constantToString(index, Const.CONSTANT_Class)
            )
                .append(">").append(if (verbose) " ($index)" else "")
        }

        Const.INSTANCEOF -> {
            index = bytes.readUnsignedShort()
            buf.append("\t<").append(
                constant_pool.constantToString(index, Const.CONSTANT_Class)
            )
                .append(">").append(if (verbose) " ($index)" else "")
        }

        Const.INVOKESPECIAL, Const.INVOKESTATIC -> {
            index = bytes.readUnsignedShort()
            val c = constant_pool.getConstant(index, Constant::class.java)
            // With Java8 operand may be either a CONSTANT_Methodref
            // or a CONSTANT_InterfaceMethodref.   (markro)
            buf.append("\t").append(
                constant_pool.constantToString(index, c.tag)
            )
                .append(if (verbose) " ($index)" else "")
        }

        Const.INVOKEVIRTUAL -> {
            index = bytes.readUnsignedShort()
            buf.append("\t").append(
                constant_pool.constantToString(index, Const.CONSTANT_Methodref)
            )
                .append(if (verbose) " ($index)" else "")
        }

        Const.INVOKEINTERFACE -> {
            index = bytes.readUnsignedShort()
            val nargs = bytes.readUnsignedByte() // historical, redundant
            buf.append("\t").append(
                constant_pool
                    .constantToString(index, Const.CONSTANT_InterfaceMethodref)
            )
                .append(if (verbose) " ($index)\t" else "").append(nargs).append("\t")
                .append(bytes.readUnsignedByte()) // Last byte is a reserved space
        }
        /*
        * Const.INVOKEDYNAMIC indicates lambda function call
        * Lambda function bodies do not contribute to method hash;
        * Thus it is skipped
        */
        Const.INVOKEDYNAMIC -> {
            bytes.readUnsignedShort() // read required to allow further stream processing
        }

        Const.LDC_W, Const.LDC2_W -> {
            index = bytes.readUnsignedShort()

            buf.append("\t\t").append(
                constant_pool.constantToString(
                    index, constant_pool.getConstant(index, Constant::class.java)
                        .tag
                )
            ).append(if (verbose) " ($index)" else "")
        }

        Const.LDC -> {
            index = bytes.readUnsignedByte()
            buf.append("\t\t").append(
                constant_pool.constantToString(
                    index, constant_pool.getConstant(index, Constant::class.java)
                        .tag
                )
            ).append(if (verbose) " ($index)" else "")
        }

        Const.ANEWARRAY -> {
            index = bytes.readUnsignedShort()
            buf.append("\t\t<").append(
                Utility.compactClassName(
                    constant_pool.getConstantString(
                        index,
                        Const.CONSTANT_Class
                    ), false
                )
            ).append(">").append(
                if (verbose) " ($index)" else ""
            )
        }

        Const.MULTIANEWARRAY -> {
            index = bytes.readUnsignedShort()
            val dimensions = bytes.readUnsignedByte()
            buf.append("\t<").append(
                Utility.compactClassName(
                    constant_pool.getConstantString(
                        index,
                        Const.CONSTANT_Class
                    ), false
                )
            ).append(">\t").append(dimensions)
                .append(if (verbose) " ($index)" else "")
        }

        Const.IINC -> {
            if (wide) {
                vindex = bytes.readUnsignedShort()
                constant = bytes.readShort().toInt()
                wide = false
            } else {
                vindex = bytes.readUnsignedByte()
                constant = bytes.readByte().toInt()
            }
            buf.append("\t\t%").append(vindex).append("\t").append(constant)
        }

        else -> if (Const.getNoOfOperands(opcode.toInt()) > 0) {
            var i = 0
            while (i < Const.getOperandTypeCount(opcode.toInt())) {
                buf.append("\t\t")
                when (Const.getOperandType(opcode.toInt(), i)) {
                    Const.T_BYTE.toShort() -> buf.append(bytes.readByte().toInt())
                    Const.T_SHORT.toShort() -> buf.append(bytes.readShort().toInt())
                    Const.T_INT.toShort() -> buf.append(bytes.readInt())
                    else -> throw IllegalStateException("Unreachable default case reached!")
                }
                i++
            }
        }
    }
    return buf.toString()
}
