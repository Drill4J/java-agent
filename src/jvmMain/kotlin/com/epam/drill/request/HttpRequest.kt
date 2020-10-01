package com.epam.drill.request

import com.epam.drill.plugin.DrillRequest
import java.nio.ByteBuffer

object HttpRequest {
    private const val HTTP_DETECTOR_BYTES_COUNT = 8
    private val HTTP_VERBS =
        setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT", "PRI")
    private val HEADERS_END_MARK = "\r\n\r\n".encodeToByteArray()
    private const val DRILL_SESSION_ID_HEADER_NAME = "drill-session-id"

    fun parse(buffers: Array<ByteBuffer>) = runCatching {
        val rawBytes = buffers[0].array()
        if (HTTP_VERBS.any { rawBytes.copyOf(HTTP_DETECTOR_BYTES_COUNT).decodeToString().startsWith(it) }) {
            val idx = rawBytes.indexOf(HEADERS_END_MARK)
            if (idx != -1) {
                val headers =
                    rawBytes.copyOf(idx).decodeToString().split("\r\n").drop(1).filter { it.isNotBlank() }.associate {
                        val (headerKey, headerValue) = it.split(":", limit = 2)
                        headerKey.trim() to headerValue.trim()
                    }
                //todo add processing of header mapping
                headers[DRILL_SESSION_ID_HEADER_NAME]?.let { drillSessionId ->
                    RequestHolder.store(DrillRequest(drillSessionId, headers))
                }
            }
        }
    }.onFailure {  }.getOrNull()

    private fun ByteArray.indexOf(arr: ByteArray) = run {
        for (index in indices) {
            if (index + arr.size <= this.size) {
                val regionMatches = arr.foldIndexed(true) { i, acc, byte ->
                    acc && this[index + i] == byte
                }
                if (regionMatches) return@run index
            } else break
        }
        -1
    }
}
