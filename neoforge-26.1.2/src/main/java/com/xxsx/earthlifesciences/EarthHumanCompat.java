package com.xxsx.earthlifesciences;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Earth Human v1/v2 bridge and B-053 fallback implementation.
 */
public final class EarthHumanCompat {
    private static final String MODID = "earth_human";
    private static final String API_CLASS = "com.xxsx.earthhuman.api.EarthHumanApi";
    private static final Logger LOGGER = LoggerFactory.getLogger(EarthHumanCompat.class);
    private static volatile ApiBinding binding;
    private static volatile boolean resolved;
    private static volatile boolean warned;

    private EarthHumanCompat() {
    }

    public static boolean isLoaded() {
        return api() != null;
    }

    public static int apiVersion() {
        ApiBinding b = api();
        return b == null ? 0 : b.version;
    }

    public static VitalsSnapshot snapshot(Player player) {
        ApiBinding b = api();
        if (b == null || !(player instanceof ServerPlayer serverPlayer)) {
            return VitalsSnapshot.fallback(player);
        }
        try {
            Object val = b.snapshot.invoke(null, serverPlayer);
            return new VitalsSnapshot(
                    true,
                    num(b.hydration.invoke(val)),
                    num(b.bodyTemperature.invoke(val)),
                    num(b.fatigue.invoke(val)),
                    num(b.exertionLoad.invoke(val)),
                    num(b.headIntegrity.invoke(val)),
                    num(b.bodyIntegrity.invoke(val))
            );
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("EarthHumanApi.snapshot failed; using fallback", e);
            return VitalsSnapshot.fallback(player);
        }
    }

