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
package com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl;

import com.epam.drill.agent.ttl.threadpool.agent.internal.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.epam.drill.agent.ttl.threadpool.agent.internal.transformlet.impl.Utils.*;
import static org.junit.Assert.*;

public class UtilsTest {
    @BeforeClass
    public static void beforeClass() {
        Logger.setLoggerImplTypeIfNotSetYet("stderr");
    }

    @Test
    public void test_get_unboxing_boolean_fromMap() {
        Map<String, Object> map = new HashMap<>();

        try {
            getUnboxingBoolean(map, "not_existed");
            fail();
        } catch (NullPointerException expected) {
            // do nothing
        }
    }

    private static boolean getUnboxingBoolean(Map<String, Object> map, String key) {
        return (Boolean) map.get(key);
    }

    @Test
    public void test_className_package() {
        assertEquals("", getPackageName("Hello"));
        assertEquals("com.foo", getPackageName("com.foo.Hello"));

        assertTrue(isClassAtPackage("java.util.TimerTask", "java.util"));
        assertFalse(isClassAtPackage("java.util.TimerTask", "java.utils"));
        assertFalse(isClassAtPackage("java.util.TimerTask", "java"));
        assertFalse(isClassAtPackage("java.util.TimerTask", "java.util.zip"));

        assertTrue(isClassUnderPackage("java.util.TimerTask", "java.util"));
        assertFalse(isClassUnderPackage("java.util.TimerTask", "java.utils"));
        assertTrue(isClassUnderPackage("java.util.TimerTask", "java"));
        assertFalse(isClassUnderPackage("java.util.TimerTask", "javax"));

        assertTrue(isClassAtPackageJavaUtil("java.util.PriorityQueue"));
        assertFalse(isClassAtPackageJavaUtil("java.util.zip.ZipInputStream"));

        assertTrue(isClassOrInnerClass(Map.class.getName(), Map.class.getName()));
        assertTrue(isClassOrInnerClass(Map.Entry.class.getName(), Map.class.getName()));
        assertTrue(isClassOrInnerClass(Map.Entry.class.getName(), Map.Entry.class.getName()));
    }
}
