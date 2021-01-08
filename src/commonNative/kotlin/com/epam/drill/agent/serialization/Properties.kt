package com.epam.drill.agent.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

inline fun <reified T : Any> Map<String, String>.parseAs(): T = run {
    val serializer = T::class.serializer()
    val module = serializersModuleOf(serializer)
    serializer.deserialize(SimpleMapDecoder(module, this))
}

class SimpleMapDecoder(
    override val serializersModule: SerializersModule = EmptySerializersModule,
    map: Map<String, String>
) : AbstractDecoder() {

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

        } ?: CompositeDecoder.DECODE_DONE
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
