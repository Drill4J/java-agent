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

import java.io.File
import io.aesy.datasize.ByteUnit
import io.aesy.datasize.DataSize
import mu.KotlinLogging

private const val QUEUE_DEFAULT_SIZE: Long = 512 * 1024 * 1024

actual object TransportConfiguration {

    private val logger = KotlinLogging.logger {}

    actual external fun getAgentConfigBytes(): ByteArray

    actual external fun getAgentId(): String

    actual external fun getBuildVersion(): String

    actual external fun getSslTruststore(): String

    actual external fun getSslTruststorePassword(): String

    actual external fun getDrillInstallationDir(): String

    actual external fun getCoverageRetentionLimit(): String

    actual external fun getAdminAddress(): String

    actual external fun getApiKey(): String

    fun getSslTruststoreResolved() = resolveRelativePath(getSslTruststore())

    fun getCoverageRetentionLimitBytes() = getCoverageRetentionLimit().run {
        val logError: (Throwable) -> Unit = {
            logger.warn(it) { "getCoverageRetentionLimitBytes: Exception while parsing coverageRetentionLimit: $this" }
        }
        this.runCatching(DataSize::parse)
            .onFailure(logError)
            .getOrDefault(DataSize.of(QUEUE_DEFAULT_SIZE, ByteUnit.BYTE))
            .toUnit(ByteUnit.BYTE).value.toLong()
    }

    private fun resolveRelativePath(filePath: String) = File(filePath).run {
        val drillPath = getDrillInstallationDir()
            .removeSuffix(File.pathSeparator)
            .takeIf(String::isNotEmpty)
            ?: "."
        val resolveAbsolutePath: (File) -> String = {
            File(drillPath).resolve(it).absolutePath
        }
        val resolved = this.takeIf(File::exists)?.let(File::getAbsolutePath)
            ?: this.takeUnless(File::isAbsolute)?.let(resolveAbsolutePath)
            ?: filePath
        resolved.also {
            logger.trace { "resolveRelativePath: Resolved $filePath to $resolved" }
        }
    }

}
