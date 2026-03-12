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
package com.epam.drill.agent.ttl.spi;

import com.epam.drill.agent.ttl.TtlRunnable;
import com.epam.drill.agent.ttl.TtlUnwrap;
import com.epam.drill.agent.ttl.TtlCallable;
import com.epam.drill.agent.ttl.threadpool.DisableInheritableForkJoinWorkerThreadFactory;
import com.epam.drill.agent.ttl.threadpool.DisableInheritableThreadFactory;
import com.epam.drill.agent.ttl.threadpool.TtlExecutors;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Ttl Wrapper interface.
 * <p>
 * Used to mark wrapper types, for example:
 * <ul>
 *     <li>{@link TtlCallable}</li>
 *     <li>{@link TtlExecutors}</li>
 *     <li>{@link DisableInheritableThreadFactory}</li>
 * </ul>
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see TtlUnwrap#unwrap
 * @see TtlCallable
 * @see TtlRunnable
 * @see TtlExecutors
 * @see DisableInheritableThreadFactory
 * @see DisableInheritableForkJoinWorkerThreadFactory
 * @since 2.11.4
 */
public interface TtlWrapper<T> extends TtlEnhanced {
    /**
     * unwrap {@link TtlWrapper} to the original/underneath one.
     *
     * @see TtlUnwrap#unwrap(Object)
     */
    @NonNull
    T unwrap();
}
