package com.xxsx.earthlifesciences;

import com.xxsx.earthlore.api.Grade;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure deterministic and parameterized domain math for Earth: Life Sciences (16 册 §1–§6, 13 册).
 */
public final class LifeScienceMath {
    public static final double CULTURE_PURE_MIN = 0.80D;
    public static final int INCUBATE_TICKS = 2400;
    public static final int FERMENT_BATCH_TICKS = 6000;
    public static final int FERMENTER_TANK_MB = 16000;
    public static final int BROTH_PER_BATCH_MB = 4000;
    public static final int CENTRIFUGE_MB_PER_CYCLE = 1000;
    public static final int GENOTYPE_TRAITS = 5;
    public static final int TRAIT_MAX = 10;
    public static final double MUTATION_CHANCE = 0.05D;
    public static final double OFF_TARGET_CHANCE = 0.10D;
    public static final int SAMPLE_COOLDOWN_TICKS = 40;
    public static final int EH_API_BASE = 1;
    public static final int EH_API_CONDITIONS = 2;
    public static final int MAX_STRAIN_GENERATION = 32;
    public static final int VACCINE_IMMUNITY_TICKS = 72000;

    private LifeScienceMath() {
    }

    /** B-009: purity = min(0.99, 0.55 + 0.10*(sterile?1:0) + 0.05*mediumTier + rand0to01). */
    public static double computeCulturePurity(boolean dishSterile, Grade mediumGrade, double rand0to01) {
        int tier = gradeTier(mediumGrade);
        double r = Math.max(0.0D, Math.min(0.10D, rand0to01));
        double raw = 0.55D + (dishSterile ? 0.10D : 0.0D) + 0.05D * tier + r;
        return Math.max(0.0D, Math.min(0.99D, raw));
    }

    public static int gradeTier(Grade grade) {
        if (grade == null) {
            return 1;
        }
        return switch (grade) {
            case CRUDE -> 0;
            case COMMON -> 1;
            case FINE -> 2;
            case MASTERWORK -> 3;
            case HEIRLOOM -> 4;
        };
    }

    public static boolean isCulturePure(double purity) {
        return purity >= CULTURE_PURE_MIN - 1e-9D;
    }

    public record SubcultureResult(boolean ok, double purity, int generation, boolean mixed, String fault) {
    }

    /** B-009 subculture rule: purity - 0.02, generation + 1; generation >= 32 -> strain_exhausted. */
    public static SubcultureResult computeSubculture(double parentPurity, int parentGeneration, boolean dishSterile) {
        if (parentGeneration >= MAX_STRAIN_GENERATION) {
            return new SubcultureResult(false, parentPurity, parentGeneration, true, "strain_exhausted");
        }
        double penalty = dishSterile ? 0.02D : 0.12D;
        double nextPurity = Math.max(0.0D, Math.min(0.99D, parentPurity - penalty));
        int nextGen = parentGeneration + 1;
        boolean mixed = !isCulturePure(nextPurity);
        return new SubcultureResult(true, nextPurity, nextGen, mixed, mixed ? "contaminated" : "");
    }

    public record FermentBatchResult(int brothMb, boolean contaminated, String fault) {
    }

    /** B-011: pure (>=0.80) produces 4000 mB; impure (<0.80) produces 4000 * 0.3 = 1200 mB and reports contaminated. */
    public static FermentBatchResult computeFermentBatch(double inoculumPurity) {
        if (isCulturePure(inoculumPurity)) {
            return new FermentBatchResult(BROTH_PER_BATCH_MB, false, "");
        }
        int reduced = (int) Math.round(BROTH_PER_BATCH_MB * 0.3D);
        return new FermentBatchResult(reduced, true, "contaminated");
    }

    /** B-012: purity = min(0.99, 0.70 + 0.12*(activatedCarbon?1:0) + 0.15*(inoculumPurity - 0.80)). */
    public static double computeCentrifugeDrugPurity(double inoculumPurity, boolean activatedCarbon) {
        double raw = 0.70D + (activatedCarbon ? 0.12D : 0.0D) + 0.15D * (inoculumPurity - 0.80D);
        return Math.max(0.0D, Math.min(0.99D, raw));
    }

