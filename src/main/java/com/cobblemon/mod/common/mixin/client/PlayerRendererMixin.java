/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.ClientPlayerIcon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player rendering is split into extracting a render state and drawing it since 1.21.2. Both former hooks (the end of
 * render, and the crouch flags that set the model pose and the render offset) happen when the state is extracted,
 * which is once per rendered frame for each player, as render was.
 */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void cobblemon$extractRenderState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        ClientPlayerIcon.Companion.onRenderPlayer(player);
        // Riders of Pokémon neither crouch (the model pose) nor get the crouch render offset
        if (player.isPassenger() && player.getVehicle() instanceof PokemonEntity) {
            state.isCrouching = false;
        }
    }
}
