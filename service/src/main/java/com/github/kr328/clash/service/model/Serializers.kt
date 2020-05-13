package com.github.kr328.clash.service.model

import android.net.Uri
import kotlinx.serialization.*

class UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveDescriptor("Uri", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }
}