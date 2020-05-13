package com.github.kr328.clash.common.serialization

import android.os.Parcel
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

object MergedParcels: SerialFormat {
    fun <T> dump(serializer: SerializationStrategy<T>, obj: T, parcel: Parcel) {
        val data = Parcel.obtain()
        val encoder = ParcelsEncoder(data)

        try {
            serializer.serialize(encoder, obj)

            data.setDataPosition(0)

            parcel.writeStringList(encoder.getStringList())
            parcel.appendFrom(data, 0, data.dataSize())
        } finally {
            data.recycle()
        }
    }

    fun <T> load(deserializer: DeserializationStrategy<T>, parcel: Parcel): T {
        val strings = mutableListOf<String>().apply { parcel.readStringList(this) }

        return deserializer.deserialize(ParcelsDecoder(strings, parcel))
    }

    private class ParcelsEncoder(private val parcel: Parcel) :
        Encoder, CompositeEncoder {
        private val strings = mutableMapOf<String, Int>()
        private var stringIndex = 0

        fun getStringList(): List<String> {
            val result = mutableListOf<String>()
            strings.map { it.value to it.key }
                .sortedBy { it.first }
                .forEach { result.add(it.second) }
            return result
        }

        override val context: SerialModule
            get() = EmptyModule

        override fun beginCollection(
            descriptor: SerialDescriptor,
            collectionSize: Int,
            vararg typeSerializers: KSerializer<*>
        ): CompositeEncoder {
            encodeInt(collectionSize)
            return super.beginCollection(descriptor, collectionSize, *typeSerializers)
        }

        override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) =
            encodeBoolean(value)

        override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
            encodeByte(value)

        override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
            encodeChar(value)

        override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) =
            encodeDouble(value)

        override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
            encodeFloat(value)

        override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
            encodeInt(value)

        override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
            encodeLong(value)

        override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
            encodeShort(value)

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) =
            encodeString(value)

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) =
            encodeUnit()

        override fun endStructure(descriptor: SerialDescriptor) {}

        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) = encodeNullableSerializableValue(serializer, value)

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) = encodeSerializableValue(serializer, value)

        override fun beginStructure(
            descriptor: SerialDescriptor,
            vararg typeSerializers: KSerializer<*>
        ): CompositeEncoder = this

        override fun encodeBoolean(value: Boolean) =
            parcel.writeByte(if (value) 1 else 0)

        override fun encodeByte(value: Byte) =
            parcel.writeByte(value)

        override fun encodeChar(value: Char) =
            parcel.writeInt(value.toInt())

        override fun encodeDouble(value: Double) =
            parcel.writeDouble(value)

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
            parcel.writeInt(index)

        override fun encodeFloat(value: Float) =
            parcel.writeFloat(value)

        override fun encodeInt(value: Int) =
            parcel.writeInt(value)

        override fun encodeLong(value: Long) =
            parcel.writeLong(value)

        override fun encodeNotNullMark() =
            encodeBoolean(true)

        override fun encodeNull() =
            encodeBoolean(false)

        override fun encodeShort(value: Short) =
            parcel.writeInt(value.toInt())

        override fun encodeUnit() {}
        override fun encodeString(value: String) {
            val index = strings.computeIfAbsent(value) {
                stringIndex++
            }

            parcel.writeInt(index)
        }
    }

    class ParcelsDecoder(private val strings: List<String>, private val parcel: Parcel) : Decoder,
        CompositeDecoder {
        override val context: SerialModule
            get() = EmptyModule
        override val updateMode: UpdateMode
            get() = UpdateMode.BANNED

        override fun decodeSequentially() =
            true

        override fun decodeElementIndex(descriptor: SerialDescriptor) =
            CompositeDecoder.UNKNOWN_NAME

        override fun decodeCollectionSize(descriptor: SerialDescriptor) =
            decodeInt()

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int) =
            decodeBoolean()

        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int) =
            decodeByte()

        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int) =
            decodeChar()

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int) =
            decodeDouble()

        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int) =
            decodeFloat()

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int) =
            decodeInt()

        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int) =
            decodeShort()

        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int) =
            decodeLong()

        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int) =
            decodeString()

        override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) =
            decodeUnit()

        override fun endStructure(descriptor: SerialDescriptor) {}

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>
        ) = decodeNullableSerializableValue(deserializer)

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
        ) = decodeSerializableValue(deserializer)

        override fun <T : Any> updateNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            old: T?
        ) = updateNullableSerializableValue(deserializer, old)

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
        ) = updateSerializableValue(deserializer, old)

        override fun beginStructure(
            descriptor: SerialDescriptor,
            vararg typeParams: KSerializer<*>
        ): CompositeDecoder = this

        override fun decodeBoolean() =
            parcel.readByte() != 0.toByte()

        override fun decodeByte() =
            parcel.readByte()

        override fun decodeChar() =
            parcel.readInt().toChar()

        override fun decodeDouble() =
            parcel.readDouble()

        override fun decodeEnum(enumDescriptor: SerialDescriptor) =
            parcel.readInt()

        override fun decodeFloat() =
            parcel.readFloat()

        override fun decodeInt() =
            parcel.readInt()

        override fun decodeLong() =
            parcel.readLong()

        override fun decodeNotNullMark() =
            decodeBoolean()

        override fun decodeNull() =
            null

        override fun decodeShort() =
            parcel.readInt().toShort()

        override fun decodeUnit() {}
        override fun decodeString(): String {
            val index = parcel.readInt()

            return strings[index]
        }

    }

    override val context: SerialModule = EmptyModule
}


