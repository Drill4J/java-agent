package com.epam.drill.agent.serialization

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*

inline fun <reified T : Any> Map<String, String>.parseAs(): T = T::class.serializer().deserialize(
    SimpleMapDecoder(this)
)

class SimpleMapDecoder(map: Map<String, String>) : AbstractDecoder() {

    private val iterator = map.iterator()

    private var current: Pair<SerialDescriptor, String>? = null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val next = iterator.takeIf { it.hasNext() }?.next()
        return next?.key?.let(descriptor::getElementIndex)?.let { index ->
            when (index) {
                in 0 until descriptor.elementsCount -> {
                    current = descriptor.getElementDescriptor(index) to next.value
                    index
                }
                //ignore unknown properties
                else -> decodeElementIndex(descriptor)
            }

        } ?: CompositeDecoder.READ_DONE
    }

    override fun decodeValue(): Any = current?.let { (desc, value) ->
        when (desc.kind) {
            PrimitiveKind.STRING -> value
            PrimitiveKind.BOOLEAN -> value.toBoolean()
            PrimitiveKind.BYTE -> value.toByte()
            PrimitiveKind.SHORT -> value.toShort()
            PrimitiveKind.INT -> value.toInt()
            PrimitiveKind.LONG -> value.toLong()
            PrimitiveKind.FLOAT -> value.toFloat()
            PrimitiveKind.DOUBLE -> value.toDouble()
            else -> super.decodeValue()
        }
    } ?: super.decodeValue()
}
