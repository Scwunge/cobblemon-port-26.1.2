package ludichat.cobbreeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.DataKeys
import com.cobblemon.mod.common.util.isServerSide
import kotlinx.serialization.internal.throwArrayMissingFieldException
import ludichat.cobbreeding.compat.CobblemonSizeVariations
import ludichat.cobbreeding.components.CobbreedingComponents
import ludichat.cobbreeding.utils.toSemVer
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import kotlin.math.floor

/**
 * Pokémon egg item.
 */
class PokemonEgg(settings: Properties) : Item(settings) {
    companion object {
        const val DEFAULT_TIMER = 600
        const val DEFAULT_SECOND = 0

        const val CURRENT_COMPONENT_VERSION = "2.0.3"

        val NAME: DataComponentType<String> by lazy { CobbreedingComponents.NAME.get() }
        val TIMER: DataComponentType<Int> by lazy { CobbreedingComponents.TIMER.get() }
        val SECOND: DataComponentType<Int> by lazy { CobbreedingComponents.SECOND.get() }
        val EGG_INFO: DataComponentType<String> by lazy { CobbreedingComponents.EGG_INFO.get() }
        val POKEMON_PROPERTIES: DataComponentType<String> by lazy { CobbreedingComponents.POKEMON_PROPERTIES.get() }
        val VERSION: DataComponentType<String> by lazy { CobbreedingComponents.VERSION.get() }
    }

