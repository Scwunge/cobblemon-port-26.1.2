/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.datafixer

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.datafixer.fix.*
import com.cobblemon.mod.common.datafixer.schema.CobblemonRootSchema
import com.cobblemon.mod.common.datafixer.schema.CobblemonSchemaV2
import com.mojang.datafixers.DSL.TypeReference
import com.mojang.datafixers.DataFixUtils
import com.mojang.datafixers.DataFixer
import com.mojang.datafixers.DataFixerBuilder
import com.mojang.datafixers.DataFixerUpper
import com.mojang.datafixers.TypeRewriteRule
import com.mojang.datafixers.functions.PointFreeRule
import com.mojang.datafixers.schemas.Schema
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Dynamic
import com.mojang.serialization.DynamicOps
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix
import org.apache.logging.log4j.LogManager
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap

@Suppress("MemberVisibilityCanBePrivate", "unused")
object CobblemonSchemas {
    private val SAME: (Int, Schema) -> Schema = ::Schema
    private val LOGGER = LogManager.getLogger()

    /**
     * Data versions whose fix rules could not be built. Data saved at these versions is loaded without fixing.
     */
    private val BROKEN_VERSIONS = mutableSetOf<Int>()
    private val REPORTED_SKIPS = ConcurrentHashMap.newKeySet<String>()
    private val RESULT: DataFixerBuilder.Result = this.create()
    private val FIXER: DataFixer = GuardedDataFixer(RESULT.fixer())

    const val DATA_VERSION = 3

    /**
     * The Cobblemon [DataFixer].
     */
    @JvmStatic
    val DATA_FIXER: DataFixer get() = FIXER

    const val VERSION_KEY = "${Cobblemon.MODID}:data_version"

    /**
     * Wraps the given [Codec] with the Cobblemon [DataFixer].
     *
     * @param T The [Codec] element type.
     * @param codec The [Codec] being wrapped.
     * @param typeReference The [TypeReference] used for this wrap.
     * @return The new generated [Codec].
     */
    fun <T> wrapCodec(codec: Codec<T>, typeReference: TypeReference): Codec<T> = CobblemonDataFixerCodec(codec, typeReference)

    private fun create(): DataFixerBuilder.Result {
        val builder = DataFixerBuilder(DATA_VERSION)
        this.appendSchemas(builder)
        val result = builder.build()
        this.prepareRules(result.fixer())
        return result
    }

    /**
     * Builds the rules for upgrading every older data version now, instead of lazily on whichever thread loads old data first.
     *
     * DFU's own optimize only starts from versions that have fixes, so data with no version (saved before Cobblemon 1.6) was never covered.
     * If building a rewrite throws, DFU 10 leaves its pending entry behind and every later load from that version waits on it forever.
     * Versions that fail here are logged and skipped by [GuardedDataFixer] rather than hanging the server.
     */
    private fun prepareRules(fixer: DataFixer) {
        val getRule = DataFixerUpper::class.java.getDeclaredMethod("getRule", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }
        val optimizationRule = DataFixerUpper::class.java.getDeclaredField("OPTIMIZATION_RULE")
            .apply { isAccessible = true }
            .get(null) as PointFreeRule
        for (version in 0 until DATA_VERSION) {
            try {
                val rule = getRule.invoke(fixer, version, DATA_VERSION) as TypeRewriteRule
                val schema = fixer.getSchema(DataFixUtils.makeKey(version))
                for (typeName in schema.types()) {
                    schema.getType { typeName }.rewrite(rule, optimizationRule)
                }
            } catch (e: Exception) {
                BROKEN_VERSIONS += version
                val cause = (e as? InvocationTargetException)?.targetException ?: e
                LOGGER.error("Could not build the Cobblemon data fixes from data version {}, data saved at that version will load without them", version, cause)
            }
        }
    }

    private fun appendSchemas(builder: DataFixerBuilder) {
        builder.addSchema(0, ::CobblemonRootSchema)
        val schema1 = builder.addSchema(1, ::CobblemonSchemaV2)
        builder.addFixer(EvolutionProxyNestingFix(schema1))
        builder.addFixer(IvEvToIdentifierFix(schema1))
        builder.addFixer(TeraTypeFix(schema1))
        builder.addFixer(TradeableMissingFix(schema1))
        builder.addFixer(ItemStackComponentizationFix(schema1))
        builder.addFixer(BlockPosUpdateFix(schema1))
        builder.addFixer(FeatureFix(schema1))
        builder.addFixer(MovesetJsonFix(schema1))
        builder.addFixer(ShoulderStateJsonFix(schema1))
        builder.addFixer(NicknameFix(schema1))
        builder.addFixer(RaisedPPStagesFix(schema1))
        val schema2 = builder.addSchema(2, SAME)
        builder.addFixer(HypertrainIVFix(schema2))
        val schema3 = builder.addSchema(3, SAME)
        builder.addFixer(TypeGemGrowthTypeFix(schema3))
    }

    private class CobblemonDataFixerCodec<R>(private val baseCodec: Codec<R>, private val typeReference: TypeReference) : Codec<R> {

        override fun <T> encode(input: R, ops: DynamicOps<T>, prefix: T): DataResult<T> {
            return this.baseCodec.encode(input, ops, prefix)
                .flatMap { encoded -> ops.mergeToMap(encoded, ops.createString(VERSION_KEY), ops.createInt(DATA_VERSION)) }
        }

        override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<R, T>> {
            val inputVersion = ops.get(input, VERSION_KEY)
                .flatMap(ops::getNumberValue)
                .map(Number::toInt)
                .result()
                // If none always do op unlike vanilla.
                .orElse(0)
            val dynamicWithoutVersion = Dynamic(ops, ops.remove(input, VERSION_KEY))
            val dataFixedDynamic = DATA_FIXER.update(this.typeReference, dynamicWithoutVersion, inputVersion, DATA_VERSION)
            return this.baseCodec.decode(dataFixedDynamic)
        }
    }

    private class GuardedDataFixer(private val fixer: DataFixer) : DataFixer {

        override fun <T> update(type: TypeReference, input: Dynamic<T>, version: Int, newVersion: Int): Dynamic<T> {
            if (version < newVersion && version in BROKEN_VERSIONS) {
                if (REPORTED_SKIPS.add("${type.typeName()} $version")) {
                    LOGGER.error("Loading {} from Cobblemon data version {} without data fixes, see the error logged at startup", type.typeName(), version)
                }
                return input
            }
            return this.fixer.update(type, input, version, newVersion)
        }

        override fun getSchema(key: Int): Schema = this.fixer.getSchema(key)
    }



}