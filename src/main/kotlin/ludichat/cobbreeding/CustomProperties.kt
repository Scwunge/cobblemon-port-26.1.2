package ludichat.cobbreeding

import net.minecraft.world.level.block.state.properties.BooleanProperty

object CustomProperties {
    @JvmField
    val HAS_EGG: BooleanProperty = BooleanProperty.create("has_egg")
    @JvmField
    val BREEDING_ACTIVATED: BooleanProperty = BooleanProperty.create("breeding_activated")
}
