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

import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameterDefinitionCollection
import com.epam.drill.agent.common.configuration.AgentParameters
import com.epam.drill.agent.common.configuration.NullableAgentParameterDefinition
import com.epam.drill.agent.konform.validation.jsonschema.minItems
import com.epam.drill.agent.konform.validation.jsonschema.minLength

object DefaultParameterDefinitions : AgentParameterDefinitionCollection() {

    val applicationAgentEnabled: (AgentParameters) -> Boolean = { config ->
        config[CapabilityParameterDefinitions.CLASS_SCANNING_ENABLED]
                || config[CapabilityParameterDefinitions.COVERAGE_COLLECTION_ENABLED]
    }

    val GROUP_ID = AgentParameterDefinition.forString(
        name = "groupId",
        description = "Unique arbitrary string identifying your application group. Example: my-cool-app",
        validator = {
            identifier()
            minLength(3)
        }).register()
    val APP_ID = NullableAgentParameterDefinition.forString(
        name = "appId",
        description = "Unique arbitrary string identifying your application. Example: api-service",
        requiredIf = applicationAgentEnabled,
        validator = {
            identifier()
            minLength(3)
        }).register()
    val BUILD_VERSION = NullableAgentParameterDefinition.forString(
        name = "buildVersion",
        description = "Build version of your application. Typically set to version tag. Example: 0.1.2",
    ).register()
    val COMMIT_SHA = NullableAgentParameterDefinition.forString(
        name = "commitSha",
        description = "Full SHA hash of commit from which your application .jar is built. Example: 8d87b0c2379a925f2f5f4d85c731c8e77d9f2b3c"
    ).register()
    val INSTANCE_ID = NullableAgentParameterDefinition.forString(name = "instanceId").register()
    val ENV_ID = NullableAgentParameterDefinition.forString(
        name = "envId",
        description = "Environment identifier in which the application is running. Example: develop"
    ).register()
    val PACKAGE_PREFIXES = AgentParameterDefinition.forList(
        name = "packagePrefixes",
        description = """
            Packages starting with matching string will be scanned.
            It's usually set to the topmost common package of your application.

            Syntax:
            1. Parts of package names are separated with forward slashes / (and not dots .)
            2. Multiple packages can be specified using ; delimiter
            3. To exclude a package use ! before package name.

            Examples:
            my/org/some/cool/app;
            my/org/some/cool/app;!my/org/some/cool/app/dto
            my/org/some/cool/app;my/org/some/dependency

            Documentation:
            https://drill4j.github.io/docs/agents/java-agent/#how-to-set-package-prefixes
        """.trimIndent(),
        requiredIf = applicationAgentEnabled,
        parser = { it.split(";") },
        listValidator = {
            minItems(1)
        },
        itemValidator = {
            isValidPackage()
        }
    ).register()
    val INSTALLATION_DIR = NullableAgentParameterDefinition.forString(name = "drillInstallationDir", validator = {
        minLength(1)
    }).register()
    val CONFIG_PATH = NullableAgentParameterDefinition.forString(name = "configPath").register()

}
