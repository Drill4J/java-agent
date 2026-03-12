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

import com.epam.drill.agent.ttl.spi.TtlWrapper;
import com.epam.drill.agent.ttl.threadpool.TtlExecutors;
import com.epam.drill.agent.ttl.threadpool.TtlForkJoinPoolHelper;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.Contract;

/**
 * Util methods for TTL Wrapper: unwrap TTL Wrapper and check TTL Wrapper.
 * <p>
 * <b><i>Note:</i></b><br>
 * all methods are {@code null}-safe, when input parameter is {@code null}, return {@code null}.
 * <p>
 * <b><i>Implementation Note:</i></b><br>
 * The util methods in this class should have been inside {@link TtlWrappers}.<br>
 * But for {@code Java 6} support, it's required splitting the util methods
 * which involved {@code Java 8} from {@link TtlWrappers}.
 * In order to avoid loading {@code Java 8} class (eg: {@link java.util.function.Consumer}, {@link java.util.function.Supplier}),
 * when invoking any methods of {@link TtlWrappers}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see TtlRunnable
 * @see TtlCallable
 * @see TtlExecutors
 * @see TtlForkJoinPoolHelper
 * @see TtlWrappers
 * @since 2.11.4
 */
public final class TtlUnwrap {
    /**
     * Generic unwrap method, unwrap {@link TtlWrapper} to the original/underneath one.
     * <p>
     * this method is {@code null}-safe, when input parameter is {@code null}, return {@code null};
     * if input parameter is not a {@link TtlWrapper} just return input.
     *
     * @see TtlRunnable#unwrap(Runnable)
     * @see TtlCallable#unwrap(java.util.concurrent.Callable)
     * @see TtlExecutors#unwrap(java.util.concurrent.Executor)
     * @see TtlExecutors#unwrap(java.util.concurrent.ThreadFactory)
     * @see TtlExecutors#unwrap(java.util.Comparator)
     * @see TtlForkJoinPoolHelper#unwrap(java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory)
     * @see TtlWrappers#wrapSupplier(java.util.function.Supplier)
     * @see TtlWrappers#wrapConsumer(java.util.function.Consumer)
     * @see TtlWrappers#wrapBiConsumer(java.util.function.BiConsumer)
     * @see TtlWrappers#wrapFunction(java.util.function.Function)
     * @see TtlWrappers#wrapBiFunction(java.util.function.BiFunction)
     * @see #isWrapper(Object)
     * @since 2.11.4
     */
    @Nullable
    @Contract(value = "null -> null; !null -> !null", pure = true)
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(@Nullable T obj) {
        if (!isWrapper(obj)) return obj;
        else return ((TtlWrapper<T>) obj).unwrap();
    }

    /**
     * check the input object is a {@code TtlWrapper} or not.
     *
     * @see #unwrap(Object)
     * @since 2.11.4
     */
    public static <T> boolean isWrapper(@Nullable T obj) {
        return obj instanceof TtlWrapper;
    }

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    private TtlUnwrap() {
        throw new InstantiationError("Must not instantiate this class");
    }
}
