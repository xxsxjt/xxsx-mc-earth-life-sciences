package com.xxsx.earthlifesciences.mixin;

import com.xxsx.earthlifesciences.LifeEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
abstract class LifeVillagerMixin {
    @Inject(method = "updateTrades", at = @At("HEAD"))
    private void earthLifeSciences$addProfessionTrades(ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        LifeEvents.addProfessionTrades(self, self.getVillagerData().level());
    }
}