    public static List<PartIntegrity> detailedParts(ServerPlayer player) {
        ApiBinding b = api();
        if (b == null || b.detailedSnapshot == null || player == null) {
            return List.of();
        }
        try {
            Object det = b.detailedSnapshot.invoke(null, player);
            Object listObj = b.bodyParts.invoke(det);
            if (listObj instanceof List<?> rawList) {
                List<PartIntegrity> out = new ArrayList<>();
                for (Object item : rawList) {
                    String id = String.valueOf(b.partId.invoke(item));
                    double pct = num(b.partIntegrity.invoke(item));
                    out.add(new PartIntegrity(id, pct));
                }
                return out;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("EarthHumanApi.detailedSnapshot failed; using fallback", e);
        }
        return List.of();
    }

    public static RecoveryReport recover(ServerPlayer player, double fatigueReduction, double bodyHeal, String... partIds) {
        ApiBinding b = api();
        if (b == null || player == null) {
            return RecoveryReport.EMPTY;
        }
        try {
            Object res = b.recover.invoke(null, new Object[]{player, fatigueReduction, bodyHeal, partIds == null ? new String[0] : partIds});
            return new RecoveryReport(true, num(b.fatigueReduced.invoke(res)), num(b.bodyHealed.invoke(res)));
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce("EarthHumanApi.recover failed; using fallback", e);
            return RecoveryReport.EMPTY;
        }
    }

    public static RecoveryReport recoverAllParts(ServerPlayer player, double fatigueReduction, double bodyHeal) {
        return recover(player, fatigueReduction, bodyHeal, "head", "torso", "limbs", "left_arm", "right_arm", "left_leg", "right_leg");
    }

    /** B-053 fallback condition applicator when v2 is unavailable. */
    public static void addConditionOrFallback(LivingEntity target, String conditionId, double severity, int fallbackTicks) {
        if (target == null) {
            return;
        }
        switch (conditionId) {
            case "wound_infection", "envenomation" ->
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, Math.max(100, fallbackTicks), 0));
            case "food_poisoning" ->
                    target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, Math.max(100, fallbackTicks), 0));
            case "dysentery" ->
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, Math.max(200, fallbackTicks), 0));
            case "tetanus" ->
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.max(200, fallbackTicks), 0));
            case "influenza" ->
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(200, fallbackTicks), 0));
            case "radiation_sickness" ->
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.max(100, fallbackTicks), 0));
            default -> {
            }
        }
    }

    /** B-053 fallback treatment when v2 is unavailable. */
    public static boolean treatConditionOrFallback(LivingEntity target, String conditionCategory) {
        if (target == null) {
            return false;
        }
        return switch (conditionCategory) {
            case "antibiotic", "wound_infection" -> target.removeEffect(MobEffects.POISON);
            case "food_poisoning" -> target.removeEffect(MobEffects.NAUSEA);
            case "dysentery" -> target.removeEffect(MobEffects.HUNGER);
            case "tetanus" -> target.removeEffect(MobEffects.SLOWNESS);
            case "influenza", "antiviral" -> target.removeEffect(MobEffects.WEAKNESS);
            case "radiation_sickness" -> target.removeEffect(MobEffects.WITHER);
            default -> false;
        };
    }

    private static ApiBinding api() {
        if (ModList.get() == null || !ModList.get().isLoaded(MODID)) {
            return null;
        }
        if (resolved) {
            return binding;
        }
        synchronized (EarthHumanCompat.class) {
            if (resolved) {
                return binding;
            }
            resolved = true;
            try {
                Class<?> cls = Class.forName(API_CLASS);
                int ver = ((Number) cls.getMethod("apiVersion").invoke(null)).intValue();
                if (ver < LifeScienceMath.EH_API_BASE) {
                    return null;
                }
                Method recover = cls.getMethod("recover", ServerPlayer.class, double.class, double.class, String[].class);
                Method snapshot = cls.getMethod("snapshot", ServerPlayer.class);
                Class<?> recType = recover.getReturnType();
                Class<?> snapType = snapshot.getReturnType();
                Method detSnap = null;
                Method bodyParts = null;
                Method partId = null;
                Method partInt = null;
                try {
                    detSnap = cls.getMethod("detailedSnapshot", ServerPlayer.class);
                    Class<?> detType = detSnap.getReturnType();
                    bodyParts = detType.getMethod("bodyParts");
                    Class<?> partClass = Class.forName(API_CLASS + "$PartSnapshot");
                    partId = partClass.getMethod("id");
                    partInt = partClass.getMethod("integrity");
                } catch (ReflectiveOperationException ignored) {
                }
                binding = new ApiBinding(
                        ver,
                        recover,
                        recType.getMethod("fatigueReduced"),
                        recType.getMethod("bodyHealed"),
                        snapshot,
                        snapType.getMethod("hydration"),
                        snapType.getMethod("bodyTemperature"),
                        snapType.getMethod("fatigue"),
                        snapType.getMethod("exertionLoad"),
                        snapType.getMethod("headIntegrity"),
                        snapType.getMethod("bodyIntegrity"),
                        detSnap,
                        bodyParts,
                        partId,
                        partInt
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                warnOnce("EarthHumanApi binding failed; using fallback", e);
            }
            return binding;
        }
    }

    private static double num(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0D;
    }

    private static void warnOnce(String msg, Throwable err) {
        if (!warned) {
            warned = true;
            LOGGER.warn(msg, err);
        }
    }

    public record VitalsSnapshot(boolean linked, double hydration, double bodyTemperature, double fatigue,
                                 double exertionLoad, double headIntegrity, double bodyIntegrity) {
        public static VitalsSnapshot fallback(Player player) {
            double hpRatio = player == null ? 1.0D : Math.max(0.0D, Math.min(1.0D, player.getHealth() / Math.max(1.0F, player.getMaxHealth())));
            return new VitalsSnapshot(false, 100.0D, 310.15D, 0.0D, 0.0D, hpRatio, hpRatio);
        }

        public boolean abnormal() {
            return bodyTemperature < 308.5D || bodyTemperature > 311.5D || fatigue > 60.0D || bodyIntegrity < 0.85D || headIntegrity < 0.85D;
        }
    }

    public record PartIntegrity(String id, double integrity) {
    }

    public record RecoveryReport(boolean linked, double fatigueReduced, double bodyHealed) {
        public static final RecoveryReport EMPTY = new RecoveryReport(false, 0.0D, 0.0D);
    }

    private record ApiBinding(
            int version,
            Method recover, Method fatigueReduced, Method bodyHealed,
            Method snapshot, Method hydration, Method bodyTemperature, Method fatigue, Method exertionLoad, Method headIntegrity, Method bodyIntegrity,
            Method detailedSnapshot, Method bodyParts, Method partId, Method partIntegrity
    ) {
    }
}
