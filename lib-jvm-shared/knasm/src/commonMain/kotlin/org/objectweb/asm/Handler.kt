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
 * Information about an exception handler. Corresponds to an element of the exception_table array of
 * a Code attribute, as defined in the Java Virtual Machine Specification (JVMS). Handler instances
 * can be chained together, with their [.nextHandler] field, to describe a full JVMS
 * exception_table array.
 *
 * @see [JVMS
 * 4.7.3](https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html.jvms-4.7.3)
 *
 * @author Eric Bruneton
 */
internal class Handler(
    startPc: Label?,
    endPc: Label?,
    handlerPc: Label?,
    catchType: Int,
    catchTypeDescriptor: String?
) {
    /**
     * The start_pc field of this JVMS exception_table entry. Corresponds to the beginning of the
     * exception handler's scope (inclusive).
     */
    val startPc: Label?

    /**
     * The end_pc field of this JVMS exception_table entry. Corresponds to the end of the exception
     * handler's scope (exclusive).
     */
    val endPc: Label?

    /**
     * The handler_pc field of this JVMS exception_table entry. Corresponding to the beginning of the
     * exception handler's code.
     */
    val handlerPc: Label?

    /**
     * The catch_type field of this JVMS exception_table entry. This is the constant pool index of the
     * internal name of the type of exceptions handled by this handler, or 0 to catch any exceptions.
     */
    val catchType: Int

    /**
     * The internal name of the type of exceptions handled by this handler, or null to
     * catch any exceptions.
     */
    val catchTypeDescriptor: String?

    /** The next exception handler.  */
    var nextHandler: Handler? = null

    /**
     * Constructs a new Handler from the given one, with a different scope.
     *
     * @param handler an existing Handler.
     * @param startPc the start_pc field of this JVMS exception_table entry.
     * @param endPc the end_pc field of this JVMS exception_table entry.
     */
    constructor(handler: Handler, startPc: Label?, endPc: Label) : this(startPc,
        endPc,
        handler.handlerPc,
        handler.catchType,
        handler.catchTypeDescriptor) {
        nextHandler = handler.nextHandler
    }

    companion object {
        /**
         * Removes the range between start and end from the Handler list that begins with the given
         * element.
         *
         * @param firstHandler the beginning of a Handler list. May be null.
         * @param start the start of the range to be removed.
         * @param end the end of the range to be removed. Maybe null.
         * @return the exception handler list with the start-end range removed.
         */
        fun removeRange(firstHandler: Handler?, start: Label, end: Label?): Handler? {
            if (firstHandler == null) {
                return null
            } else {
                firstHandler.nextHandler = removeRange(firstHandler.nextHandler, start, end)
            }
            val handlerStart: Int = firstHandler.startPc!!.bytecodeOffset
            val handlerEnd: Int = firstHandler.endPc!!.bytecodeOffset
            val rangeStart: Int = start.bytecodeOffset
            val rangeEnd = if (end == null) Int.MAX_VALUE else end.bytecodeOffset
            // Return early if [handlerStart,handlerEnd[ and [rangeStart,rangeEnd[ don't intersect.
            if (rangeStart >= handlerEnd || rangeEnd <= handlerStart) {
                return firstHandler
            }
            return if (rangeStart <= handlerStart) {
                if (rangeEnd >= handlerEnd) {
                    // If [handlerStart,handlerEnd[ is included in [rangeStart,rangeEnd[, remove firstHandler.
                    firstHandler.nextHandler
                } else {
                    // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ = [rangeEnd,handlerEnd[
                    Handler(firstHandler, end, firstHandler.endPc)
                }
            } else if (rangeEnd >= handlerEnd) {
                // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ = [handlerStart,rangeStart[
                Handler(firstHandler, firstHandler.startPc, start)
            } else {
                // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ =
                //     [handlerStart,rangeStart[ + [rangeEnd,handerEnd[
                firstHandler.nextHandler = Handler(firstHandler, end, firstHandler.endPc)
                Handler(firstHandler, firstHandler.startPc, start)
            }
        }

        /**
         * Returns the number of elements of the Handler list that begins with the given element.
         *
         * @param firstHandler the beginning of a Handler list. May be null.
         * @return the number of elements of the Handler list that begins with 'handler'.
         */
        fun getExceptionTableLength(firstHandler: Handler?): Int {
            var length = 0
            var handler = firstHandler
            while (handler != null) {
                length++
                handler = handler.nextHandler
            }
            return length
        }

        /**
         * Returns the size in bytes of the JVMS exception_table corresponding to the Handler list that
         * begins with the given element. *This includes the exception_table_length field.*
         *
         * @param firstHandler the beginning of a Handler list. May be null.
         * @return the size in bytes of the exception_table_length and exception_table structures.
         */
        fun getExceptionTableSize(firstHandler: Handler?): Int {
            return 2 + 8 * getExceptionTableLength(firstHandler)
        }

        /**
         * Puts the JVMS exception_table corresponding to the Handler list that begins with the given
         * element. *This includes the exception_table_length field.*
         *
         * @param firstHandler the beginning of a Handler list. May be null.
         * @param output where the exception_table_length and exception_table structures must be put.
         */
        fun putExceptionTable(firstHandler: Handler?, output: ByteVector) {
            output.putShort(getExceptionTableLength(firstHandler))
            var handler = firstHandler
            while (handler != null) {
                output
                    .putShort(handler.startPc!!.bytecodeOffset)
                    .putShort(handler.endPc!!.bytecodeOffset)
                    .putShort(handler.handlerPc!!.bytecodeOffset)
                    .putShort(handler.catchType)
                handler = handler.nextHandler
            }
        }
    }

    /**
     * Constructs a new Handler.
     *
     * @param startPc the start_pc field of this JVMS exception_table entry.
     * @param endPc the end_pc field of this JVMS exception_table entry.
     * @param handlerPc the handler_pc field of this JVMS exception_table entry.
     * @param catchType The catch_type field of this JVMS exception_table entry.
     * @param catchTypeDescriptor The internal name of the type of exceptions handled by this handler,
     * or null to catch any exceptions.
     */
    init {
        this.startPc = startPc
        this.endPc = endPc
        this.handlerPc = handlerPc
        this.catchType = catchType
        this.catchTypeDescriptor = catchTypeDescriptor
    }
}
