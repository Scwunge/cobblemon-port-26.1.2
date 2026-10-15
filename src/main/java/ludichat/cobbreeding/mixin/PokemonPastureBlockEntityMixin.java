package ludichat.cobbreeding.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.Pair;
import ludichat.cobbreeding.*;
import ludichat.cobbreeding.components.CobbreedingComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import org.jetbrains.annotations.NotNull;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static ludichat.cobbreeding.BreedingUtilities.chooseEgg;

@Mixin(PokemonPastureBlockEntity.class)
public abstract class PokemonPastureBlockEntityMixin extends BlockEntity implements PastureInventory, WorldlyContainer {
    public PokemonPastureBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Unique
    private static final Logger cobbreeding$LOGGER = Cobbreeding.LOGGER;
    @Unique
    private static final BooleanProperty cobbreeding$HAS_EGG = CustomProperties.HAS_EGG;
    @Unique
    private static final BooleanProperty cobbreeding$BREEDING_ACTIVATED = CustomProperties.BREEDING_ACTIVATED;

    @Unique
    private static int[] COBBREEDING_INVENTORY_SLOTS = null;

    @Override
    public NonNullList<ItemStack> getItems() {
        BlockPos pos = this.worldPosition;
        PastureBreedingData data;

        if (PastureBreedingData.registry.containsKey(pos)) {
            data = PastureBreedingData.registry.get(pos);
        } else {
            data = new PastureBreedingData(-1, NonNullList.withSize(cobbreeding$getConfig().getPastureInventorySize(), ItemStack.EMPTY), cobbreeding$getNewBreedingTime());
            PastureBreedingData.registry.put(pos, data);
        }

        return data.getEggs();
    }

    /**
     * Get mod configuration
     *
     * @return Configuration instance
     */
    @Unique
    private static Config cobbreeding$getConfig() {
        Config config;
        try{
            config = Cobbreeding.INSTANCE.getConfig();
        } catch (kotlin.UninitializedPropertyAccessException e) {
            cobbreeding$LOGGER.warn("Trying to read configurations but they haven't been initialized. Returning Default configs instead. This might happen because the client is running logic it shouldn't.");
            return new Config();
        }
        return config;
    }

    /**
     * Get random breeding time between configured min and max.
     */
    @Unique
    private static int cobbreeding$getNewBreedingTime()
    {
        return new Random().nextInt(cobbreeding$getConfig().getMinBreedingTimeInTicks(), cobbreeding$getConfig().getMaxBreedingTimeInTicks() + 1);
    }

