package com.xxsx.earthlifesciences;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data components and player/entity attachments for Earth: Life Sciences (16 册 B-003..B-051).
 */
public final class LifeComponents {
    public static final List<String> VALID_STRAINS = List.of(
            "bacillus", "streptomyces", "penicillium", "aspergillus",
            "saccharomyces", "lactobacillus", "escherichia", "clostridium"
    );

    private LifeComponents() {
    }

    /** B-016 earth_life_sciences:genotype component. */
    public record GenotypeComponent(boolean sequenced, int yield, int tolerance, int potency, int growth, int hardiness, int generation) {
        public static final GenotypeComponent DEFAULT = new GenotypeComponent(false, 3, 3, 3, 3, 3, 0);

        public GenotypeComponent {
            yield = LifeScienceMath.clampTrait(yield);
            tolerance = LifeScienceMath.clampTrait(tolerance);
            potency = LifeScienceMath.clampTrait(potency);
            growth = LifeScienceMath.clampTrait(growth);
            hardiness = LifeScienceMath.clampTrait(hardiness);
            generation = Math.max(0, Math.min(255, generation));
        }

        public int[] traitsArray() {
            return new int[]{yield, tolerance, potency, growth, hardiness};
        }

        public int traitValue(String traitName) {
            return traitsArray()[LifeScienceMath.traitIndex(traitName)];
        }

        public GenotypeComponent withSequenced(boolean seq) {
            return new GenotypeComponent(seq, yield, tolerance, potency, growth, hardiness, generation);
        }

