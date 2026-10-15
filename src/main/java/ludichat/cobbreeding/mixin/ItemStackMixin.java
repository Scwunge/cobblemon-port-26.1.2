package ludichat.cobbreeding.mixin;

import ludichat.cobbreeding.EggUtilities;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void matches(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        if (EggUtilities.areSameEgg(a, b)) {
            cir.setReturnValue(true);
        }
    }

}
