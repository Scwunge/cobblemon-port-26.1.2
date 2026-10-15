package ludichat.cobbreeding.components

import com.mojang.serialization.Codec
import ludichat.cobbreeding.Registrar
import net.minecraft.core.registries.BuiltInRegistries
import ludichat.cobbreeding.Cobbreeding.MOD_ID
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries

object CobbreedingComponents {
    internal val COMPONENT_TYPES = Registrar<DataComponentType<*>>(BuiltInRegistries.DATA_COMPONENT_TYPE, Registries.DATA_COMPONENT_TYPE)

    /**
     * Name of the egg. Composed of the species of the pokémon, and if relevant, its form
     */
    @JvmField
    val NAME: Registrar.Entry<DataComponentType<String>> = COMPONENT_TYPES.register("name") {
        DataComponentType.builder<String>().persistent(Codec.STRING).build()
    }

    /**
     * Remaining time in ticks before the egg hatches.
     */
    @JvmField
    val TIMER: Registrar.Entry<DataComponentType<Int>> = COMPONENT_TYPES.register("timer") {
        DataComponentType.builder<Int>().persistent(Codec.INT).build()
    }

    /**
     * Counter to get the egg to update every 20 ticks for performances
     */
    @JvmField
    val SECOND: Registrar.Entry<DataComponentType<Int>> = COMPONENT_TYPES.register("second") {
        DataComponentType.builder<Int>().persistent(Codec.INT).build()
    }

    /**
     * Encrypted version of the PokemonProperties stored in the egg.
     */
    @JvmField
    val EGG_INFO: Registrar.Entry<DataComponentType<String>> = COMPONENT_TYPES.register("egg_info") {
        DataComponentType.builder<String>().persistent(Codec.STRING).build()
    }

    /**
     * Non-encrypted original_string of the PokémonProperties defining the Pokémon in the egg.
     */
    @JvmField
    val POKEMON_PROPERTIES: Registrar.Entry<DataComponentType<String>> = COMPONENT_TYPES.register("pokemon_properties") {
        DataComponentType.builder<String>().persistent(Codec.STRING).build()
    }

    /**
     * The version the egg was created in. This is to avoid breaking eggs between version.
     */
    @JvmField
    val VERSION: Registrar.Entry<DataComponentType<String>> = COMPONENT_TYPES.register("version") {
        DataComponentType.builder<String>().persistent(Codec.STRING).build()
    }
}