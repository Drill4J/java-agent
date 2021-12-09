/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.agent

import com.epam.drill.kni.*

@Kni
actual object HeadersRetriever {
    actual external fun adminAddressHeader(): String?
    actual external fun retrieveAdminAddress(): String?
    actual external fun sessionHeaderPattern(): String?
    actual external fun idHeaderConfigKey(): String?
    actual external fun idHeaderConfigValue(): String?
}
