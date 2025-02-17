package doggytalents.common.entity.accessory;

import doggytalents.DoggyAccessories;
import doggytalents.DoggyAccessoryTypes;
import doggytalents.DoggyAttributes;
import doggytalents.DoggyTalents2;
import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.inferface.IDogAlteration;
import doggytalents.api.registry.Accessory;
import doggytalents.api.registry.AccessoryInstance;
import doggytalents.api.registry.AccessoryType;
import doggytalents.common.util.ColourCache;
import doggytalents.common.util.Util;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IItemProvider;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Supplier;

public class ArmourAccessory extends Accessory {

    public ArmourAccessory(Supplier<? extends AccessoryType> typeIn, Supplier<? extends IItemProvider> itemIn) {
        super(typeIn, itemIn);
    }

    public AccessoryInstance create(ItemStack armourStack) {
        if (armourStack.isEmpty()) {
            if (this.of(DoggyAccessories.IRON_HELMET)) {
                armourStack = new ItemStack(Items.IRON_HELMET);
            } else if (this.of(DoggyAccessories.DIAMOND_HELMET)) {
                armourStack = new ItemStack(Items.DIAMOND_HELMET);
            } else if (this.of(DoggyAccessories.GOLDEN_HELMET)) {
                armourStack = new ItemStack(Items.GOLDEN_HELMET);
            } else if (this.of(DoggyAccessories.CHAINMAIL_HELMET)) {
                armourStack = new ItemStack(Items.CHAINMAIL_HELMET);
            } else if (this.of(DoggyAccessories.TURTLE_HELMET)) {
                armourStack = new ItemStack(Items.TURTLE_HELMET);
            } else if (this.of(DoggyAccessories.NETHERITE_HELMET)) {
                armourStack = new ItemStack(Items.NETHERITE_HELMET);
            } else {
                throw new IllegalArgumentException();
            }
        }

        return new ArmourAccessory.Instance(armourStack.copy());
    }

    @Override
    public AccessoryInstance getDefault() {
        return new ArmourAccessory.Instance(ItemStack.EMPTY);
    }

    @Override
    public AccessoryInstance createFromStack(ItemStack stackIn) {
        return this.create(stackIn.copy());
    }

    @Override
    public AccessoryInstance createInstance(PacketBuffer buf) {
        return this.create(buf.readItem());
    }

    @Override
    public void write(AccessoryInstance instance, PacketBuffer buf) {
        ArmourAccessory.Instance exact = instance.cast(ArmourAccessory.Instance.class);
        buf.writeItem(exact.armourStack);
    }

    @Override
    public void write(AccessoryInstance instance, CompoundNBT compound) {
        ArmourAccessory.Instance exact = instance.cast(ArmourAccessory.Instance.class);
        CompoundNBT itemTag = new CompoundNBT();
        exact.armourStack.save(itemTag);
        compound.put("item", itemTag);
    }

    @Override
    public AccessoryInstance read(CompoundNBT compound) {
        return this.create(ItemStack.of(compound.getCompound("item")));
    }

    public class Instance extends AccessoryInstance implements IDogAlteration {

        @Nonnull
        protected final ItemStack armourStack;

        public Instance(ItemStack armourStack) {
            super(ArmourAccessory.this);
            this.armourStack = armourStack;
        }

        @Override
        public void init(AbstractDogEntity dogIn) {
            EquipmentSlotType slotType = null;

            if (this.armourStack.getItem() instanceof ArmorItem) {
                slotType = ((ArmorItem) this.armourStack.getItem()).getSlot();
            }

            dogIn.getAttributes().addTransientAttributeModifiers(this.armourStack.getAttributeModifiers(slotType));
        }

        @Override
        public void remove(AbstractDogEntity dogIn) {
            EquipmentSlotType slotType = null;

            if (this.armourStack.getItem() instanceof ArmorItem) {
                slotType = ((ArmorItem) this.armourStack.getItem()).getSlot();
            }

            dogIn.getAttributes().removeAttributeModifiers(this.armourStack.getAttributeModifiers(slotType));
        }

        @Override
        public AccessoryInstance copy() {
            return new ArmourAccessory.Instance(this.armourStack.copy());
        }

        public boolean hasEffect() {
            return this.armourStack.hasFoil();
        }

        @Override
        public ItemStack getReturnItem() {
            return this.armourStack;
        }
    }
}
