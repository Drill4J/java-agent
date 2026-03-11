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
import com.epam.drill.agent.ttl.spi.TtlWrapper;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Comparator;

/**
 * @see TtlExecutors#getTtlRunnableUnwrapComparator(Comparator)
 * @see TtlExecutors#isTtlRunnableUnwrapComparator(Comparator)
 * @see TtlExecutors#unwrap(Comparator)
 * @since 2.12.3
 */
final class TtlUnwrapComparator<T> implements Comparator<T>, TtlWrapper<Comparator<T>> {
    private final Comparator<T> comparator;

    public TtlUnwrapComparator(@NonNull Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public int compare(T o1, T o2) {
        return comparator.compare(TtlUnwrap.unwrap(o1), TtlUnwrap.unwrap(o2));
    }

    @NonNull
    @Override
    public Comparator<T> unwrap() {
        return comparator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TtlUnwrapComparator<?> that = (TtlUnwrapComparator<?>) o;

        return comparator.equals(that.comparator);
    }

    @Override
    public int hashCode() {
        return comparator.hashCode();
    }

    @Override
    public String toString() {
        return "TtlUnwrapComparator{comparator=" + comparator + '}';
    }
}