        public static final Codec<GenotypeComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.optionalFieldOf("sequenced", false).forGetter(GenotypeComponent::sequenced),
                Codec.INT.optionalFieldOf("yield", 3).forGetter(GenotypeComponent::yield),
                Codec.INT.optionalFieldOf("tolerance", 3).forGetter(GenotypeComponent::tolerance),
                Codec.INT.optionalFieldOf("potency", 3).forGetter(GenotypeComponent::potency),
                Codec.INT.optionalFieldOf("growth", 3).forGetter(GenotypeComponent::growth),
                Codec.INT.optionalFieldOf("hardiness", 3).forGetter(GenotypeComponent::hardiness),
                Codec.INT.optionalFieldOf("generation", 0).forGetter(GenotypeComponent::generation)
        ).apply(inst, GenotypeComponent::new));

        public static final StreamCodec<ByteBuf, GenotypeComponent> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    buf.writeBoolean(val.sequenced());
                    buf.writeByte(val.yield());
                    buf.writeByte(val.tolerance());
                    buf.writeByte(val.potency());
                    buf.writeByte(val.growth());
                    buf.writeByte(val.hardiness());
                    buf.writeShort(val.generation());
                },
                buf -> new GenotypeComponent(
                        buf.readBoolean(),
                        buf.readByte(),
                        buf.readByte(),
                        buf.readByte(),
                        buf.readByte(),
                        buf.readByte(),
                        buf.readUnsignedShort()
                )
        );
    }

    /** B-017 earth_life_sciences:strain component (+ mixed flag for B-010 pure_culture). */
    public record StrainComponent(String species, String subtype, boolean identified, double purity, int generation, boolean mixed) {
        public static final StrainComponent DEFAULT = new StrainComponent("penicillium", "none", false, 0.50D, 0, false);

        public StrainComponent {
            species = (species == null || species.isBlank()) ? "penicillium" : species;
            subtype = (subtype == null || subtype.isBlank()) ? "none" : subtype;
            purity = Math.max(0.0D, Math.min(1.0D, purity));
            generation = Math.max(0, Math.min(LifeScienceMath.MAX_STRAIN_GENERATION, generation));
        }

        public StrainComponent withIdentified(boolean id) {
            return new StrainComponent(species, subtype, id, purity, generation, mixed);
        }

        public StrainComponent withSubtype(String sub) {
            return new StrainComponent(species, sub, true, purity, generation, mixed);
        }

        public static final Codec<StrainComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("species", "penicillium").forGetter(StrainComponent::species),
                Codec.STRING.optionalFieldOf("subtype", "none").forGetter(StrainComponent::subtype),
                Codec.BOOL.optionalFieldOf("identified", false).forGetter(StrainComponent::identified),
                Codec.DOUBLE.optionalFieldOf("purity", 0.50D).forGetter(StrainComponent::purity),
                Codec.INT.optionalFieldOf("generation", 0).forGetter(StrainComponent::generation),
                Codec.BOOL.optionalFieldOf("mixed", false).forGetter(StrainComponent::mixed)
        ).apply(inst, StrainComponent::new));

        public static final StreamCodec<ByteBuf, StrainComponent> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, StrainComponent::species,
                ByteBufCodecs.STRING_UTF8, StrainComponent::subtype,
                ByteBufCodecs.BOOL, StrainComponent::identified,
                ByteBufCodecs.DOUBLE, StrainComponent::purity,
                ByteBufCodecs.VAR_INT, StrainComponent::generation,
                ByteBufCodecs.BOOL, StrainComponent::mixed,
                StrainComponent::new
        );
    }

    /** B-025 dna_template data component. */
    public record DnaTemplateData(String species, String trait, int value) {
        public static final DnaTemplateData DEFAULT = new DnaTemplateData("minecraft:wheat_seeds", "yield", 5);

        public DnaTemplateData {
            species = species == null ? "minecraft:wheat_seeds" : species;
            trait = trait == null ? "yield" : trait;
            value = LifeScienceMath.clampTrait(value);
        }

        public static final Codec<DnaTemplateData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("species", "minecraft:wheat_seeds").forGetter(DnaTemplateData::species),
                Codec.STRING.optionalFieldOf("trait", "yield").forGetter(DnaTemplateData::trait),
                Codec.INT.optionalFieldOf("value", 5).forGetter(DnaTemplateData::value)
        ).apply(inst, DnaTemplateData::new));

        public static final StreamCodec<ByteBuf, DnaTemplateData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, DnaTemplateData::species,
                ByteBufCodecs.STRING_UTF8, DnaTemplateData::trait,
                ByteBufCodecs.VAR_INT, DnaTemplateData::value,
                DnaTemplateData::new
        );
    }

    /** B-047 iv_bag data component. */
    public record IvBagData(String content, String drug, int volumeMl) {
        public static final IvBagData EMPTY = new IvBagData("empty", "", 0);

        public IvBagData {
            content = (content == null || content.isBlank()) ? "empty" : content;
            drug = drug == null ? "" : drug;
            volumeMl = Math.max(0, Math.min(500, volumeMl));
            if (volumeMl == 0) {
                content = "empty";
                drug = "";
            }
        }

        public static final Codec<IvBagData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("content", "empty").forGetter(IvBagData::content),
                Codec.STRING.optionalFieldOf("drug", "").forGetter(IvBagData::drug),
                Codec.INT.optionalFieldOf("volume_ml", 0).forGetter(IvBagData::volumeMl)
        ).apply(inst, IvBagData::new));

        public static final StreamCodec<ByteBuf, IvBagData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, IvBagData::content,
                ByteBufCodecs.STRING_UTF8, IvBagData::drug,
                ByteBufCodecs.VAR_INT, IvBagData::volumeMl,
                IvBagData::new
        );
    }

    /** B-003, B-020, B-021, B-051 specimen/tissue/vaccine tag data. */
    public record SpecimenData(String species, String kind, boolean identified) {
        public static final SpecimenData EMPTY = new SpecimenData("", "empty", false);

        public SpecimenData {
            species = species == null ? "" : species;
            kind = (kind == null || kind.isBlank()) ? "empty" : kind;
        }

        public static final Codec<SpecimenData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("species", "").forGetter(SpecimenData::species),
                Codec.STRING.optionalFieldOf("kind", "empty").forGetter(SpecimenData::kind),
                Codec.BOOL.optionalFieldOf("identified", false).forGetter(SpecimenData::identified)
        ).apply(inst, SpecimenData::new));

        public static final StreamCodec<ByteBuf, SpecimenData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SpecimenData::species,
                ByteBufCodecs.STRING_UTF8, SpecimenData::kind,
                ByteBufCodecs.BOOL, SpecimenData::identified,
                SpecimenData::new
        );
    }

    /** B-004 field_guide data component. */
    public record FieldGuideData(List<String> seen, List<String> pressed) {
        public static final FieldGuideData EMPTY = new FieldGuideData(List.of(), List.of());

        public FieldGuideData {
            seen = seen == null ? List.of() : List.copyOf(seen.subList(0, Math.min(256, seen.size())));
            pressed = pressed == null ? List.of() : List.copyOf(pressed.subList(0, Math.min(256, pressed.size())));
        }

        public FieldGuideData withSeen(String entityId) {
            if (entityId == null || entityId.isBlank() || seen.contains(entityId) || seen.size() >= 256) {
                return this;
            }
            List<String> next = new ArrayList<>(seen);
            next.add(entityId);
            return new FieldGuideData(next, pressed);
        }

        public FieldGuideData withPressed(String itemId) {
            if (itemId == null || itemId.isBlank() || pressed.contains(itemId) || pressed.size() >= 256) {
                return this;
            }
            List<String> next = new ArrayList<>(pressed);
            next.add(itemId);
            return new FieldGuideData(seen, next);
        }

        public static final Codec<FieldGuideData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.listOf().optionalFieldOf("seen", List.of()).forGetter(FieldGuideData::seen),
                Codec.STRING.listOf().optionalFieldOf("pressed", List.of()).forGetter(FieldGuideData::pressed)
        ).apply(inst, FieldGuideData::new));

        public static final StreamCodec<ByteBuf, FieldGuideData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), FieldGuideData::seen,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), FieldGuideData::pressed,
                FieldGuideData::new
        );
    }

    /** B-045 patient_chart data component. */
    public record ChartEntry(long tick, String condition, double severity, String treatment) {
        public static final Codec<ChartEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.optionalFieldOf("tick", 0L).forGetter(ChartEntry::tick),
                Codec.STRING.optionalFieldOf("condition", "normal").forGetter(ChartEntry::condition),
                Codec.DOUBLE.optionalFieldOf("severity", 0.0D).forGetter(ChartEntry::severity),
                Codec.STRING.optionalFieldOf("treatment", "").forGetter(ChartEntry::treatment)
        ).apply(inst, ChartEntry::new));

        public static final StreamCodec<ByteBuf, ChartEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, ChartEntry::tick,
                ByteBufCodecs.STRING_UTF8, ChartEntry::condition,
                ByteBufCodecs.DOUBLE, ChartEntry::severity,
                ByteBufCodecs.STRING_UTF8, ChartEntry::treatment,
                ChartEntry::new
        );
    }

    public record PatientChartData(String patientUuid, String patientName, List<ChartEntry> entries) {
        public static final PatientChartData EMPTY = new PatientChartData("", "", List.of());

        public PatientChartData {
            patientUuid = patientUuid == null ? "" : patientUuid;
            patientName = patientName == null ? "" : (patientName.length() > 32 ? patientName.substring(0, 32) : patientName);
            entries = entries == null ? List.of() : List.copyOf(entries.subList(0, Math.min(32, entries.size())));
        }

        public PatientChartData append(String uuid, String name, ChartEntry entry) {
            List<ChartEntry> next = new ArrayList<>(entries);
            if (next.size() >= 32) {
                next.remove(0);
            }
            next.add(entry);
            return new PatientChartData(uuid, name, next);
        }

        public static final Codec<PatientChartData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("patient", "").forGetter(PatientChartData::patientUuid),
                Codec.STRING.optionalFieldOf("patient_name", "").forGetter(PatientChartData::patientName),
                ChartEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(PatientChartData::entries)
        ).apply(inst, PatientChartData::new));

        public static final StreamCodec<ByteBuf, PatientChartData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PatientChartData::patientUuid,
                ByteBufCodecs.STRING_UTF8, PatientChartData::patientName,
                ChartEntry.STREAM_CODEC.apply(ByteBufCodecs.list(32)), PatientChartData::entries,
                PatientChartData::new
        );
    }

    /** B-051 & B-053: Vaccine immunity and diagnosis attachment on entities/players. */
    public static final class MedicalAttachment {
        private long tetanusImmuneUntil;
        private long dysenteryImmuneUntil;
        private long diagnosedUntil;
        private int dependence;

        public MedicalAttachment() {
        }

        public MedicalAttachment(long tetanusImmuneUntil, long dysenteryImmuneUntil, long diagnosedUntil, int dependence) {
            this.tetanusImmuneUntil = tetanusImmuneUntil;
            this.dysenteryImmuneUntil = dysenteryImmuneUntil;
            this.diagnosedUntil = diagnosedUntil;
            this.dependence = Math.max(0, Math.min(100, dependence));
        }

        public long tetanusImmuneUntil() {
            return tetanusImmuneUntil;
        }

        public long dysenteryImmuneUntil() {
            return dysenteryImmuneUntil;
        }

        public long diagnosedUntil() {
            return diagnosedUntil;
        }

        public int dependence() {
            return dependence;
        }

        public boolean isImmune(String target, long now) {
            if ("tetanus".equals(target)) {
                return tetanusImmuneUntil > now;
            }
            if ("dysentery".equals(target)) {
                return dysenteryImmuneUntil > now;
            }
            return tetanusImmuneUntil > now || dysenteryImmuneUntil > now;
        }

        public void immunize(String target, long until) {
            if ("dysentery".equals(target)) {
                this.dysenteryImmuneUntil = Math.max(this.dysenteryImmuneUntil, until);
            } else {
                this.tetanusImmuneUntil = Math.max(this.tetanusImmuneUntil, until);
            }
        }

        public void markDiagnosed(long until) {
            this.diagnosedUntil = Math.max(this.diagnosedUntil, until);
        }

        public void addDependence(int delta) {
            this.dependence = Math.max(0, Math.min(100, this.dependence + delta));
        }

        public static final MapCodec<MedicalAttachment> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.LONG.optionalFieldOf("tetanus_immune_until", 0L).forGetter(MedicalAttachment::tetanusImmuneUntil),
                Codec.LONG.optionalFieldOf("dysentery_immune_until", 0L).forGetter(MedicalAttachment::dysenteryImmuneUntil),
                Codec.LONG.optionalFieldOf("diagnosed_until", 0L).forGetter(MedicalAttachment::diagnosedUntil),
                Codec.INT.optionalFieldOf("dependence", 0).forGetter(MedicalAttachment::dependence)
        ).apply(inst, MedicalAttachment::new));
    }
}
