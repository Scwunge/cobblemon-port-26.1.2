package ludichat.cobbreeding.network

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import ludichat.cobbreeding.NeuterProperty
import net.minecraft.client.Minecraft
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
* Packet fired when the user clicks on the button to neuter or un-neuter a Pokémon.
* The server will check if they are the Pokémon's OT to allow it.
*/
class ToggleNeuterPacket(val pokemonId: UUID) : NetworkPacket<ToggleNeuterPacket> {
    override val id = ID
    companion object {
        val ID = cobblemonResource("toggle_neuter")
        fun decode(buffer: RegistryFriendlyByteBuf) = ToggleNeuterPacket(buffer.readUUID())
    }

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUUID(pokemonId)
    }
}

object ToggleNeuterPacketHandler: ServerNetworkPacketHandler<ToggleNeuterPacket> {
    override fun handle(packet: ToggleNeuterPacket, server: MinecraftServer, player: ServerPlayer) {
        val pokemon: Pokemon? = Cobblemon.storage.getParty(player)[packet.pokemonId]
        if (player.stringUUID == pokemon?.originalTrainer)
        {
            val neutered: Boolean = NeuterProperty.get(pokemon)
            NeuterProperty.applyToPokemon(pokemon, !neutered)
            ToggledNeuterPacket(packet.pokemonId, !neutered).sendToPlayer(player)
        }
        else
        {
            player.sendSystemMessage(Component.translatable("cobbreeding.msg.neuter_button.not_ot"))
        }
    }
}

/**
 * Packet fired by the server after updating the Pokémon's NeuterProperty so that the client GUI may update accordingly
 */
class ToggledNeuterPacket(val pokemonId: UUID, val value: Boolean) : NetworkPacket<ToggledNeuterPacket> {
    override val id = ID
    companion object {
        val ID = cobblemonResource("toggled_neuter")
        fun decode(buffer: RegistryFriendlyByteBuf) = ToggledNeuterPacket(buffer.readUUID(), buffer.readBoolean())
    }

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUUID(pokemonId)
        buffer.writeBoolean(value)
    }
}

object ToggledNeuterPacketHandler: ClientNetworkPacketHandler<ToggledNeuterPacket> {
    override fun handle(packet: ToggledNeuterPacket, client: Minecraft) {
        val slot: Int = CobblemonClient.storage.party.getPosition(packet.pokemonId)
        val pokemon: Pokemon? = CobblemonClient.storage.party.get(slot)
        if (pokemon != null)
        {
            NeuterProperty.applyToPokemon(pokemon, packet.value)
        }
        else
        {
            client.player?.sendSystemMessage(Component.translatable("cobbreeding.msg.neuter_button.pokemon_not_found"))
        }
    }
}
