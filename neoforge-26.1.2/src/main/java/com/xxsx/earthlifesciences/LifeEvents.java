package com.xxsx.earthlifesciences;

import com.xxsx.earthonminecraft.component.Grade;
import com.xxsx.earthonminecraft.component.PharmaceuticalComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Event listeners and villager profession trades for Earth: Life Sciences (16 册 §5–§6).
 */
public final class LifeEvents {
    private LifeEvents() {
    }

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(LivingConversionEvent.Pre.class, LifeEvents::onLivingConversionPre);
        bus.addListener(LivingDamageEvent.Post.class, LifeEvents::onLivingDamagePost);
        bus.addListener(LivingDeathEvent.class, LifeEvents::onLivingDeath);
    }

    /** B-051: Vaccinated villager does not convert to zombie_villager when killed by zombie within 72000 ticks. */
    private static void onLivingConversionPre(LivingConversionEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Villager && event.getOutcome() == EntityType.ZOMBIE_VILLAGER) {
            long now = entity.level().getGameTime();
            LifeComponents.MedicalAttachment att = entity.getData(EarthLifeSciences.MEDICAL_ATTACHMENT.get());
            if (att.isImmune("tetanus", now) || att.isImmune("dysentery", now)) {
                event.setCanceled(true);
            }
        }
    }

    /** B-053: Spider/bee stings apply envenomation fallback (poison). */
    private static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var attacker = event.getSource().getEntity();
        if (attacker != null) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).getPath();
            if ("cave_spider".equals(id) || "bee".equals(id)) {
                EarthHumanCompat.addConditionOrFallback(player, "envenomation", 0.25D, 100);
            }
        }
    }

    /** B-004: Killing an animal records its species in field_guide if present in player inventory. */
    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            LifeItems.recordSeenInGuide(player, typeId.toString());
        }
    }

    public static void addProfessionTrades(Villager villager, int level) {
        var prof = villager.getVillagerData().profession();
        MerchantOffers offers = villager.getOffers();
        if (prof.is(EarthLifeSciences.PHYSICIAN.getId())) {
            addPhysicianTrades(offers, level);
        } else if (prof.is(EarthLifeSciences.BIOLOGIST.getId())) {
            addBiologistTrades(offers, level);
        } else if (prof.is(EarthLifeSciences.NURSE.getId())) {
            addNurseTrades(offers, level);
        }
    }

    private static Item eomBandage() {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("earth_on_minecraft", "bandage"));
        return item == Items.AIR ? Items.PAPER : item;
    }

    /** BB-01 physician trades. */
    public static void addPhysicianTrades(MerchantOffers offers, int level) {
        switch (level) {
            case 1 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(eomBandage(), 4), 16, 2, 0.05F));
            case 2 -> offers.add(new MerchantOffer(new ItemCost(EarthLifeSciences.PATIENT_CHART.get(), 2), new ItemStack(Items.EMERALD, 1), 16, 5, 0.05F));
            case 3 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(EarthLifeSciences.DIAGNOSTIC_KIT.get(), 1), 12, 10, 0.05F));
            case 4 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 16), new ItemStack(EarthLifeSciences.SURGICAL_KIT.get(), 1), 8, 15, 0.05F));
            case 5 -> {
                ItemStack pen = LifeLoreIntegration.stampPharmaceutical(
                        EarthLifeSciences.PENICILLIN.get(), 2, "penicillin",
                        com.xxsx.earthlore.api.Grade.COMMON, 0.85D, 250, PharmaceuticalComponent.Form.TABLET);
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12), pen, 8, 30, 0.05F));
            }
        }
    }

    /** BB-02 biologist trades. */
    public static void addBiologistTrades(MerchantOffers offers, int level) {
        switch (level) {
            case 1 -> offers.add(new MerchantOffer(new ItemCost(EarthLifeSciences.DRIED_SPECIMEN.get(), 4), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05F));
            case 2 -> offers.add(new MerchantOffer(new ItemCost(EarthLifeSciences.TISSUE_SAMPLE.get(), 4), new ItemStack(Items.EMERALD, 1), 16, 5, 0.05F));
            case 3 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(EarthLifeSciences.CULTURE_MEDIUM.get(), 4), 12, 10, 0.05F));
            case 4 -> {
                ItemStack sample = new ItemStack(EarthLifeSciences.MICROBE_SAMPLE.get(), 1);
                sample.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent("penicillium", "none", true, 0.65D, 0, false));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), sample, 8, 15, 0.05F));
            }
            case 5 -> {
                ItemStack pure = new ItemStack(EarthLifeSciences.PURE_CULTURE.get(), 1);
                pure.set(EarthLifeSciences.STRAIN.get(), new LifeComponents.StrainComponent("saccharomyces", "none", true, 0.88D, 0, false));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 8), pure, 8, 30, 0.05F));
            }
        }
    }

    /** BB-03 nurse trades. */
    public static void addNurseTrades(MerchantOffers offers, int level) {
        switch (level) {
            case 1 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(eomBandage(), 4), 16, 2, 0.05F));
            case 2 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), new ItemStack(EarthLifeSciences.CLINICAL_THERMOMETER.get(), 1), 12, 5, 0.05F));
            case 3 -> {
                ItemStack bag = new ItemStack(EarthLifeSciences.IV_BAG.get(), 1);
                bag.set(EarthLifeSciences.IV_BAG_DATA.get(), new LifeComponents.IvBagData("saline", "", 500));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), bag, 12, 10, 0.05F));
            }
            case 4 -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(EarthLifeSciences.BLOOD_TEST_KIT.get(), 2), 12, 15, 0.05F));
            case 5 -> {
                ItemStack vac = LifeItems.AttenuatedVaccineItem.create("tetanus", 1);
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), vac, 8, 30, 0.05F));
            }
        }
    }
}
