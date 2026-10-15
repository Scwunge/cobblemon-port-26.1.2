package ludichat.cobbreeding.components

import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.pokemon.Nature
import com.cobblemon.mod.common.pokemon.Species
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.resources.Identifier

object CobblemonCodecs {
    val SPECIES = SpeciesCodec.CODEC
    val NATURE = NatureCodec.CODEC
    val MOVE_TEMPLATE = MoveTemplateCodec.CODEC

    object SpeciesCodec {
        val CODEC: Codec<Species> = Codec.STRING.comapFlatMap(::validate, Species::showdownId)

        private fun validate(name: String): DataResult<Species> {
            val species = PokemonSpecies.getByName(name)
            return if (species is Species) DataResult.success(species)
            else DataResult.error { "$name is not a valid pokemon species." }
        }
    }

    object NatureCodec {
        val CODEC: Codec<Nature> = Identifier.CODEC.comapFlatMap(::validate, Nature::name)

        private fun validate(id: Identifier): DataResult<Nature> {
            val nature = Natures.getNature(id)
            return if (nature is Nature) DataResult.success(nature)
            else DataResult.error { "$id is not a valid pokemon nature." }
        }
    }

    object MoveTemplateCodec {
        val CODEC: Codec<MoveTemplate> = Codec.INT.comapFlatMap(::validate, MoveTemplate::num)

        private fun validate(id: Int): DataResult<MoveTemplate> {
            val move = Moves.getByNumericalId(id)
            return if (move is MoveTemplate) DataResult.success(move)
            else DataResult.error { "$id is not a valid pokemon move." }
        }
    }
}