    @Inject(at = @At("HEAD"), method = "TICKER$lambda$0")
    private static void tick(Level world, BlockPos pos, BlockState state, PokemonPastureBlockEntity blockEntity, CallbackInfo ci) {
        long world_time = world.getGameTime();
        // Encapsulating to avoid conflict with other pasture mods
        if (!world.isClientSide && world_time%Cobblemon.config.getPastureBlockUpdateTicks() == 0) {
            List<Pokemon> pokemon = new LinkedList<>(); // List of pastured pokémon
            try {
                List<PokemonPastureBlockEntity.Tethering> tetheredPokemon = blockEntity.getTetheredPokemon();
                pokemon = BreedingUtilities.getPokemon(tetheredPokemon);
            } catch (NullPointerException e)
            {
                // Muting the exception
                // If we get a NullPointerException there, it's probably that the block is still ticking while the
                // world is closing.
            }

            // Applying Mirror Herb effect to Pokémon holding it
            if (world_time % cobbreeding$getConfig().getMirrorHerbTimeInTicks() == 0) {
                BreedingUtilities.applyMirrorHerb(pokemon);
            }

            // Getting the breeding data of this Pasture block
            PastureBreedingData data;

            if (PastureBreedingData.registry.containsKey(pos)) {
                data = PastureBreedingData.registry.get(pos);
            } else {
                data = new PastureBreedingData(world.getGameTime(), NonNullList.withSize(cobbreeding$getConfig().getPastureInventorySize(), ItemStack.EMPTY), cobbreeding$getNewBreedingTime());
                PastureBreedingData.registry.put(pos, data);
            }
            Collection<Map.Entry<FormData, List<Pair<Pokemon, Pokemon>>>> possibleEggs = BreedingUtilities.getPossibleEggs(pokemon);
            if (state.getValue(cobbreeding$BREEDING_ACTIVATED) && !possibleEggs.isEmpty()) {
                // If the pasture block has room for eggs, then we check the time that passed to create more eggs
                if (data.getEggs().stream().anyMatch(itemStack -> itemStack.getCount() == 0)) {
                    boolean egg_created = false;
                    long pasture_time = data.getTime() == -1 ? world_time : data.getTime();
                    data.setTime(pasture_time);
                    long time_difference = world_time - pasture_time;

                    while (time_difference >= data.getRequiredTicks()) {
                        egg_created = true;

                        cobbreeding$addEgg(data, possibleEggs);

                        time_difference -= data.getRequiredTicks();
                        data.setRequiredTicks(cobbreeding$getNewBreedingTime());

                        // Stopping the loop early if the Pasture Block is full
                        if (data.getEggs().stream().noneMatch(itemStack -> itemStack.getCount() == 0))
                            break;
                    }
                    if (egg_created) {
                        data.setTime(world_time);
                        world.setBlockAndUpdate(pos, state.setValue(cobbreeding$HAS_EGG, data.getEggs().stream().anyMatch(itemStack -> itemStack.getCount() != 0)));
                        // Playing a sound
                        world.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1f, 1f);
                    }
                }

            }
            else
            {
                // Resetting the timer if the pasture isn't being used for breeding at the moment.
                // This is to ensure a Pasture will not instantly get filled with eggs as we set a new pair in it or
                // reactivate breeding after a long period.
                data.setTime(-1);
            }

            // Reset HAS_EGG state if all eggs are gone and the state was set (hopper)
            if (state.getValue(cobbreeding$HAS_EGG) && data.getEggs().stream().allMatch(itemStack -> itemStack.getCount() == 0)) {
                world.setBlockAndUpdate(pos, state.setValue(cobbreeding$HAS_EGG, false));
            }
        }
    }

    @Unique
    private static void cobbreeding$addEgg(PastureBreedingData data,
                                           Collection<Map.Entry<FormData, List<Pair<Pokemon, Pokemon>>>> possibleEggs)
    {
        PokemonProperties eggData = chooseEgg(possibleEggs);
        ItemStack eggItem = null;

        if (eggData != null)
        {
            // Creating the egg
            if(eggData.getSpecies() != null)
            {
                Species egg_pokemon = PokemonSpecies.getByName(eggData.getSpecies());
                if (egg_pokemon != null)
                {
                    // Choosing egg texture
                    FormData form = egg_pokemon.getStandardForm();
                    if (eggData.getForm() != null) {
                        form = egg_pokemon.getFormByShowdownId(eggData.getForm());
                    }

                    // Choosing egg color
                    eggItem = EggUtilities.selectEggItem(form);

                    // Setting the egg name
                    String name = eggData.getSpecies();
                    if (eggData.getForm() != null && !form.getName().equals("Normal")) {
                        name += " " + form.getName();
                    }
                    eggItem.set(
                            CobbreedingComponents.NAME.get(),
                            name
                    );
                    // Setting the egg timer
                    eggItem.set(
                            CobbreedingComponents.TIMER.get(),
                            EggUtilities.calculateTimer(egg_pokemon)
                    );
                }
                if (eggData.getSpecies().equals("random"))
                {
                    eggItem = new ItemStack(Cobbreeding.EGG_ITEMS.get("pokemon_egg").get());
                    // Setting the egg timer
                    eggItem.set(
                            CobbreedingComponents.TIMER.get(),
                            EggUtilities.calculateTimer(PokemonSpecies.random())
                    );
                }
                if (egg_pokemon != null || eggData.getSpecies().equals("random"))
                {
                    // Setting the pokémon in the egg
                    if (!cobbreeding$getConfig().getEggEncryptionEnabled())
                    {
                        eggItem.set(CobbreedingComponents.POKEMON_PROPERTIES.get(), eggData.asString(" "));
                    }
                    eggItem.set(CobbreedingComponents.EGG_INFO.get(), EggUtilities.encrypt(eggData));
                    // Setting the version the egg is from
                    eggItem.set(CobbreedingComponents.VERSION.get(), PokemonEgg.CURRENT_COMPONENT_VERSION);

                    // Adding the egg to the inventory
                    cobbreeding$set(data.getEggs(), eggItem);
                }
            }

            if (eggItem == null)
                Cobbreeding.LOGGER.error("Couldn't add egg to pasture. Properties : {}", eggData);
        }
        data.setRequiredTicks(cobbreeding$getNewBreedingTime());
    }

    @Unique
    private static void cobbreeding$set(NonNullList<ItemStack> list, ItemStack stack) {
        ItemStack itemStack;
        for (int slot = 0; slot < list.size(); slot++)
        {
            itemStack = list.get(slot);
            if (itemStack.getCount() == 0)
            {
                list.set(slot, stack);
                return;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "saveAdditional")
    private void writeNbt(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        ContainerHelper.saveAllItems(nbt, getItems(), provider);
    }

    @Inject(at = @At("TAIL"), method = "loadAdditional")
    private void readNbt(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
//        LOGGER.info("Loading pasture inventory:");
//        for (var item : getItems()) { LOGGER.info("\t{}", item); }
        ContainerHelper.loadAllItems(nbt, getItems(), provider);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return cobbreeding$getConfig().getAllowHoppersToPullFromPastureBlock();
    }

    @Override
    public int @NotNull [] getSlotsForFace(Direction direction) {
        if (COBBREEDING_INVENTORY_SLOTS == null || COBBREEDING_INVENTORY_SLOTS.length > cobbreeding$getConfig().getPastureInventorySize()) {
            COBBREEDING_INVENTORY_SLOTS = new int[cobbreeding$getConfig().getPastureInventorySize()];
            for (int i = 0; i < COBBREEDING_INVENTORY_SLOTS.length; i++) {
                COBBREEDING_INVENTORY_SLOTS[i] = i;
            }
        }
        return COBBREEDING_INVENTORY_SLOTS;
    }
}
