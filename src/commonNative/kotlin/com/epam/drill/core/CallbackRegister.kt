package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.*
import com.epam.drill.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.protobuf.*

@kotlin.native.concurrent.SharedImmutable
private val logger = Logging.logger("CallbackLogger")

fun globalCallbacks(): Unit = run {
    getClassesByConfig = {
        val classScanDelay = config.classScanDelay - state.startMark.elapsedNow()
        if (classScanDelay.isPositive()) {
            logger.debug { "Waiting class scan delay ($classScanDelay left)..." }
            delay(classScanDelay)
        }
        when (waitForMultipleWebApps(config.webAppLoadingTimeout)) {
            null -> logger.warn {
                val timedOut = state.webApps.filterValues { !it }.keys
                "Apps: $timedOut have not initialized in ${config.webAppLoadingTimeout}. " +
                    "Please check the app names or increase the timeout"
            }
            else -> logger.info { "app is initialized" }
        }
        DataService.retrieveClassesData(PackagesPrefixes.serializer() stringify agentConfig.packagesPrefixes)
    }

    setPackagesPrefixes = { prefixes ->
        agentConfig = agentConfig.copy(packagesPrefixes = prefixes)
        updateState {
            copy(
                alive = true,
                packagePrefixes = prefixes.packagesPrefixes
            )
        }
    }

    sessionStorage = RequestHolder::storeRequestMetadata
    closeSession = RequestHolder::closeSession
    drillRequest = RequestHolder::get

    loadPlugin = ::loadJvmPlugin
    nativePlugin = { _, _, _ -> null }

}

fun RequestHolder.storeRequestMetadata(request: DrillRequest) {
    store(ProtoBuf.dump(DrillRequest.serializer(), request))
}

fun RequestHolder.get(): DrillRequest? {
    return dump()?.let { ProtoBuf.load(DrillRequest.serializer(), it) }
}
