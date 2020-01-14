import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.*

open class TestBase {

    fun runTest(timeout: Duration = 20.seconds, block: suspend () -> Unit) {
        val context = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }
        runBlocking(context) {
            var finished = false
            launch(context) {
                val expirationMark = MonoClock.markNow() + timeout
                while (!finished) {
                    delay(500)
                    if (expirationMark.hasPassedNow()) {
                        fail("ups timeout")
                    }
                }

            }
            launch(context) {
                block()
            }.join()
            finished = true
        }
    }

}