    public static Grade gradeForDrugPurity(double purity) {
        if (purity < 0.70D) {
            return Grade.CRUDE;
        }
        if (purity < 0.85D) {
            return Grade.COMMON;
        }
        if (purity < 0.95D) {
            return Grade.FINE;
        }
        if (purity < 0.99D) {
            return Grade.MASTERWORK;
        }
        return Grade.HEIRLOOM;
    }

    /** Applies soft-gate knowledge cap (e.g. K-MED-20 for penicillin: unlearned caps at COMMON, learned caps at MASTERWORK, K-MED-35 cap allows HEIRLOOM). */
    public static Grade resolveCentrifugeDrugGrade(double purity, boolean knowsNode, boolean knowsMastery) {
        Grade raw = gradeForDrugPurity(purity);
        Grade cap = !knowsNode ? Grade.COMMON : (knowsMastery ? Grade.HEIRLOOM : Grade.MASTERWORK);
        return raw.min(cap);
    }

    /** B-014: crosses two genotypes. choicePerTrait in {0=A, 1=B, 2=avg}. mutationRoll < 0.05 mutates mutationTraitIdx by mutationDelta (+1 or -1). */
    public static LifeComponents.GenotypeComponent crossGenotypes(
            LifeComponents.GenotypeComponent a,
            LifeComponents.GenotypeComponent b,
            int[] choicePerTrait,
            double mutationRoll,
            int mutationTraitIdx,
            int mutationDelta) {
        int[] valsA = a.traitsArray();
        int[] valsB = b.traitsArray();
        int[] out = new int[GENOTYPE_TRAITS];
        for (int i = 0; i < GENOTYPE_TRAITS; i++) {
            int c = (choicePerTrait != null && i < choicePerTrait.length) ? choicePerTrait[i] : 2;
            int v = switch (c) {
                case 0 -> valsA[i];
                case 1 -> valsB[i];
                default -> (valsA[i] + valsB[i]) / 2;
            };
            out[i] = clampTrait(v);
        }
        if (mutationRoll < MUTATION_CHANCE && mutationTraitIdx >= 0 && mutationTraitIdx < GENOTYPE_TRAITS) {
            int delta = mutationDelta >= 0 ? 1 : -1;
            out[mutationTraitIdx] = clampTrait(out[mutationTraitIdx] + delta);
        }
        int nextGen = Math.min(255, Math.max(a.generation(), b.generation()) + 1);
        return new LifeComponents.GenotypeComponent(true, out[0], out[1], out[2], out[3], out[4], nextGen);
    }

    public record GeneEditResult(LifeComponents.GenotypeComponent genotype, boolean offTarget, String fault) {
    }

    /** B-024: sets target trait to template value (0..10); with 0.10 chance another trait -1 and fault=off_target. */
    public static GeneEditResult applyGeneEdit(
            LifeComponents.GenotypeComponent base,
            String traitName,
            int value,
            double offTargetRoll,
            int offTargetTraitIdx) {
        int[] traits = base.traitsArray();
        int targetIdx = traitIndex(traitName);
        if (targetIdx >= 0) {
            traits[targetIdx] = clampTrait(value);
        }
        boolean off = false;
        if (offTargetRoll < OFF_TARGET_CHANCE) {
            int idx = Math.floorMod(offTargetTraitIdx, GENOTYPE_TRAITS);
            if (idx == targetIdx) {
                idx = (idx + 1) % GENOTYPE_TRAITS;
            }
            traits[idx] = clampTrait(traits[idx] - 1);
            off = true;
        }
        LifeComponents.GenotypeComponent updated = new LifeComponents.GenotypeComponent(
                true, traits[0], traits[1], traits[2], traits[3], traits[4], base.generation());
        return new GeneEditResult(updated, off, off ? "off_target" : "");
    }

    public static int traitIndex(String name) {
        if (name == null) {
            return 0;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "yield" -> 0;
            case "tolerance" -> 1;
            case "potency" -> 2;
            case "growth" -> 3;
            case "hardiness" -> 4;
            default -> 0;
        };
    }

