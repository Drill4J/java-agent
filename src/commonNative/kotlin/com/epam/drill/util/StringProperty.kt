package com.epam.drill.util

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

inline fun <reified T : Any> Properties.Default.loadStringValued(map: Map<String, String>): T =
    T::class.serializer().deserialize(StringProperty(map))

class StringProperty(val map: Map<String, String>) : NamedValueDecoder() {
    override val context: SerialModule = Properties.context

    private var currentIndex = 0

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return StringProperty(map).also { copyTagsTo(it) }
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return decodeTaggedInt(nested("size"))
    }

    override fun decodeTaggedValue(tag: String): Any {
        return map.getValue(tag)
    }

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return map.getValue(tag).toBoolean()
    }

    override fun decodeTaggedLong(tag: String): Long {
        return map.getValue(tag).toLong()
    }

    override fun decodeTaggedEnum(tag: String, enumDescription: SerialDescriptor): Int {
        return enumDescription.getElementIndex(map[tag]!!)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val tag = nested("size")
        val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
        while (currentIndex < size) {
            val name = descriptor.getTag(currentIndex++)
            if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
        }
        return CompositeDecoder.READ_DONE
    }

}
