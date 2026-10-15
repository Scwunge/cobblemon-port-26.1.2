package ludichat.cobbreeding.mixin;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.sugar.Local;
import ludichat.cobbreeding.gui.ToggleNeuterButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Summary.class, remap = false)
public class PokemonSummaryMixin extends Screen {
    protected PokemonSummaryMixin(Component title) {
        super(title);
    }

    @Shadow
    private Pokemon selectedPokemon;

    @Unique
    private ToggleNeuterButton cobbreeding$button;

    @Inject(method = "init()V", at = @At("TAIL"), remap = true)
    protected void init(CallbackInfo ci, @Local(name = "x") int x, @Local(name = "y") int y) {
        cobbreeding$button = new ToggleNeuterButton(x - 16, y + 31, 19, 19, selectedPokemon);
        addRenderableWidget(cobbreeding$button);
    }

    @Inject(method = "switchSelection", at = @At("TAIL"))
    protected void switchSelection(int newSelection, CallbackInfo ci) {
        cobbreeding$button.setPokemon(selectedPokemon);
    }
}
