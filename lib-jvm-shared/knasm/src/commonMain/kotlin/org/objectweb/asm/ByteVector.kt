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

import System

/**
 * A dynamically extensible vector of bytes. This class is roughly equivalent to a DataOutputStream
 * on top of a ByteArrayOutputStream, but is more efficient.
 *
 * @author Eric Bruneton
 */
class ByteVector {
    /** The content of this vector. Only the first [.length] bytes contain real data.  */
    var data: ByteArray

    /** The actual number of bytes in this vector.  */
    var length = 0

    /** Constructs a new [ByteVector] with a default initial capacity.  */
    constructor() {
        data = ByteArray(64)
    }

    /**
     * Constructs a new [ByteVector] with the given initial capacity.
     *
     * @param initialCapacity the initial capacity of the byte vector to be constructed.
     */
    constructor(initialCapacity: Int) {
        data = ByteArray(initialCapacity)
    }

    /**
     * Constructs a new [ByteVector] from the given initial data.
     *
     * @param data the initial data of the new byte vector.
     */
    internal constructor(data: ByteArray) {
        this.data = data
        length = data.size
    }

    /**
     * Returns the actual number of bytes in this vector.
     *
     * @return the actual number of bytes in this vector.
     */
    fun size(): Int {
        return length
    }

    /**
     * Puts a byte into this byte vector. The byte vector is automatically enlarged if necessary.
     *
     * @param byteValue a byte.
     * @return this byte vector.
     */
    fun putByte(byteValue: Int): ByteVector {
        var currentLength = length
        if (currentLength + 1 > data.size) {
            enlarge(1)
        }
        data[currentLength++] = byteValue.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts two bytes into this byte vector. The byte vector is automatically enlarged if necessary.
     *
     * @param byteValue1 a byte.
     * @param byteValue2 another byte.
     * @return this byte vector.
     */
    fun put11(byteValue1: Int, byteValue2: Int): ByteVector {
        var currentLength = length
        if (currentLength + 2 > data.size) {
            enlarge(2)
        }
        val currentData = data
        currentData[currentLength++] = byteValue1.toByte()
        currentData[currentLength++] = byteValue2.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts a short into this byte vector. The byte vector is automatically enlarged if necessary.
     *
     * @param shortValue a short.
     * @return this byte vector.
     */
    fun putShort(shortValue: Int): ByteVector {
        var currentLength = length
        if (currentLength + 2 > data.size) {
            enlarge(2)
        }
        val currentData = data
        currentData[currentLength++] = (shortValue ushr 8).toByte()
        currentData[currentLength++] = shortValue.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts a byte and a short into this byte vector. The byte vector is automatically enlarged if
     * necessary.
     *
     * @param byteValue a byte.
     * @param shortValue a short.
     * @return this byte vector.
     */
    fun put12(byteValue: Int, shortValue: Int): ByteVector {
        var currentLength = length
        if (currentLength + 3 > data.size) {
            enlarge(3)
        }
        val currentData = data
        currentData[currentLength++] = byteValue.toByte()
        currentData[currentLength++] = (shortValue ushr 8).toByte()
        currentData[currentLength++] = shortValue.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts two bytes and a short into this byte vector. The byte vector is automatically enlarged if
     * necessary.
     *
     * @param byteValue1 a byte.
     * @param byteValue2 another byte.
     * @param shortValue a short.
     * @return this byte vector.
     */
    fun put112(byteValue1: Int, byteValue2: Int, shortValue: Int): ByteVector {
        var currentLength = length
        if (currentLength + 4 > data.size) {
            enlarge(4)
        }
        val currentData = data
        currentData[currentLength++] = byteValue1.toByte()
        currentData[currentLength++] = byteValue2.toByte()
        currentData[currentLength++] = (shortValue ushr 8).toByte()
        currentData[currentLength++] = shortValue.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts an int into this byte vector. The byte vector is automatically enlarged if necessary.
     *
     * @param intValue an int.
     * @return this byte vector.
     */
    fun putInt(intValue: Int): ByteVector {
        var currentLength = length
        if (currentLength + 4 > data.size) {
            enlarge(4)
        }
        val currentData = data
        currentData[currentLength++] = (intValue ushr 24).toByte()
        currentData[currentLength++] = (intValue ushr 16).toByte()
        currentData[currentLength++] = (intValue ushr 8).toByte()
        currentData[currentLength++] = intValue.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts one byte and two shorts into this byte vector. The byte vector is automatically enlarged
     * if necessary.
     *
     * @param byteValue a byte.
     * @param shortValue1 a short.
     * @param shortValue2 another short.
     * @return this byte vector.
     */
    fun put122(byteValue: Int, shortValue1: Int, shortValue2: Int): ByteVector {
        var currentLength = length
        if (currentLength + 5 > data.size) {
            enlarge(5)
        }
        val currentData = data
        currentData[currentLength++] = byteValue.toByte()
        currentData[currentLength++] = (shortValue1 ushr 8).toByte()
        currentData[currentLength++] = shortValue1.toByte()
        currentData[currentLength++] = (shortValue2 ushr 8).toByte()
        currentData[currentLength++] = shortValue2.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts a long into this byte vector. The byte vector is automatically enlarged if necessary.
     *
     * @param longValue a long.
     * @return this byte vector.
     */
    fun putLong(longValue: Long): ByteVector {
        var currentLength = length
        if (currentLength + 8 > data.size) {
            enlarge(8)
        }
        val currentData = data
        var intValue = (longValue ushr 32).toInt()
        currentData[currentLength++] = (intValue ushr 24).toByte()
        currentData[currentLength++] = (intValue ushr 16).toByte()
        currentData[currentLength++] = (intValue ushr 8).toByte()
        currentData[currentLength++] = intValue.toByte()
        intValue = longValue.toInt()
        currentData[currentLength++] = (intValue ushr 24).toByte()
        currentData[currentLength++] = (intValue ushr 16).toByte()
        currentData[currentLength++] = (intValue ushr 8).toByte()
        currentData[currentLength++] = intValue.toByte()
        length = currentLength
        return this
    }

    /**
     * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if
     * necessary.
     *
     * @param stringValue a String whose UTF8 encoded length must be less than 65536.
     * @return this byte vector.
     */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
    fun putUTF8(stringValue: String?): ByteVector {
        val charLength = stringValue!!.length
        if (charLength > 65535) {
            throw IllegalArgumentException("UTF8 string too large")
        }
        var currentLength = length
        if (currentLength + 2 + charLength > data.size) {
            enlarge(2 + charLength)
        }
        val currentData = data
        // Optimistic algorithm: instead of computing the byte length and then serializing the string
        // (which requires two loops), we assume the byte length is equal to char length (which is the
        // most frequent case), and we start serializing the string right away. During the
        // serialization, if we find that this assumption is wrong, we continue with the general method.
        currentData[currentLength++] = (charLength ushr 8).toByte()
        currentData[currentLength++] = charLength.toByte()
        for (i in 0 until charLength) {
            val charValue = stringValue[i]
            if (charValue >= '\u0001' && charValue <= '\u007F') {
                currentData[currentLength++] = charValue.toByte()
            } else {
                length = currentLength
                return encodeUtf8(stringValue, i, 65535)
            }
        }
        length = currentLength
        return this
    }

    /**
     * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if
     * necessary. The string length is encoded in two bytes before the encoded characters, if there is
     * space for that (i.e. if this.length - offset - 2 &gt;= 0).
     *
     * @param stringValue the String to encode.
     * @param offset the index of the first character to encode. The previous characters are supposed
     * to have already been encoded, using only one byte per character.
     * @param maxByteLength the maximum byte length of the encoded string, including the already
     * encoded characters.
     * @return this byte vector.
     */
    fun encodeUtf8(stringValue: String?, offset: Int, maxByteLength: Int): ByteVector {
        val charLength = stringValue!!.length
        var byteLength = offset
        for (i in offset until charLength) {
            val charValue = stringValue[i]
            if (charValue.toInt() >= 0x0001 && charValue.toInt() <= 0x007F) {
                byteLength++
            } else if (charValue.toInt() <= 0x07FF) {
                byteLength += 2
            } else {
                byteLength += 3
            }
        }
        if (byteLength > maxByteLength) {
            throw IllegalArgumentException("UTF8 string too large")
        }
        // Compute where 'byteLength' must be stored in 'data', and store it at this location.
        val byteLengthOffset = length - offset - 2
        if (byteLengthOffset >= 0) {
            data[byteLengthOffset] = (byteLength ushr 8).toByte()
            data[byteLengthOffset + 1] = byteLength.toByte()
        }
        if (length + byteLength - offset > data.size) {
            enlarge(byteLength - offset)
        }
        var currentLength = length
        for (i in offset until charLength) {
            val charValue = stringValue[i]
            if (charValue.toInt() >= 0x0001 && charValue.toInt() <= 0x007F) {
                data[currentLength++] = charValue.toByte()
            } else if (charValue.toInt() <= 0x07FF) {
                data[currentLength++] = (0xC0 or charValue.toInt() shr 6 and 0x1F).toByte()
                data[currentLength++] = (0x80 or charValue.toInt() and 0x3F).toByte()
            } else {
                data[currentLength++] = (0xE0 or charValue.toInt() shr 12 and 0xF).toByte()
                data[currentLength++] = (0x80 or charValue.toInt() shr 6 and 0x3F).toByte()
                data[currentLength++] = (0x80 or charValue.toInt() and 0x3F).toByte()
            }
        }
        length = currentLength
        return this
    }

    /**
     * Puts an array of bytes into this byte vector. The byte vector is automatically enlarged if
     * necessary.
     *
     * @param byteArrayValue an array of bytes. May be null to put `byteLength` null
     * bytes into this byte vector.
     * @param byteOffset index of the first byte of byteArrayValue that must be copied.
     * @param byteLength number of bytes of byteArrayValue that must be copied.
     * @return this byte vector.
     */
    fun putByteArray(
        byteArrayValue: ByteArray?, byteOffset: Int, byteLength: Int
    ): ByteVector {
        if (length + byteLength > data.size) {
            enlarge(byteLength)
        }
        if (byteArrayValue != null) {
            System.arraycopy(byteArrayValue, byteOffset, data, length, byteLength)
        }
        length += byteLength
        return this
    }

    /**
     * Enlarges this byte vector so that it can receive 'size' more bytes.
     *
     * @param size number of additional bytes that this byte vector should be able to receive.
     */
    private fun enlarge(size: Int) {
        if (length > data.size) {
            throw AssertionError("Internal error")
        }
        val doubleCapacity = 2 * data.size
        val minimalCapacity = length + size
        val newData = ByteArray(if (doubleCapacity > minimalCapacity) doubleCapacity else minimalCapacity)
        System.arraycopy(data, 0, newData, 0, length)
        data = newData
    }
}
