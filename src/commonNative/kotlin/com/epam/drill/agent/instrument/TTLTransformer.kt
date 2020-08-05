package com.epam.drill.agent.instrument

actual object TTLTransformer {
    val directTtlClasses = listOf(
        "java/util/concurrent/ScheduledThreadPoolExecutor",
        "java/util/concurrent/ThreadPoolExecutor",
        "java/util/concurrent/ForkJoinTask",
        "java/util/concurrent/ForkJoinPool"
    )
    const val timerTaskClass = "java/util/TimerTask"
    const val runnableInterface = "java/lang/Runnable"
    const val poolExecutor = "java/util/concurrent/ThreadPoolExecutor"

    actual fun transform(
        loader: Any?,
        classFile: String?,
        classBeingRedefined: Any?,
        classFileBuffer: ByteArray
    ): ByteArray? {
        return TTLTransformerStub.transform(loader, classFile, classBeingRedefined, classFileBuffer)
    }
}
