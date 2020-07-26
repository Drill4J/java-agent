package com.epam.drill.agent.classloading

expect object WebContainerSource {
    actual fun webAppStarted(appPath: String)
}
