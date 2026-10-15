package ludichat.cobbreeding

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.properties.BooleanProperty

class NeuterProperty : CustomPokemonPropertyType<BooleanProperty> {
    fun create(value: Boolean): BooleanProperty {
        return BooleanProperty(
                KEY,
                value,
                { pokemon: Pokemon, value: Boolean -> applyToPokemon(pokemon, value) },
                { pokemon: PokemonEntity, value: Boolean -> applyToPokemonEntity(pokemon, value) },
                { pokemon: Pokemon, value: Boolean -> matchPokemon(pokemon, value) },
                { pokemon: PokemonEntity, value: Boolean -> matchPokemonEntity(pokemon, value) }
        )
    }

    override val keys: Iterable<String>
        get() = listOf(KEY)

    override val needsKey: Boolean
        get() = true

    override fun fromString(value: String?): BooleanProperty? {
        if (value == null) return null

        val bool = "true".equals(value, ignoreCase = true) || "yes".equals(value, ignoreCase = true)
        return this.create(bool)
    }

    override fun examples(): Collection<String> {
        return listOf("true", "false")
    }

    companion object {
        private const val KEY = "neutered"

        fun get(pokemon: Pokemon): Boolean {
            return matchPokemon(pokemon, true)
        }

        fun applyToPokemon(pokemon: Pokemon, value: Boolean) {
            pokemon.persistentData.putBoolean(KEY, value)
            pokemon.onChange()
        }

        fun applyToPokemonEntity(pokemon: PokemonEntity, value: Boolean) {
            return applyToPokemon(pokemon.pokemon, value)
        }

        fun matchPokemon(pokemon: Pokemon, value: Boolean): Boolean {
            if (pokemon.persistentData.contains(KEY)) {
                return pokemon.persistentData.getBooleanOr(KEY, false) == value
            }
            return false
        }

        fun matchPokemonEntity(pokemon: PokemonEntity, value: Boolean): Boolean {
            return matchPokemon(pokemon.pokemon, value)
        }
    }
}