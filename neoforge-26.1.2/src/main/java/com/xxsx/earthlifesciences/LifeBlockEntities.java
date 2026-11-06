package com.xxsx.earthlifesciences;

import net.minecraft.world.InteractionResult;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xxsx.earthlore.api.GateResult;
import com.xxsx.earthlore.api.LoreApi;
import com.xxsx.earthlore.api.LoreMachines;
import com.xxsx.earthonminecraft.ElectricEnergyStorage;
import com.xxsx.earthonminecraft.EnergyNetwork;
import com.xxsx.earthonminecraft.component.EarthDataComponents;
import com.xxsx.earthonminecraft.component.MaterialBatch;
import com.xxsx.earthonminecraft.component.PharmaceuticalComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * All 12 BlockEntities for Earth: Life Sciences (16 册 §3–§4).
 */
public final class LifeBlockEntities {
    private LifeBlockEntities() {
    }

    /** Common powered container machine base implementing ElectricEnergyStorage. */
    public abstract static class PoweredMachineBlockEntity extends BaseContainerBlockEntity implements ElectricEnergyStorage {
        protected NonNullList<ItemStack> items;
        protected int progress;
        protected int maxProgress;
        protected int energyStored;
        protected int maxEnergy;
        protected String fault = "";

        protected final ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStored;
                    case 3 -> maxEnergy;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> maxProgress = value;
                    case 2 -> energyStored = value;
                    case 3 -> maxEnergy = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };

        protected PoweredMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int maxProg, int maxEn) {
            super(type, pos, state);
            this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
            this.maxProgress = maxProg;
            this.maxEnergy = maxEn;
        }

        protected void pullNetworkEnergy(Level level, BlockPos pos, int pullRate) {
            if (energyStored < maxEnergy) {
                int pulled = EnergyNetwork.extractUpTo(level, pos, Math.min(pullRate, maxEnergy - energyStored));
                if (pulled > 0) {
                    energyStored += pulled;
                    setChanged();
                }
            }
        }

        protected boolean consumeEnergy(Level level, BlockPos pos, int eou) {
            pullNetworkEnergy(level, pos, Math.max(eou * 4, 64));
            if (energyStored >= eou) {
                energyStored -= eou;
                setChanged();
                return true;
            }
            return EnergyNetwork.consume(level, pos, eou);
        }

        protected void updateBlockFlags(Level level, BlockPos pos, BlockState state, boolean active, boolean hasFault) {
            if (state.hasProperty(LifeBlocks.MachineBlock.ACTIVE) && state.hasProperty(LifeBlocks.MachineBlock.FAULT)) {
                if (state.getValue(LifeBlocks.MachineBlock.ACTIVE) != active
                        || state.getValue(LifeBlocks.MachineBlock.FAULT) != hasFault) {
                    level.setBlock(pos, state.setValue(LifeBlocks.MachineBlock.ACTIVE, active)
                            .setValue(LifeBlocks.MachineBlock.FAULT, hasFault), Block.UPDATE_ALL);
                }
            }
        }

        public String fault() {
            return fault;
        }

        public void setFaultCode(String code) {
            String c = code == null ? "" : code;
            if (!this.fault.equals(c)) {
                this.fault = c;
                setChanged();
            }
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getEnergyCapacity() {
            return maxEnergy;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.max(0, Math.min(amount, maxEnergy - energyStored));
            if (!simulate && accepted > 0) {
                energyStored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        protected NonNullList<ItemStack> getItems() {
            return items;
        }

        @Override
        protected void setItems(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            ContainerHelper.saveAllItems(output, items);
            output.putInt("progress", progress);
            output.putInt("energy_stored", energyStored);
            output.putString("fault", fault == null ? "" : fault);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
            progress = Math.max(0, input.getIntOr("progress", 0));
            energyStored = Math.max(0, Math.min(maxEnergy, input.getIntOr("energy_stored", 0)));
            fault = input.getStringOr("fault", "");
        }
    }

    /** B-009 incubator BlockEntity (4 slots: 0=dish, 1=medium, 2=sample/pure_culture, 3=output). */
    public static final class IncubatorBlockEntity extends PoweredMachineBlockEntity {
        private int temperatureK = 310;

        public IncubatorBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.INCUBATOR_BE.get(), pos, state, 4, LifeScienceMath.INCUBATE_TICKS, 20000);
        }

        public int temperatureK() {
            return temperatureK;
        }

        public void setTemperatureK(int tempK) {
            this.temperatureK = Math.max(293, Math.min(318, tempK));
            setChanged();
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity be) {
            ItemStack dish = be.items.get(0);
            ItemStack med = be.items.get(1);
            ItemStack in = be.items.get(2);
            ItemStack out = be.items.get(3);
            boolean vaccineMode = be.temperatureK == 318 && in.is(EarthLifeSciences.PURE_CULTURE.get());
            boolean validInput = !med.isEmpty() && med.is(EarthLifeSciences.CULTURE_MEDIUM.get())
                    && (vaccineMode || (!dish.isEmpty() && dish.is(EarthLifeSciences.PETRI_DISH.get())))
                    && (in.is(EarthLifeSciences.MICROBE_SAMPLE.get()) || in.is(EarthLifeSciences.PURE_CULTURE.get()));
            if (!validInput || !out.isEmpty()) {
                be.progress = 0;
                be.setFaultCode(validInput ? "" : "no_input");
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            if (!vaccineMode && Math.abs(be.temperatureK - 310) > 5) {
                be.setFaultCode("temperature_out_of_range");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (!be.consumeEnergy(level, pos, 2)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= LifeScienceMath.INCUBATE_TICKS) {
                be.progress = 0;
                be.finishIncubation(level);
            }
            be.setChanged();
        }

        private void finishIncubation(Level level) {
            ItemStack dish = items.get(0);
            ItemStack med = items.get(1);
            ItemStack in = items.get(2);
            LifeComponents.StrainComponent strain = in.getOrDefault(EarthLifeSciences.STRAIN.get(), LifeComponents.StrainComponent.DEFAULT);
            if (temperatureK == 318 && in.is(EarthLifeSciences.PURE_CULTURE.get())
                    && ("clostridium".equals(strain.species()) || "escherichia".equals(strain.species()))) {
                String target = "clostridium".equals(strain.species()) ? "tetanus" : "dysentery";
                in.shrink(1);
                med.shrink(1);
                items.set(3, LifeItems.AttenuatedVaccineItem.create(target, 4));
                return;
            }
            boolean dishSterile = LifeLoreIntegration.isSterile(dish);
            if (in.is(EarthLifeSciences.PURE_CULTURE.get())) {
                LifeScienceMath.SubcultureResult sub = LifeScienceMath.computeSubculture(strain.purity(), strain.generation(), dishSterile);
                if (!sub.ok()) {
                    setFaultCode(sub.fault());
                    return;
                }
                dish.shrink(1);
                med.shrink(1);
                in.shrink(1);
                ItemStack result = new ItemStack(EarthLifeSciences.PURE_CULTURE.get(), 2);
                result.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent(
                        strain.species(), strain.subtype(), true, sub.purity(), sub.generation(), sub.mixed()));
                items.set(3, result);
                setFaultCode(sub.fault());
                return;
            }
            MaterialBatch batch = med.get(EarthDataComponents.MATERIAL_BATCH.get());
            com.xxsx.earthlore.api.Grade medGrade = batch != null ? LifeLoreIntegration.toLoreGrade(batch.grade()) : com.xxsx.earthlore.api.Grade.COMMON;
            double purity = LifeScienceMath.computeCulturePurity(dishSterile, medGrade, level.getRandom().nextDouble() * 0.10D);
            boolean pure = LifeScienceMath.isCulturePure(purity);
            dish.shrink(1);
            med.shrink(1);
            in.shrink(1);
            ItemStack result = new ItemStack(EarthLifeSciences.PURE_CULTURE.get(), 1);
            result.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent(
                    strain.species(), strain.subtype(), true, purity, 0, !pure));
            items.set(3, result);
            setFaultCode(pure ? "" : "contaminated");
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.incubator");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.INCUBATOR_MENU.get(), id, inv, this, data, worldPosition, 4);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("temperature_k", temperatureK);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            temperatureK = Math.max(293, Math.min(318, input.getIntOr("temperature_k", 310)));
        }
    }

    /** B-011 fermenter BlockEntity (slots: 0=pure_culture, 1=substrate A, 2=substrate B/output). */
    public static final class FermenterBlockEntity extends PoweredMachineBlockEntity {
        private int tankMb;
        private String brothProduct = "";
        private double inoculumPurity = 0.85D;

        public FermenterBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.FERMENTER_BE.get(), pos, state, 3, LifeScienceMath.FERMENT_BATCH_TICKS, 60000);
        }

        public int tankMb() {
            return tankMb;
        }

        public String brothProduct() {
            return brothProduct;
        }

        public double inoculumPurity() {
            return inoculumPurity;
        }

        public int drainBroth(int amountMb) {
            int drained = Math.max(0, Math.min(amountMb, tankMb));
            tankMb -= drained;
            if (tankMb == 0) {
                brothProduct = "";
            }
            setChanged();
            return drained;
        }

        public void addBrothForTest(String product, int mb, double purity) {
            this.brothProduct = product;
            this.tankMb = Math.min(LifeScienceMath.FERMENTER_TANK_MB, this.tankMb + mb);
            this.inoculumPurity = purity;
            setChanged();
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, FermenterBlockEntity be) {
            ItemStack culture = be.items.get(0);
            ItemStack subA = be.items.get(1);
            if (!culture.is(EarthLifeSciences.PURE_CULTURE.get())) {
                be.progress = 0;
                be.setFaultCode("no_inoculum");
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            LifeComponents.StrainComponent strain = culture.getOrDefault(EarthLifeSciences.STRAIN.get(), LifeComponents.StrainComponent.DEFAULT);
            String product = resolveFermentProduct(strain, subA, be.items.get(2));
            if (product.isEmpty()) {
                be.progress = 0;
                be.setFaultCode("no_substrate");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (be.tankMb + LifeScienceMath.BROTH_PER_BATCH_MB > LifeScienceMath.FERMENTER_TANK_MB
                    || (!be.brothProduct.isEmpty() && !be.brothProduct.equals(product))) {
                be.setFaultCode("tank_full");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (!be.consumeEnergy(level, pos, 6)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= LifeScienceMath.FERMENT_BATCH_TICKS) {
                be.progress = 0;
                LifeScienceMath.FermentBatchResult batch = LifeScienceMath.computeFermentBatch(strain.purity());
                be.brothProduct = product;
                be.inoculumPurity = strain.purity();
                be.tankMb = Math.min(LifeScienceMath.FERMENTER_TANK_MB, be.tankMb + batch.brothMb());
                culture.shrink(1);
                subA.shrink(Math.min(4, subA.getCount()));
                be.setFaultCode(batch.fault());
            }
            be.setChanged();
        }

        private static String resolveFermentProduct(LifeComponents.StrainComponent strain, ItemStack subA, ItemStack subB) {
            if (subA.isEmpty()) {
                return "";
            }
            return switch (strain.species()) {
                case "penicillium" -> "penicillin";
                case "streptomyces" -> "none".equals(strain.subtype()) ? "streptomycin" : strain.subtype();
                case "saccharomyces" -> "ethanol";
                case "lactobacillus" -> "yogurt";
                case "aspergillus", "bacillus" -> "enzyme";
                default -> "";
            };
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.fermenter");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.FERMENTER_MENU.get(), id, inv, this, data, worldPosition, 3);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("tank_mb", tankMb);
            output.putString("broth_product", brothProduct == null ? "" : brothProduct);
            output.putDouble("inoculum_purity", inoculumPurity);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            tankMb = Math.max(0, Math.min(LifeScienceMath.FERMENTER_TANK_MB, input.getIntOr("tank_mb", 0)));
            brothProduct = input.getStringOr("broth_product", "");
            inoculumPurity = Math.max(0.0D, Math.min(1.0D, input.getDoubleOr("inoculum_purity", 0.85D)));
        }
    }

    /** B-012 centrifuge BlockEntity (slots: 0=optional activated_carbon, 1=output). */
    public static final class CentrifugeBlockEntity extends PoweredMachineBlockEntity {
        public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.CENTRIFUGE_BE.get(), pos, state, 2, 400, 20000);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity be) {
            FermenterBlockEntity adjFermenter = findAdjacentFermenter(level, pos);
            if (adjFermenter == null || adjFermenter.tankMb() < LifeScienceMath.CENTRIFUGE_MB_PER_CYCLE) {
                be.progress = 0;
                be.setFaultCode("no_broth");
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            if (!be.items.get(1).isEmpty() && be.items.get(1).getCount() >= be.items.get(1).getMaxStackSize() - 2) {
                be.setFaultCode("output_full");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (!be.consumeEnergy(level, pos, 10)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= 400) {
                be.progress = 0;
                String product = adjFermenter.brothProduct();
                double inocPurity = adjFermenter.inoculumPurity();
                adjFermenter.drainBroth(LifeScienceMath.CENTRIFUGE_MB_PER_CYCLE);
                ItemStack carbon = be.items.get(0);
                boolean hasCarbon = !carbon.isEmpty()
                        && "earth_on_minecraft:activated_carbon".equals(BuiltInRegistries.ITEM.getKey(carbon.getItem()).toString());
                if (hasCarbon) {
                    carbon.shrink(1);
                }
                ItemStack produced = be.buildCentrifugeOutput(product, inocPurity, hasCarbon);
                ItemStack outSlot = be.items.get(1);
                if (outSlot.isEmpty()) {
                    be.items.set(1, produced);
                } else if (ItemStack.isSameItemSameComponents(outSlot, produced)) {
                    outSlot.grow(produced.getCount());
                }
            }
            be.setChanged();
        }

        private ItemStack buildCentrifugeOutput(String product, double inocPurity, boolean hasCarbon) {
            if ("enzyme".equals(product)) {
                return new ItemStack(EarthLifeSciences.ENZYME_EXTRACT.get(), 4);
            }
            double purity = LifeScienceMath.computeCentrifugeDrugPurity(inocPurity, hasCarbon);
            Optional<UUID> owner = LoreMachines.owner(this);
            Identifier gateNode = "penicillin".equals(product)
                    ? LifeLoreIntegration.NODE_ANTIBIOTICS
                    : LifeLoreIntegration.NODE_BROAD_SPECTRUM;
            boolean knowsNode = owner.isPresent() && LoreApi.knows(owner.get(), gateNode);
            boolean knowsMastery = owner.isPresent() && LoreApi.knows(owner.get(), LifeLoreIntegration.NODE_MED_MASTERY);
            com.xxsx.earthlore.api.Grade grade = LifeScienceMath.resolveCentrifugeDrugGrade(purity, knowsNode, knowsMastery);
            Item drugItem = EarthLifeSciences.drugBySlug(product);
            return LifeLoreIntegration.stampPharmaceutical(drugItem, 2, product, grade, purity, 250, PharmaceuticalComponent.Form.TABLET);
        }

        private static FermenterBlockEntity findAdjacentFermenter(Level level, BlockPos pos) {
            for (Direction dir : Direction.values()) {
                if (level.getBlockEntity(pos.relative(dir)) instanceof FermenterBlockEntity fb) {
                    return fb;
                }
            }
            return null;
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.centrifuge");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.CENTRIFUGE_MENU.get(), id, inv, this, data, worldPosition, 2);
        }
    }

    /** B-013 gene_sequencer BlockEntity (1 slot). */
    public static final class GeneSequencerBlockEntity extends PoweredMachineBlockEntity {
        public GeneSequencerBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.GENE_SEQUENCER_BE.get(), pos, state, 1, 1200, 60000);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, GeneSequencerBlockEntity be) {
            ItemStack in = be.items.get(0);
            if (in.isEmpty()) {
                be.progress = 0;
                be.setFaultCode("");
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            boolean isCulture = in.is(EarthLifeSciences.PURE_CULTURE.get()) || in.is(EarthLifeSciences.MICROBE_SAMPLE.get());
            boolean isTissue = in.is(EarthLifeSciences.TISSUE_SAMPLE.get());
            boolean isSeed = !isCulture && !isTissue;
            if (!be.consumeEnergy(level, pos, 40)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= 1200) {
                be.progress = 0;
                if (isCulture) {
                    LifeComponents.StrainComponent s = in.getOrDefault(EarthLifeSciences.STRAIN.get(), LifeComponents.StrainComponent.DEFAULT);
                    String sub = "streptomyces".equals(s.species())
                            ? LifeScienceMath.rollStreptomycesSubtype(level.getRandom().nextDouble())
                            : s.subtype();
                    in.set(EarthLifeSciences.STRAIN.get(), s.withSubtype(sub));
                } else if (isTissue) {
                    LifeComponents.SpecimenData sp = in.getOrDefault(EarthLifeSciences.SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY);
                    in.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(sp.species(), "tissue", true));
                } else if (isSeed) {
                    LifeComponents.GenotypeComponent g = in.getOrDefault(EarthLifeSciences.GENOTYPE.get(), LifeComponents.GenotypeComponent.DEFAULT);
                    in.set(EarthLifeSciences.GENOTYPE.get(), g.withSequenced(true));
                }
            }
            be.setChanged();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.gene_sequencer");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.GENE_SEQUENCER_MENU.get(), id, inv, this, data, worldPosition, 1);
        }
    }

    /** B-014 breeding_station BlockEntity (slots: 0=parent_a, 1=parent_b, 2=bone_meal, 3=output). */
    public static final class BreedingStationBlockEntity extends PoweredMachineBlockEntity {
        public BreedingStationBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.BREEDING_STATION_BE.get(), pos, state, 4, 2400, 20000);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, BreedingStationBlockEntity be) {
            GateResult gate = LoreMachines.check(be, state.getBlock());
            if (gate.locked()) {
                be.setFaultCode("lore_locked");
                LoreMachines.setFault(be, "lore_locked", List.of(LifeLoreIntegration.NODE_PLANT_BREEDING));
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            ItemStack a = be.items.get(0);
            ItemStack b = be.items.get(1);
            ItemStack meal = be.items.get(2);
            ItemStack out = be.items.get(3);
            if (a.isEmpty() || b.isEmpty()) {
                be.progress = 0;
                be.setFaultCode("no_seed");
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            if (!a.is(b.getItem())) {
                be.progress = 0;
                be.setFaultCode("species_mismatch");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (meal.isEmpty() || !meal.is(Items.BONE_MEAL)) {
                be.progress = 0;
                be.setFaultCode("no_bone_meal");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (!out.isEmpty()) {
                return;
            }
            if (!be.consumeEnergy(level, pos, 4)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            LoreMachines.clearFault(be);
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= 2400) {
                be.progress = 0;
                LifeComponents.GenotypeComponent ga = a.getOrDefault(EarthLifeSciences.GENOTYPE.get(), LifeComponents.GenotypeComponent.DEFAULT);
                LifeComponents.GenotypeComponent gb = b.getOrDefault(EarthLifeSciences.GENOTYPE.get(), LifeComponents.GenotypeComponent.DEFAULT);
                int[] choices = new int[LifeScienceMath.GENOTYPE_TRAITS];
                for (int i = 0; i < choices.length; i++) {
                    choices[i] = level.getRandom().nextInt(3);
                }
                LifeComponents.GenotypeComponent childGeno = LifeScienceMath.crossGenotypes(
                        ga, gb, choices,
                        level.getRandom().nextDouble(),
                        level.getRandom().nextInt(LifeScienceMath.GENOTYPE_TRAITS),
                        level.getRandom().nextBoolean() ? 1 : -1
                );
                ItemStack child = new ItemStack(a.getItem(), 2);
                child.set(EarthLifeSciences.GENOTYPE.get(), childGeno);
                a.shrink(1);
                b.shrink(1);
                meal.shrink(1);
                be.items.set(3, child);
            }
            be.setChanged();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.breeding_station");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.BREEDING_STATION_MENU.get(), id, inv, this, data, worldPosition, 4);
        }
    }

    /** B-015 seed_bank BlockEntity (records up to 64 genotypes; never duplicates seeds). */
    public static final class SeedBankBlockEntity extends PoweredMachineBlockEntity {
        public record BankEntry(String itemId, LifeComponents.GenotypeComponent genotype) {
            public static final Codec<BankEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.fieldOf("item").forGetter(BankEntry::itemId),
                    LifeComponents.GenotypeComponent.CODEC.fieldOf("genotype").forGetter(BankEntry::genotype)
            ).apply(inst, BankEntry::new));
        }

        private final List<BankEntry> entries = new ArrayList<>();

        public SeedBankBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.SEED_BANK_BE.get(), pos, state, 2, 1, 1);
        }

        public List<BankEntry> entries() {
            return List.copyOf(entries);
        }

        /** Insert 1 seed with genotype: consumes the seed and records the entry. */
        public String recordSeed(ItemStack seedStack) {
            if (seedStack == null || seedStack.isEmpty()) {
                return "no_seed";
            }
            if (entries.size() >= 64) {
                return "bank_full";
            }
            String id = BuiltInRegistries.ITEM.getKey(seedStack.getItem()).toString();
            LifeComponents.GenotypeComponent geno = seedStack.getOrDefault(EarthLifeSciences.GENOTYPE.get(), LifeComponents.GenotypeComponent.DEFAULT);
            entries.add(new BankEntry(id, geno));
            seedStack.shrink(1);
            setChanged();
            return "";
        }

        /** Transcribe recorded genotype onto a provided plain seed of the same species; entry stays in bank. */
        public Optional<ItemStack> transcribeOntoSeed(int entryIndex, ItemStack plainSeed) {
            if (plainSeed == null || plainSeed.isEmpty() || entryIndex < 0 || entryIndex >= entries.size()) {
                return Optional.empty();
            }
            BankEntry entry = entries.get(entryIndex);
            String id = BuiltInRegistries.ITEM.getKey(plainSeed.getItem()).toString();
            if (!entry.itemId().equals(id)) {
                return Optional.empty();
            }
            plainSeed.shrink(1);
            ItemStack out = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.itemId())), 1);
            out.set(EarthLifeSciences.GENOTYPE.get(), entry.genotype());
            return Optional.of(out);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.seed_bank");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.SEED_BANK_MENU.get(), id, inv, this, data, worldPosition, 2);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.store("entries", BankEntry.CODEC.listOf(), entries);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            entries.clear();
            entries.addAll(input.read("entries", BankEntry.CODEC.listOf()).orElse(List.of()));
        }
    }

    /** B-023 pcr_cycler BlockEntity (slots: 0=sequenced source, 1=glucose, 2=dna_template output). */
    public static final class PcrCyclerBlockEntity extends PoweredMachineBlockEntity {
        private String selectedTrait = "yield";

        public PcrCyclerBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.PCR_CYCLER_BE.get(), pos, state, 3, 600, 20000);
        }

        public void setSelectedTrait(String trait) {
            this.selectedTrait = (trait == null || trait.isBlank()) ? "yield" : trait;
            setChanged();
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, PcrCyclerBlockEntity be) {
            ItemStack src = be.items.get(0);
            ItemStack glu = be.items.get(1);
            ItemStack out = be.items.get(2);
            if (src.isEmpty() || glu.isEmpty() || !out.isEmpty()) {
                be.progress = 0;
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            LifeComponents.GenotypeComponent geno = src.get(EarthLifeSciences.GENOTYPE.get());
            if (geno == null || !geno.sequenced()) {
                be.progress = 0;
                be.setFaultCode("not_sequenced");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (!be.consumeEnergy(level, pos, 20)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= 600) {
                be.progress = 0;
                glu.shrink(1);
                String speciesId = BuiltInRegistries.ITEM.getKey(src.getItem()).toString();
                int val = geno.traitValue(be.selectedTrait);
                ItemStack tmpl = new ItemStack(EarthLifeSciences.DNA_TEMPLATE.get(), 1);
                tmpl.set(EarthLifeSciences.DNA_TEMPLATE_DATA.get(), new LifeComponents.DnaTemplateData(speciesId, be.selectedTrait, val));
                be.items.set(2, tmpl);
            }
            be.setChanged();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.pcr_cycler");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.PCR_CYCLER_MENU.get(), id, inv, this, data, worldPosition, 3);
        }
    }

    /** B-024 gene_editor BlockEntity (hard gate K-BIO-15 via LoreMachines; slots: 0=target, 1=dna_template). */
    public static final class GeneEditorBlockEntity extends PoweredMachineBlockEntity {
        public GeneEditorBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.GENE_EDITOR_BE.get(), pos, state, 2, 2400, 200000);
        }

        public static boolean checkOwnerGate(GeneEditorBlockEntity be) {
            Optional<UUID> owner = LoreMachines.owner(be);
            if (owner.isEmpty() || !LoreApi.knows(owner.get(), LifeLoreIntegration.NODE_GENETIC_ENGINEERING)) {
                be.setFaultCode("lore_locked");
                LoreMachines.setFault(be, "lore_locked", List.of(LifeLoreIntegration.NODE_GENETIC_ENGINEERING));
                return false;
            }
            LoreMachines.clearFault(be);
            return true;
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, GeneEditorBlockEntity be) {
            if (!checkOwnerGate(be)) {
                be.progress = 0;
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            ItemStack target = be.items.get(0);
            ItemStack tmpl = be.items.get(1);
            if (target.isEmpty() || !tmpl.is(EarthLifeSciences.DNA_TEMPLATE.get())) {
                be.progress = 0;
                be.setFaultCode("");
                be.updateBlockFlags(level, pos, state, false, false);
                return;
            }
            LifeComponents.DnaTemplateData tmplData = tmpl.getOrDefault(EarthLifeSciences.DNA_TEMPLATE_DATA.get(), LifeComponents.DnaTemplateData.DEFAULT);
            String targetId = BuiltInRegistries.ITEM.getKey(target.getItem()).toString();
            if (!tmplData.species().equals(targetId)) {
                be.progress = 0;
                be.setFaultCode("species_mismatch");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            if (!be.consumeEnergy(level, pos, 80)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.progress++;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            if (be.progress >= 2400) {
                be.progress = 0;
                LifeComponents.GenotypeComponent base = target.getOrDefault(EarthLifeSciences.GENOTYPE.get(), LifeComponents.GenotypeComponent.DEFAULT);
                LifeScienceMath.GeneEditResult edited = LifeScienceMath.applyGeneEdit(
                        base, tmplData.trait(), tmplData.value(),
                        level.getRandom().nextDouble(),
                        level.getRandom().nextInt(LifeScienceMath.GENOTYPE_TRAITS)
                );
                target.set(EarthLifeSciences.GENOTYPE.get(), edited.genotype());
                tmpl.shrink(1);
                be.setFaultCode(edited.fault());
            }
            be.setChanged();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.gene_editor");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.GENE_EDITOR_MENU.get(), id, inv, this, data, worldPosition, 2);
        }
    }

    /** B-044 xray_machine BlockEntity (30 EOU/tick, 100 ticks). */
    public static final class XrayMachineBlockEntity extends PoweredMachineBlockEntity {
        public XrayMachineBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.XRAY_MACHINE_BE.get(), pos, state, 1, 100, 10000);
        }

        public InteractionResult scanPatient(ServerLevel level, BlockPos pos, ServerPlayer operator) {
            AABB frontBox = new AABB(pos).inflate(2.0D);
            List<Player> patients = level.getEntitiesOfClass(Player.class, frontBox);
            if (patients.isEmpty()) {
                operator.sendOverlayMessage(LifeItems.failMsg("no_patient"));
                return InteractionResult.FAIL;
            }
            if (!consumeEnergy(level, pos, 3000)) {
                operator.sendOverlayMessage(LifeItems.failMsg("no_power"));
                return InteractionResult.FAIL;
            }
            Player patient = patients.get(0);
            boolean knowsRad = LifeLoreIntegration.knows(operator, LifeLoreIntegration.NODE_RADIOLOGY);
            if (patient instanceof ServerPlayer sp && EarthHumanCompat.isLoaded()) {
                List<EarthHumanCompat.PartIntegrity> parts = EarthHumanCompat.detailedParts(sp);
                if (!knowsRad) {
                    boolean frac = parts.stream().anyMatch(p -> p.integrity() < 0.60D);
                    operator.sendOverlayMessage(LifeItems.infoMsg("xray_coarse", frac ? "fracture_detected" : "no_fracture"));
                } else {
                    operator.sendOverlayMessage(LifeItems.infoMsg("xray_detailed", parts.toString()));
                }
            } else {
                operator.sendOverlayMessage(LifeItems.infoMsg("xray_fallback", "MaxHP=" + patient.getMaxHealth() + " Armor=" + patient.getArmorValue()));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.xray_machine");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return null;
        }
    }

    /** B-046 iv_stand BlockEntity (1 slot for iv_bag; infuses 50 mL every 100 ticks to patient on adjacent sickbed). */
    public static final class IvStandBlockEntity extends BlockEntity {
        private ItemStack bag = ItemStack.EMPTY;
        private int infusionTicks;

        public IvStandBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.IV_STAND_BE.get(), pos, state);
        }

        public ItemStack bag() {
            return bag;
        }

        public void setBag(ItemStack stack) {
            this.bag = stack == null ? ItemStack.EMPTY : stack;
            this.infusionTicks = 0;
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                BlockState st = getBlockState();
                boolean has = !this.bag.isEmpty();
                if (st.hasProperty(LifeBlocks.IvStandBlock.HAS_BAG) && st.getValue(LifeBlocks.IvStandBlock.HAS_BAG) != has) {
                    serverLevel.setBlock(worldPosition, st.setValue(LifeBlocks.IvStandBlock.HAS_BAG, has), Block.UPDATE_ALL);
                }
            }
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, IvStandBlockEntity be) {
            if (!(level instanceof ServerLevel serverLevel) || be.bag.isEmpty()) {
                be.infusionTicks = 0;
                return;
            }
            LifeComponents.IvBagData bagData = be.bag.getOrDefault(EarthLifeSciences.IV_BAG_DATA.get(), LifeComponents.IvBagData.EMPTY);
            if (bagData.volumeMl() <= 0 || "empty".equals(bagData.content())) {
                be.infusionTicks = 0;
                return;
            }
            LivingEntity patient = findPatientNearSickbed(serverLevel, pos);
            if (patient == null) {
                be.infusionTicks = 0;
                return;
            }
            be.infusionTicks++;
            if (be.infusionTicks >= 100) {
                be.infusionTicks = 0;
                be.applyInfusionStep(serverLevel, pos, patient, bagData);
            }
            be.setChanged();
        }

        public void applyInfusionStep(ServerLevel level, BlockPos pos, LivingEntity patient, LifeComponents.IvBagData bagData) {
            int nextVol = Math.max(0, bagData.volumeMl() - 50);
            bag.set(EarthLifeSciences.IV_BAG_DATA.get(), new LifeComponents.IvBagData(bagData.content(), bagData.drug(), nextVol));
            level.playSound(null, pos, EarthLifeSciences.SOUND_IV_DRIP.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            switch (bagData.content()) {
                case "saline" -> {
                    if (patient instanceof ServerPlayer sp && EarthHumanCompat.isLoaded()) {
                        EarthHumanCompat.recoverAllParts(sp, 2.0D, 0.0D);
                    } else {
                        patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
                    }
                }
                case "glucose" -> {
                    if (patient instanceof ServerPlayer sp && EarthHumanCompat.isLoaded()) {
                        EarthHumanCompat.recover(sp, 4.0D, 0.5D, "torso");
                    } else {
                        patient.addEffect(new MobEffectInstance(MobEffects.SATURATION, 60, 0));
                    }
                }
                case "antibiotic" -> EarthHumanCompat.treatConditionOrFallback(patient, "wound_infection");
                default -> {
                }
            }
            setChanged();
        }

        private static LivingEntity findPatientNearSickbed(ServerLevel level, BlockPos pos) {
            boolean hasBed = false;
            for (Direction dir : Direction.values()) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.relative(dir)).getBlock());
                if ("earth_on_minecraft:sickbed".equals(id.toString())) {
                    hasBed = true;
                    break;
                }
            }
            if (!hasBed) {
                return null;
            }
            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2.5D),
                    e -> e instanceof Player || e instanceof Villager);
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.storeNullable("bag", ItemStack.CODEC, bag.isEmpty() ? null : bag);
            output.putInt("infusion_ticks", infusionTicks);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            bag = input.read("bag", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            infusionTicks = Math.max(0, Math.min(99, input.getIntOr("infusion_ticks", 0)));
        }
    }

    /** B-049 anesthesia_machine BlockEntity (hard gate K-MED-24; 4 EOU/tick). */
    public static final class AnesthesiaMachineBlockEntity extends PoweredMachineBlockEntity {
        private String agent = "none";
        private int agentTicks;

        public AnesthesiaMachineBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.ANESTHESIA_MACHINE_BE.get(), pos, state, 1, 2400, 20000);
        }

        public boolean isRunning() {
            return agentTicks > 0 && !"none".equals(agent);
        }

        public void loadAgent(String agentName, int ticks) {
            this.agent = agentName;
            this.agentTicks = Math.max(0, Math.min(2400, ticks));
            setChanged();
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, AnesthesiaMachineBlockEntity be) {
            Optional<UUID> owner = LoreMachines.owner(be);
            if (owner.isEmpty() || !LoreApi.knows(owner.get(), LifeLoreIntegration.NODE_ANESTHESIOLOGY)) {
                be.setFaultCode("lore_locked");
                LoreMachines.setFault(be, "lore_locked", List.of(LifeLoreIntegration.NODE_ANESTHESIOLOGY));
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            LoreMachines.clearFault(be);
            if (be.agentTicks <= 0) {
                ItemStack in = be.items.get(0);
                String inId = BuiltInRegistries.ITEM.getKey(in.getItem()).toString();
                if ("earth_on_minecraft:nitrous_oxide".equals(inId)) {
                    in.shrink(1);
                    be.agent = "nitrous_oxide";
                    be.agentTicks = 1200;
                } else if ("earth_on_minecraft:diethyl_ether".equals(inId)) {
                    in.shrink(1);
                    be.agent = "diethyl_ether";
                    be.agentTicks = 2400;
                } else {
                    be.agent = "none";
                    be.setFaultCode("no_agent");
                    be.updateBlockFlags(level, pos, state, false, false);
                    return;
                }
            }
            if (!be.consumeEnergy(level, pos, 4)) {
                be.setFaultCode("no_power");
                be.updateBlockFlags(level, pos, state, false, true);
                return;
            }
            be.agentTicks--;
            be.setFaultCode("");
            be.updateBlockFlags(level, pos, state, true, false);
            be.setChanged();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.anesthesia_machine");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return null;
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putString("agent", agent == null ? "none" : agent);
            output.putInt("agent_ticks", agentTicks);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            agent = input.getStringOr("agent", "none");
            agentTicks = Math.max(0, Math.min(2400, input.getIntOr("agent_ticks", 0)));
        }
    }

    /** B-052 clinic_desk BlockEntity (6 patient_chart slots; job site for physician). */
    public static final class ClinicDeskBlockEntity extends PoweredMachineBlockEntity {
        public ClinicDeskBlockEntity(BlockPos pos, BlockState state) {
            super(EarthLifeSciences.CLINIC_DESK_BE.get(), pos, state, 6, 1, 1);
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return stack.is(EarthLifeSciences.PATIENT_CHART.get());
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.earth_life_sciences.clinic_desk");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new LifeMenus(EarthLifeSciences.CLINIC_DESK_MENU.get(), id, inv, this, data, worldPosition, 6);
        }
    }
}
