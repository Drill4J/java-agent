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

class UrlValidatorTest {
    private val wsUrl = Validation<SomeObject> {
        SomeObject::str required {
            validWsUrl()
        }
    }

    @Test
    fun `given ws schema and correct host and port wsUrl validator must be valid`() {
        assertTrue { wsUrl.validate(SomeObject("ws://localhost:8080")) is Valid }
    }

    @Test
    fun `given ws schema and correct domain wsUrl validator must be valid`() {
        assertTrue { wsUrl.validate(SomeObject("ws://example.com")) is Valid }
    }

    @Test
    fun `given wss schema wsUrl validator must be valid`() {
        assertTrue { wsUrl.validate(SomeObject("wss://example.com")) is Valid }
    }

    @Test
    fun `given http schema wsUrl validator must be invalid`() {
        assertTrue { wsUrl.validate(SomeObject("http://example.com")) is Invalid }
    }

    @Test
    fun `given without schema wsUrl validator must be invalid`() {
        assertTrue { wsUrl.validate(SomeObject("example.com")) is Invalid }
    }

}

class IdentifierValidatorTest {
    private val identifier = Validation<SomeObject> {
        SomeObject::str required {
            identifier()
        }
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
}

class PackageValidator {
    private val validPackage = Validation<SomeObject> {
        SomeObject::str required {
            isValidPackage()
        }
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

class LogLevelValidator {
    private val logLevel = Validation<SomeObject> {
        SomeObject::strToList onEach {
            isValidLogLevel()
        }
    }

    @Test
    fun `given valid logging level logLevel validator must be valid`() {
        assertTrue { logLevel.validate(SomeObject("INFO")) is Valid }
    }

    @Test
    fun `given invalid logging level logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("CUSTOM")) is Invalid }
    }

    @Test
    fun `given package and valid logging level logLevel validator must be valid`() {
        assertTrue { logLevel.validate(SomeObject("com.example=INFO")) is Valid }
    }

    @Test
    fun `given package and invalid logging level logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("com.example=CUSTOM")) is Invalid }
    }

    @Test
    fun `given root package and valid logging level logLevel validator must be valid`() {
        assertTrue { logLevel.validate(SomeObject("=DEBUG")) is Valid }
    }

    @Test
    fun `given invalid package separator logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("com/example=INFO")) is Invalid }
    }

    @Test
    fun `given the dot in at the beginning of the package logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject(".com.example=INFO")) is Invalid }
    }

    @Test
    fun `given the dot in the end of the package logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("com.example.=INFO")) is Invalid }
    }
    @Test
    fun `given double dot in the middle of the package logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("com..example=INFO")) is Invalid }
    }

    @Test
    fun `given numbers at the beginning of the package logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("123com.example=INFO")) is Invalid }
    }

    @Test
    fun `given several valid packages logLevel validator must be valid`() {
        assertTrue { logLevel.validate(SomeObject("com.example=DEBUG;other.domain=INFO")) is Valid }
    }

    @Test
    fun `given several invalid packages with logging levels logLevel validator must be invalid`() {
        assertTrue { logLevel.validate(SomeObject("com.example=CUSTOM;other.domain=INFO")) is Invalid }
    }

    @Test
    fun `given combination of logging level and packages logLevel validator must be valid`() {
        assertTrue { logLevel.validate(SomeObject("INFO;com.example=DEBUG;other.domain=ERROR")) is Valid }
    }
}

data class SomeObject(var str: String?) {
    val strToList: List<String>
        get() = if (str.isNullOrEmpty()) emptyList() else str?.split(";")?.toList() ?: emptyList()
}