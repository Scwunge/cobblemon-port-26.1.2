package ludichat.cobbreeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.criterion.CountableContext
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.net.PacketRegisterInfo
import com.mojang.logging.LogUtils.getLogger
import kotlinx.serialization.ExperimentalSerializationApi
import ludichat.cobbreeding.components.CobbreedingComponents
import ludichat.cobbreeding.network.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import org.slf4j.Logger
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object Cobbreeding {
    const val MOD_ID = "cobbreeding"
    const val VERSION = "2.3.0"

    private const val CONFIG_PATH = "config/cobbreeding/main.json"
    const val ENCRYPTION_KEY_PATH = "config/cobbreeding/encryption"

    internal val ITEMS = Registrar(BuiltInRegistries.ITEM, Registries.ITEM)

    @JvmField
    val LOGGER: Logger = getLogger()

    @JvmField
    val EGG_ITEMS: HashMap<String, Registrar.Entry<PokemonEgg>> = HashMap.newHashMap(326)

    var SHINY_ERROR_FLAG = false

    val BREEDING_PACKET_INFO: PacketRegisterInfo<ToggleBreedingPacket> = PacketRegisterInfo(ToggleBreedingPacket.ID, ToggleBreedingPacket::decode, ToggleBreedingPacketPacketHandler)
    val NEUTER_PACKET_INFO: PacketRegisterInfo<ToggleNeuterPacket> = PacketRegisterInfo(ToggleNeuterPacket.ID, ToggleNeuterPacket::decode, ToggleNeuterPacketHandler)
    val NEUTERED_PACKET_INFO: PacketRegisterInfo<ToggledNeuterPacket> = PacketRegisterInfo(ToggledNeuterPacket.ID, ToggledNeuterPacket::decode, ToggledNeuterPacketHandler)

    lateinit var config: Config

    fun init() {
        LOGGER.info("Registering items")

        registerEggs()

        LOGGER.info("Registering data components")
        CobbreedingComponents.NAME // forces the data components to register

        loadConfig()
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ -> reloadConfig() }

        // Hooking to the egg hatched post event to handle advancements
        CobblemonEvents.HATCH_EGG_POST.subscribe {
            onHatch(it)
        }
    }

    private fun registerEggs()
    {
        val types = listOf("bug", "dark", "dragon", "electric", "fairy", "fighting", "fire", "flying", "ghost",
                "grass", "ground", "ice", "normal", "poison", "psychic", "rock", "steel", "water")

        EGG_ITEMS["pokemon_egg"] = ITEMS.register("pokemon_egg") {PokemonEgg(Item.Properties().stacksTo(1))}
        EGG_ITEMS["manaphy_egg"] = ITEMS.register("manaphy_egg") {PokemonEgg(Item.Properties().stacksTo(1))}
        for (type in types)
        {
            EGG_ITEMS[type+"_pokemon_egg"] = ITEMS.register(type+"_pokemon_egg") {PokemonEgg(Item.Properties().stacksTo(1))}
            for (type2 in types)
            {
                if (type != type2)
                    EGG_ITEMS[type+"_"+type2+"_pokemon_egg"] = ITEMS.register(type+"_"+type2+"_pokemon_egg") {PokemonEgg(Item.Properties().stacksTo(1))}
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadConfig() {
        val configFile = File(CONFIG_PATH)
        configFile.parentFile.mkdirs()

        if (configFile.exists()) {
            try {
                val fileReader = FileReader(configFile)
                config = Config.GSON.fromJson(fileReader, Config::class.java)
                fileReader.close()
            } catch (e: Exception) {
                LOGGER.error("Config error: ${e.message}")
                LOGGER.warn("Config has been reset.")
                config = Config()
            }
        } else {
            config = Config()
        }

        saveConfig()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveConfig() {
        try {
            val configFile = File(CONFIG_PATH)
            val fileWriter = FileWriter(configFile)
            // Put the config to json then flush the writer to commence writing.
            Config.GSON.toJson(config, fileWriter)
            fileWriter.flush()
            fileWriter.close()
        } catch (exception: Exception) {
            LOGGER.error("Failed to save the config! Please consult the following stack trace:")
            exception.printStackTrace()
        }

    }

    fun reloadConfig() {
        LOGGER.info("Reloading configuration")
        loadConfig()
        LOGGER.info("Done reloading configuration")
    }

    /**
     * Handles Advancements when hatching an egg
     */
    fun onHatch(event : HatchEggEvent.Post)
    {
        val playerData = Cobblemon.playerDataManager.getGenericData(event.player)
        val advancementData = playerData.advancementData

        // Total eggs hatched
        advancementData.updateTotalEggsHatched() // Probably unused by Cobblemon yet but may be used for custom advancements
        CobblemonCriteria.EGG_HATCH.trigger(event.player, CountableContext(advancementData.totalEggsHatched))

        // Pokémon is shiny
        // Will never trigger if the shininess of the egg isn't specified, though
        if (event.pokemon.shiny)
        {
            advancementData.updateTotalShinyCaptureCount()
            CobblemonCriteria.CATCH_SHINY_POKEMON.trigger(event.player, CountableContext(advancementData.totalShinyCaptureCount))
        }

        // Update aspect based advancements
        CobblemonCriteria.COLLECT_ASPECT.trigger(event.player, advancementData.aspectsCollected)
        Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
    }
}