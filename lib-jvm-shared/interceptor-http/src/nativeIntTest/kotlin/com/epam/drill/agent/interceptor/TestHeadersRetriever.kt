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
package com.epam.drill.agent.interceptor

import com.epam.drill.agent.common.request.HeadersRetriever

object TestHeadersRetriever : HeadersRetriever {

    override fun adminAddressHeader() = "drill-admin-url"

    override fun adminAddressValue() = "test-admin:8080"

    override fun sessionHeader() = "drill-session-id"

    override fun agentIdHeader() = "drill-agent-id"

    override fun agentIdHeaderValue() = "test-agent"

}
