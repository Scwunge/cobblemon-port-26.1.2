package ludichat.cobbreeding.network

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.pasture.PastureLinkManager
import com.cobblemon.mod.common.util.cobblemonResource
import ludichat.cobbreeding.Cobbreeding
import ludichat.cobbreeding.CustomProperties
import ludichat.cobbreeding.utils.CobbreedingFile
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import java.io.File
import java.nio.file.Path

/**
 * Packet fired when loading the PastureBlock GUI and clicking on the Toggle Breeding button.
 * initialQuery is true if the packet is sent because the player interacts with the Pasture Block.
 */

class ToggleBreedingPacket() : NetworkPacket<ToggleBreedingPacket> {
    override val id = ID
    companion object {
        val ID = cobblemonResource("toggle_breeding")
        fun decode(@Suppress("unused") buffer: RegistryFriendlyByteBuf) = ToggleBreedingPacket()
    }

    override fun encode(buffer: RegistryFriendlyByteBuf) {
    }
}

object ToggleBreedingPacketPacketHandler: ServerNetworkPacketHandler<ToggleBreedingPacket> {
    override fun handle(packet: ToggleBreedingPacket, server: MinecraftServer, player: ServerPlayer) {
        val uuid: String = player.stringUUID
        val world = player.level()
        val pastureLink = PastureLinkManager.getLinkByPlayer(player)
        if (pastureLink === null)
            return
        val pastureBlock = pastureLink.pos
        val pastureState = world.getBlockState(pastureBlock)

        // Save file path
        val path: Path = server.getWorldPath(LevelResource.ROOT).resolve("pokemon").resolve("cobbreeding")
        val pastureFile: File = path.resolve("pastures").toFile()
        val playerFile: File = path.resolve(uuid.take(2)).resolve(uuid).toFile()
        playerFile.parentFile.mkdirs() // Creating the directories if they don't exist

        if (pastureState.getValue(CustomProperties.BREEDING_ACTIVATED))
        {
            val ownerUuid = CobbreedingFile.getUuid(pastureFile, pastureBlock.toString()).toString()
            val ownerFile = path.resolve(ownerUuid.take(2)).resolve(ownerUuid).toFile()
            CobbreedingFile.removeCompoundTag(pastureFile, pastureBlock.toString())
            CobbreedingFile.removeCompoundTag(ownerFile, pastureBlock.toString())

            world.setBlockAndUpdate(pastureBlock, pastureState.setValue(CustomProperties.BREEDING_ACTIVATED, false))
        }
        else
        {
            val activatedPasture = CobbreedingFile.getNumberOfActivatedPastures(playerFile)
            if (Cobbreeding.config.maxNumberOfActivatedPasturePerPlayer == -1 || (activatedPasture < Cobbreeding.config.maxNumberOfActivatedPasturePerPlayer && activatedPasture != -1))
            {
                CobbreedingFile.addCompoundTag(pastureFile, pastureBlock.toString(), null, player.uuid)
                CobbreedingFile.addCompoundTag(playerFile, pastureBlock.toString(), intArrayOf(pastureBlock.x, pastureBlock.y, pastureBlock.z), null)
                world.setBlockAndUpdate(pastureBlock, pastureState.setValue(CustomProperties.BREEDING_ACTIVATED, true))
            }
            else
            {
                if (activatedPasture == -1)
                    player.sendSystemMessage(Component.translatable("cobbreeding.msg.add_pasture.fail"))
                else
                    player.sendSystemMessage(Component.translatable("cobbreeding.msg.add_pasture.too_many_pastures"))
            }
        }
    }
}