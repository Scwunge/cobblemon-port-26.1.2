package ludichat.cobbreeding

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Species
import ludichat.cobbreeding.components.CobbreedingComponents
import ludichat.cobbreeding.utils.CobbreedingFile
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object EggUtilities {
    @JvmStatic
    fun selectEggItem(pokemon: FormData): ItemStack {
        if (pokemon.species.showdownId() == "manaphy") return ItemStack(Cobbreeding.EGG_ITEMS["manaphy_egg"]!!.get())

        try {
            val type1 = pokemon.primaryType
            val type2 = pokemon.secondaryType
            if (type2 != null && type1 != type2) return ItemStack(Cobbreeding.EGG_ITEMS[type1.showdownId + "_" + type2.showdownId + "_pokemon_egg"]!!.get())

            return ItemStack(Cobbreeding.EGG_ITEMS[type1.showdownId + "_pokemon_egg"]!!.get())
        } catch (_: NullPointerException) {}

        return ItemStack(Cobbreeding.EGG_ITEMS["pokemon_egg"]!!.get())
    }

    @JvmStatic
    fun calculateTimer(pokemon: Species): Int {
        return Mth.floor(pokemon.eggCycles * PokemonEgg.DEFAULT_TIMER * Cobbreeding.config.eggHatchMultiplier)
    }

    @JvmStatic
    fun getEggFromPokemonProperties(properties: PokemonProperties, timer: Int? = null) : ItemStack?
    {
        val species = PokemonSpecies.getByName(properties.species ?: "")
        val eggItem: ItemStack
        if (species != null)
        {
            var form = species.standardForm
            if (properties.form != null)
                form = species.getFormByShowdownId(properties.form!!)

            eggItem = selectEggItem(form)

            // Setting the name
            var name: String = properties.species!!
            if (properties.form != null && form.name != "Normal") {
                name += " " + form.name
            }
            eggItem.set(
                    CobbreedingComponents.NAME.get(),
                    name
            )
            // Setting Pokémon Properties
            if (!Cobbreeding.config.eggEncryptionEnabled)
                eggItem.set(CobbreedingComponents.POKEMON_PROPERTIES.get(), properties.asString(" "))
            eggItem.set(CobbreedingComponents.EGG_INFO.get(), encrypt(properties))
            // Setting the egg timer
            if (timer == null)
                eggItem.set<Int>(
                        CobbreedingComponents.TIMER.get(),
                        calculateTimer(species)
                )
            else
                eggItem.set<Int>(
                        CobbreedingComponents.TIMER.get(),
                        timer
                )
            // Setting the version
            eggItem.set(CobbreedingComponents.VERSION.get(), PokemonEgg.CURRENT_COMPONENT_VERSION)

            return eggItem
        }
        else if (properties.species == "random")
        {
            eggItem = ItemStack(Cobbreeding.EGG_ITEMS["pokemon_egg"]!!.get())
            eggItem.set(CobbreedingComponents.TIMER.get(), calculateTimer(PokemonSpecies.random()))
            if (!Cobbreeding.config.eggEncryptionEnabled)
                eggItem.set(CobbreedingComponents.POKEMON_PROPERTIES.get(), properties.asString(" "))
            eggItem.set(CobbreedingComponents.EGG_INFO.get(), encrypt(properties))
            // Setting the version
            eggItem.set(CobbreedingComponents.VERSION.get(), PokemonEgg.CURRENT_COMPONENT_VERSION)
            return eggItem
        }

        return null
    }

    @JvmStatic
    fun randomEgg() : FormData
    {
        // Choosing a random species
        var species = PokemonSpecies.random()
        var form = species.standardForm
        if (species.forms.count() > 0)
            form = species.forms.random()

        // Rolling until the Pokémon is allowed by the configs
        while (!isRandomSpeciesAllowed(form))
        {
            species = PokemonSpecies.random()
            form = species.standardForm
            if (species.forms.count() > 0)
                form = species.forms.random()
        }

        // Choosing a random form
        return BreedingUtilities.getBaby(form)
    }

    @JvmStatic
    private fun isRandomSpeciesAllowed(pokemon: FormData) : Boolean
    {
        val isLegendary = pokemon.labels.contains("legendary") || pokemon.labels.contains("mythical")
        val isParadox = pokemon.labels.contains("paradox")
        val isUltraBeast = pokemon.labels.contains("ultra_beast")
        val isUndiscovered = pokemon.eggGroups.contains(EggGroup.UNDISCOVERED)

        // The Pokémon can normally be obtained via egg
        if (!isLegendary && !isParadox && !isUltraBeast && !isUndiscovered)
            return true

        // Legendary / Mythical check
        if (Cobbreeding.config.dittoAndDittoAllowLegendary && isLegendary)
            return true
        // Paradox check
        if (!isLegendary && Cobbreeding.config.dittoAndDittoAllowParadox && isParadox)
            return true
        // Ultra Beast check
        if (!isLegendary && !isParadox && Cobbreeding.config.dittoAndDittoAllowUltraBeast && isUltraBeast)
            return true
        // Undiscovered check
        if (!isLegendary && !isParadox && !isUltraBeast && Cobbreeding.config.dittoAndDittoAllowUndiscovered)
            return true


        // Default return is false because some check may be overlapping (eg "legendary" and "undiscovered")
        return false
    }

    @JvmStatic
    private fun isEgg(itemStack: ItemStack): Boolean
    {
        return itemStack.item is PokemonEgg
    }

    @JvmStatic
    fun areSameEgg(a: ItemStack, b: ItemStack): Boolean
    {
        if (isEgg(a) && isEgg(b))
        {
            val pp_a = a.getOrDefault(CobbreedingComponents.POKEMON_PROPERTIES.get(), "")
            val pp_b = b.getOrDefault(CobbreedingComponents.POKEMON_PROPERTIES.get(), "")

            val ei_a = a.getOrDefault(CobbreedingComponents.EGG_INFO.get(), "")
            val ei_b = b.getOrDefault(CobbreedingComponents.EGG_INFO.get(), "")
            if (pp_a.equals(pp_b) && ei_a.equals(ei_b))
                return true
        }
        return false
    }

    @OptIn(ExperimentalEncodingApi::class)
    @JvmStatic
    fun encrypt(pokemonProperties: PokemonProperties): String
    {
        val encryptionKey = CobbreedingFile.getEncryptionKey()

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val ivParameterSpec = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivParameterSpec)

        var array = pokemonProperties.asString(" ").toByteArray()
        if (array.size % 16 != 0) array = array.copyOf(array.size + 16 - (array.size % 16))
        return Base64.encode(cipher.doFinal(array))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @JvmStatic
    fun decrypt(pokemonProperties: String): PokemonProperties
    {
        val encryptionKey = CobbreedingFile.getEncryptionKey()

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val ivParameterSpec = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, ivParameterSpec)

        var array = Base64.decode(pokemonProperties)
        if (array.size % 16 != 0) array = array.copyOf(array.size + 16 - (array.size % 16))
        return PokemonProperties.parse(String(cipher.doFinal(array)).trim().filterNot { it == '\u0000' })
    }

    /**
     * PokemonProperties stored in the egg, decrypted if not already.
     */
    @JvmStatic
    fun extractProperties(item: ItemStack): PokemonProperties? {
        return if (item.has(PokemonEgg.POKEMON_PROPERTIES)) {
            item.get(PokemonEgg.POKEMON_PROPERTIES)?.let { PokemonProperties.parse(it) }
        } else {
            item.get(PokemonEgg.EGG_INFO)?.let { decrypt(it) }
        }
    }
}