    public static int clampTrait(int val) {
        return Math.max(0, Math.min(TRAIT_MAX, val));
    }

    /** B-016: yield extra drops = max(0, floor((yield - 3) / 2)). */
    public static int extraHarvestDrops(int yieldTrait) {
        return Math.max(0, (yieldTrait - 3) / 2);
    }

    /** B-017: streptomyces subtype probabilities 0.40 / 0.30 / 0.20 / 0.10. */
    public static String rollStreptomycesSubtype(double roll0to1) {
        if (roll0to1 < 0.40D) {
            return "streptomycin";
        }
        if (roll0to1 < 0.70D) {
            return "tetracycline";
        }
        if (roll0to1 < 0.90D) {
            return "chloramphenicol";
        }
        return "vancomycin";
    }

    /** B-006 microbe source table roll. Returns empty Optional if "none". */
    public static Optional<String> sampleMicrobeFromSource(String sourceKey, double roll0to1) {
        if (sourceKey == null) {
            return Optional.empty();
        }
        return switch (sourceKey) {
            case "dirt" -> {
                if (roll0to1 < 0.40D) yield Optional.of("bacillus");
                if (roll0to1 < 0.65D) yield Optional.of("streptomyces");
                if (roll0to1 < 0.70D) yield Optional.of("clostridium");
                yield Optional.empty();
            }
            case "podzol" -> {
                if (roll0to1 < 0.10D) yield Optional.of("bacillus");
                if (roll0to1 < 0.30D) yield Optional.of("streptomyces");
                if (roll0to1 < 0.50D) yield Optional.of("penicillium");
                if (roll0to1 < 0.60D) yield Optional.of("aspergillus");
                yield Optional.empty();
            }
            case "moss_block" -> roll0to1 < 0.30D ? Optional.of("penicillium") : Optional.empty();
            case "mycelium" -> roll0to1 < 0.30D ? Optional.of("aspergillus") : Optional.empty();
            case "water" -> roll0to1 < 0.30D ? Optional.of("escherichia") : Optional.empty();
            case "milk" -> roll0to1 < 0.60D ? Optional.of("lactobacillus") : Optional.empty();
            case "yeast" -> Optional.of("saccharomyces");
            default -> Optional.empty();
        };
    }

    /** B-001 microscope readout: unlearned K-BIO-03 only returns coarse status; learned returns strain + purity. */
    public static String formatMicroscopeReadout(String species, double purity, boolean knowsCellBiology) {
        if (species == null || species.isBlank()) {
            return "none";
        }
        if (!knowsCellBiology) {
            return "has_microbe";
        }
        return species + ":" + String.format(Locale.ROOT, "%.2f", purity);
    }

    public record SurgeryOutcome(boolean success, boolean infected, double healAmount, String part, String failCode) {
    }

    /** B-050 surgery validation and outcome calculation. */
    public static SurgeryOutcome evaluateSurgery(
            boolean knowsSurgery,
            boolean selfSurgery,
            boolean onOperatingTable,
            boolean hasAnesthesia,
            boolean hasKit,
            boolean kitSterile,
            String targetPart) {
        if (!knowsSurgery) {
            return new SurgeryOutcome(false, false, 0.0D, targetPart, "lore_locked");
        }
        if (selfSurgery) {
            return new SurgeryOutcome(false, false, 0.0D, targetPart, "self_surgery");
        }
        if (!hasKit) {
            return new SurgeryOutcome(false, false, 0.0D, targetPart, "no_kit");
        }
        if (!onOperatingTable) {
            return new SurgeryOutcome(false, false, 0.0D, targetPart, "no_table");
        }
        if (!hasAnesthesia) {
            return new SurgeryOutcome(false, false, 0.0D, targetPart, "no_anesthesia");
        }
        String normalizedPart = (targetPart == null || targetPart.isBlank()) ? "torso" : targetPart;
        return new SurgeryOutcome(true, !kitSterile, 8.0D, normalizedPart, "");
    }
}
