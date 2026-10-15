package ludichat.cobbreeding

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.core.NonNullList

class PastureBreedingData(var time: Long, var eggs: NonNullList<ItemStack>, var requiredTicks: Int) {
    companion object {
        @JvmField
        val registry: Map<BlockPos, PastureBreedingData> = hashMapOf()
    }
}
