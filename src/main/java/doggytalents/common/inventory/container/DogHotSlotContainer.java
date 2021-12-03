package doggytalents.common.inventory.container;

import java.util.ArrayList;
import java.util.List;

import doggytalents.ChopinLogger;
import doggytalents.DoggyContainerTypes;
import doggytalents.DoggyTalents;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.inventory.DogHotSlotItemHandler;
import doggytalents.common.inventory.PackPuppyItemHandler;
import doggytalents.common.inventory.container.slot.DogInventorySlot;
import doggytalents.common.talent.PackPuppyTalent;
import doggytalents.common.talent.ToolUtilizerTalent;
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
import net.minecraftforge.items.SlotItemHandler;

/**
 * @author ext DashieDev 
 */
public class DogHotSlotContainer extends Container {

    private World world;
    private PlayerEntity player;
    private DogEntity dog;
    private final List<SlotItemHandler> dogSlots = new ArrayList<>();
    private int possibleSlots = 0;

    //Server method
    public DogHotSlotContainer(int windowId, PlayerInventory playerInventory, DogEntity dog) {
        super(DoggyContainerTypes.DOG_HOTSLOT.get(), windowId);
        this.world = playerInventory.player.level;
        this.player = playerInventory.player;
        this.dog = dog;

        //For Owner
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        //End for owner

        this.addDogSlots();
    }
    
    public void addDogSlots() {

        DogHotSlotItemHandler hotSlot = this.dog.getTalent(DoggyTalents.TOOL_UTILIZER)
                .map((inst) -> inst.cast(ToolUtilizerTalent.class).inventory()).orElse(null);
        if (hotSlot == null) {
            return;
        }
        for (int col = 0; col < 3; col++) {
            SlotItemHandler slot = new SlotItemHandler(hotSlot, col, 14+col*18, 22);
            this.addHotSlot(slot);
        }

    }

    private void addHotSlot(SlotItemHandler slotIn) {
        this.addSlot(slotIn);
        this.dogSlots.add(slotIn);
    }

    public int getTotalNumColumns() {
        return this.possibleSlots;
    }


    public List<SlotItemHandler> getSlots() {
        return this.dogSlots;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int i) {
        ChopinLogger.l("in quickMoveStack");
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (i >= this.slots.size() - this.dogSlots.size() && i < this.slots.size()) {
                if (!moveItemStackTo(itemstack1, 0, this.slots.size() - this.dogSlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(itemstack1, this.slots.size() - this.dogSlots.size(), this.slots.size(), false)) {
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
