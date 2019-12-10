import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import platform.posix.*
import kotlin.coroutines.*
import kotlin.native.ThreadLocal
import kotlin.native.concurrent.*
import kotlin.test.*

@SharedImmutable
val wws = Worker.start(true)

@ThreadLocal
val ws = Channel<String>()

val ww: Channel<String>
    get() = wws.execute(TransferMode.UNSAFE, {}) { ws }.result

@SharedImmutable
val dispatcher = newSingleThreadContext("0")
@SharedImmutable
val dispatcher1 = newSingleThreadContext("1")
@SharedImmutable
val dispatcher2 = newSingleThreadContext("2")


@SharedImmutable
lateinit var xx: String

object wtf : CoroutineScope {
    operator fun invoke(block: suspend () -> Unit) {
        launch {
            block()
        }

    }

    override val coroutineContext: CoroutineContext = dispatcher1

}

class Context(override val coroutineContext: CoroutineContext = dispatcher) : CoroutineScope {
    fun run() {
        launch(dispatcher2) {
            repeat(10) {
                println("sender")
                delay(50)
                ww.send("test")
            }
        }
        launch(dispatcher1) {
            repeat(10) {
                println("retriever")
                delay(50)
                println(ww.receive())
            }
        }
    }
}

class MultiThreadedCoroutinesTest {

    @Test
    fun easy() {
        xx = "xx"
        wtf {
            println("OMG")
        }
//        Context().run()
        sleep(10)
        println("test main")
        println(xx)

    }
}