package com.epam.drill.request

import com.epam.drill.agent.instrument.*
import com.epam.drill.agent.instrument.http.apache.*
import com.epam.drill.agent.instrument.http.java.*
import com.epam.drill.agent.instrument.http.ok.*
import com.epam.drill.kni.*
import org.objectweb.asm.*

@Kni
actual object HttpClientInstrumentation {

    private val strategies: List<TransformStrategy> = listOf(
        OkHttpClient(), ApacheClient(), JavaHttpUrlConnection()
    )

    actual fun initCallbacks() {
        ClientsCallback.initRequestCallback {
            HttpRequest.loadDrillHeaders() ?: emptyMap()
        }
        ClientsCallback.initResponseCallback { headers ->
            HttpRequest.storeDrillHeaders(headers)
        }
    }

    actual fun permitAndTransform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?,
    ): ByteArray? = ClassReader(classFileBuffer).let { classReader ->
        strategies.firstOrNull {
            it.permit(classReader)
        }?.transform(className, classFileBuffer, loader, protectionDomain)
    }


}

