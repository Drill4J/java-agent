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

import com.epam.drill.agent.ttl.TransmittableThreadLocal;
import com.epam.drill.agent.ttl.TtlCallable;
import com.epam.drill.agent.ttl.TtlRunnable;
import com.epam.drill.agent.ttl.spi.TtlEnhanced;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * {@link TransmittableThreadLocal} Wrapper of {@link ScheduledExecutorService},
 * transmit the {@link TransmittableThreadLocal} from the task submit time of {@link Runnable} or {@link Callable}
 * to the execution time of {@link Runnable} or {@link Callable}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @since 0.9.0
 */
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
class ScheduledExecutorServiceTtlWrapper extends ExecutorServiceTtlWrapper implements ScheduledExecutorService, TtlEnhanced {
    final ScheduledExecutorService scheduledExecutorService;

    public ScheduledExecutorServiceTtlWrapper(@NonNull ScheduledExecutorService scheduledExecutorService, boolean idempotent) {
        super(scheduledExecutorService, idempotent);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @NonNull
    @Override
    public ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
        return scheduledExecutorService.schedule(TtlRunnable.get(command, false, idempotent), delay, unit);
    }

    @NonNull
    @Override
    public <V> ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
        return scheduledExecutorService.schedule(TtlCallable.get(callable, false, idempotent), delay, unit);
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
        return scheduledExecutorService.scheduleAtFixedRate(TtlRunnable.get(command, false, idempotent), initialDelay, period, unit);
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
        return scheduledExecutorService.scheduleWithFixedDelay(TtlRunnable.get(command, false, idempotent), initialDelay, delay, unit);
    }

    @NonNull
    @Override
    public ScheduledExecutorService unwrap() {
        return scheduledExecutorService;
    }
}
