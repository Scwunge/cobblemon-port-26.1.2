/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Locale

/**
 * Serializes enums by their lower case names. Minecraft shipped this as com.cobblemon.mod.common.util.adapters.LowerCaseEnumTypeAdapterFactory
 * until 1.21.2; this is the same behaviour.
 */
class LowerCaseEnumTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        @Suppress("UNCHECKED_CAST")
        val clazz = typeToken.rawType as Class<T>
        if (!clazz.isEnum) {
            return null
        }
        val map = clazz.enumConstants.associateBy { toLowercase(it) }
        return object : TypeAdapter<T>() {
            override fun write(writer: JsonWriter, value: T?) {
                if (value == null) writer.nullValue() else writer.value(toLowercase(value))
            }

            override fun read(reader: JsonReader): T? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                }
                return map[reader.nextString()]
            }
        }
    }

    private fun toLowercase(value: Any?): String = if (value is Enum<*>) value.name.lowercase(Locale.ROOT) else value.toString().lowercase(Locale.ROOT)
}
