package ludichat.cobbreeding.mixin;

import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.client.gui.pc.PCGUIConfiguration;
import ludichat.cobbreeding.gui.PastureBreedingButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import com.cobblemon.mod.common.client.gui.pasture.PasturePCGUIConfiguration;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PCGUI.class, priority = 2000)
public class PastureGUIMixin extends Screen {
    @Shadow(remap = false) @Final public static int BASE_WIDTH;
    @Shadow(remap = false) @Final public static int BASE_HEIGHT;

    @Shadow(remap = false) @Final
    private PCGUIConfiguration configuration;

    protected PastureGUIMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void onInit(CallbackInfo ci) {
        if (configuration instanceof PasturePCGUIConfiguration)
        {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel world = minecraft.level;
            HitResult hitResult =  minecraft.hitResult;
            if (hitResult instanceof BlockHitResult && world != null)
            {
                BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
                BlockState state = world.getBlockState(blockPos);
                if (state.getValue(PastureBlock.Companion.getPART()) == PastureBlock.PasturePart.TOP)
                    blockPos = blockPos.below();
                int x = (width - BASE_WIDTH) / 2;
                int y = (height - BASE_HEIGHT) / 2;
                this.addRenderableWidget(new PastureBreedingButton(x + 270, y - 10, 20, 18, blockPos));
            }
        }
    }
}
