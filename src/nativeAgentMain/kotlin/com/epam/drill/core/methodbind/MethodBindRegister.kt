package com.epam.drill.core.methodbind

import com.epam.drill.core.*
import kotlinx.cinterop.*
import kotlin.collections.set
import kotlin.native.concurrent.*


@SharedImmutable
val nativeMethodBindMapper =
    mapOf(
        SocketDispatcher + ::read0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },
        SocketDispatcher + ::readv0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },
        SocketDispatcher + ::write0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::write0] = reinterpret
                staticCFunction(::write0)
            }
        },
        FileDispatcherImpl + ::write0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::write0] = reinterpret
                staticCFunction(::write0)
            }
        },
        FileDispatcherImpl + ::read0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },
        FileDispatcherImpl + ::readv0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },

        Netty + ::readAddress.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::readAddress] = reinterpret
                staticCFunction(::readAddress)
            }
        }
        ,
        Netty + ::writeAddress.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::writeAddress] = reinterpret
                staticCFunction(::writeAddress)
            }
        }
    )


