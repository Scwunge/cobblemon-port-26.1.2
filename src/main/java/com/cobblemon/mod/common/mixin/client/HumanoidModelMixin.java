/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.client.render.player.MountedPlayerRenderer;
import com.cobblemon.mod.common.client.render.player.RecallArmAnimationTracker;
import com.cobblemon.mod.common.duck.RidePassenger;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow @Final
    public ModelPart head;
    @Shadow @Final
    public ModelPart hat;
    @Shadow @Final
    public ModelPart rightArm;
    @Shadow @Final
    public ModelPart leftArm;
    @Shadow @Final
    public ModelPart rightLeg;
    @Shadow @Final
    public ModelPart leftLeg;
    @Shadow @Final
    public ModelPart body;

    /**
     * Models are posed from a render state since 1.21.2, so the entity is looked up from the player render state's id.
     * This used to run before the hat copied the head's pose, the last thing setupAnim did; the hat is a child of the
     * head now, so that is the end of setupAnim.
     */
    @Unique
    private static @Nullable LivingEntity cobblemon$entityOf(HumanoidRenderState state) {
        if (!(state instanceof AvatarRenderState avatarState)) return null;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        return level.getEntity(avatarState.id) instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    public void cobblemon$animateRiders(HumanoidRenderState renderState, CallbackInfo ci) {
        MountedPlayerRenderer.shouldApplyRootAnimation = false;

        if (MountedPlayerRenderer.relevantPartsByName.size() <= 1) {
            MountedPlayerRenderer.relevantPartsByName.put("arm_left", leftArm);
            MountedPlayerRenderer.relevantPartsByName.put("arm_right", rightArm);
            MountedPlayerRenderer.relevantPartsByName.put("leg_left", leftLeg);
            MountedPlayerRenderer.relevantPartsByName.put("leg_right", rightLeg);
            MountedPlayerRenderer.relevantPartsByName.put("head", head);
            MountedPlayerRenderer.relevantPartsByName.put("hat", hat);
            MountedPlayerRenderer.relevantPartsByName.put("torso", body);
        }

        LivingEntity entity = cobblemon$entityOf(renderState);
        if (entity == null) return;
        float netHeadYaw = renderState.yRot;
        float headPitch = renderState.xRot;

        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof PokemonEntity pokemonEntity && entity instanceof OrientationControllable) {
            var shouldRotatePlayerHead = pokemonEntity.ifRidingAvailableSupply(false, (behaviour, settings, state) ->
                    behaviour.shouldRotateRiderHead(settings, state, pokemonEntity)
            );

            if (!shouldRotatePlayerHead) {
                netHeadYaw = 0f;
                headPitch = 0f;
                if (this.head != null) {
                    this.head.yRot = 0f;
                    this.head.xRot = 0f;
                }
            }

            if (!(entity instanceof AbstractClientPlayer player)) return;

            MountedPlayerRenderer.shouldApplyRootAnimation = true;
            MountedPlayerRenderer.INSTANCE.animate((HumanoidModel<HumanoidRenderState>)(Object)this, renderState, pokemonEntity, player, netHeadYaw, headPitch, renderState.ageInTicks, renderState.walkAnimationPos, renderState.walkAnimationSpeed);
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    public void cobblemon$poseRecallArm(HumanoidRenderState renderState, CallbackInfo ci) {
        LivingEntity entity = cobblemon$entityOf(renderState);
        if (!(entity instanceof AbstractClientPlayer) || entity.getVehicle() instanceof PokemonEntity) return;
        float raise = RecallArmAnimationTracker.INSTANCE.raiseFactor(entity.getUUID());
        if (raise <= 0f) return;
        this.rightArm.xRot = Mth.lerp(raise, this.rightArm.xRot, (float) (-Math.PI / 2.0));
        this.rightArm.yRot = Mth.lerp(raise, this.rightArm.yRot, 0f);
        this.rightArm.zRot = Mth.lerp(raise, this.rightArm.zRot, 0f);
    }
}