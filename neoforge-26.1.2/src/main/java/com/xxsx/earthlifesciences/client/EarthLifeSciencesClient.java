package com.xxsx.earthlifesciences.client;

import com.xxsx.earthlifesciences.EarthLifeSciences;
import com.xxsx.earthlifesciences.LifeMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class EarthLifeSciencesClient {
    private EarthLifeSciencesClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EarthLifeSciencesClient::registerScreens);
        modBus.addListener(EarthLifeSciencesClient::registerFluidModels);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(EarthLifeSciences.INCUBATOR_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.FERMENTER_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.CENTRIFUGE_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.GENE_SEQUENCER_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.BREEDING_STATION_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.SEED_BANK_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.PCR_CYCLER_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.GENE_EDITOR_MENU.get(), LifeMachineScreen::new);
        event.register(EarthLifeSciences.CLINIC_DESK_MENU.get(), LifeMachineScreen::new);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        FluidModel.Unbaked model = new FluidModel.Unbaked(
                new Material(EarthLifeSciences.id("block/fermentation_broth_still")),
                new Material(EarthLifeSciences.id("block/fermentation_broth_flow")),
                null,
                null);
        event.register(model, EarthLifeSciences.FERMENTATION_BROTH_SOURCE.get(), EarthLifeSciences.FERMENTATION_BROTH_FLOWING.get());
    }

    public static final class LifeMachineScreen extends AbstractContainerScreen<LifeMenus> {
        public LifeMachineScreen(LifeMenus menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            super.extractBackground(g, mouseX, mouseY, delta);
            g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFE9ECEE);
            g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFD8DEE3);
            int progW = (int) (60.0F * menu.progress() / Math.max(1, menu.maxProgress()));
            g.fill(leftPos + 105, topPos + 32, leftPos + 165, topPos + 40, 0xFF8C96A0);
            g.fill(leftPos + 105, topPos + 32, leftPos + 105 + Math.min(60, progW), topPos + 40, 0xFF4FAF4F);
            int enW = (int) (60.0F * menu.energy() / Math.max(1, menu.maxEnergy()));
            g.fill(leftPos + 105, topPos + 48, leftPos + 165, topPos + 56, 0xFF8C96A0);
            g.fill(leftPos + 105, topPos + 48, leftPos + 105 + Math.min(60, enW), topPos + 56, 0xFFD23C48);
        }
    }
}
