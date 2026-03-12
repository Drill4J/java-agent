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

import com.epam.drill.agent.ttl.spi.TtlAttachments;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @see TtlAttachments
 * @deprecated Use {@link com.epam.drill.agent.ttl.spi.TtlEnhanced} instead.
 */
@Deprecated
@SuppressFBWarnings({"NM_SAME_SIMPLE_NAME_AS_INTERFACE"})
//   [ERROR] The class name com.epam.drill.agent.ttl.TtlEnhanced shadows
//   the simple name of implemented interface com.epam.drill.agent.ttl.spi.TtlEnhanced [com.epam.drill.agent.ttl.TtlEnhanced]
public interface TtlEnhanced extends com.epam.drill.agent.ttl.spi.TtlEnhanced {
}
