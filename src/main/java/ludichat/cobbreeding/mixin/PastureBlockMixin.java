package ludichat.cobbreeding.mixin;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.CollectEggEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.pokemon.Pokemon;
import ludichat.cobbreeding.EggUtilities;
import ludichat.cobbreeding.PastureInventory;
import ludichat.cobbreeding.utils.CobbreedingFile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import ludichat.cobbreeding.CustomProperties;
import ludichat.cobbreeding.PastureBreedingData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;


@Mixin(PastureBlock.class)
public abstract class PastureBlockMixin {
    // This mixin can't extend Block (its codec() visibility no longer lets a Java subclass compile), so it shadows what it needs.
    @Shadow
    protected abstract void registerDefaultState(BlockState state);

    @Shadow
    public abstract BlockState defaultBlockState();

    @Unique
    private static final BooleanProperty HAS_EGG = CustomProperties.HAS_EGG;
    @Unique
    private static final BooleanProperty BREEDING_ACTIVATED = CustomProperties.BREEDING_ACTIVATED;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(BlockBehaviour.Properties properties, CallbackInfo ci) {
        registerDefaultState(defaultBlockState().setValue(HAS_EGG, false));
        registerDefaultState(defaultBlockState().setValue(BREEDING_ACTIVATED, false));
    }

    @Inject(at = @At("TAIL"), method = "createBlockStateDefinition")
    private void appendProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(HAS_EGG);
        builder.add(BREEDING_ACTIVATED);
    }

    @Inject(at = @At("HEAD"), method = "useWithoutItem", cancellable = true)
    private void onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!world.isClientSide()) {
            boolean is_bottom_part = state.getValue(PastureBlock.Companion.getPART()) == PastureBlock.PasturePart.BOTTOM;
            PastureInventory pastureInventory = (PastureInventory) world.getBlockEntity(pos);

            if (pastureInventory != null && !pastureInventory.isEmpty() && is_bottom_part) {
                // Get the egg
                ItemStack egg = pastureInventory.removeItemNoUpdate();
                player.getInventory().placeItemBackInInventory(egg);
                player.getInventory().tick();
                world.setBlockAndUpdate(pos, state.setValue(HAS_EGG, !pastureInventory.isEmpty()));

                // Emit COLLECT_EGG
                PokemonProperties properties = EggUtilities.extractProperties(egg);
                if (properties == null)
                    properties = new PokemonProperties();
                CobblemonEvents.COLLECT_EGG.emit(new CollectEggEvent(
                        properties,
                        new Pokemon(),
                        new Pokemon(),
                        (ServerPlayer) player
                ));

                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "playerWillDestroy")
    private void onBroken(Level world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (!world.isClientSide()) {
            PastureBreedingData.registry.remove(pos);

            if (state.getValue(PastureBlock.Companion.getPART()) == PastureBlock.PasturePart.TOP)
                pos = pos.below();

            // We remove this Pasture Block for breeding
            Path path = Objects.requireNonNull(world.getServer()).getWorldPath(LevelResource.ROOT).resolve("pokemon").resolve("cobbreeding");
            File pastureFile = path.resolve("pastures").toFile();
            UUID ownerUuid = CobbreedingFile.getUuid(pastureFile, pos.toString());

            CobbreedingFile.removeCompoundTag(pastureFile, pos.toString());
            if (ownerUuid != null)
            {
                File playerFile = path.resolve(ownerUuid.toString().substring(0, 2)).resolve(ownerUuid.toString()).toFile();
                CobbreedingFile.removeCompoundTag(playerFile, pos.toString());
            }
        }
    }
}
