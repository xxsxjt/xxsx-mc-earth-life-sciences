package com.xxsx.earthlifesciences;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menus for the 9 blocks that register a menu in Earth: Life Sciences (16 册 §3–§4).
 */
public class LifeMenus extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;
    private final int slotCount;

    public LifeMenus(MenuType<?> type, int containerId, Inventory inventory, int slotCount, int dataCount, RegistryFriendlyByteBuf buf) {
        this(type, containerId, inventory, new SimpleContainer(slotCount), new SimpleContainerData(dataCount),
                buf != null && buf.readableBytes() >= 8 ? buf.readBlockPos() : BlockPos.ZERO, slotCount);
    }

    public LifeMenus(MenuType<?> type, int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos, int slotCount) {
        super(type, containerId);
        checkContainerSize(container, slotCount);
        this.container = container;
        this.data = data;
        this.pos = pos;
        this.slotCount = slotCount;

        for (int i = 0; i < slotCount; i++) {
            int col = i % 3;
            int row = i / 3;
            addSlot(new Slot(container, i, 44 + col * 26, 22 + row * 24));
        }
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = slotCount;
        int playerEnd = playerStart + 36;
        if (index < slotCount) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, slotCount, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public int progress() {
        return data.getCount() > 0 ? data.get(0) : 0;
    }

    public int maxProgress() {
        return data.getCount() > 1 ? Math.max(1, data.get(1)) : 100;
    }

    public int energy() {
        return data.getCount() > 2 ? data.get(2) : 0;
    }

    public int maxEnergy() {
        return data.getCount() > 3 ? Math.max(1, data.get(3)) : 20000;
    }

    public BlockPos pos() {
        return pos;
    }

    public static LifeMenus createIncubator(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.INCUBATOR_MENU.get(), id, inv, 4, 4, buf);
    }

    public static LifeMenus createFermenter(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.FERMENTER_MENU.get(), id, inv, 3, 4, buf);
    }

    public static LifeMenus createCentrifuge(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.CENTRIFUGE_MENU.get(), id, inv, 2, 4, buf);
    }

    public static LifeMenus createGeneSequencer(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.GENE_SEQUENCER_MENU.get(), id, inv, 1, 4, buf);
    }

    public static LifeMenus createBreedingStation(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.BREEDING_STATION_MENU.get(), id, inv, 4, 4, buf);
    }

    public static LifeMenus createSeedBank(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.SEED_BANK_MENU.get(), id, inv, 2, 2, buf);
    }

    public static LifeMenus createPcrCycler(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.PCR_CYCLER_MENU.get(), id, inv, 3, 4, buf);
    }

    public static LifeMenus createGeneEditor(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.GENE_EDITOR_MENU.get(), id, inv, 2, 4, buf);
    }

    public static LifeMenus createClinicDesk(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new LifeMenus(EarthLifeSciences.CLINIC_DESK_MENU.get(), id, inv, 6, 2, buf);
    }
}
