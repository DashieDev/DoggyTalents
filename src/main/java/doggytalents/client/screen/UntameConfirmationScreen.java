package doggytalents.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import doggytalents.ChopinLogger;
import doggytalents.common.entity.DogEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.client.event.GuiScreenEvent.KeyboardCharTypedEvent;

public class UntameConfirmationScreen extends Screen {
    private final DogEntity dog;
    private final LivingEntity owner;

    public UntameConfirmationScreen (DogEntity dog, LivingEntity owner) {
        super(new StringTextComponent("untame_confirmation"));
        this.dog = dog;
        this.owner = owner;
    }
    
    @Override
    public void init() {
        StringTextComponent syb = new StringTextComponent("Confirm");
        syb.setStyle(
            Style.EMPTY
            .withColor(Color.fromRgb(16733525))
            .withBold(true)
        );
        Button yb = new Button(this.width/2 - 56, this.height/2 -10, 52, 20, syb
            , btn -> {this.confirmUntame();} );
        Button nb = new Button(this.width/2 + 4, this.height/2 -10, 52, 20, 
            new StringTextComponent("Cancel"), btn -> {this.minecraft.setScreen(null);} );
        this.addButton(yb);
        this.addButton(nb);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        RenderSystem.pushMatrix();
        RenderSystem.scalef(1.5F, 1.5F, 1.5F);
        this.font.draw(stack, "You are about to untame the dog !", 2, 2, 0xff0000);
        RenderSystem.popMatrix();
        this.font.draw(stack, "You have just clicked on the dog with the collar shear who ", 2, 22, 0xffffffff );
        this.font.draw(stack, "currently isn't wearing anything, this will untame", 2, 33, 0xffffffff );
        this.font.draw(stack, "the dog, proceed? ", 2, 44, 0xffffffff );
        super.render(stack, mouseX, mouseY, partialTicks);
    }

    public static void open(DogEntity dog) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new UntameConfirmationScreen(dog, dog.getOwner()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private void confirmUntame() {
        ChopinLogger.l("Confirmed");
        //TODO : implement untame request and handle
    }

}
