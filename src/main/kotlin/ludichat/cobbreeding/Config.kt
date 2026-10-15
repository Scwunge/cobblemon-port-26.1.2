package ludichat.cobbreeding

import com.google.gson.GsonBuilder
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Suppress("KotlinConstantConditions")
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Config(
    @EncodeDefault var minBreedingTimeInTicks: Int = Defaults.MIN_BREEDING_TIME_IN_TICKS,
    @EncodeDefault var maxBreedingTimeInTicks: Int = Defaults.MAX_BREEDING_TIME_IN_TICKS,
    @EncodeDefault var mirrorHerbTimeInTicks: Int = Defaults.MIRROR_HERB_TIME_IN_TICKS,
    @EncodeDefault var eggHatchMultiplier: Float = Defaults.EGG_HATCH_MULTIPLIER,
    @EncodeDefault var shinyMethod: MutableMap<String, Float> = Defaults.SHINY_METHOD,
    @EncodeDefault var hiddenAbilitiesEnabled: Boolean = Defaults.HIDDEN_ABILITIES_ENABLED,
    @EncodeDefault var forcedAbilitiesEnabled: Boolean = Defaults.FORCED_ABILITIES_ENABLED,
    @EncodeDefault var dittoAndDittoRandomEgg: Boolean = Defaults.DITTO_AND_DITTO_RANDOM_EGG,
    @EncodeDefault var dittoAndDittoAllowLegendary: Boolean = Defaults.DITTO_AND_DITTO_ALLOW_LEGENDARY,
    @EncodeDefault var dittoAndDittoAllowParadox: Boolean = Defaults.DITTO_AND_DITTO_ALLOW_PARADOX,
    @EncodeDefault var dittoAndDittoAllowUltraBeast: Boolean = Defaults.DITTO_AND_DITTO_ALLOW_ULTRA_BEAST,
    @EncodeDefault var dittoAndDittoAllowUndiscovered: Boolean = Defaults.DITTO_AND_DITTO_ALLOW_UNDISCOVERED,
    @EncodeDefault var allowHoppersToPullFromPastureBlock: Boolean = Defaults.ALLOW_HOPPERS_TO_PULL_FROM_PASTURE_BLOCK,
    @EncodeDefault var inheritedFeatures: List<String> = Defaults.INHERITED_FEATURES,
    @EncodeDefault var maxNumberOfActivatedPasturePerPlayer: Int = Defaults.MAX_PASTURE,
    @EncodeDefault var pastureInventorySize: Int = Defaults.PASTURE_INVENTORY_SIZE,
    @EncodeDefault var eggEncryptionEnabled: Boolean = Defaults.EGG_ENCRYPTION_ENABLED,
    @EncodeDefault var cobblemonSizeVariationsCompatEnabled: Boolean = Defaults.COBBLEMON_SIZE_VARIATIONS_COMPAT_ENABLED,
) {
    companion object {
        val GSON = GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
    }


    object Defaults {
        const val MIN_BREEDING_TIME_IN_TICKS = 8000
        const val MAX_BREEDING_TIME_IN_TICKS = 14000
        const val MIRROR_HERB_TIME_IN_TICKS = 600
        const val EGG_HATCH_MULTIPLIER = 1.0F
        val SHINY_METHOD = mutableMapOf("masuda" to 4.0F, "crystal" to 1.0F, "always" to 1.0F)
        const val HIDDEN_ABILITIES_ENABLED = true
        const val FORCED_ABILITIES_ENABLED = false
        const val DITTO_AND_DITTO_RANDOM_EGG = false
        const val DITTO_AND_DITTO_ALLOW_LEGENDARY = false
        const val DITTO_AND_DITTO_ALLOW_PARADOX = false
        const val DITTO_AND_DITTO_ALLOW_ULTRA_BEAST = false
        const val DITTO_AND_DITTO_ALLOW_UNDISCOVERED = false
        const val ALLOW_HOPPERS_TO_PULL_FROM_PASTURE_BLOCK = true
        @Suppress("SpellCheckingInspection")
        val INHERITED_FEATURES = listOf(
            "bagworm_cloak",
            "color",
            "dance_style",
            "fish_stripes",
            "striped",
            "magikarp_jump",
            "mooshtank",
            "region_bias",
            "alolan",
            "galarian",
            "hisuian",
            "paldean",
            "bull_breed",
            "tatsugiri_texture",
            "whiscash_nero",
            "wooper_heart",
            "wolf_form"
        )
        const val MAX_PASTURE = -1
        const val PASTURE_INVENTORY_SIZE = 5
        const val EGG_ENCRYPTION_ENABLED = true
        const val COBBLEMON_SIZE_VARIATIONS_COMPAT_ENABLED = true
    }

    init {
        // Correcting potential errors
        if (minBreedingTimeInTicks < 20)
            minBreedingTimeInTicks = 20
        if(maxBreedingTimeInTicks < minBreedingTimeInTicks)
            maxBreedingTimeInTicks = minBreedingTimeInTicks
        if (mirrorHerbTimeInTicks <= 0)
            mirrorHerbTimeInTicks = 20
        if (eggHatchMultiplier <= 0)
            eggHatchMultiplier = 0.001f
        if (pastureInventorySize < 1)
            pastureInventorySize = 1
    }
}