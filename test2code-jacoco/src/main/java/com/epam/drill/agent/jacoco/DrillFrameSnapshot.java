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
package com.epam.drill.agent.jacoco;

import java.util.ArrayList;
import java.util.List;

import org.jacoco.core.internal.flow.IFrame;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;

/**
 * IFrame implementation which creates snapshots from an {@link AnalyzerAdapter}
 */
class DrillFrameSnapshot implements IFrame {

	private static final DrillFrameSnapshot NOP = new DrillFrameSnapshot(null, null);

	private final Object[] locals;
	private final Object[] stack;

	private DrillFrameSnapshot(final Object[] locals, final Object[] stack) {
		this.locals = locals;
		this.stack = stack;
	}

	/**
	 * Create a IFrame instance based on the given analyzer.
	 *
	 * @param analyzer
	 *            analyzer instance or <code>null</code>
	 * @param popCount
	 *            number of items to remove from the operand stack
	 * @return IFrame instance. In case the analyzer is <code>null</code> or
	 *         does not contain stackmap information a "NOP" IFrame is returned.
	 */
	static IFrame create(final AnalyzerAdapter analyzer, final int popCount) {
		if (analyzer == null || analyzer.locals == null) {
			return NOP;
		}
		final Object[] locals = reduce(analyzer.locals, 0);
		final Object[] stack = reduce(analyzer.stack, popCount);
		return new DrillFrameSnapshot(locals, stack);
	}

	/**
	 * Reduce double word types into a single slot as required
	 * {@link MethodVisitor#visitFrame(int, int, Object[], int, Object[])}
	 * method.
	 */
	private static Object[] reduce(final List<Object> source,
			final int popCount) {
		final List<Object> copy = new ArrayList<>(source);
		final int size = source.size() - popCount;
		copy.subList(size, source.size()).clear();
		for (int i = size; --i >= 0;) {
			final Object type = source.get(i);
			if (type == Opcodes.LONG || type == Opcodes.DOUBLE) {
				copy.remove(i + 1);
			}
		}
		return copy.toArray();
	}

	// === IFrame implementation ===

	public void accept(final MethodVisitor mv) {
		if (locals != null) {
			mv.visitFrame(Opcodes.F_NEW, locals.length, locals, stack.length,
					stack);
		}
	}

}
