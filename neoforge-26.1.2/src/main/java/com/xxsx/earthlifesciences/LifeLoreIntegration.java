package com.xxsx.earthlifesciences;

import com.xxsx.earthlore.api.LoreApi;
import com.xxsx.earthonminecraft.component.EarthDataComponents;
import com.xxsx.earthonminecraft.component.Grade;
import com.xxsx.earthonminecraft.component.PharmaceuticalComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Knowledge node identifiers and pharmaceutical component builders for Earth: Life Sciences.
 */
public final class LifeLoreIntegration {
    public static final Identifier DISCIPLINE_BIO = Identifier.fromNamespaceAndPath("earth_lore", "bio");
    public static final Identifier DISCIPLINE_MED = Identifier.fromNamespaceAndPath("earth_lore", "med");

    // BIO nodes (17)
    public static final Identifier NODE_BIO_BASICS = EarthLifeSciences.id("bio/biology_basics");
    public static final Identifier NODE_NATURAL_HISTORY = EarthLifeSciences.id("bio/natural_history");
    public static final Identifier NODE_MICROSCOPY = EarthLifeSciences.id("bio/microscopy");
    public static final Identifier NODE_CELL_BIOLOGY = EarthLifeSciences.id("bio/cell_biology");
    public static final Identifier NODE_MICROBIOLOGY = EarthLifeSciences.id("bio/microbiology");
    public static final Identifier NODE_BOTANY = EarthLifeSciences.id("bio/botany");
    public static final Identifier NODE_ZOOLOGY = EarthLifeSciences.id("bio/zoology");
    public static final Identifier NODE_ECOLOGY = EarthLifeSciences.id("bio/ecology");
    public static final Identifier NODE_GENETICS = EarthLifeSciences.id("bio/genetics");
    public static final Identifier NODE_PLANT_BREEDING = EarthLifeSciences.id("bio/plant_breeding");
    public static final Identifier NODE_BIOCHEMISTRY = EarthLifeSciences.id("bio/biochemistry");
    public static final Identifier NODE_INDUSTRIAL_FERMENTATION = EarthLifeSciences.id("bio/industrial_fermentation");
    public static final Identifier NODE_MOLECULAR_BIOLOGY = EarthLifeSciences.id("bio/molecular_biology");
    public static final Identifier NODE_GENOMICS = EarthLifeSciences.id("bio/genomics");
    public static final Identifier NODE_MANUAL_SEQUENCING = EarthLifeSciences.id("bio/manual_sequencing");
    public static final Identifier NODE_GENETIC_ENGINEERING = EarthLifeSciences.id("bio/genetic_engineering");
    public static final Identifier NODE_BIO_MASTERY = EarthLifeSciences.id("bio/life_science_mastery");

    // MED nodes (21)
    public static final Identifier NODE_ANATOMY = EarthLifeSciences.id("med/anatomy");
    public static final Identifier NODE_PHYSIOLOGY = EarthLifeSciences.id("med/physiology");
    public static final Identifier NODE_DIAGNOSTICS = EarthLifeSciences.id("med/diagnostics");
    public static final Identifier NODE_LABORATORY_MEDICINE = EarthLifeSciences.id("med/laboratory_medicine");
    public static final Identifier NODE_INFECTIOUS_DISEASE = EarthLifeSciences.id("med/infectious_disease");
    public static final Identifier NODE_ANTIBIOTICS = EarthLifeSciences.id("med/antibiotics");
    public static final Identifier NODE_BROAD_SPECTRUM = EarthLifeSciences.id("med/broad_spectrum_antibiotics");
    public static final Identifier NODE_ANTIVIRALS_ANTIFUNGALS = EarthLifeSciences.id("med/antivirals_antifungals");
    public static final Identifier NODE_IMMUNOLOGY = EarthLifeSciences.id("med/immunology");
    public static final Identifier NODE_ANESTHESIOLOGY = EarthLifeSciences.id("med/anesthesiology");
    public static final Identifier NODE_CONTROLLED_ANALGESICS = EarthLifeSciences.id("med/controlled_analgesics");
    public static final Identifier NODE_SURGERY = EarthLifeSciences.id("med/surgery");
    public static final Identifier NODE_INTERNAL_MEDICINE = EarthLifeSciences.id("med/internal_medicine");
    public static final Identifier NODE_ENDOCRINOLOGY = EarthLifeSciences.id("med/endocrinology");
    public static final Identifier NODE_PSYCHIATRY = EarthLifeSciences.id("med/psychiatry");
    public static final Identifier NODE_ONCOLOGY = EarthLifeSciences.id("med/oncology");
    public static final Identifier NODE_IV_THERAPY = EarthLifeSciences.id("med/iv_therapy");
    public static final Identifier NODE_RADIOLOGY = EarthLifeSciences.id("med/radiology");
    public static final Identifier NODE_RADIOGRAPHY_SELF = EarthLifeSciences.id("med/radiography_self_taught");
    public static final Identifier NODE_EPIDEMIOLOGY = EarthLifeSciences.id("med/epidemiology");
    public static final Identifier NODE_MED_MASTERY = EarthLifeSciences.id("med/medical_mastery");

    private LifeLoreIntegration() {
    }

    public static boolean knows(ServerPlayer player, Identifier node) {
        return player != null && LoreApi.knows(player, node);
    }

    public static Grade toEomGrade(com.xxsx.earthlore.api.Grade loreGrade) {
        if (loreGrade == null) {
            return Grade.COMMON;
        }
        return switch (loreGrade) {
            case CRUDE -> Grade.CRUDE;
            case COMMON -> Grade.COMMON;
            case FINE -> Grade.FINE;
            case MASTERWORK -> Grade.MASTERWORK;
            case HEIRLOOM -> Grade.HEIRLOOM;
        };
    }

    public static com.xxsx.earthlore.api.Grade toLoreGrade(Grade eomGrade) {
        if (eomGrade == null) {
            return com.xxsx.earthlore.api.Grade.COMMON;
        }
        return switch (eomGrade) {
            case CRUDE -> com.xxsx.earthlore.api.Grade.CRUDE;
            case COMMON -> com.xxsx.earthlore.api.Grade.COMMON;
            case FINE -> com.xxsx.earthlore.api.Grade.FINE;
            case MASTERWORK -> com.xxsx.earthlore.api.Grade.MASTERWORK;
            case HEIRLOOM -> com.xxsx.earthlore.api.Grade.HEIRLOOM;
        };
    }

    public static ItemStack stampPharmaceutical(Item item, int count, String drugSlug,
                                                com.xxsx.earthlore.api.Grade loreGrade, double purity,
                                                int doseMg, PharmaceuticalComponent.Form form) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(EarthDataComponents.PHARMACEUTICAL.get(), new PharmaceuticalComponent(
                1, drugSlug, toEomGrade(loreGrade), Math.max(0.0D, Math.min(1.0D, purity)),
                Math.max(1, Math.min(2000, doseMg)), form, "ELS-01", "cool_dry"
        ));
        return stack;
    }

    public static boolean isSterile(ItemStack stack) {
        return stack != null && !stack.isEmpty() && Boolean.TRUE.equals(stack.get(EarthDataComponents.STERILE.get()));
    }

    public static void setSterile(ItemStack stack, boolean sterile) {
        if (stack != null && !stack.isEmpty()) {
            stack.set(EarthDataComponents.STERILE.get(), sterile);
        }
    }
}
