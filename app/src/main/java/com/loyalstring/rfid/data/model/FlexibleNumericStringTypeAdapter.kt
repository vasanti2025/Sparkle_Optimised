package com.loyalstring.rfid.data.model

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Locale

/**
 * Always serializes as a JSON string primitive.
 * Safely reads JSON numbers or strings; skips objects/arrays.
 */
class FlexibleNumericStringTypeAdapter : TypeAdapter<String?>() {
    override fun write(out: JsonWriter, value: String?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.value(value)
    }

    override fun read(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.NUMBER -> formatNumericString(reader.nextDouble())
            JsonToken.STRING -> {
                val raw = reader.nextString()
                raw.takeIf { it.isNotBlank() && !it.equals("null", true) }
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    private fun formatNumericString(value: Double): String =
        String.format(Locale.US, "%.3f", value)
}
