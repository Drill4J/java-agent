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
package com.epam.drill.jacoco;

import org.jacoco.core.internal.instr.IProbeArrayStrategy;
import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.*;

/**
 * It is based on {@link ProbeInserter}, but use wrapper behind BooleanArray.
 * Internal utility to add probes into the control flow of a method. The code
 * for a probe simply sets a certain slot of a booleanArray to true. In
 * addition the probe array has to be retrieved at the beginning of the method
 * and stored in a local variable.
 */
public class BooleanArrayProbeInserter extends MethodVisitor {

    public final static String PROBE_IMPL = AgentProbes.class.getCanonicalName().replace(".", "/");

    private final IProbeArrayStrategy arrayStrategy;

    /**
     * <code>true</code> if method is a class or interface initialization
     * method.
     */
    private final boolean clinit;

    /**
     * Position of the inserted variable.
     */
    private final int variable;

    /**
     * Maximum stack usage of the code to access the probe array.
     */
    private int accessorStackSize;

    /**
     * Creates a new {@link BooleanArrayProbeInserter}.
     *
     * @param access        access flags of the adapted method
     * @param name          the method's name
     * @param desc          the method's descriptor
     * @param mv            the method visitor to which this adapter delegates calls
     * @param arrayStrategy callback to create the code that retrieves the reference to
     *                      the probe array
     */
    public BooleanArrayProbeInserter(final int access, final String name, final String desc,
                                     final MethodVisitor mv, final IProbeArrayStrategy arrayStrategy) {
        super(InstrSupport.ASM_API_VERSION, mv);
        //todo use const:
//		this.clinit = InstrSupport.CLINIT_NAME.equals(name);
        this.clinit = "<clinit>".equals(name);
        this.arrayStrategy = arrayStrategy;
        int pos = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
        for (final Type t : Type.getArgumentTypes(desc)) {
            pos += t.getSize();
        }
        variable = pos;
    }

    public void insertProbe(final int id) {
        // For a probe we set the corresponding position in the boolean[] array
        // to true.

        mv.visitVarInsn(Opcodes.ALOAD, variable);

        // Stack[0]: $PROBE_IMPL

        InstrSupport.push(mv, id);

        // Stack[1]: I
        // Stack[0]: $PROBE_IMPL

//		mv.visitInsn(Opcodes.ICONST_1);

        visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, PROBE_IMPL, "set", "(I)V",
                false
        );
    }

    /**
     * visitVarInsn(Opcodes.ALOAD, variable)
     * InstrSupport.push(this, probeCount)
     * visitMethodInsn(
     * Opcodes.INVOKEVIRTUAL, PROBE_IMPL, "set", "(I)V",
     * false
     * )
     */

    @Override
    public void visitCode() {
        accessorStackSize = arrayStrategy.storeInstance(mv, clinit, variable);
        mv.visitCode();
    }

    @Override
    public final void visitVarInsn(final int opcode, final int var) {
        mv.visitVarInsn(opcode, map(var));
    }

    @Override
    public final void visitIincInsn(final int var, final int increment) {
        mv.visitIincInsn(map(var), increment);
    }

    @Override
    public final void visitLocalVariable(final String name, final String desc,
                                         final String signature, final Label start, final Label end,
                                         final int index) {
        mv.visitLocalVariable(name, desc, signature, start, end, map(index));
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(final int typeRef,
                                                          final TypePath typePath, final Label[] start, final Label[] end,
                                                          final int[] index, final String descriptor, final boolean visible) {
        final int[] newIndex = new int[index.length];
        for (int i = 0; i < newIndex.length; i++) {
            newIndex[i] = map(index[i]);
        }
        return mv.visitLocalVariableAnnotation(typeRef, typePath, start, end,
                newIndex, descriptor, visible);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        // Max stack size of the probe code is 3 which can add to the
        // original stack size depending on the probe locations. The accessor
        // stack size is an absolute maximum, as the accessor code is inserted
        // at the very beginning of each method when the stack size is empty.
        final int increasedStack = Math.max(maxStack + 3, accessorStackSize);
        mv.visitMaxs(increasedStack, maxLocals + 1);
    }

    private int map(final int var) {
        if (var < variable) {
            return var;
        } else {
            return var + 1;
        }
    }

    @Override
    public final void visitFrame(final int type, final int nLocal,
                                 final Object[] local, final int nStack, final Object[] stack) {

        if (type != Opcodes.F_NEW) { // uncompressed frame
            throw new IllegalArgumentException(
                    "ClassReader.accept() should be called with EXPAND_FRAMES flag");
        }

        final Object[] newLocal = new Object[Math.max(nLocal, variable) + 1];
        int idx = 0; // Arrays index for existing locals
        int newIdx = 0; // Array index for new locals
        int pos = 0; // Current variable position
        while (idx < nLocal || pos <= variable) {
            if (pos == variable) {
//				newLocal[newIdx++] = InstrSupport.DATAFIELD_DESC;
                newLocal[newIdx++] = PROBE_IMPL;
                //TODO InstrSupport change value of constants to bitSet:
//				InstrSupport.DATAFIELD_DESC;
//  			InstrSupport.INITMETHOD_DESC
                pos++;
            } else {
                if (idx < nLocal) {
                    final Object t = local[idx++];
                    newLocal[newIdx++] = t;
                    pos++;
                    if (t == Opcodes.LONG || t == Opcodes.DOUBLE) {
                        pos++;
                    }
                } else {
                    // Fill unused slots with TOP
                    newLocal[newIdx++] = Opcodes.TOP;
                    pos++;
                }
            }
        }
        mv.visitFrame(type, newIdx, newLocal, nStack, stack);
    }

}
