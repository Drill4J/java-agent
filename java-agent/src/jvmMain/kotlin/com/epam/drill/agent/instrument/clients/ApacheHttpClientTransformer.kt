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
package com.epam.drill.agent.instrument.clients

import com.epam.drill.agent.instrument.ClassPathProvider
import com.epam.drill.agent.instrument.DefaultHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.RuntimeClassPathProvider
import com.epam.drill.agent.instrument.TransformerObject

actual object ApacheHttpClientTransformer :
    TransformerObject,
    ApacheHttpClientTransformerObject(),
    HeadersProcessor by DefaultHeadersProcessor,
    ClassPathProvider by RuntimeClassPathProvider
