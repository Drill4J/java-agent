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
 * A reference to a field or a method.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
class Handle
/**
 * Constructs a new field or method handle.
 *
 * @param tag the kind of field or method designated by this Handle. Must be [     ][Opcodes.H_GETFIELD], [Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD], [     ][Opcodes.H_PUTSTATIC], [Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC],
 * [Opcodes.H_INVOKESPECIAL], [Opcodes.H_NEWINVOKESPECIAL] or [     ][Opcodes.H_INVOKEINTERFACE].
 * @param owner the internal name of the class that owns the field or method designated by this
 * handle.
 * @param name the name of the field or method designated by this handle.
 * @param descriptor the descriptor of the field or method designated by this handle.
 * @param isInterface whether the owner is an interface or not.
 */(
    /**
     * The kind of field or method designated by this Handle. Should be [Opcodes.H_GETFIELD],
     * [Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD], [Opcodes.H_PUTSTATIC], [ ][Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC], [Opcodes.H_INVOKESPECIAL],
     * [Opcodes.H_NEWINVOKESPECIAL] or [Opcodes.H_INVOKEINTERFACE].
     */
    private val tag: Int,
    /** The internal name of the class that owns the field or method designated by this handle.  */
    private val owner: String?,
    /** The name of the field or method designated by this handle.  */
    private val name: String?,
    /** The descriptor of the field or method designated by this handle.  */
    private val descriptor: String?,
    /** Whether the owner is an interface or not.  */
    private val isInterface: Boolean
) {
    /**
     * Constructs a new field or method handle.
     *
     * @param tag the kind of field or method designated by this Handle. Must be [     ][Opcodes.H_GETFIELD], [Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD], [     ][Opcodes.H_PUTSTATIC], [Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC],
     * [Opcodes.H_INVOKESPECIAL], [Opcodes.H_NEWINVOKESPECIAL] or [     ][Opcodes.H_INVOKEINTERFACE].
     * @param owner the internal name of the class that owns the field or method designated by this
     * handle.
     * @param name the name of the field or method designated by this handle.
     * @param descriptor the descriptor of the field or method designated by this handle.
     */
    @Deprecated("""this constructor has been superseded by {@link #Handle(int, String, String, String,
   *     boolean)}.""")
    constructor(tag: Int, owner: String?, name: String?, descriptor: String?) : this(tag,
        owner,
        name,
        descriptor,
        tag == Opcodes.H_INVOKEINTERFACE) {
    }

    /**
     * Returns the kind of field or method designated by this handle.
     *
     * @return [Opcodes.H_GETFIELD], [Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD],
     * [Opcodes.H_PUTSTATIC], [Opcodes.H_INVOKEVIRTUAL], [     ][Opcodes.H_INVOKESTATIC], [Opcodes.H_INVOKESPECIAL], [     ][Opcodes.H_NEWINVOKESPECIAL] or [Opcodes.H_INVOKEINTERFACE].
     */
    fun getTag(): Int {
        return tag
    }

    /**
     * Returns the internal name of the class that owns the field or method designated by this handle.
     *
     * @return the internal name of the class that owns the field or method designated by this handle.
     */
    fun getOwner(): String? {
        return owner
    }

    /**
     * Returns the name of the field or method designated by this handle.
     *
     * @return the name of the field or method designated by this handle.
     */
    fun getName(): String? {
        return name
    }

    /**
     * Returns the descriptor of the field or method designated by this handle.
     *
     * @return the descriptor of the field or method designated by this handle.
     */
    fun getDesc(): String? {
        return descriptor
    }

    /**
     * Returns true if the owner of the field or method designated by this handle is an interface.
     *
     * @return true if the owner of the field or method designated by this handle is an interface.
     */
    fun isInterface(): Boolean {
        return isInterface
    }

    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) {
            return true
        }
        if (`object` !is Handle) {
            return false
        }
        val handle = `object`
        return tag == handle.tag && isInterface == handle.isInterface && owner == handle.owner && name == handle.name && descriptor == handle.descriptor
    }

    override fun hashCode(): Int {
        return (tag
                + (if (isInterface) 64 else 0)
                + owner.hashCode() * name.hashCode() * descriptor.hashCode())
    }

    /**
     * Returns the textual representation of this handle. The textual representation is:
     *
     *
     *  * for a reference to a class: owner "." name descriptor " (" tag ")",
     *  * for a reference to an interface: owner "." name descriptor " (" tag " itf)".
     *
     */
    override fun toString(): String {
        return owner + '.' + name + descriptor + " (" + tag + (if (isInterface) " itf" else "") + ')'
    }
}
