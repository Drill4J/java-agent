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
package com.epam.drill.agent.ttl.threadpool;

import com.epam.drill.agent.ttl.TtlUnwrap;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ThreadFactory;

/**
 * Util methods to wrap/check/unwrap methods for disable Inheritable wrapper of {@link ForkJoinWorkerThreadFactory}.
 * <p>
 * <b><i>Note:</i></b>
 * <p>
 * all method is {@code null}-safe.
 * for wrap/unwrap methods when input parameter is {@code null}, return {@code null}.
 * for check methods when input parameter is {@code null}, return {@code false}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see ForkJoinPool
 * @see ForkJoinWorkerThreadFactory
 * @see ForkJoinPool#defaultForkJoinWorkerThreadFactory
 * @see java.util.stream.Stream
 * @see TtlExecutors
 * @since 2.10.1
 */
public final class TtlForkJoinPoolHelper {
    /**
     * Wrapper of {@link ForkJoinWorkerThreadFactory}, disable inheritable.
     *
     * @param threadFactory input thread factory
     * @see DisableInheritableForkJoinWorkerThreadFactory
     * @see TtlExecutors#getDisableInheritableThreadFactory(ThreadFactory)
     * @since 2.10.1
     */
    @Nullable
    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static ForkJoinWorkerThreadFactory getDisableInheritableForkJoinWorkerThreadFactory(@Nullable ForkJoinWorkerThreadFactory threadFactory) {
        if (threadFactory == null || isDisableInheritableForkJoinWorkerThreadFactory(threadFactory))
            return threadFactory;

        return new DisableInheritableForkJoinWorkerThreadFactoryWrapper(threadFactory);
    }

    /**
     * Wrapper of {@link ForkJoinPool#defaultForkJoinWorkerThreadFactory}, disable inheritable.
     *
     * @see #getDisableInheritableForkJoinWorkerThreadFactory(ForkJoinWorkerThreadFactory)
     * @see TtlExecutors#getDefaultDisableInheritableThreadFactory()
     * @since 2.10.1
     */
    @NonNull
    public static ForkJoinWorkerThreadFactory getDefaultDisableInheritableForkJoinWorkerThreadFactory() {
        return getDisableInheritableForkJoinWorkerThreadFactory(ForkJoinPool.defaultForkJoinWorkerThreadFactory);
    }

    /**
     * check the {@link ForkJoinWorkerThreadFactory} is {@link DisableInheritableForkJoinWorkerThreadFactory} or not.
     *
     * @see #getDisableInheritableForkJoinWorkerThreadFactory(ForkJoinWorkerThreadFactory)
     * @see #getDefaultDisableInheritableForkJoinWorkerThreadFactory()
     * @see DisableInheritableForkJoinWorkerThreadFactory
     * @since 2.10.1
     */
    public static boolean isDisableInheritableForkJoinWorkerThreadFactory(@Nullable ForkJoinWorkerThreadFactory threadFactory) {
        return threadFactory instanceof DisableInheritableForkJoinWorkerThreadFactory;
    }

    /**
     * Unwrap {@link DisableInheritableForkJoinWorkerThreadFactory} to the original/underneath one.
     *
     * @see TtlUnwrap#unwrap(Object)
     * @see DisableInheritableForkJoinWorkerThreadFactory
     * @see TtlExecutors#unwrap(ThreadFactory)
     * @since 2.10.1
     */
    @Nullable
    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static ForkJoinWorkerThreadFactory unwrap(@Nullable ForkJoinWorkerThreadFactory threadFactory) {
        if (!isDisableInheritableForkJoinWorkerThreadFactory(threadFactory)) return threadFactory;

        return ((DisableInheritableForkJoinWorkerThreadFactory) threadFactory).unwrap();
    }

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    private TtlForkJoinPoolHelper() {
        throw new InstantiationError("Must not instantiate this class");
    }
}
