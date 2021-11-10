package doggytalents.common;

import doggytalents.ChopinLogger;
import doggytalents.common.inventory.DogHotSlotItemHandler;
import doggytalents.common.inventory.PackPuppyItemHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class Capabilities {

    public static void init() {
        CapabilityManager.INSTANCE.register(PackPuppyItemHandler.class, new Capability.IStorage<PackPuppyItemHandler>()  {

            @Override
            public INBT writeNBT(Capability<PackPuppyItemHandler> capability, PackPuppyItemHandler instance, Direction side)  {
                ListNBT nbtTagList = new ListNBT();
                int size = instance.getSlots();
                for (int i = 0; i < size; i++) {
                    ItemStack stack = instance.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        CompoundNBT itemTag = new CompoundNBT();
                        itemTag.putInt("Slot", i);
                        stack.save(itemTag);
                        nbtTagList.add(itemTag);
                    }
                }
                ChopinLogger.l("in cap::packpuppy::writeNBT");
                return nbtTagList;
            }

            @Override
            public void readNBT(Capability<PackPuppyItemHandler> capability, PackPuppyItemHandler instance, Direction side, INBT base)  {
                ListNBT tagList = (ListNBT) base;
                for (int i = 0; i < tagList.size(); i++) {
                    CompoundNBT itemTags = tagList.getCompound(i);
                    int j = itemTags.getInt("Slot");

                    if (j >= 0 && j < instance.getSlots())  {
                        instance.setStackInSlot(j, ItemStack.of(itemTags));
                    }
                }
                ChopinLogger.l("in cap::packpuppy::readNBT");
            }
        }, PackPuppyItemHandler::new);
        
        CapabilityManager.INSTANCE.register(DogHotSlotItemHandler.class,
             new Capability.IStorage<DogHotSlotItemHandler>() {
                @Override
                public INBT writeNBT(Capability<DogHotSlotItemHandler> capability, DogHotSlotItemHandler instance, Direction side)  {
                    return null;
                }

                @Override
                public void readNBT(Capability<DogHotSlotItemHandler> capability, DogHotSlotItemHandler instance, Direction side, INBT base)  { 
                }
             }, DogHotSlotItemHandler::new);
        
    }
}