    /**
     * Used to transition eggs from older version to avoid breaking them.
     */
    override fun verifyComponentsAfterLoad(itemStack: ItemStack) {
        super.verifyComponentsAfterLoad(itemStack)

        when (val componentVersion = itemStack.getOrDefault(VERSION, "")) {
            // Short-circuit to avoid semver conversions (potentially expensive regex)
            CURRENT_COMPONENT_VERSION -> return
            // Version upgrade
            else -> {
                // No need to update component version working value for incremental updates
                val componentVersion = componentVersion.toSemVer()
                if (componentVersion < "2.0.0".toSemVer())
                {
                    if (itemStack.has(EGG_INFO))
                    {
                        val pokemonProperties = itemStack.get(EGG_INFO).toString()
                        itemStack.set(NAME, pokemonProperties.split(" ")[0])
                        itemStack.set(POKEMON_PROPERTIES, pokemonProperties)
                        itemStack.set(EGG_INFO, EggUtilities.encrypt(PokemonProperties.parse(pokemonProperties)))
                        itemStack.set(VERSION, "2.0.0")
                    }
                }

                if (componentVersion < "2.0.3".toSemVer())
                {
                    if (itemStack.has(EGG_INFO))
                    {
                        val pokemonProperties = EggUtilities.decrypt(itemStack.get(EGG_INFO)!!)
                        pokemonProperties.species?.let { itemStack.set(NAME, it) }
                        itemStack.set(VERSION, "2.0.3")
                    }
                }

                // Finished updating, pin version to current components version to prevent useless semver conversion
                itemStack.set(VERSION, CURRENT_COMPONENT_VERSION)
            }
        }
    }

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand
    ): InteractionResultHolder<ItemStack?>? {
        val itemStack = player.getItemInHand(usedHand)

        if (!level.isServerSide()) return InteractionResultHolder.pass(itemStack)

        // If encryption disabled and egg is not decrypted already
        if (!Cobbreeding.config.eggEncryptionEnabled && !itemStack.has(POKEMON_PROPERTIES)) {
            // No egg data: normally impossible state but just to be sure
            if (!itemStack.has(EGG_INFO)) {
                Cobbreeding.LOGGER.error("Failed to decrypt egg: no data (player: ${player.scoreboardName})")
                player.sendSystemMessage(Component.translatable("cobbreeding.msg.decrypt.fail"))
                return InteractionResultHolder.fail(itemStack)
                // Maybe consume instead since egg is irrecoverable?
            }

            itemStack.set(POKEMON_PROPERTIES, EggUtilities.decrypt(itemStack.get(EGG_INFO)!!).asString())
            Cobbreeding.LOGGER.info("Successfully decrypted egg (player: ${player.scoreboardName})")
            player.sendSystemMessage(Component.translatable("cobbreeding.msg.decrypt.success"))
            return InteractionResultHolder.success(itemStack)
        }

        return InteractionResultHolder.pass(itemStack)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        lines: MutableList<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, lines, type)

        val pokemonName = stack.get(NAME)?.split(" ")
        if (pokemonName != null)
        {
            val species = PokemonSpecies.getByName(pokemonName[0].lowercase())
            if (species != null)
                lines.add(species.translatedName)
            if (pokemonName.size > 1)
                lines.add(Component.literal(pokemonName[1]))
        }

        val timer = stack.getOrDefault(TIMER, DEFAULT_TIMER)
        lines.add(Component.literal(ticksToTime(timer)))
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        if (entity is Player) {
            // Handling a timer to play this action every second instead of every tick
            val second = stack.getOrDefault(SECOND, DEFAULT_SECOND)
            stack.set(SECOND, second + 1)
            if (second < 20) return
            stack.set(SECOND, 0)

            val timer = stack.getOrDefault(TIMER, DEFAULT_TIMER)
            stack.set(TIMER, timer - 20)

            // Egg hatches faster is the player has a Pokémon with an ability increasing hatching speed
            if (world.isClientSide)
            {
                if (hasIncubatorAbility(CobblemonClient.storage.party))
                    stack.set(TIMER, timer - 40)
            }
            else
            {
                if (hasIncubatorAbility(Cobblemon.storage.getParty(entity as ServerPlayer)))
                    stack.set(TIMER, timer - 40)
                if (timer <= 0)
                {
                    val pokemonProperties = EggUtilities.extractProperties(stack)
                    val species = pokemonProperties?.species

                    if(species != null && (PokemonSpecies.getByName(species) != null || species == "random"))
                    {
                        if (species == "random")
                        {
                            val randomPokemon = EggUtilities.randomEgg()
                            pokemonProperties.species = randomPokemon.species.showdownId()
                            pokemonProperties.form = randomPokemon.formOnlyShowdownId()
                        }

                        resolveForm(pokemonProperties)
                        hatchEgg(entity, pokemonProperties)
                    }
                    else
                    {
                        entity.sendSystemMessage(Component.literal("Couldn't resolve pokemon species to hatch, deleting egg."))
                    }
                    stack.shrink(1)
                }
            }
        }
    }

    private fun hasIncubatorAbility(party: Iterable<Pokemon?>): Boolean
    {
        for (pokemon in party) {
            // Looking for an ability reducing hatching time in party
            val incubator = pokemon?.let {
                val ability = it.ability.template.name
                @Suppress("SpellCheckingInspection")
                (ability == "magmaarmor" || ability == "flamebody" || ability == "steamengine")
            } ?: false
            if (incubator) {
                return true
            }
        }
        return false
    }

    private fun resolveForm(properties: PokemonProperties)
    {
        if (properties.form != null)
        {
            properties.species?.let { PokemonSpecies.getByName(it) }!!.forms.find { it.formOnlyShowdownId() == properties.form }?.run {
                aspects.forEach {
                    // alternative form
                    properties.customProperties.add(FlagSpeciesFeature(it, true))
                    // regional bias
                    properties.customProperties.add(
                            StringSpeciesFeature(
                                    "region_bias",
                                    it.split("-").last()
                            )
                    )
                }
            }
        }
    }

    private fun hatchEgg(entity: Player, properties: PokemonProperties)
    {
        try {
            CobblemonEvents.HATCH_EGG_PRE.post(HatchEggEvent.Pre(properties, entity as ServerPlayer))
            val pokemon = properties.create()
            pokemon.setFriendship(120)

            // CobblemonSizeVariations compat
            if (CobblemonSizeVariations.enabled) {
                CobblemonSizeVariations.event?.let { pokemon.it() }
            }

            // Baby sheep have to grow their wool
            if (pokemon.species == PokemonSpecies.getByName("wooloo") || pokemon.species == PokemonSpecies.getByName("mareep"))
            {
                val feature = pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED)
                if (feature != null)
                {
                    feature.enabled = true
                    pokemon.markFeatureDirty(feature)
                    pokemon.updateAspects()
                }
            }

            // Adds the hatched pokémon to the party/pc and registers it in the Pokédex
            Cobblemon.storage.getParty(entity.uuid, entity.registryAccess()).add(pokemon)
            Cobblemon.playerDataManager.getPokedexData(entity.uuid).obtain(pokemon)

            CobblemonEvents.HATCH_EGG_POST.post(HatchEggEvent.Post(entity, pokemon))
        } catch (e: Exception) {
            Cobbreeding.LOGGER.error("Egg hatching failed: ${e.message}")
            e.printStackTrace()
            entity.sendSystemMessage(Component.translatable("cobbreeding.msg.egg_hatch.fail"))
        }
    }

    /**
     * Convert ticks to minutes and seconds
     * @return string representing time.
     */
    private fun ticksToTime(ticks: Int): String {
        val minutes = floor((ticks / 1200).toFloat()).toInt()
        val seconds = "%02d".format(floor((ticks % 1200 / 20).toFloat()).toInt())


        return if (minutes <= 0)
            seconds
        else
            "$minutes:$seconds"
    }
}
