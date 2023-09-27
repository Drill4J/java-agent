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
    fun `given correct host and port hostAndPort validator must be valid`() {
        assertTrue { hostAndPort.validate(SomeObject("localhost:8080")) is Valid }
    }

    @Test
    fun `given correct domain hostAndPort validator must be valid`() {
        assertTrue { hostAndPort.validate(SomeObject("example.com")) is Valid }
    }

    @Test
    fun `given schema hostAndPort validator must be invalid`() {
        assertTrue { hostAndPort.validate(SomeObject("http://localhost:8080")) is Invalid }
    }
    @Test
    fun `given lowercase latin letters identifier validator must be valid`() {
        assertTrue { identifier.validate(SomeObject("myproject")) is Valid }
    }
    @Test
    fun `given lowercase latin letters and numbers identifier validator must be valid`() {
        assertTrue { identifier.validate(SomeObject("project123")) is Valid }
    }
    @Test
    fun `given lowercase latin letters and dashes identifier validator must be valid`() {
        assertTrue { identifier.validate(SomeObject("my-project")) is Valid }
    }
    @Test
    fun `given lowercase latin letters and underscores identifier validator must be valid`() {
        assertTrue { identifier.validate(SomeObject("my_project")) is Valid }
    }
    @Test
    fun `given upper case letters identifier validator must be invalid`() {
        assertTrue { identifier.validate(SomeObject("myProject")) is Invalid }
    }
    @Test
    fun `given extra symbols identifier validator must be invalid`() {
        assertTrue { identifier.validate(SomeObject("myProject@")) is Invalid }
    }

    @Test
    fun `given directories are separated by a forward slash package validator must be valid`() {
        assertTrue { validPackage.validate(SomeObject("com/example")) is Valid }
    }

    @Test
    fun `given directories are separated by a dot package validator must be invalid`() {
        assertTrue { validPackage.validate(SomeObject("com.example")) is Invalid }
    }

    @Test
    fun `given the first numbers package validator must be invalid`() {
        assertTrue { validPackage.validate(SomeObject("123com/example")) is Invalid }
    }
}

data class SomeObject(var str: String?)