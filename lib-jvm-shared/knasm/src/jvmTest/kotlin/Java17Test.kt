import org.junit.jupiter.api.Test
import org.objectweb.asm.*
import java.io.*
import kotlin.test.*

class Java17Test {

    private val classBytes = File(".").resolve("src/jvmTest/resources/java17/Controller.class").readBytes()

    @Test
    fun classReadTest() {
        val classReader = ClassReader(classBytes)
        assertEquals(classReader.className, "com/epam/rest/Controller")
        assertEquals(classReader.superName, "java/lang/Object")
        assertTrue(classReader.interfaces.isEmpty())
    }


}
