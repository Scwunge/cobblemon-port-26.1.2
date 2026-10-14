/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonVillagerProfessions;
import com.cobblemon.mod.common.duck.VillagerNameHolder;
import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Villagers of a Cobblemon profession whose name matches its name tag override use the override's texture instead of
 * the profession's. This used to cancel the draw and draw the override texture itself; models are submitted with their
 * texture since 1.21.9, so the texture is swapped on the way in. The villager comes from its render state.
 */
@Mixin(RenderLayer.class)
public abstract class VillagerProfessionLayerMixin {
    @ModifyVariable(method = "renderColoredCutoutModel", at = @At("HEAD"), argsOnly = true)
    private static Identifier cobblemon$renderVillagerProfessionLayer(Identifier resourceLocation, @Local(argsOnly = true) LivingEntityRenderState state) {
        if (resourceLocation.getNamespace().equals(Cobblemon.MODID) && state instanceof VillagerDataHolderRenderState holder) {
            VillagerData villagerData = holder.getVillagerData();
            if (villagerData == null) return resourceLocation;
            Holder<VillagerProfession> profession = villagerData.profession();
            boolean isZombie = state instanceof ZombieVillagerRenderState;

            Pair<String, String[]> override = CobblemonVillagerProfessions.INSTANCE.getNameTagOverride(profession.value());
            if (override != null && profession.unwrapKey().isPresent()) {
                String entityName = state instanceof VillagerNameHolder nameHolder ? nameHolder.cobblemon$getEntityName() : "";
                boolean nameMatches = false;
                for (String name: override.getSecond())
                    if (entityName.endsWith(name)) {
                        nameMatches = true;
                        break;
                    }
                if (nameMatches && resourceLocation.equals(getProfessionResourceLocation(resourceLocation.getNamespace(), profession.unwrapKey().get().identifier().getPath(), isZombie))) {
                    return getProfessionResourceLocation(resourceLocation.getNamespace(), override.getFirst(), isZombie);
                }
            }
        }
        return resourceLocation;
    }

    private static Identifier getProfessionResourceLocation(String namespace, String profession, Boolean isZombie) {
        return Identifier.fromNamespaceAndPath(namespace, "textures/entity/" + (isZombie ? "zombie_villager" : "villager") + "/profession/" + profession + ".png");
    }
}
