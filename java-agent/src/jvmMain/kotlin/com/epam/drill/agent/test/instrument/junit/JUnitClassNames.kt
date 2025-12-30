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
package com.epam.drill.agent.test.instrument.junit

const val PostDiscoveryFilter = "org.junit.platform.launcher.PostDiscoveryFilter"
const val FilterResult = "org.junit.platform.engine.FilterResult"
const val TestDescriptor = "org.junit.platform.engine.TestDescriptor"
const val Segment = "org.junit.platform.engine.UniqueId.Segment"
const val LauncherDiscoveryRequest = "org.junit.platform.launcher.LauncherDiscoveryRequest"
const val ConfigurationParameters = "org.junit.platform.engine.ConfigurationParameters"
const val TestTag = "org.junit.platform.engine.TestTag"

fun getMetadata(descriptor: String) = """
        java.util.Map testMetadata = new java.util.HashMap();
        for (int i = 0; i < $descriptor.getUniqueId().getSegments().size(); i++) {
            java.lang.String key = (($Segment)$descriptor.getUniqueId().getSegments().get(i)).getType();
            java.lang.String value = (($Segment)$descriptor.getUniqueId().getSegments().get(i)).getValue();
            testMetadata.put(key, value);                                
        }       
    """.trimIndent()

fun getTags(descriptor: String) = """
        java.util.List testTags = new java.util.ArrayList();
        java.util.Set tags = $descriptor.getTags();        
        java.util.Iterator iterator = tags.iterator();
        while (iterator.hasNext()) {
             $TestTag tag = ($TestTag) iterator.next();            
            testTags.add(tag.getName());
        }
    """.trimIndent()