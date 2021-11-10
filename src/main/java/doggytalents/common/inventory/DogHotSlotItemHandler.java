package doggytalents.common.inventory;

import doggytalents.ChopinLogger;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

public class DogHotSlotItemHandler extends ItemStackHandler {

    public DogHotSlotItemHandler() {
        super(3);
    }

    @Override
    public CompoundNBT serializeNBT() {
        ListNBT itemsList = new ListNBT();

        for(int i = 0; i < this.stacks.size(); ++i) {
           ItemStack stack = this.stacks.get(i);
           if (!stack.isEmpty()) {
              CompoundNBT itemTag = new CompoundNBT();
              itemTag.putByte("Slot", (byte) i);
              stack.save(itemTag);
              itemsList.add(itemTag);
           }
        }

        CompoundNBT compound = new CompoundNBT();
        compound.put("hot_slots", itemsList);

        return compound;
    }

    @Override
    public void deserializeNBT(CompoundNBT compound) {

        if (compound.contains("hot_slots", Constants.NBT.TAG_LIST)) {
            ListNBT tagList = compound.getList("hot_slots", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.size(); i++) {
                CompoundNBT itemTag = tagList.getCompound(i);
                int slot = itemTag.getInt("Slot");

                if (slot >= 0 && slot < this.stacks.size()) {
                    this.stacks.set(slot, ItemStack.of(itemTag));
                }
            }
            this.onLoad();
        }
    }
    
    public int getItemSlotIfExitst(Item x) {
        for (int i = 0; i < this.stacks.size(); ++i) {
            if (this.stacks.get(i).getItem() == x) {
                return i; 
            }
        }
        return -1;
    }

    public int getItemSlotIfExitst(Class<? extends Item> x) {
        for (int i = 0; i < this.stacks.size(); ++i) {
            if (this.stacks.get(i).getItem().getClass() == x) {
                return i; 
            }
        }
        return -1;
    }
}
