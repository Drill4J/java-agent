/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.configuration

import kotlin.math.min

data class URL constructor(
    val isOpaque: Boolean,
    val scheme: String?,
    val userInfo: String?,
    val host: String?,
    val path: String,
    val query: String?,
    val fragment: String?,
    val defaultPort: Int
) {

    val port: Int
        get() = if (defaultPort == DEFAULT_PORT) {
            when (scheme) {
                "http", "ws" -> 80
                "https", "wss" -> 443
                else -> -1
            }
        } else {
            defaultPort
        }

    private val fullUrl: String by lazy { toUrlString().toString() }

    fun toUrlString(includeScheme: Boolean = true, out: StringBuilder = StringBuilder()): StringBuilder {
        if (includeScheme && scheme != null) {
            out.append("$scheme:")
            if (!isOpaque) out.append("//")
        }
        if (userInfo != null) out.append("$userInfo@")
        if (host != null) out.append(host)
        if (port != 80 && port != 443 && port != -1) out.append(":$port")
        out.append(path)
        if (query != null) out.append("?$query")
        if (fragment != null) out.append("#$fragment")
        return out
    }

    override fun toString(): String = fullUrl

    companion object {

        const val DEFAULT_PORT = 0

        operator fun invoke(
            scheme: String?,
            userInfo: String?,
            host: String?,
            path: String,
            query: String?,
            fragment: String?,
            opaque: Boolean = false,
            port: Int = DEFAULT_PORT
        ): URL =
            URL(
                opaque,
                scheme,
                userInfo,
                host,
                path,
                query,
                fragment,
                port
            )

        private val schemeRegex = Regex("\\w+:")

        operator fun invoke(url: String): URL {
            val r = StrReader(url)
            val schemeColon = r.tryRegex(schemeRegex)
            return when {
                schemeColon != null -> {
                    val isHierarchical = r.tryLit("//") != null
                    val nonScheme = r.readRemaining()
                    val scheme = schemeColon.dropLast(1)
                    val (nonFragment, fragment) = nonScheme.split('#', limit = 2).run { first() to getOrNull(1) }
                    val (nonQuery, query) = nonFragment.split('?', limit = 2).run { first() to getOrNull(1) }
                    val (authority, path) = nonQuery.split('/', limit = 2).run { first() to getOrNull(1) }
                    val (host, port, userInfo) = authority.split('@', limit = 2).reversed().run {
                        val first = first()
                        if (first.contains(":")) {
                            val second = first.split(":").last()
                            Triple(first.split(":").first(), second.toInt(), getOrNull(1))
                        } else {
                            Triple(first,
                                DEFAULT_PORT, getOrNull(1))
                        }
                    }
                    URL(
                        opaque = !isHierarchical,
                        scheme = scheme,
                        userInfo = userInfo,
                        host = host.takeIf { it.isNotEmpty() },
                        path = if (path != null) "/$path" else "",
                        query = query,
                        fragment = fragment,
                        port = port
                    )
                }
                else -> {
                    val (nonFragment, fragment) = url.split("#", limit = 2).run { first() to getOrNull(1) }
                    val (path, query) = nonFragment.split("?", limit = 2).run { first() to getOrNull(1) }
                    URL(
                        opaque = false,
                        scheme = null,
                        userInfo = null,
                        host = null,
                        path = path,
                        query = query,
                        fragment = fragment
                    )
                }
            }
        }

    }

}

private class StrReader(private val str: String, private var pos: Int = 0) {

    private val length: Int = this.str.length
    private val available: Int get() = length - this.pos

    private fun peek(count: Int): String = substr(this.pos, count)

    private fun read(count: Int): String = this.peek(count).apply { skip(count) }

    fun readRemaining(): String = read(available)

    private fun skip(count: Int = 1) = this.apply { this.pos += count; }

    private fun substr(pos: Int, length: Int): String {
        return this.str.substring(min(pos, this.length), min(pos + length, this.length))
    }

    fun tryLit(lit: String): String? {
        if (substr(this.pos, lit.length) != lit) return null
        this.pos += lit.length
        return lit
    }

    fun tryRegex(v: Regex): String? {
        val result = v.find(this.str.substring(this.pos)) ?: return null
        val m = result.groups[0]!!.value
        this.pos += m.length
        return m
    }

}
