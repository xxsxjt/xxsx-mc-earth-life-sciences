package com.xxsx.earthlifesciences;

import com.xxsx.earthonminecraft.component.EarthDataComponents;
import com.xxsx.earthonminecraft.component.Grade;
import com.xxsx.earthonminecraft.component.PharmaceuticalComponent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * All 23 biology/medical equipment items and 26 pharmaceutical drug items for Earth: Life Sciences (16 册 §3–§4, catalog/medicine.md).
 */
public final class LifeItems {
    private static final Map<UUID, Long> ENTITY_SAMPLE_COOLDOWN = new HashMap<>();

    private LifeItems() {
    }

    public static Component failMsg(String code) {
        return Component.translatable("life.earth_life_sciences.fail." + code).withStyle(ChatFormatting.RED);
    }

    public static Component infoMsg(String key, Object... args) {
        return Component.translatable("life.earth_life_sciences.info." + key, args).withStyle(ChatFormatting.AQUA);
    }

    public static String resolveMicrobeSourceBlock(BlockState state) {
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
            return "dirt";
        }
        if (state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT)) {
            return "podzol";
        }
        if (state.is(Blocks.MOSS_BLOCK)) {
            return "moss_block";
        }
        if (state.is(Blocks.MYCELIUM)) {
            return "mycelium";
        }
        if (state.is(Blocks.WATER) || !state.getFluidState().isEmpty()) {
            return "water";
        }
        return "";
    }

    /** B-001 microscope. */
    public static final class MicroscopeItem extends Item {
        public MicroscopeItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = context.getItemInHand();
            if (stack.getDamageValue() >= stack.getMaxDamage() - 1 && stack.getMaxDamage() > 0) {
                serverPlayer.sendOverlayMessage(failMsg("broken"));
                return InteractionResult.FAIL;
            }
            String sourceKey = resolveMicrobeSourceBlock(level.getBlockState(context.getClickedPos()));
            if (sourceKey.isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("nothing_to_observe"));
                return InteractionResult.FAIL;
            }
            int slideSlot = findItemInInventory(serverPlayer, EarthLifeSciences.SLIDE_KIT.get());
            if (slideSlot < 0) {
                serverPlayer.sendOverlayMessage(failMsg("no_slide"));
                return InteractionResult.FAIL;
            }
            serverPlayer.getInventory().getItem(slideSlot).shrink(1);
            stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
            boolean knowsCell = LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_CELL_BIOLOGY);
            Optional<String> rolled = LifeScienceMath.sampleMicrobeFromSource(sourceKey, level.getRandom().nextDouble());
            if (rolled.isPresent()) {
                String species = rolled.get();
                ItemStack sample = new ItemStack(EarthLifeSciences.MICROBE_SAMPLE.get(), 1);
                sample.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent(species, "none", true, 0.55D, 0, false));
                if (!serverPlayer.getInventory().add(sample)) {
                    serverPlayer.drop(sample, false);
                }
                String readout = LifeScienceMath.formatMicroscopeReadout(species, 0.55D, knowsCell);
                serverPlayer.sendOverlayMessage(infoMsg("microscope_readout", readout));
            } else {
                serverPlayer.sendOverlayMessage(failMsg("no_microbe_found"));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack off = serverPlayer.getOffhandItem();
            boolean knowsCell = LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_CELL_BIOLOGY);
            if (off.is(EarthLifeSciences.MICROBE_SAMPLE.get()) || off.is(EarthLifeSciences.PURE_CULTURE.get())) {
                LifeComponents.StrainComponent strain = off.getOrDefault(EarthLifeSciences.STRAIN.get(), LifeComponents.StrainComponent.DEFAULT);
                off.set(EarthLifeSciences.STRAIN.get(), strain.withIdentified(true));
                String readout = LifeScienceMath.formatMicroscopeReadout(strain.species(), strain.purity(), knowsCell);
                serverPlayer.sendOverlayMessage(infoMsg("microscope_readout", readout));
                return InteractionResult.SUCCESS;
            }
            if (off.is(EarthLifeSciences.TISSUE_SAMPLE.get())) {
                LifeComponents.SpecimenData spec = off.getOrDefault(EarthLifeSciences.SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY);
                off.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(spec.species(), "tissue", true));
                String readout = knowsCell ? ("cell:" + (spec.species().isEmpty() ? "animal" : spec.species())) : "has_cells";
                serverPlayer.sendOverlayMessage(infoMsg("microscope_readout", readout));
                return InteractionResult.SUCCESS;
            }
            serverPlayer.sendOverlayMessage(failMsg("nothing_to_observe"));
            return InteractionResult.FAIL;
        }
    }

    /** B-003 specimen_jar. */
    public static final class SpecimenJarItem extends Item {
        public SpecimenJarItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            LifeComponents.SpecimenData data = stack.getOrDefault(EarthLifeSciences.SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY);
            if (!"empty".equals(data.kind()) && !data.species().isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("jar_full"));
                return InteractionResult.FAIL;
            }
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
            String path = typeId.getPath();
            String kind = switch (path) {
                case "bee", "silverfish", "endermite" -> "insect";
                case "tadpole", "cod", "salmon", "tropical_fish", "axolotl" -> "small_animal";
                default -> "";
            };
            if (kind.isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("not_collectable"));
                return InteractionResult.FAIL;
            }
            ItemStack filled = new ItemStack(EarthLifeSciences.SPECIMEN_JAR.get(), 1);
            filled.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(typeId.toString(), kind, true));
            stack.shrink(1);
            if (stack.isEmpty()) {
                serverPlayer.setItemInHand(hand, filled);
            } else if (!serverPlayer.getInventory().add(filled)) {
                serverPlayer.drop(filled, false);
            }
            target.discard();
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = serverPlayer.getItemInHand(hand);
            LifeComponents.SpecimenData data = stack.getOrDefault(EarthLifeSciences.SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY);
            ItemStack off = serverPlayer.getOffhandItem();
            if ("empty".equals(data.kind()) && off.is(EarthLifeSciences.TISSUE_SAMPLE.get())) {
                LifeComponents.SpecimenData tissue = off.getOrDefault(EarthLifeSciences.SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY);
                off.shrink(1);
                ItemStack filled = new ItemStack(EarthLifeSciences.SPECIMEN_JAR.get(), 1);
                filled.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(tissue.species(), "tissue", true));
                stack.shrink(1);
                if (stack.isEmpty()) {
                    serverPlayer.setItemInHand(hand, filled);
                } else if (!serverPlayer.getInventory().add(filled)) {
                    serverPlayer.drop(filled, false);
                }
                return InteractionResult.SUCCESS;
            }
            boolean knowsZoo = LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_ZOOLOGY);
            String msg = knowsZoo ? (data.species() + " [" + data.kind() + "]") : (data.species().isEmpty() ? "empty" : data.species());
            serverPlayer.sendOverlayMessage(infoMsg("specimen_readout", msg));
            return InteractionResult.SUCCESS;
        }
    }

    /** B-004 field_guide. */
    public static final class FieldGuideItem extends Item {
        public FieldGuideItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = serverPlayer.getItemInHand(hand);
            LifeComponents.FieldGuideData guide = stack.getOrDefault(EarthLifeSciences.FIELD_GUIDE_DATA.get(), LifeComponents.FieldGuideData.EMPTY);
            boolean knowsNat = LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_NATURAL_HISTORY);
            String summary = knowsNat
                    ? ("seen=" + guide.seen().size() + ", pressed=" + guide.pressed().size() + " (habitat/diet unlocked)")
                    : ("seen=" + guide.seen().size() + ", pressed=" + guide.pressed().size());
            serverPlayer.sendOverlayMessage(infoMsg("field_guide_summary", summary));
            return InteractionResult.SUCCESS;
        }
    }

    /** B-005 sampling_kit. */
    public static final class SamplingKitItem extends Item {
        public SamplingKitItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = context.getItemInHand();
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                serverPlayer.sendOverlayMessage(failMsg("kit_empty"));
                return InteractionResult.FAIL;
            }
            String sourceKey = resolveMicrobeSourceBlock(level.getBlockState(context.getClickedPos()));
            if (sourceKey.isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("not_sampleable"));
                return InteractionResult.FAIL;
            }
            stack.setDamageValue(stack.getDamageValue() + 1);
            Optional<String> rolled = LifeScienceMath.sampleMicrobeFromSource(sourceKey, level.getRandom().nextDouble());
            if (rolled.isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("no_microbe_found"));
                return InteractionResult.FAIL;
            }
            ItemStack sample = new ItemStack(EarthLifeSciences.MICROBE_SAMPLE.get(), 1);
            sample.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent(rolled.get(), "none", false, 0.50D, 0, false));
            if (!serverPlayer.getInventory().add(sample)) {
                serverPlayer.drop(sample, false);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                serverPlayer.sendOverlayMessage(failMsg("kit_empty"));
                return InteractionResult.FAIL;
            }
            boolean validEntity = target instanceof Animal || target.getType().getCategory().isFriendly() || target instanceof Villager
                    || (target instanceof TamableAnimal ta && ta.isTame()) || target instanceof Player;
            if (!validEntity) {
                serverPlayer.sendOverlayMessage(failMsg("not_sampleable"));
                return InteractionResult.FAIL;
            }
            long now = serverPlayer.level().getGameTime();
            long until = ENTITY_SAMPLE_COOLDOWN.getOrDefault(target.getUUID(), 0L);
            if (now < until) {
                serverPlayer.sendOverlayMessage(failMsg("sample_cooldown"));
                return InteractionResult.FAIL;
            }
            ENTITY_SAMPLE_COOLDOWN.put(target.getUUID(), now + LifeScienceMath.SAMPLE_COOLDOWN_TICKS);
            stack.setDamageValue(stack.getDamageValue() + 1);
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
            ItemStack tissue = new ItemStack(EarthLifeSciences.TISSUE_SAMPLE.get(), 1);
            tissue.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(typeId.toString(), "tissue", false));
            if (!serverPlayer.getInventory().add(tissue)) {
                serverPlayer.drop(tissue, false);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                serverPlayer.sendOverlayMessage(failMsg("kit_empty"));
                return InteractionResult.FAIL;
            }
            ItemStack off = serverPlayer.getOffhandItem();
            Identifier offId = BuiltInRegistries.ITEM.getKey(off.getItem());
            String sourceKey = "";
            if ("earth_on_minecraft:milk".equals(offId.toString())) {
                sourceKey = "milk";
            } else if ("earth_on_minecraft:yeast".equals(offId.toString())) {
                sourceKey = "yeast";
            }
            if (sourceKey.isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("not_sampleable"));
                return InteractionResult.FAIL;
            }
            stack.setDamageValue(stack.getDamageValue() + 1);
            Optional<String> rolled = LifeScienceMath.sampleMicrobeFromSource(sourceKey, level.getRandom().nextDouble());
            if (rolled.isEmpty()) {
                serverPlayer.sendOverlayMessage(failMsg("no_microbe_found"));
                return InteractionResult.FAIL;
            }
            ItemStack sample = new ItemStack(EarthLifeSciences.MICROBE_SAMPLE.get(), 1);
            sample.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent(rolled.get(), "none", false, 0.50D, 0, false));
            if (!serverPlayer.getInventory().add(sample)) {
                serverPlayer.drop(sample, false);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** B-019 herbarium_press. */
    public static final class HerbariumPressItem extends Item {
        public HerbariumPressItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack press = serverPlayer.getItemInHand(hand);
            if (press.getDamageValue() >= press.getMaxDamage()) {
                serverPlayer.sendOverlayMessage(failMsg("broken"));
                return InteractionResult.FAIL;
            }
            ItemStack off = serverPlayer.getOffhandItem();
            boolean isPlant = off.is(ItemTags.FLOWERS) || off.is(ItemTags.SAPLINGS) || off.is(ItemTags.LEAVES)
                    || off.has(EarthDataComponents.HERB.get());
            if (off.isEmpty() || !isPlant) {
                serverPlayer.sendOverlayMessage(failMsg("not_a_plant"));
                return InteractionResult.FAIL;
            }
            Identifier plantId = BuiltInRegistries.ITEM.getKey(off.getItem());
            off.shrink(1);
            press.setDamageValue(press.getDamageValue() + 1);
            ItemStack specimen = new ItemStack(EarthLifeSciences.DRIED_SPECIMEN.get(), 1);
            specimen.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(plantId.toString(), "plant", true));
            if (!serverPlayer.getInventory().add(specimen)) {
                serverPlayer.drop(specimen, false);
            }
            recordPressedInGuide(serverPlayer, plantId.toString());
            return InteractionResult.SUCCESS;
        }
    }

    /** B-022 ecology_survey_kit. */
    public static final class EcologySurveyKitItem extends Item {
        public EcologySurveyKitItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            if (!LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_ECOLOGY)) {
                serverPlayer.sendOverlayMessage(failMsg("lore_locked"));
                return InteractionResult.FAIL;
            }
            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
                serverPlayer.sendOverlayMessage(failMsg("cooldown"));
                return InteractionResult.FAIL;
            }
            serverPlayer.getCooldowns().addCooldown(stack, 600);
            AABB box = new AABB(serverPlayer.blockPosition()).inflate(32.0D);
            long entityKinds = level.getEntitiesOfClass(LivingEntity.class, box).stream()
                    .map(Entity::getType).distinct().count();
            int plantBlocks = 0;
            BlockPos origin = serverPlayer.blockPosition();
            for (int dx = -8; dx <= 8; dx += 2) {
                for (int dy = -4; dy <= 4; dy += 2) {
                    for (int dz = -8; dz <= 8; dz += 2) {
                        BlockState st = level.getBlockState(origin.offset(dx, dy, dz));
                        if (st.is(BlockTags.FLOWERS) || st.is(BlockTags.LEAVES) || st.is(BlockTags.CROPS)) {
                            plantBlocks++;
                        }
                    }
                }
            }
            int index = (int) Math.min(100L, entityKinds * 8L + plantBlocks * 2L);
            serverPlayer.sendOverlayMessage(infoMsg("ecology_index", index, entityKinds));
            return InteractionResult.SUCCESS;
        }
    }

    /** B-040 stethoscope. */
    public static final class StethoscopeItem extends Item {
        public StethoscopeItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return inspectTarget(serverPlayer, target);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return inspectTarget(serverPlayer, serverPlayer);
        }

        public static InteractionResult inspectTarget(ServerPlayer doctor, LivingEntity target) {
            if (!(target instanceof Player || target instanceof Villager)) {
                doctor.sendOverlayMessage(failMsg("no_target"));
                return InteractionResult.FAIL;
            }
            doctor.level().playSound(null, doctor.blockPosition(), EarthLifeSciences.SOUND_STETHOSCOPE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            boolean knowsDiag = LifeLoreIntegration.knows(doctor, LifeLoreIntegration.NODE_DIAGNOSTICS);
            if (target instanceof Player patient) {
                EarthHumanCompat.VitalsSnapshot snap = EarthHumanCompat.snapshot(patient);
                if (!knowsDiag) {
                    doctor.sendOverlayMessage(infoMsg("stethoscope_coarse", snap.abnormal() ? "abnormal" : "normal"));
                } else if (snap.linked()) {
                    doctor.sendOverlayMessage(infoMsg("stethoscope_detailed",
                            String.format(Locale.ROOT, "Fatigue=%.1f Load=%.1f Head=%.0f%% Body=%.0f%%",
                                    snap.fatigue(), snap.exertionLoad(), snap.headIntegrity() * 100.0D, snap.bodyIntegrity() * 100.0D)));
                } else {
                    doctor.sendOverlayMessage(infoMsg("stethoscope_fallback",
                            String.format(Locale.ROOT, "HP=%.1f/%.1f Effects=%d",
                                    patient.getHealth(), patient.getMaxHealth(), patient.getActiveEffects().size())));
                }
            } else {
                doctor.sendOverlayMessage(infoMsg("stethoscope_fallback",
                        String.format(Locale.ROOT, "HP=%.1f/%.1f", target.getHealth(), target.getMaxHealth())));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** B-041 clinical_thermometer. */
    public static final class ClinicalThermometerItem extends Item {
        public ClinicalThermometerItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return measure(serverPlayer, target);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return measure(serverPlayer, serverPlayer);
        }

        private static InteractionResult measure(ServerPlayer doctor, LivingEntity target) {
            if (!(target instanceof Player || target instanceof Villager)) {
                doctor.sendOverlayMessage(failMsg("no_target"));
                return InteractionResult.FAIL;
            }
            boolean knowsDiag = LifeLoreIntegration.knows(doctor, LifeLoreIntegration.NODE_DIAGNOSTICS);
            if (target instanceof Player patient && EarthHumanCompat.isLoaded()) {
                EarthHumanCompat.VitalsSnapshot snap = EarthHumanCompat.snapshot(patient);
                if (knowsDiag) {
                    doctor.sendOverlayMessage(infoMsg("thermometer_precise", String.format(Locale.ROOT, "%.1f K", snap.bodyTemperature())));
                } else {
                    String band = snap.bodyTemperature() > 311.0D ? "fever" : (snap.bodyTemperature() < 309.0D ? "hypothermia" : "normal");
                    doctor.sendOverlayMessage(infoMsg("thermometer_band", band));
                }
            } else {
                float biomeTemp = doctor.level().getBiome(target.blockPosition()).value().getBaseTemperature();
                double envK = 273.15D + biomeTemp * 20.0D;
                doctor.sendOverlayMessage(infoMsg("thermometer_env", String.format(Locale.ROOT, "%.1f K (Earth Human not installed)", envK)));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** B-042 diagnostic_kit (hard gate K-MED-18). */
    public static final class DiagnosticKitItem extends Item {
        public DiagnosticKitItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return diagnose(serverPlayer, stack, target);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return diagnose(serverPlayer, serverPlayer.getItemInHand(hand), serverPlayer);
        }

        private static InteractionResult diagnose(ServerPlayer doctor, ItemStack kit, LivingEntity patient) {
            if (!LifeLoreIntegration.knows(doctor, LifeLoreIntegration.NODE_LABORATORY_MEDICINE)) {
                doctor.sendOverlayMessage(failMsg("lore_locked"));
                return InteractionResult.FAIL;
            }
            if (kit.getDamageValue() >= kit.getMaxDamage()) {
                doctor.sendOverlayMessage(failMsg("kit_empty"));
                return InteractionResult.FAIL;
            }
            ItemStack off = doctor.getOffhandItem();
            if (!off.is(EarthLifeSciences.PATIENT_CHART.get())) {
                doctor.sendOverlayMessage(failMsg("no_chart"));
                return InteractionResult.FAIL;
            }
            kit.setDamageValue(kit.getDamageValue() + 1);
            String cond = detectPrimaryCondition(patient);
            LifeComponents.PatientChartData chart = off.getOrDefault(EarthLifeSciences.CHART_DATA.get(), LifeComponents.PatientChartData.EMPTY);
            off.set(EarthLifeSciences.CHART_DATA.get(), chart.append(
                    patient.getUUID().toString(),
                    patient.getName().getString(),
                    new LifeComponents.ChartEntry(doctor.level().getGameTime(), cond, "normal".equals(cond) ? 0.0D : 0.4D, "rest_or_medication")
            ));
            LifeComponents.MedicalAttachment att = patient.getData(EarthLifeSciences.MEDICAL_ATTACHMENT.get());
            att.markDiagnosed(doctor.level().getGameTime() + 24000L);
            patient.setData(EarthLifeSciences.MEDICAL_ATTACHMENT.get(), att);
            doctor.sendOverlayMessage(infoMsg("diagnosed", cond));
            return InteractionResult.SUCCESS;
        }
    }

    /** B-043 blood_test_kit (hard gate K-MED-18, requires microscope in inventory). */
    public static final class BloodTestKitItem extends Item {
        public BloodTestKitItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return runBloodTest(serverPlayer, stack, target);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return runBloodTest(serverPlayer, serverPlayer.getItemInHand(hand), serverPlayer);
        }

        private static InteractionResult runBloodTest(ServerPlayer doctor, ItemStack kit, LivingEntity patient) {
            if (!LifeLoreIntegration.knows(doctor, LifeLoreIntegration.NODE_LABORATORY_MEDICINE)) {
                doctor.sendOverlayMessage(failMsg("lore_locked"));
                return InteractionResult.FAIL;
            }
            if (findItemInInventory(doctor, EarthLifeSciences.MICROSCOPE.get()) < 0) {
                doctor.sendOverlayMessage(failMsg("no_microscope"));
                return InteractionResult.FAIL;
            }
            ItemStack off = doctor.getOffhandItem();
            if (!off.is(EarthLifeSciences.PATIENT_CHART.get())) {
                doctor.sendOverlayMessage(failMsg("no_chart"));
                return InteractionResult.FAIL;
            }
            kit.shrink(1);
            String cond = detectPrimaryCondition(patient);
            LifeComponents.PatientChartData chart = off.getOrDefault(EarthLifeSciences.CHART_DATA.get(), LifeComponents.PatientChartData.EMPTY);
            off.set(EarthLifeSciences.CHART_DATA.get(), chart.append(
                    patient.getUUID().toString(),
                    patient.getName().getString(),
                    new LifeComponents.ChartEntry(doctor.level().getGameTime(), cond, "normal".equals(cond) ? 0.0D : 0.5D, "targeted_therapy")
            ));
            LifeComponents.MedicalAttachment att = patient.getData(EarthLifeSciences.MEDICAL_ATTACHMENT.get());
            att.markDiagnosed(doctor.level().getGameTime() + 24000L);
            patient.setData(EarthLifeSciences.MEDICAL_ATTACHMENT.get(), att);
            doctor.sendOverlayMessage(infoMsg("blood_test_result", cond));
            return InteractionResult.SUCCESS;
        }
    }

    /** B-045 patient_chart. */
    public static final class PatientChartItem extends Item {
        public PatientChartItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = serverPlayer.getItemInHand(hand);
            LifeComponents.PatientChartData data = stack.getOrDefault(EarthLifeSciences.CHART_DATA.get(), LifeComponents.PatientChartData.EMPTY);
            boolean knowsInf = LifeLoreIntegration.knows(serverPlayer, LifeLoreIntegration.NODE_INFECTIOUS_DISEASE);
            if (data.entries().isEmpty()) {
                serverPlayer.sendOverlayMessage(infoMsg("chart_empty"));
            } else {
                LifeComponents.ChartEntry last = data.entries().get(data.entries().size() - 1);
                String text = knowsInf
                        ? (last.condition() + " (sev=" + String.format(Locale.ROOT, "%.2f", last.severity()) + ", rx=" + last.treatment() + ")")
                        : last.condition();
                serverPlayer.sendOverlayMessage(infoMsg("chart_readout", text));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** B-048 surgical_kit & B-050 surgery workflow. */
    public static final class SurgicalKitItem extends Item {
        public SurgicalKitItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer surgeon)) {
                return InteractionResult.SUCCESS;
            }
            return performSurgery(surgeon, stack, target, "torso");
        }

        public static InteractionResult performSurgery(ServerPlayer surgeon, ItemStack kit, LivingEntity patient, String part) {
            boolean knowsSurgery = LifeLoreIntegration.knows(surgeon, LifeLoreIntegration.NODE_SURGERY);
            boolean selfSurgery = surgeon == patient;
            boolean hasKit = kit != null && !kit.isEmpty() && kit.getDamageValue() < kit.getMaxDamage();
            boolean onTable = isNearOperatingTable(surgeon.level(), patient.blockPosition());
            boolean hasAnesthesia = isNearActiveAnesthesia(surgeon.level(), patient.blockPosition());
            boolean sterile = LifeLoreIntegration.isSterile(kit);

            LifeScienceMath.SurgeryOutcome outcome = LifeScienceMath.evaluateSurgery(
                    knowsSurgery, selfSurgery, onTable, hasAnesthesia, hasKit, sterile, part);
            if (!outcome.success()) {
                surgeon.sendOverlayMessage(failMsg(outcome.failCode()));
                return InteractionResult.FAIL;
            }
            kit.setDamageValue(kit.getDamageValue() + 1);
            LifeLoreIntegration.setSterile(kit, false);
            if (patient instanceof ServerPlayer spPatient && EarthHumanCompat.isLoaded()) {
                EarthHumanCompat.recover(spPatient, 0.0D, outcome.healAmount(), outcome.part());
            } else {
                patient.heal(10.0F);
                patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            if (outcome.infected()) {
                EarthHumanCompat.addConditionOrFallback(patient, "wound_infection", 0.3D, 100);
            }
            surgeon.sendOverlayMessage(infoMsg("surgery_complete", outcome.part()));
            return InteractionResult.SUCCESS;
        }
    }

    /** B-051 attenuated_vaccine. */
    public static final class AttenuatedVaccineItem extends Item {
        public AttenuatedVaccineItem(Properties properties) {
            super(properties);
        }

        public static ItemStack create(String targetCondition, int count) {
            ItemStack stack = new ItemStack(EarthLifeSciences.ATTENUATED_VACCINE.get(), count);
            stack.set(EarthLifeSciences.SPECIMEN.get(), new LifeComponents.SpecimenData(targetCondition, "vaccine", true));
            return stack;
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return vaccinate(serverPlayer, stack, target);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            return vaccinate(serverPlayer, serverPlayer.getItemInHand(hand), serverPlayer);
        }

        public static InteractionResult vaccinate(ServerPlayer doctor, ItemStack vaccine, LivingEntity target) {
            ItemStack off = doctor.getOffhandItem();
            Identifier offId = BuiltInRegistries.ITEM.getKey(off.getItem());
            if (!"earth_on_minecraft:syringe".equals(offId.toString())) {
                doctor.sendOverlayMessage(failMsg("no_syringe"));
                return InteractionResult.FAIL;
            }
            if (!(target instanceof Player || target instanceof Villager)) {
                doctor.sendOverlayMessage(failMsg("no_target"));
                return InteractionResult.FAIL;
            }
            LifeComponents.SpecimenData spec = vaccine.getOrDefault(EarthLifeSciences.SPECIMEN.get(),
                    new LifeComponents.SpecimenData("tetanus", "vaccine", true));
            String conditionTarget = spec.species().isEmpty() ? "tetanus" : spec.species();
            long now = doctor.level().getGameTime();
            LifeComponents.MedicalAttachment att = target.getData(EarthLifeSciences.MEDICAL_ATTACHMENT.get());
            if (att.isImmune(conditionTarget, now)) {
                doctor.sendOverlayMessage(failMsg("already_immune"));
                return InteractionResult.FAIL;
            }
            att.immunize(conditionTarget, now + LifeScienceMath.VACCINE_IMMUNITY_TICKS);
            target.setData(EarthLifeSciences.MEDICAL_ATTACHMENT.get(), att);
            vaccine.shrink(1);
            doctor.sendOverlayMessage(infoMsg("vaccinated", conditionTarget));
            return InteractionResult.SUCCESS;
        }
    }

    /** Pharmaceutical drug item (catalog/medicine.md §三–§五, owner: life). */
    public static final class LifeDrugItem extends Item {
        private final String drugSlug;
        private final String category;
        private final boolean chronic;
        private final boolean controlled;

        public LifeDrugItem(Properties properties, String drugSlug, String category, boolean chronic, boolean controlled) {
            super(properties);
            this.drugSlug = drugSlug;
            this.category = category;
            this.chronic = chronic;
            this.controlled = controlled;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
                return InteractionResult.FAIL;
            }
            PharmaceuticalComponent pharma = stack.getOrDefault(EarthDataComponents.PHARMACEUTICAL.get(),
                    new PharmaceuticalComponent(1, drugSlug, Grade.COMMON, 0.90D, 100, PharmaceuticalComponent.Form.TABLET, "", ""));
            if (pharma.form() == PharmaceuticalComponent.Form.INJECTION && pharma.purity() < 0.95D) {
                serverPlayer.sendOverlayMessage(failMsg("contaminated"));
                return InteractionResult.FAIL;
            }
            LifeComponents.MedicalAttachment att = serverPlayer.getData(EarthLifeSciences.MEDICAL_ATTACHMENT.get());
            if (chronic && att.diagnosedUntil() < level.getGameTime()) {
                serverPlayer.sendOverlayMessage(failMsg("ineffective"));
                return InteractionResult.FAIL;
            }
            if (!"naloxone".equals(category) && !EarthHumanCompat.needsTreatment(serverPlayer)) {
                serverPlayer.sendOverlayMessage(failMsg("ineffective"));
                return InteractionResult.FAIL;
            }
            serverPlayer.getCooldowns().addCooldown(stack, 1200);
            applyDrugEffect(serverPlayer, pharma, att);
            stack.shrink(1);
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.7F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        private void applyDrugEffect(ServerPlayer player, PharmaceuticalComponent pharma, LifeComponents.MedicalAttachment att) {
            double mult = switch (pharma.grade()) {
                case CRUDE -> 0.60D;
                case COMMON -> 0.85D;
                case FINE -> 1.00D;
                case MASTERWORK -> 1.15D;
                case HEIRLOOM -> 1.30D;
            };
            int duration = (int) Math.round(600 * mult);
            switch (category) {
                case "antibiotic" -> {
                    EarthHumanCompat.treatConditionOrFallback(player, "wound_infection");
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 3, 0));
                    EarthHumanCompat.treatFractureAndCleanseAir(player, 0.45D * mult, 3.5D * mult, 25.0D * mult, 0.0D, 12.0D * mult);
                }
                case "antifungal", "antiviral" -> {
                    EarthHumanCompat.treatConditionOrFallback(player, "influenza");
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 4, 0));
                    EarthHumanCompat.treatFractureAndCleanseAir(player, 0.30D * mult, 2.5D * mult, 35.0D * mult, 0.0D, 10.0D * mult);
                }
                case "analgesic", "anesthetic" -> {
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0));
                    EarthHumanCompat.treatFractureAndCleanseAir(player, 0.65D * mult, 3.0D * mult, 15.0D * mult, 0.0D, 20.0D * mult);
                    if (controlled) {
                        att.addDependence(10);
                        player.setData(EarthLifeSciences.MEDICAL_ATTACHMENT.get(), att);
                    }
                }
                case "naloxone" -> {
                    att.addDependence(-50);
                    player.setData(EarthLifeSciences.MEDICAL_ATTACHMENT.get(), att);
                    player.removeEffect(MobEffects.NAUSEA);
                }
                default -> {
                    if (EarthHumanCompat.isLoaded()) {
                        EarthHumanCompat.treatFractureAndCleanseAir(player, 0.50D * mult, 3.0D * mult, 30.0D * mult, 15.0D * mult, 15.0D * mult);
                    } else {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 3, 0));
                    }
                }
            }
        }
    }

    public static int findItemInInventory(Player player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    public static void recordPressedInGuide(Player player, String itemId) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(EarthLifeSciences.FIELD_GUIDE.get())) {
                LifeComponents.FieldGuideData g = s.getOrDefault(EarthLifeSciences.FIELD_GUIDE_DATA.get(), LifeComponents.FieldGuideData.EMPTY);
                s.set(EarthLifeSciences.FIELD_GUIDE_DATA.get(), g.withPressed(itemId));
            }
        }
    }

    public static void recordSeenInGuide(Player player, String entityId) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(EarthLifeSciences.FIELD_GUIDE.get())) {
                LifeComponents.FieldGuideData g = s.getOrDefault(EarthLifeSciences.FIELD_GUIDE_DATA.get(), LifeComponents.FieldGuideData.EMPTY);
                s.set(EarthLifeSciences.FIELD_GUIDE_DATA.get(), g.withSeen(entityId));
            }
        }
    }

    public static String detectPrimaryCondition(LivingEntity patient) {
        if (patient.hasEffect(MobEffects.POISON)) {
            return "wound_infection";
        }
        if (patient.hasEffect(MobEffects.NAUSEA)) {
            return "food_poisoning";
        }
        if (patient.hasEffect(MobEffects.HUNGER)) {
            return "dysentery";
        }
        if (patient.hasEffect(MobEffects.SLOWNESS)) {
            return "tetanus";
        }
        if (patient.hasEffect(MobEffects.WEAKNESS)) {
            return "influenza";
        }
        if (patient.hasEffect(MobEffects.WITHER)) {
            return "radiation_sickness";
        }
        return "normal";
    }

    public static boolean isNearOperatingTable(Level level, BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(center.offset(dx, dy, dz)).getBlock());
                    if ("earth_on_minecraft:operating_table".equals(id.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isNearActiveAnesthesia(Level level, BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (level.getBlockEntity(center.offset(dx, dy, dz)) instanceof LifeBlockEntities.AnesthesiaMachineBlockEntity amBe
                            && amBe.isRunning()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
