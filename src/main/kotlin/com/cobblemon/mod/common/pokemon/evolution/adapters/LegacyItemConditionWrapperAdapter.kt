/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.evolution.adapters

import com.cobblemon.mod.common.util.adapters.CodecBackedAdapter
import com.cobblemon.mod.common.util.adapters.RegistryAccessResolver
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.serialization.JsonOps
import net.minecraft.core.component.predicates.CustomDataPredicate
import net.minecraft.advancements.criterion.ItemPredicate
import net.minecraft.core.component.predicates.DataComponentPredicates
import net.minecraft.advancements.criterion.NbtPredicate
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import java.lang.reflect.Type
import com.mojang.datafixers.util.Either
import net.minecraft.core.Holder
import net.minecraft.core.HolderOwner
import net.minecraft.core.HolderSet
import net.minecraft.util.RandomSource
import net.minecraft.world.item.Item
import java.util.Optional
import java.util.stream.Stream

// This is an ugly adapter because breaking changes are scary and people would ;(
class LegacyItemConditionWrapperAdapter(val registryAccess: HolderLookup.Provider = RegistryAccessResolver.getAccess())
    : JsonDeserializer<ItemPredicate>, JsonSerializer<ItemPredicate> {

    companion object {
        private const val TAG_PREFIX = "#"
        private const val LEGACY_ITEM = "item"
        private const val LEGACY_NBT = "nbt"
    }

    private val codecAdapter = CodecBackedAdapter(ItemPredicate.CODEC, registryAccess)

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemPredicate {
        if (json.isJsonPrimitive) {
            return this.withLazyTag(this.createBuilderForItem(json.asString).build(), json.asString)
        }
        else if (json.isJsonObject) {
            val jObject = json.asJsonObject
            if (jObject.size() == 2 && jObject.has(LEGACY_ITEM) && jObject.has(LEGACY_NBT)) {
                val builder = this.createBuilderForItem(jObject.get(LEGACY_ITEM).asString)
                val nbtPredicate = NbtPredicate.CODEC.decode(JsonOps.INSTANCE, jObject.get(LEGACY_NBT)).result().get().first
                val itemCustomDataPredicate = CustomDataPredicate(nbtPredicate)
                return this.withLazyTag(builder
                    .withComponents(net.minecraft.advancements.criterion.DataComponentMatchers.Builder.components().partial(DataComponentPredicates.CUSTOM_DATA, itemCustomDataPredicate).build())
                    .build(), jObject.get(LEGACY_ITEM).asString)
            }
        }
        return this.codecAdapter.deserialize(json, typeOfT, context)
    }

    override fun serialize(src: ItemPredicate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return this.codecAdapter.serialize(src, typeOfSrc, context)
    }

    private fun createBuilderForItem(raw: String): ItemPredicate.Builder {
        val isTag = raw.startsWith(TAG_PREFIX)
        val builder = ItemPredicate.Builder.item()
        return Identifier.read(if (isTag) raw.substring(1) else raw)
            .mapOrElse(
                { id ->
                    if (isTag) {
                        // The tag is attached in withLazyTag, item tags may not be bound yet while data loads.
                        builder
                    } else {
                        BuiltInRegistries.ITEM.getOptional(id)
                            .map { builder.of(BuiltInRegistries.ITEM, it) }
                            .orElseGet { builder }
                    }
                },
                { builder }
            )
    }


    /**
     * Since 1.21.2 a tag is resolved with getOrThrow while the predicate is built, which fails while
     * species load because item tags are not bound yet. Point the predicate at a tag set that looks the
     * tag up on use instead, matching the old getOrCreateTag behaviour.
     */
    private fun withLazyTag(predicate: ItemPredicate, raw: String): ItemPredicate {
        if (!raw.startsWith(TAG_PREFIX)) return predicate
        val id = Identifier.tryParse(raw.substring(1)) ?: return predicate
        return ItemPredicate(Optional.of(LazyItemTagSet(TagKey.create(Registries.ITEM, id))), predicate.count(), predicate.components())
    }

    private class LazyItemTagSet(private val tag: TagKey<Item>) : HolderSet<Item> {
        private fun resolved(): HolderSet<Item> = BuiltInRegistries.ITEM.get(tag).map { it as HolderSet<Item> }.orElseGet { HolderSet.empty() }
        override fun stream(): Stream<Holder<Item>> = resolved().stream()
        override fun size() = resolved().size()
        override fun isBound() = true
        override fun unwrap(): Either<TagKey<Item>, List<Holder<Item>>> = Either.left(tag)
        override fun getRandomElement(random: RandomSource): Optional<Holder<Item>> = resolved().getRandomElement(random)
        override fun get(index: Int): Holder<Item> = resolved().get(index)
        override fun contains(holder: Holder<Item>) = holder.`is`(tag)
        override fun canSerializeIn(owner: HolderOwner<Item>) = true
        override fun unwrapKey(): Optional<TagKey<Item>> = Optional.of(tag)
        override fun iterator(): MutableIterator<Holder<Item>> = resolved().iterator()
    }

}
