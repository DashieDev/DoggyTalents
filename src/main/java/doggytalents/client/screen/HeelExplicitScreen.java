package doggytalents.client.screen;

import java.io.IOException;
import java.time.chrono.ThaiBuddhistDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import doggytalents.ChopinLogger;
import doggytalents.DoggyAccessories;
import doggytalents.DoggyTalents2;
import doggytalents.api.DoggyTalentsAPI;
import doggytalents.api.feature.DogLevel.Type;
import doggytalents.api.feature.EnumMode;
import doggytalents.api.registry.Talent;
import doggytalents.client.DogTextureManager;
import doggytalents.common.config.ConfigValues;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.entity.stats.StatsTracker;
import doggytalents.common.network.PacketHandler;
import doggytalents.common.network.packet.data.DogData;
import doggytalents.common.network.packet.data.DogModeData;
import doggytalents.common.network.packet.data.DogNameData;
import doggytalents.common.network.packet.data.DogObeyData;
import doggytalents.common.network.packet.data.DogTalentData;
import doggytalents.common.network.packet.data.DogTextureData;
import doggytalents.common.network.packet.data.FriendlyFireData;
import doggytalents.common.network.packet.data.RequestHeelData;
import doggytalents.common.network.packet.data.SendSkinData;
import doggytalents.common.network.packet.data.StatsRequestData;
import doggytalents.common.talent.RoaringGaleTalent;
import doggytalents.common.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.AnvilScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.SwordItem;

public class HeelExplicitScreen extends Screen {

   private Rectangle2d rect;
   private DogEntity dog;
   private PlayerEntity player;
   private List<String> dogNameList;
   private List<Integer> dogIdList;
   private List<String> dogNameFilterList;
   private List<Integer> dogIdFilterList;
   private int hightlightDogName;
   private boolean showUuid = false;
   private String value = "";
   private String posstring = "_";
   private String curposString = "";
   
    private final int MAX_BUFFER_SIZE = 64;

    public HeelExplicitScreen(DogEntity dog, PlayerEntity player) {
        super(new TranslationTextComponent("doggytalents.screen.whistler.heelExplicit"));
        this.dog = dog;
        this.player = player;
        this.dogNameList = new ArrayList<String>(4);
        this.dogIdList = new ArrayList<Integer>(4);
        this.dogIdFilterList = new ArrayList<Integer>(4);
        this.dogNameFilterList = new ArrayList<String>(4);
        this.hightlightDogName = 0;
    }

    public static void open(DogEntity dog) { 
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new HeelExplicitScreen(dog, mc.player));
    }

    @Override
    public void init() {
        super.init();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.rect = new Rectangle2d(0, 0,500, 500);
        List<DogEntity> dogsList = this.minecraft.level.getEntitiesOfClass(DogEntity.class, this.player.getBoundingBox().inflate(100D, 50D, 100D), d -> d.isOwnedBy(player));
        for (DogEntity d : dogsList) {
            this.dogNameList.add(d.getName().getString());
            this.dogNameFilterList.add(d.getName().getString());
            this.dogIdList.add(d.getId());
            this.dogIdFilterList.add(d.getId());
        }
        Button showUuid = new Button(3, 3, 20, 20, new StringTextComponent("Show UUID"), (btn) -> {
            btn.setMessage(new StringTextComponent("Hide UUID"));
            this.showUuid = !this.showUuid;
        });
        this.addButton(showUuid);
    }


    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {

        int half_width = this.width >> 1;
        int half_height = this.height >> 1;
      
        AbstractGui.fill(stack, half_width - 100, half_height - 100, half_width + 100, half_height + 100, Integer.MIN_VALUE);
        AbstractGui.fill(stack, half_width - 100, half_height + 105, half_width + 100, half_height + 117, Integer.MIN_VALUE);

        super.render(stack, mouseX, mouseY, partialTicks);
        int offset = 0;
        int textx = half_width - 100 + 2;
        int texty = half_height - 100 + 2;
        for (int i = 0; i < this.dogNameFilterList.size(); ++i) {
            int color = 0xffffffff;
            if (i == this.hightlightDogName) color = 0xFF10F9;
            String text = this.dogNameFilterList.get(i) + (
                this.showUuid ? 
                " ( " + this.minecraft.level.getEntity(this.dogIdFilterList.get(i)).getStringUUID() + " ) " :
                ""
            );
            this.font.draw(stack, text, textx, texty + offset, color);
            offset+=10;
        }

        int txtorgx = half_width - 90;
        int txtorgy = half_height + 107;
        
        if (this.player.tickCount %10 == 0) {
            if (this.curposString == this.posstring) this.curposString = "";
            else this.curposString = this.posstring;
        }
        this.font.draw(stack, this.value + this.curposString, txtorgx, txtorgy,  0xffffffff);
         
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputMappings.Input mouseKey = InputMappings.getKey(keyCode, scanCode);
        ChopinLogger.l("" + keyCode);
        if (keyCode == 264) {
            this.hightlightDogName = MathHelper.clamp(this.hightlightDogName +1, 0, this.dogNameFilterList.size());
        } else if (keyCode == 265) {
            this.hightlightDogName = MathHelper.clamp(this.hightlightDogName -1, 0, this.dogNameFilterList.size());
        } else if (keyCode == 257) {
            this.requestHeel(this.dogIdFilterList.get(this.hightlightDogName));
            this.minecraft.setScreen(null);
        } // TODO backspace detect

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char code, int p_231042_2_) {
        if (SharedConstants.isAllowedChatCharacter(code)) {
            this.insertText(Character.toString(code));
            this.updateFilter();
            return true;
        } else {
            return false;
        }
        
    }

    private void updateFilter() {
        this.dogNameFilterList.clear();
        this.dogIdFilterList.clear();
        this.hightlightDogName =0;

        if (this.value == "") {
            for (String i : this.dogNameList) {
                this.dogNameFilterList.add(i);
            }
            for (Integer i : this.dogIdList) {
                this.dogIdFilterList.add(i);
            }
        } else {
            for (int i = 0; i < this.dogIdList.size(); ++i) {
                ChopinLogger.l(this.dogNameList.get(i) + " contains " + this.value); 
                if (this.dogNameList.get(i).length() < this.value.length()) continue; 
                if (this.dogNameList.get(i).contains(this.value)) {
                    this.dogIdFilterList.add(this.dogIdList.get(i));
                    this.dogNameFilterList.add(this.dogNameList.get(i));
                }
            }
        }
    } 

    private void insertText(String x) {
        if (this.value.length() < MAX_BUFFER_SIZE) {
            this.value = this.value + x;
        }
    }
    
    @Override
    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void requestHeel(int id) {
        PacketHandler.send(PacketDistributor.SERVER.noArg(), new RequestHeelData(id));
    }

}