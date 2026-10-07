package com.tideo.autobrightness.app.settings

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

// DD-014: repair this field before numeric overflow can reject the rest of a profile.
object Zone1ExponentSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("Zone1Exponent", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val value = if (decoder is JsonDecoder) {
            (decoder.decodeJsonElement() as? JsonPrimitive)?.doubleOrNull
        } else {
            decoder.decodeDouble()
        }
        return value?.takeIf { it.isFinite() && it > 0.0 } ?: 0.5
    }

    override fun serialize(encoder: Encoder, value: Double) =
        encoder.encodeDouble(value.takeIf { it.isFinite() && it > 0.0 } ?: 0.5)
}

object AabSettingsSerializer : Serializer<AabSettings> {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override val defaultValue: AabSettings = AabSettings()

    override suspend fun readFrom(input: InputStream): AabSettings {
        return runCatching {
            val raw = json.decodeFromString(AabSettings.serializer(), input.readBytes().decodeToString())
            require(raw.schemaVersion in 1..CURRENT_SCHEMA_VERSION)
            migrate(raw).validate()
        }.getOrDefault(defaultValue)
    }

    override suspend fun writeTo(t: AabSettings, output: OutputStream) {
        output.write(json.encodeToString(AabSettings.serializer(), t).encodeToByteArray())
    }

    // v1→v2: animSteps, thresholdMidpoint, contextOverride, setupTitle added; scale Int→Float.
    // v2→v3 (G2R-F85): dropped thresholdDynamic; ignoreUnknownKeys handles it.
    internal fun migrate(settings: AabSettings): AabSettings {
        if (settings.schemaVersion >= CURRENT_SCHEMA_VERSION) return settings
        var s = settings
        if (s.schemaVersion < 2) s = s.copy(schemaVersion = 2)
        if (s.schemaVersion < 3) s = s.copy(schemaVersion = 3)
        return s
    }
}
