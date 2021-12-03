package doggytalents.common.talent;

import doggytalents.ChopinLogger;
import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import doggytalents.common.Screens.DogHotSlotContainerProvider;
import doggytalents.common.inventory.DogHotSlotItemHandler;
import doggytalents.common.network.PacketHandler;
import doggytalents.common.network.packet.data.DogHotSlotScreenData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;


/**
 * @author DashieDev
 */
public class ToolUtilizerTalent extends TalentInstance {

    private DogHotSlotItemHandler hSlots;

    private byte selectSlot = 0;

    public ToolUtilizerTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
        this.hSlots = new DogHotSlotItemHandler();
        ChopinLogger.l("in toolutil constructor");
    }

    public DogHotSlotItemHandler inventory() {
        return this.hSlots;
    }

    @Override
    public ActionResultType processInteract(AbstractDogEntity dogIn, World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (
            worldIn.isClientSide 
            && dogIn.canInteract(playerIn)
            && playerIn.getItemInHand(handIn).getItem() == Items.STONE_PICKAXE
            && playerIn.isShiftKeyDown()
        ) {
            //Request open gui on client
            ChopinLogger.l("opening hot slot .......");
            PacketHandler.send(PacketDistributor.SERVER.noArg(), new DogHotSlotScreenData(dogIn.getId()));
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.PASS;
    }

    @Override
    public void writeToNBT(AbstractDogEntity dogIn, CompoundNBT compound) {
        super.writeToNBT(dogIn, compound);
        compound.merge(this.hSlots.serializeNBT());
    }

    @Override
    public void readFromNBT(AbstractDogEntity dogIn, CompoundNBT compound) {
        super.readFromNBT(dogIn, compound);
        this.hSlots.deserializeNBT(compound);
    }

    public byte getSelectedSlot() {
        return this.selectSlot;
    }

    public void setSelectedSlot(byte x) {
        this.selectSlot = x;
    }

    public ItemStack getSelectedStack() {
        return this.hSlots.getStackInSlot((int)this.selectSlot);
    }

    public boolean selectItemIfExist(Item x) {
        int a = this.hSlots.getItemSlotIfExitst(x);
        if (a >= 0) {
            this.setSelectedSlot((byte)a);
            return true;
        } else {
            return false;
        }
    }

    public boolean selectItemIfExist(Class<? extends Item> x) {
        int a = this.hSlots.getItemSlotIfExitst(x);
        if (a >= 0) {
            this.setSelectedSlot((byte)a);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasRenderer() {
        return true;
    }

}

