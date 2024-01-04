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
package com.epam.drill.agent

const val DRILL_PACKAGE = "com/epam/drill"
const val SYSTEM_CONFIG_PATH = "DRILL_AGENT_CONFIG_PATH"

const val KAFKA_PRODUCER_INTERFACE = "org/apache/kafka/clients/producer/Producer"
const val KAFKA_CONSUMER_SPRING = "org/springframework/kafka/listener/KafkaMessageListenerContainer\$ListenerConsumer"

const val CADENCE_PRODUCER =  "com/uber/cadence/internal/sync/WorkflowStubImpl"
const val CADENCE_CONSUMER = "com/uber/cadence/internal/sync/WorkflowRunnable"
