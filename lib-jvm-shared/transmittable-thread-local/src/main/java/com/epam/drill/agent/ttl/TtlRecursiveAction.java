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
package com.epam.drill.agent.ttl;

import com.epam.drill.agent.ttl.spi.TtlEnhanced;
import com.epam.drill.agent.ttl.threadpool.TtlForkJoinPoolHelper;
import com.epam.drill.agent.ttl.threadpool.agent.TtlAgent;

import java.util.concurrent.ForkJoinTask;

import static com.epam.drill.agent.ttl.TransmittableThreadLocal.Transmitter.*;

/**
 * A recursive resultless {@link ForkJoinTask} enhanced by {@link TransmittableThreadLocal}.
 * <p>
 * Recommend to use {@link TtlAgent};
 * Specially for {@code Java 8} {@link java.util.stream.Stream} and {@link java.util.concurrent.CompletableFuture},
 * these async task are executed by {@link java.util.concurrent.ForkJoinPool} via {@link ForkJoinTask} at the bottom.
 *
 * @author LNAmp
 * @see java.util.concurrent.RecursiveAction
 * @see TtlForkJoinPoolHelper
 * @see TtlAgent
 * @since 2.4.0
 */
public abstract class TtlRecursiveAction extends ForkJoinTask<Void> implements TtlEnhanced {

    private static final long serialVersionUID = -5753568484583412377L;

    private final Object captured = capture();

    protected TtlRecursiveAction() {
    }

    /**
     * The main computation performed by this task.
     */
    protected abstract void compute();

    /**
     * see {@link ForkJoinTask#getRawResult()}
     */
    public final Void getRawResult() {
        return null;
    }

    /**
     * see {@link ForkJoinTask#setRawResult(Object)}
     */
    protected final void setRawResult(Void mustBeNull) {
    }

    /**
     * Implements execution conventions for RecursiveActions.
     */
    protected final boolean exec() {
        final Object backup = replay(captured);
        try {
            compute();
            return true;
        } finally {
            restore(backup);
        }
    }
}
