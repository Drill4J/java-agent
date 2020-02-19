package com.epam.drill.classloading

import org.junit.*
import org.junit.Test
import kotlin.test.*

class ClassPathTest {
    @Test
    fun `should return Analyzer bytecode`() {
        val classLoader = ClassLoader.getSystemClassLoader()
        val classPath = ClassPath()
        val classes = classPath.scanItPlease(classLoader)
        assertNotNull(classes.getByteCodeOf("com/alibaba/ttl/internal/javassist/ClassPath.class"))
    }


    /**
     * Top level class is the main file which doesn't contain any $ symbols
     */
    @Test
    fun `should filter only top level class`() {
        assertTrue { isTopLevelClass("com/epam/drill/Omg") }
        //anonymous case
        assertFalse { isTopLevelClass("com/epam/drill/Omg$1") }
    }
}
