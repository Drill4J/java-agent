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

import com.epam.drill.agent.ttl.TtlCallable;
import com.epam.drill.agent.ttl.TtlRecursiveAction;
import com.epam.drill.agent.ttl.TtlRecursiveTask;
import com.epam.drill.agent.ttl.TtlRunnable;

/**
 * a Ttl marker/tag interface, for ttl enhanced class, for example {@code TTL wrapper}
 * like {@link TtlRunnable}, {@link TtlCallable}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see TtlRunnable
 * @see TtlCallable
 * @see TtlRecursiveAction
 * @see TtlRecursiveTask
 * @see TtlAttachments
 * @since 2.11.0
 */
public interface TtlEnhanced {
}
