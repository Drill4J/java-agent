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
 * A constant whose value is computed at runtime, with a bootstrap method.
 *
 * @author Remi Forax
 */
class ConstantDynamic(
    /** The constant name (can be arbitrary).  */
    private val name: String?,
    /** The constant type (must be a field descriptor).  */
    private val descriptor: String?,
    bootstrapMethod: Handle?,
    vararg bootstrapMethodArguments: Any,
) {
    /** The bootstrap method to use to compute the constant value at runtime.  */
    private val bootstrapMethod: Handle?

    /**
     * The arguments to pass to the bootstrap method, in order to compute the constant value at
     * runtime.
     */
    private val bootstrapMethodArguments: Array<out Any>

    /**
     * Returns the name of this constant.
     *
     * @return the name of this constant.
     */
    fun getName(): String? {
        return name
    }

    /**
     * Returns the type of this constant.
     *
     * @return the type of this constant, as a field descriptor.
     */
    fun getDescriptor(): String? {
        return descriptor
    }

    /**
     * Returns the bootstrap method used to compute the value of this constant.
     *
     * @return the bootstrap method used to compute the value of this constant.
     */
    fun getBootstrapMethod(): Handle? {
        return bootstrapMethod
    }

    /**
     * Returns the number of arguments passed to the bootstrap method, in order to compute the value
     * of this constant.
     *
     * @return the number of arguments passed to the bootstrap method, in order to compute the value
     * of this constant.
     */
    fun getBootstrapMethodArgumentCount(): Int {
        return bootstrapMethodArguments.size
    }

    /**
     * Returns an argument passed to the bootstrap method, in order to compute the value of this
     * constant.
     *
     * @param index an argument index, between 0 and [.getBootstrapMethodArgumentCount]
     * (exclusive).
     * @return the argument passed to the bootstrap method, with the given index.
     */
    fun getBootstrapMethodArgument(index: Int): Any {
        return bootstrapMethodArguments[index]
    }

    /**
     * Returns the arguments to pass to the bootstrap method, in order to compute the value of this
     * constant. WARNING: this array must not be modified, and must not be returned to the user.
     *
     * @return the arguments to pass to the bootstrap method, in order to compute the value of this
     * constant.
     */
    fun getBootstrapMethodArgumentsUnsafe(): Array<out Any> {
        return bootstrapMethodArguments
    }

    /**
     * Returns the size of this constant.
     *
     * @return the size of this constant, i.e., 2 for `long` and `double`, 1 otherwise.
     */
    fun getSize(): Int {
        val firstCharOfDescriptor = descriptor!![0]
        return if (firstCharOfDescriptor == 'J' || firstCharOfDescriptor == 'D') 2 else 1
    }

    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) {
            return true
        }
        if (`object` !is ConstantDynamic) {
            return false
        }
        val constantDynamic = `object`
        return (name == constantDynamic.name && descriptor == constantDynamic.descriptor && bootstrapMethod!!.equals(
            constantDynamic.bootstrapMethod)
                && bootstrapMethodArguments.contentEquals(constantDynamic.bootstrapMethodArguments))
    }

    override fun hashCode(): Int {
        return (name.hashCode()
//                xor Int.rotateLeft(descriptor.hashCode(), 8)
//                xor Integer.rotateLeft(bootstrapMethod.hashCode(), 16)
//                xor Integer.rotateLeft(Arrays.hashCode(bootstrapMethodArgumentsUnsafe), 24))
                )
    }

//    override fun toString(): String {
//        return (name
//                + " : "
//                + descriptor
//                + ' '
//                + bootstrapMethod
//                + ' '
//                + Arrays.toString(bootstrapMethodArgumentsUnsafe))
//    }

    /**
     * Constructs a new [ConstantDynamic].
     *
     * @param name the constant name (can be arbitrary).
     * @param descriptor the constant type (must be a field descriptor).
     * @param bootstrapMethod the bootstrap method to use to compute the constant value at runtime.
     * @param bootstrapMethodArguments the arguments to pass to the bootstrap method, in order to
     * compute the constant value at runtime.
     */
    init {
        this.bootstrapMethod = bootstrapMethod
        this.bootstrapMethodArguments = bootstrapMethodArguments
    }
}
