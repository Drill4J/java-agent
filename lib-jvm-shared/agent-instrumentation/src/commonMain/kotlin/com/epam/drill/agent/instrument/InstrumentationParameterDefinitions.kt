package com.epam.drill.agent.instrument

import com.epam.drill.agent.common.configuration.AgentParameterDefinition
import com.epam.drill.agent.common.configuration.AgentParameterDefinitionCollection

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
object InstrumentationParameterDefinitions: AgentParameterDefinitionCollection() {
    val INSTRUMENTATION_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationEnabled", defaultValue = true).register()
    val CONTEXT_PROPAGATION_ENABLED = AgentParameterDefinition.forBoolean(
        name = "contextPropagationEnabled",
        description = "Enable/disable context propagation",
        defaultValue = true
    ).register()
    val INSTRUMENTATION_COMPATIBILITY_TESTS_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationCompatibilityTestsEnabled", defaultValue = false).register()

    //Async frameworks
    val INSTRUMENTATION_REACTOR_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationReactorEnabled", defaultValue = true).register()
    val INSTRUMENTATION_TTL_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationTtlEnabled", defaultValue = true).register()

    //Messaging
    val INSTRUMENTATION_KAFKA_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationKafkaEnabled", defaultValue = false).register()
    val INSTRUMENTATION_CADENCE_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationCadenceEnabled", defaultValue = false).register()

    //WebSocket
    val INSTRUMENTATION_WS_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationWsEnabled", defaultValue = false).register()

    //Http
    val INSTRUMENTATION_HTTP_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationHttpEnabled", defaultValue = true).register()
    val INSTRUMENTATION_SSL_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationSslEnabled", defaultValue = true).register()

    //Http Clients
    val INSTRUMENTATION_JAVA_HTTP_CLIENT_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationJavaHttpClientEnabled", defaultValue = true).register()
    val INSTRUMENTATION_APACHE_HTTP_CLIENT_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationApacheHttpClientEnabled", defaultValue = true).register()
    val INSTRUMENTATION_OK_HTTP_CLIENT_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationOkHttpClientEnabled", defaultValue = true).register()
    val INSTRUMENTATION_SPRING_WEB_CLIENT_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationSpringWebClientEnabled", defaultValue = true).register()

    //Test Frameworks
    val INSTRUMENTATION_JUNIT_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationJUnitEnabled", defaultValue = true).register()
    val INSTRUMENTATION_TESTNG_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationTestNGEnabled", defaultValue = true).register()
    val INSTRUMENTATION_CUCUMBER_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationCucumberEnabled", defaultValue = true).register()
    val INSTRUMENTATION_SELENIUM_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationSeleniumEnabled", defaultValue = true).register()
    val INSTRUMENTATION_JMETER_ENABLED = AgentParameterDefinition.forBoolean(name = "instrumentationJMeterEnabled", defaultValue = true).register()

}