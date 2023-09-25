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
package com.epam.drill.agent.configuration

import com.epam.drill.konform.validation.Invalid
import com.epam.drill.konform.validation.Valid
import com.epam.drill.konform.validation.Validation
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidatorsTest {
    private val hostAndPort = Validation<SomeObject> {
        SomeObject::str required {
            hostAndPort()
        }
    }
    private val identifier = Validation<SomeObject> {
        SomeObject::str required {
            identifier()
        }
    }
    private val validPackage = Validation<SomeObject> {
        SomeObject::str required {
            isValidPackage()
        }
    }


    @Test
    fun `check host and port matcher`() {
        //valid - there are host and port
        assertTrue { hostAndPort.validate(SomeObject("localhost:8080")) is Valid }
        //valid - there is domain
        assertTrue { hostAndPort.validate(SomeObject("example.com")) is Valid }
        //invalid - no schema needed
        assertTrue { hostAndPort.validate(SomeObject("http://localhost:8080")) is Invalid }
    }

    @Test
    fun `check identifier matcher`() {
        //valid - latin letters
        assertTrue { identifier.validate(SomeObject("myproject")) is Valid }
        //valid - latin letters and numbers
        assertTrue { identifier.validate(SomeObject("project123")) is Valid }
        //valid - latin letters and dashes
        assertTrue { identifier.validate(SomeObject("my-project")) is Valid }
        //valid - latin letters and underscores
        assertTrue { identifier.validate(SomeObject("my_project")) is Valid }
        //invalid - upper cases
        assertTrue { identifier.validate(SomeObject("myProject")) is Invalid }
        //invalid - extra symbols
        assertTrue { identifier.validate(SomeObject("myProject@")) is Invalid }
    }

    @Test
    fun `check valid package matcher`() {
        //valid - forward slash separator
        assertTrue { validPackage.validate(SomeObject("com/example")) is Valid }
        //invalid - dot separator
        assertTrue { validPackage.validate(SomeObject("com.example")) is Invalid }
        //invalid - numbers first
        assertTrue { validPackage.validate(SomeObject("123com/example")) is Invalid }
    }
}

data class SomeObject(var str: String?)