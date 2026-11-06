package com.xxsx.earthlifesciences;

import com.xxsx.earthlore.api.LoreMachines;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * All 12 blocks for Earth: Life Sciences (16 册 §3–§4).
 */
public final class LifeBlocks {
    private LifeBlocks() {
    }

    public abstract static class MachineBlock extends Block implements EntityBlock {
        public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
        public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
        public static final BooleanProperty FAULT = BooleanProperty.create("fault");

        protected MachineBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(ACTIVE, false)
                    .setValue(FAULT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE, FAULT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof ServerPlayer sp && level.getBlockEntity(pos) != null) {
                LoreMachines.setOwner(level.getBlockEntity(pos), sp);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                return InteractionResult.PASS;
            }
            if (serverPlayer.isShiftKeyDown() && serverPlayer.getMainHandItem().isEmpty()) {
                LoreMachines.claim(be, serverPlayer);
                return InteractionResult.SUCCESS;
            }
            if (be instanceof MenuProvider provider && provider.createMenu(0, serverPlayer.getInventory(), serverPlayer) != null) {
                serverPlayer.openMenu(provider, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class IncubatorBlock extends MachineBlock {
        public IncubatorBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.IncubatorBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.INCUBATOR_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.IncubatorBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.IncubatorBlockEntity) be);
        }
    }

    public static final class FermenterBlock extends MachineBlock {
        public FermenterBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.FermenterBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.FERMENTER_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.FermenterBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.FermenterBlockEntity) be);
        }
    }

    public static final class CentrifugeBlock extends MachineBlock {
        public CentrifugeBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.CentrifugeBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.CENTRIFUGE_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.CentrifugeBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.CentrifugeBlockEntity) be);
        }
    }

    public static final class GeneSequencerBlock extends MachineBlock {
        public GeneSequencerBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.GeneSequencerBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.GENE_SEQUENCER_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.GeneSequencerBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.GeneSequencerBlockEntity) be);
        }
    }

    public static final class BreedingStationBlock extends MachineBlock {
        public BreedingStationBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.BreedingStationBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.BREEDING_STATION_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.BreedingStationBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.BreedingStationBlockEntity) be);
        }
    }

    public static final class SeedBankBlock extends MachineBlock {
        public SeedBankBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.SeedBankBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof LifeBlockEntities.SeedBankBlockEntity bankBe)) {
                return InteractionResult.PASS;
            }
            if (!LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_PLANT_BREEDING)) {
                serverPlayer.sendOverlayMessage(LifeItems.failMsg("lore_locked"));
                return InteractionResult.FAIL;
            }
            if (!stack.isEmpty()) {
                if (stack.has(EarthLifeSciences.GENOTYPE.get())) {
                    String err = bankBe.recordSeed(stack);
                    if (!err.isEmpty()) {
                        serverPlayer.sendOverlayMessage(LifeItems.failMsg(err));
                        return InteractionResult.FAIL;
                    }
                    return InteractionResult.SUCCESS;
                } else if (!bankBe.entries().isEmpty()) {
                    Optional<ItemStack> out = bankBe.transcribeOntoSeed(bankBe.entries().size() - 1, stack);
                    if (out.isPresent()) {
                        if (!serverPlayer.getInventory().add(out.get())) {
                            serverPlayer.drop(out.get(), false);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    serverPlayer.sendOverlayMessage(LifeItems.failMsg("species_mismatch"));
                    return InteractionResult.FAIL;
                }
            }
            boolean knowsGenetics = LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_GENETICS);
            String msg = knowsGenetics ? bankBe.entries().toString() : ("entries=" + bankBe.entries().size());
            serverPlayer.sendOverlayMessage(LifeItems.infoMsg("seed_bank_readout", msg));
            return InteractionResult.SUCCESS;
        }
    }

    public static final class PcrCyclerBlock extends MachineBlock {
        public PcrCyclerBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.PcrCyclerBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.PCR_CYCLER_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.PcrCyclerBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.PcrCyclerBlockEntity) be);
        }
    }

    public static final class GeneEditorBlock extends MachineBlock {
        public GeneEditorBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.GeneEditorBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.GENE_EDITOR_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.GeneEditorBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.GeneEditorBlockEntity) be);
        }
    }

    public static final class XrayMachineBlock extends MachineBlock {
        public XrayMachineBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.XrayMachineBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof LifeBlockEntities.XrayMachineBlockEntity xrayBe) {
                return xrayBe.scanPatient(serverLevel, pos, serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** B-046 iv_stand block with has_bag property. */
    public static final class IvStandBlock extends Block implements EntityBlock {
        public static final BooleanProperty HAS_BAG = BooleanProperty.create("has_bag");
        private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);

        public IvStandBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HAS_BAG, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HAS_BAG);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.IvStandBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.IV_STAND_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.IvStandBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.IvStandBlockEntity) be);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof LifeBlockEntities.IvStandBlockEntity ivBe)) {
                return InteractionResult.SUCCESS;
            }
            if (stack.is(EarthLifeSciences.IV_BAG.get()) && ivBe.bag().isEmpty()) {
                ivBe.setBag(stack.split(1));
                return InteractionResult.SUCCESS;
            }
            if (stack.isEmpty() && !ivBe.bag().isEmpty()) {
                ItemStack removed = ivBe.bag();
                ivBe.setBag(ItemStack.EMPTY);
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    }

    public static final class AnesthesiaMachineBlock extends MachineBlock {
        public AnesthesiaMachineBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.AnesthesiaMachineBlockEntity(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide() || type != EarthLifeSciences.ANESTHESIA_MACHINE_BE.get()) {
                return null;
            }
            return (lvl, pos, st, be) -> LifeBlockEntities.AnesthesiaMachineBlockEntity.serverTick(lvl, pos, st, (LifeBlockEntities.AnesthesiaMachineBlockEntity) be);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide() || !(player instanceof ServerPlayer sp)
                    || !(level.getBlockEntity(pos) instanceof LifeBlockEntities.AnesthesiaMachineBlockEntity amBe)) {
                return InteractionResult.SUCCESS;
            }
            if (!LifeLoreIntegration.knows(sp, LifeLoreIntegration.NODE_ANESTHESIOLOGY)) {
                sp.sendOverlayMessage(LifeItems.failMsg("lore_locked"));
                return InteractionResult.FAIL;
            }
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if ("earth_on_minecraft:nitrous_oxide".equals(id)) {
                stack.shrink(1);
                amBe.loadAgent("nitrous_oxide", 1200);
                return InteractionResult.SUCCESS;
            }
            if ("earth_on_minecraft:diethyl_ether".equals(id)) {
                stack.shrink(1);
                amBe.loadAgent("diethyl_ether", 2400);
                return InteractionResult.SUCCESS;
            }
            return super.useWithoutItem(state, level, pos, player, hit);
        }
    }

    public static final class ClinicDeskBlock extends MachineBlock {
        public ClinicDeskBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LifeBlockEntities.ClinicDeskBlockEntity(pos, state);
        }
    }
}
