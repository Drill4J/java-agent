import org.junit.jupiter.api.Test
import org.objectweb.asm.*
import java.io.*
import kotlin.test.*

class Java8Test {

    private val classBytes = File(".").resolve("src/jvmTest/resources/java8/AtomicInteger.class").readBytes()

    @Test
    fun classReadTest() {
        val classReader = ClassReader(classBytes)
        assertEquals(classReader.className, "java/util/concurrent/atomic/AtomicInteger")
        assertEquals(classReader.superName, "java/lang/Number")
        assertEquals(classReader.interfaces[0],"java/io/Serializable")
    }

}
