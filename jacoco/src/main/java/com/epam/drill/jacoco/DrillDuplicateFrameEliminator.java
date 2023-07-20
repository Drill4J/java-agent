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

import org.jacoco.core.internal.instr.*;
import org.objectweb.asm.*;

/**
 * It duplicates from {@link DuplicateFrameEliminator}.
 * Eliminates consecutive stackmap frame definitions which causes ASM to create
 * invalid class files. This situation occurs when the original class files
 * contains additional stackmap frames at unexpected offsets, which is case for
 * some class files compiled with ECJ.
 */
public class DrillDuplicateFrameEliminator extends MethodVisitor {

    private boolean instruction;

    public DrillDuplicateFrameEliminator(final MethodVisitor mv) {
        super(InstrSupport.ASM_API_VERSION, mv);
        instruction = true;
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
                           final Object[] local, final int nStack, final Object[] stack) {
        if (instruction) {
            instruction = false;
            mv.visitFrame(type, nLocal, local, nStack, stack);
        }
    }

    @Override
    public void visitInsn(final int opcode) {
        instruction = true;
        mv.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        instruction = true;
        mv.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        instruction = true;
        mv.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        instruction = true;
        mv.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        instruction = true;
        mv.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc, final boolean itf) {
        instruction = true;
        mv.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc,
                                       final Handle bsm, final Object... bsmArgs) {
        instruction = true;
        mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        instruction = true;
        mv.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        instruction = true;
        mv.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        instruction = true;
        mv.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
                                     final Label dflt, final Label... labels) {
        instruction = true;
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
                                      final Label[] labels) {
        instruction = true;
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        instruction = true;
        mv.visitMultiANewArrayInsn(desc, dims);
    }

}
