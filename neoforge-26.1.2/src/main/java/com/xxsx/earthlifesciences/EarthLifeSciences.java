package com.xxsx.earthlifesciences;

import com.google.common.collect.ImmutableSet;
import com.xxsx.earthlifesciences.client.EarthLifeSciencesClient;
import com.xxsx.earthonminecraft.component.EarthDataComponents;
import com.xxsx.earthonminecraft.component.MaterialBatch;
import com.xxsx.earthonminecraft.component.PharmaceuticalComponent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EarthLifeSciences.MODID)
public final class EarthLifeSciences {
    public static final String MODID = "earth_life_sciences";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final List<ItemLike> TAB_ITEMS = new ArrayList<>();
    private static final Map<String, DeferredItem<LifeItems.LifeDrugItem>> DRUG_ITEMS = new LinkedHashMap<>();

    // ---- Data Components & Attachments ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.GenotypeComponent>> GENOTYPE =
            COMPONENTS.registerComponentType("genotype", b -> b.persistent(LifeComponents.GenotypeComponent.CODEC)
                    .networkSynchronized(LifeComponents.GenotypeComponent.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.StrainComponent>> STRAIN =
            COMPONENTS.registerComponentType("strain", b -> b.persistent(LifeComponents.StrainComponent.CODEC)
                    .networkSynchronized(LifeComponents.StrainComponent.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.DnaTemplateData>> DNA_TEMPLATE_DATA =
            COMPONENTS.registerComponentType("dna_template_data", b -> b.persistent(LifeComponents.DnaTemplateData.CODEC)
                    .networkSynchronized(LifeComponents.DnaTemplateData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.IvBagData>> IV_BAG_DATA =
            COMPONENTS.registerComponentType("iv_bag_data", b -> b.persistent(LifeComponents.IvBagData.CODEC)
                    .networkSynchronized(LifeComponents.IvBagData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.SpecimenData>> SPECIMEN =
            COMPONENTS.registerComponentType("specimen", b -> b.persistent(LifeComponents.SpecimenData.CODEC)
                    .networkSynchronized(LifeComponents.SpecimenData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.FieldGuideData>> FIELD_GUIDE_DATA =
            COMPONENTS.registerComponentType("field_guide_data", b -> b.persistent(LifeComponents.FieldGuideData.CODEC)
                    .networkSynchronized(LifeComponents.FieldGuideData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LifeComponents.PatientChartData>> CHART_DATA =
            COMPONENTS.registerComponentType("chart_data", b -> b.persistent(LifeComponents.PatientChartData.CODEC)
                    .networkSynchronized(LifeComponents.PatientChartData.STREAM_CODEC));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LifeComponents.MedicalAttachment>> MEDICAL_ATTACHMENT =
            ATTACHMENTS.register("medical", () -> AttachmentType.builder(LifeComponents.MedicalAttachment::new)
                    .serialize(LifeComponents.MedicalAttachment.CODEC).copyOnDeath().build());

    // ---- Fluid (B-011 fermentation_broth) ----
    public static final DeferredHolder<FluidType, FluidType> FERMENTATION_BROTH_TYPE =
            FLUID_TYPES.register("fermentation_broth", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.earth_life_sciences.fermentation_broth")
                    .density(1020).viscosity(1200).temperature(310).canDrown(false).canSwim(false).supportsBoating(false)));
    public static final DeferredHolder<Fluid, Fluid> FERMENTATION_BROTH_SOURCE =
            FLUIDS.register("fermentation_broth", () -> new BaseFlowingFluid.Source(brothProps()));
    public static final DeferredHolder<Fluid, Fluid> FERMENTATION_BROTH_FLOWING =
            FLUIDS.register("flowing_fermentation_broth", () -> new BaseFlowingFluid.Flowing(brothProps()));

    private static BaseFlowingFluid.Properties brothProps() {
        return new BaseFlowingFluid.Properties(FERMENTATION_BROTH_TYPE, FERMENTATION_BROTH_SOURCE, FERMENTATION_BROTH_FLOWING)
                .slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100.0F);
    }

    // ---- Sounds (06 册 §5.1) ----
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_STETHOSCOPE =
            SOUNDS.register("item.stethoscope.listen", () -> SoundEvent.createVariableRangeEvent(id("item.stethoscope.listen")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_IV_DRIP =
            SOUNDS.register("block.iv_stand.drip", () -> SoundEvent.createVariableRangeEvent(id("block.iv_stand.drip")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_WORK_PHYSICIAN =
            SOUNDS.register("entity.villager.work_physician", () -> SoundEvent.createVariableRangeEvent(id("entity.villager.work_physician")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_WORK_BIOLOGIST =
            SOUNDS.register("entity.villager.work_biologist", () -> SoundEvent.createVariableRangeEvent(id("entity.villager.work_biologist")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_WORK_NURSE =
            SOUNDS.register("entity.villager.work_nurse", () -> SoundEvent.createVariableRangeEvent(id("entity.villager.work_nurse")));

    static {
        for (String m : List.of("incubator", "fermenter", "centrifuge", "gene_sequencer", "breeding_station",
                "pcr_cycler", "gene_editor", "xray_machine", "anesthesia_machine")) {
            for (String s : List.of("idle", "running", "fault")) {
                String key = "block." + m + "." + s;
                SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(id(key)));
            }
        }
    }

    // ---- Blocks (12) ----
    public static final DeferredBlock<LifeBlocks.IncubatorBlock> INCUBATOR =
            regBlock("incubator", LifeBlocks.IncubatorBlock::new);
    public static final DeferredBlock<LifeBlocks.FermenterBlock> FERMENTER =
            regBlock("fermenter", LifeBlocks.FermenterBlock::new);
    public static final DeferredBlock<LifeBlocks.CentrifugeBlock> CENTRIFUGE =
            regBlock("centrifuge", LifeBlocks.CentrifugeBlock::new);
    public static final DeferredBlock<LifeBlocks.GeneSequencerBlock> GENE_SEQUENCER =
            regBlock("gene_sequencer", LifeBlocks.GeneSequencerBlock::new);
    public static final DeferredBlock<LifeBlocks.BreedingStationBlock> BREEDING_STATION =
            regBlock("breeding_station", LifeBlocks.BreedingStationBlock::new);
    public static final DeferredBlock<LifeBlocks.SeedBankBlock> SEED_BANK =
            regBlock("seed_bank", LifeBlocks.SeedBankBlock::new);
    public static final DeferredBlock<LifeBlocks.PcrCyclerBlock> PCR_CYCLER =
            regBlock("pcr_cycler", LifeBlocks.PcrCyclerBlock::new);
    public static final DeferredBlock<LifeBlocks.GeneEditorBlock> GENE_EDITOR =
            regBlock("gene_editor", LifeBlocks.GeneEditorBlock::new);
    public static final DeferredBlock<LifeBlocks.XrayMachineBlock> XRAY_MACHINE =
            regBlock("xray_machine", LifeBlocks.XrayMachineBlock::new);
    public static final DeferredBlock<LifeBlocks.IvStandBlock> IV_STAND =
            BLOCKS.registerBlock("iv_stand", LifeBlocks.IvStandBlock::new,
                    p -> p.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> IV_STAND_ITEM = regBlockItem(IV_STAND);
    public static final DeferredBlock<LifeBlocks.AnesthesiaMachineBlock> ANESTHESIA_MACHINE =
            regBlock("anesthesia_machine", LifeBlocks.AnesthesiaMachineBlock::new);
    public static final DeferredBlock<LifeBlocks.ClinicDeskBlock> CLINIC_DESK =
            regBlock("clinic_desk", LifeBlocks.ClinicDeskBlock::new);

    // ---- BlockEntities (12) ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.IncubatorBlockEntity>> INCUBATOR_BE =
            BLOCK_ENTITY_TYPES.register("incubator", () -> new BlockEntityType<>(LifeBlockEntities.IncubatorBlockEntity::new, INCUBATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.FermenterBlockEntity>> FERMENTER_BE =
            BLOCK_ENTITY_TYPES.register("fermenter", () -> new BlockEntityType<>(LifeBlockEntities.FermenterBlockEntity::new, FERMENTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.CentrifugeBlockEntity>> CENTRIFUGE_BE =
            BLOCK_ENTITY_TYPES.register("centrifuge", () -> new BlockEntityType<>(LifeBlockEntities.CentrifugeBlockEntity::new, CENTRIFUGE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.GeneSequencerBlockEntity>> GENE_SEQUENCER_BE =
            BLOCK_ENTITY_TYPES.register("gene_sequencer", () -> new BlockEntityType<>(LifeBlockEntities.GeneSequencerBlockEntity::new, GENE_SEQUENCER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.BreedingStationBlockEntity>> BREEDING_STATION_BE =
            BLOCK_ENTITY_TYPES.register("breeding_station", () -> new BlockEntityType<>(LifeBlockEntities.BreedingStationBlockEntity::new, BREEDING_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.SeedBankBlockEntity>> SEED_BANK_BE =
            BLOCK_ENTITY_TYPES.register("seed_bank", () -> new BlockEntityType<>(LifeBlockEntities.SeedBankBlockEntity::new, SEED_BANK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.PcrCyclerBlockEntity>> PCR_CYCLER_BE =
            BLOCK_ENTITY_TYPES.register("pcr_cycler", () -> new BlockEntityType<>(LifeBlockEntities.PcrCyclerBlockEntity::new, PCR_CYCLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.GeneEditorBlockEntity>> GENE_EDITOR_BE =
            BLOCK_ENTITY_TYPES.register("gene_editor", () -> new BlockEntityType<>(LifeBlockEntities.GeneEditorBlockEntity::new, GENE_EDITOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.XrayMachineBlockEntity>> XRAY_MACHINE_BE =
            BLOCK_ENTITY_TYPES.register("xray_machine", () -> new BlockEntityType<>(LifeBlockEntities.XrayMachineBlockEntity::new, XRAY_MACHINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.IvStandBlockEntity>> IV_STAND_BE =
            BLOCK_ENTITY_TYPES.register("iv_stand", () -> new BlockEntityType<>(LifeBlockEntities.IvStandBlockEntity::new, IV_STAND.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.AnesthesiaMachineBlockEntity>> ANESTHESIA_MACHINE_BE =
            BLOCK_ENTITY_TYPES.register("anesthesia_machine", () -> new BlockEntityType<>(LifeBlockEntities.AnesthesiaMachineBlockEntity::new, ANESTHESIA_MACHINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeBlockEntities.ClinicDeskBlockEntity>> CLINIC_DESK_BE =
            BLOCK_ENTITY_TYPES.register("clinic_desk", () -> new BlockEntityType<>(LifeBlockEntities.ClinicDeskBlockEntity::new, CLINIC_DESK.get()));

    // ---- Menus (9) ----
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> INCUBATOR_MENU =
            MENUS.register("incubator", () -> IMenuTypeExtension.create(LifeMenus::createIncubator));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> FERMENTER_MENU =
            MENUS.register("fermenter", () -> IMenuTypeExtension.create(LifeMenus::createFermenter));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> CENTRIFUGE_MENU =
            MENUS.register("centrifuge", () -> IMenuTypeExtension.create(LifeMenus::createCentrifuge));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> GENE_SEQUENCER_MENU =
            MENUS.register("gene_sequencer", () -> IMenuTypeExtension.create(LifeMenus::createGeneSequencer));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> BREEDING_STATION_MENU =
            MENUS.register("breeding_station", () -> IMenuTypeExtension.create(LifeMenus::createBreedingStation));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> SEED_BANK_MENU =
            MENUS.register("seed_bank", () -> IMenuTypeExtension.create(LifeMenus::createSeedBank));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> PCR_CYCLER_MENU =
            MENUS.register("pcr_cycler", () -> IMenuTypeExtension.create(LifeMenus::createPcrCycler));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> GENE_EDITOR_MENU =
            MENUS.register("gene_editor", () -> IMenuTypeExtension.create(LifeMenus::createGeneEditor));
    public static final DeferredHolder<MenuType<?>, MenuType<LifeMenus>> CLINIC_DESK_MENU =
            MENUS.register("clinic_desk", () -> IMenuTypeExtension.create(LifeMenus::createClinicDesk));

    // ---- Equipment & Sample Items (23) ----
    public static final DeferredItem<LifeItems.MicroscopeItem> MICROSCOPE =
            regItem("microscope", LifeItems.MicroscopeItem::new, p -> p.stacksTo(1).durability(256));
    public static final DeferredItem<Item> SLIDE_KIT =
            regSimpleItem("slide_kit", 16);
    public static final DeferredItem<LifeItems.SpecimenJarItem> SPECIMEN_JAR =
            regItem("specimen_jar", LifeItems.SpecimenJarItem::new, p -> p.stacksTo(16).component(SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY));
    public static final DeferredItem<LifeItems.FieldGuideItem> FIELD_GUIDE =
            regItem("field_guide", LifeItems.FieldGuideItem::new, p -> p.stacksTo(1).component(FIELD_GUIDE_DATA.get(), LifeComponents.FieldGuideData.EMPTY));
    public static final DeferredItem<LifeItems.SamplingKitItem> SAMPLING_KIT =
            regItem("sampling_kit", LifeItems.SamplingKitItem::new, p -> p.stacksTo(1).durability(16));
    public static final DeferredItem<Item> MICROBE_SAMPLE =
            regItem("microbe_sample", Item::new, p -> p.stacksTo(16).component(STRAIN.get(), LifeComponents.StrainComponent.DEFAULT));
    public static final DeferredItem<Item> PETRI_DISH =
            regItem("petri_dish", Item::new, p -> p.stacksTo(16).component(EarthDataComponents.STERILE.get(), false));
    public static final DeferredItem<Item> CULTURE_MEDIUM =
            regItem("culture_medium", Item::new, p -> p.stacksTo(16).component(EarthDataComponents.MATERIAL_BATCH.get(), MaterialBatch.defaultFor("culture_medium")));
    public static final DeferredItem<Item> PURE_CULTURE =
            regItem("pure_culture", Item::new, p -> p.stacksTo(16).component(STRAIN.get(),
                    new LifeComponents.StrainComponent("penicillium", "none", true, 0.85D, 0, false)));
    public static final DeferredItem<Item> ENZYME_EXTRACT =
            regSimpleItem("enzyme_extract", 64);
    public static final DeferredItem<LifeItems.HerbariumPressItem> HERBARIUM_PRESS =
            regItem("herbarium_press", LifeItems.HerbariumPressItem::new, p -> p.stacksTo(1).durability(64));
    public static final DeferredItem<Item> DRIED_SPECIMEN =
            regItem("dried_specimen", Item::new, p -> p.stacksTo(16).component(SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY));
    public static final DeferredItem<Item> TISSUE_SAMPLE =
            regItem("tissue_sample", Item::new, p -> p.stacksTo(16).component(SPECIMEN.get(), LifeComponents.SpecimenData.EMPTY));
    public static final DeferredItem<LifeItems.EcologySurveyKitItem> ECOLOGY_SURVEY_KIT =
            regItem("ecology_survey_kit", LifeItems.EcologySurveyKitItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<Item> DNA_TEMPLATE =
            regItem("dna_template", Item::new, p -> p.stacksTo(1).component(DNA_TEMPLATE_DATA.get(), LifeComponents.DnaTemplateData.DEFAULT));
    public static final DeferredItem<LifeItems.StethoscopeItem> STETHOSCOPE =
            regItem("stethoscope", LifeItems.StethoscopeItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<LifeItems.ClinicalThermometerItem> CLINICAL_THERMOMETER =
            regItem("clinical_thermometer", LifeItems.ClinicalThermometerItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<LifeItems.DiagnosticKitItem> DIAGNOSTIC_KIT =
            regItem("diagnostic_kit", LifeItems.DiagnosticKitItem::new, p -> p.stacksTo(1).durability(8));
    public static final DeferredItem<LifeItems.BloodTestKitItem> BLOOD_TEST_KIT =
            regItem("blood_test_kit", LifeItems.BloodTestKitItem::new, p -> p.stacksTo(8));
    public static final DeferredItem<LifeItems.PatientChartItem> PATIENT_CHART =
            regItem("patient_chart", LifeItems.PatientChartItem::new, p -> p.stacksTo(1).component(CHART_DATA.get(), LifeComponents.PatientChartData.EMPTY));
    public static final DeferredItem<Item> IV_BAG =
            regItem("iv_bag", Item::new, p -> p.stacksTo(16).component(IV_BAG_DATA.get(), LifeComponents.IvBagData.EMPTY));
    public static final DeferredItem<LifeItems.SurgicalKitItem> SURGICAL_KIT =
            regItem("surgical_kit", LifeItems.SurgicalKitItem::new, p -> p.stacksTo(1).durability(8).component(EarthDataComponents.STERILE.get(), false));
    public static final DeferredItem<LifeItems.AttenuatedVaccineItem> ATTENUATED_VACCINE =
            regItem("attenuated_vaccine", LifeItems.AttenuatedVaccineItem::new, p -> p.stacksTo(16).component(SPECIMEN.get(),
                    new LifeComponents.SpecimenData("tetanus", "vaccine", true)));

    // ---- Advanced Pharmaceutical Drugs (26 items from catalog/medicine.md owner: life) ----
    public static final DeferredItem<LifeItems.LifeDrugItem> PENICILLIN = regDrug("penicillin", "antibiotic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> STREPTOMYCIN = regDrug("streptomycin", "antibiotic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> TETRACYCLINE = regDrug("tetracycline", "antibiotic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> CHLORAMPHENICOL = regDrug("chloramphenicol", "antibiotic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> VANCOMYCIN = regDrug("vancomycin", "antibiotic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTIFUNGAL_AGENT = regDrug("antifungal_agent", "antifungal", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTIVIRAL_AGENT = regDrug("antiviral_agent", "antiviral", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> LIDOCAINE = regDrug("lidocaine", "anesthetic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> MORPHINE = regDrug("morphine", "analgesic", false, true);
    public static final DeferredItem<LifeItems.LifeDrugItem> CODEINE = regDrug("codeine", "analgesic", false, true);
    public static final DeferredItem<LifeItems.LifeDrugItem> FENTANYL = regDrug("fentanyl", "analgesic", false, true);
    public static final DeferredItem<LifeItems.LifeDrugItem> NALOXONE = regDrug("naloxone", "naloxone", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> MUSCLE_RELAXANT = regDrug("muscle_relaxant", "anesthetic", false, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTIHYPERTENSIVE = regDrug("antihypertensive", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTIDIABETIC = regDrug("antidiabetic", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> INSULIN = regDrug("insulin", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> THYROXINE = regDrug("thyroxine", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> CORTICOSTEROID = regDrug("corticosteroid", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTICOAGULANT = regDrug("anticoagulant", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> STATIN = regDrug("statin", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> BRONCHODILATOR = regDrug("bronchodilator", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTICONVULSANT = regDrug("anticonvulsant", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTIDEPRESSANT = regDrug("antidepressant", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> ANTIPSYCHOTIC = regDrug("antipsychotic", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> CHEMOTHERAPY_AGENT = regDrug("chemotherapy_agent", "chronic", true, false);
    public static final DeferredItem<LifeItems.LifeDrugItem> IMMUNOSUPPRESSANT = regDrug("immunosuppressant", "chronic", true, false);

    // ---- Villager POI & Professions (BB-01 physician, BB-02 biologist, BB-03 nurse) ----
    public static final DeferredHolder<PoiType, PoiType> PHYSICIAN_POI =
            POI_TYPES.register("physician", () -> new PoiType(
                    Set.copyOf(CLINIC_DESK.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> BIOLOGIST_POI =
            POI_TYPES.register("biologist", () -> new PoiType(
                    Set.copyOf(INCUBATOR.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> NURSE_POI =
            POI_TYPES.register("nurse", () -> new PoiType(
                    Set.copyOf(IV_STAND.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> PHYSICIAN =
            PROFESSIONS.register("physician", () -> new VillagerProfession(
                    Component.translatable("entity.minecraft.villager.earth_life_sciences.physician"),
                    h -> h.is(PHYSICIAN_POI.getKey()), h -> h.is(PHYSICIAN_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SOUND_WORK_PHYSICIAN.get(), Int2ObjectMap.ofEntries()));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> BIOLOGIST =
            PROFESSIONS.register("biologist", () -> new VillagerProfession(
                    Component.translatable("entity.minecraft.villager.earth_life_sciences.biologist"),
                    h -> h.is(BIOLOGIST_POI.getKey()), h -> h.is(BIOLOGIST_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SOUND_WORK_BIOLOGIST.get(), Int2ObjectMap.ofEntries()));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> NURSE =
            PROFESSIONS.register("nurse", () -> new VillagerProfession(
                    Component.translatable("entity.minecraft.villager.earth_life_sciences.nurse"),
                    h -> h.is(NURSE_POI.getKey()), h -> h.is(NURSE_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SOUND_WORK_NURSE.get(), Int2ObjectMap.ofEntries()));

    // ---- Creative Tab ----
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("earth_life_sciences",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.earth_life_sciences"))
                    .icon(() -> new ItemStack(MICROSCOPE.get()))
                    .displayItems((params, output) -> {
                        for (ItemLike item : TAB_ITEMS) {
                            output.accept(item);
                        }
                    })
                    .build());

    public EarthLifeSciences(IEventBus modBus) {
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENUS.register(modBus);
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
        TABS.register(modBus);
        LifeEvents.register();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            EarthLifeSciencesClient.register(modBus);
        }
        LOGGER.info("[earth_life_sciences] loaded");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static Item drugBySlug(String slug) {
        DeferredItem<LifeItems.LifeDrugItem> holder = DRUG_ITEMS.get(slug);
        return holder != null ? holder.get() : PENICILLIN.get();
    }

    private static <B extends net.minecraft.world.level.block.Block> DeferredBlock<B> regBlock(
            String name, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, B> factory) {
        DeferredBlock<B> b = BLOCKS.registerBlock(name, factory,
                p -> p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
        regBlockItem(b);
        return b;
    }

    private static DeferredItem<BlockItem> regBlockItem(DeferredBlock<?> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(block);
        TAB_ITEMS.add(item);
        return item;
    }

    private static <I extends Item> DeferredItem<I> regItem(
            String name, java.util.function.Function<Item.Properties, I> factory,
            java.util.function.UnaryOperator<Item.Properties> props) {
        DeferredItem<I> item = ITEMS.registerItem(name, factory, props);
        TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<Item> regSimpleItem(String name, int stackSize) {
        DeferredItem<Item> item = ITEMS.registerSimpleItem(name, p -> p.stacksTo(stackSize));
        TAB_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<LifeItems.LifeDrugItem> regDrug(String slug, String category, boolean chronic, boolean controlled) {
        DeferredItem<LifeItems.LifeDrugItem> item = ITEMS.registerItem(slug,
                p -> new LifeItems.LifeDrugItem(p, slug, category, chronic, controlled),
                p -> p.stacksTo(16).component(EarthDataComponents.PHARMACEUTICAL.get(),
                        new PharmaceuticalComponent(1, slug, com.xxsx.earthonminecraft.component.Grade.COMMON, 0.90D, 100,
                                PharmaceuticalComponent.Form.TABLET, "ELS-01", "cool_dry")));
        DRUG_ITEMS.put(slug, item);
        TAB_ITEMS.add(item);
        return item;
    }
}
