package doggytalents.common.inventory.container;

import java.util.ArrayList;
import java.util.List;

import doggytalents.DoggyContainerTypes;
import doggytalents.DoggyTalents;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.inventory.PackPuppyItemHandler;
import doggytalents.common.inventory.container.slot.DogInventorySlot;
import doggytalents.common.talent.PackPuppyTalent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IntArray;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * @author ProPercivalalb
 */
public class DogInventoriesContainer extends Container {

    private World world;
    private PlayerEntity player;
    private IntReferenceHolder position;
    private IntArray trackableArray;
    private final List<DogInventorySlot> dogSlots = new ArrayList<>();
    private int possibleSlots = 0;

    //Server method
    public DogInventoriesContainer(int windowId, PlayerInventory playerInventory, IntArray trackableArray) {
        super(DoggyContainerTypes.DOG_INVENTORIES.get(), windowId);
        this.world = playerInventory.player.level;
        this.player = playerInventory.player;
        this.position = IntReferenceHolder.standalone();
        checkContainerDataCount(trackableArray, 1);
        this.addDataSlot(this.position);
        this.trackableArray = trackableArray;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDogSlots();
    }

    public void addDogSlots() {
        final int TOTAL_COLUMNS = 9;

        int page = this.position.get();
        int drawingColumn = 0;

        for (int i = 0; i < this.trackableArray.getCount(); i++) {
            int entityId = this.trackableArray.get(i);
            Entity entity = this.world.getEntity(entityId);

            if (entity instanceof DogEntity) {
                DogEntity dog = (DogEntity) entity;

                PackPuppyItemHandler packInventory = dog.getTalent(DoggyTalents.PACK_PUPPY)
                        .map((inst) -> inst.cast(PackPuppyTalent.class).inventory()).orElse(null);
                if (packInventory == null) {
                    continue;
                }

                int level = MathHelper.clamp(dog.getLevel(DoggyTalents.PACK_PUPPY), 0, 5); // Number of rows for this dog
                int numCols = MathHelper.clamp(level, 0, Math.max(0, TOTAL_COLUMNS)); // Number of rows to draw

                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < numCols; col++) {
                        DogInventorySlot slot = new DogInventorySlot(dog, this.player, packInventory, drawingColumn + col, row, col, col * 3 + row, 8 + 18 * (drawingColumn + col - page), 18 * row + 18);
                        this.addDogSlot(slot);
                        int adjustedColumn = slot.getOverallColumn() - page;
                        if (adjustedColumn - page < 0 || adjustedColumn - page >= 9) {
                            slot.setEnabled(false);
                        }
                    }
                }

                this.possibleSlots += level;
                drawingColumn += numCols;
            }
        }

    }

    @Override
    public void setData(int id, int data) {
        super.setData(id, data);

        if (id == 0) {
            for (int i = 0; i < this.dogSlots.size(); i++) {
                DogInventorySlot slot = this.dogSlots.get(i);
                DogInventorySlot newSlot = new DogInventorySlot(slot, 8 + 18 * (slot.getOverallColumn() - data));
                this.replaceDogSlot(i, newSlot);
                int adjustedColumn = slot.getOverallColumn() - data;
                if (adjustedColumn < 0 || adjustedColumn >= 9) {
                    newSlot.setEnabled(false);
                }
            }
        }

    }

    private void addDogSlot(DogInventorySlot slotIn) {
        this.addSlot(slotIn);
        this.dogSlots.add(slotIn);
    }

    private void replaceDogSlot(int i, DogInventorySlot slotIn) {
        this.dogSlots.set(i, slotIn);
        // Work around to set Slot#slotNumber (MCP name) which is Slot#index in official
        // mappings. Needed because SlotItemHandler#index shadows the latter.
        Slot s = slotIn;
        this.slots.set(s.index, slotIn);
    }

    public int getTotalNumColumns() {
        return this.possibleSlots;
    }

    public int getPage() {
        return this.position.get();
    }

    public void setPage(int page) {
        this.position.set(page);
    }

    public List<DogInventorySlot> getSlots() {
        return this.dogSlots;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int i) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            int startIndex = this.slots.size() - this.dogSlots.size() + this.position.get() * 3;
            int endIndex = Math.min(startIndex + 9 * 3, this.slots.size());

            if (i >= this.slots.size() - this.dogSlots.size() && i < this.slots.size()) {
                if (!moveItemStackTo(itemstack1, 0, this.slots.size() - this.dogSlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (!moveItemStackTo(itemstack1, this.slots.size() - this.dogSlots.size(), this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
        }

        return itemstack;
    }
}
