package doggytalents.client.screen;

import java.util.Optional;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import doggytalents.ChopinLogger;
import doggytalents.DoggyAccessories;
import doggytalents.api.registry.AccessoryInstance;
import doggytalents.client.screen.widget.SmallButton;
import doggytalents.common.entity.accessory.DyeableAccessory.DyeableAccessoryInstance;
import doggytalents.common.inventory.container.DogHotSlotContainer;
import doggytalents.common.inventory.container.DogInventoriesContainer;
import doggytalents.common.inventory.container.slot.DogInventorySlot;
import doggytalents.common.lib.Resources;
import doggytalents.common.network.PacketHandler;
import doggytalents.common.network.packet.data.DogInventoryPageData;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.SlotItemHandler;

public class DogHotSlotScreen extends ContainerScreen<DogHotSlotContainer> {


    public DogHotSlotScreen(DogHotSlotContainer packPuppy, PlayerInventory playerInventory, ITextComponent displayName) {
        super(packPuppy, playerInventory, displayName);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void renderBg(MatrixStack stack, float partialTicks, int xMouse, int yMouse) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(Resources.DOG_INVENTORY);
        int l = (this.width - this.imageWidth) / 2;
        int i1 = (this.height - this.imageHeight) / 2;
        this.blit(stack, l, i1, 0, 0, this.imageWidth, this.imageHeight);

        for (SlotItemHandler slot : this.getMenu().getSlots()) {
            if (!slot.isActive()) {
                continue;
            }
            RenderSystem.color3f(1, 1, 1);

            this.blit(stack, l + slot.x - 1, i1 + slot.y - 1, 197, 2, 18, 18);
        }
    }


}
