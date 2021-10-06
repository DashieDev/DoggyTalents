package doggytalents.client.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;

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
import doggytalents.common.network.packet.data.SendSkinData;
import doggytalents.common.network.packet.data.StatsRequestData;
import doggytalents.common.talent.RoaringGaleTalent;
import doggytalents.common.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

public class DogInfoScreen extends Screen {

    public final DogEntity dog;
    public final StatsTracker dogStat;
    public final PlayerEntity player;

    private int currentPage = 0;
    private int maxPages = 1;
    private List<Widget> talentWidgets = new ArrayList<>(16);

    private Button leftBtn, rightBtn;
    private Button PrevPage, NextPage;

    private List<Talent> talentList;
    private List<ResourceLocation> customSkinList;

    public int textureIndex;

    public DogInfoScreen(DogEntity dog, PlayerEntity player, StatsTracker dogStat) {
        super(new TranslationTextComponent("doggytalents.screen.dog.title"));
        this.dog = dog;
        this.player = player;
        this.dogStat = dogStat; 
        this.talentList = DoggyTalentsAPI.TALENTS
                .getValues()
                .stream()
                .sorted(Comparator.comparing((t) -> I18n.get(t.getTranslationKey())))
                .collect(Collectors.toList());

        this.customSkinList = DogTextureManager.INSTANCE.getAll();
        this.textureIndex = this.customSkinList.indexOf(DogTextureManager.INSTANCE.getTextureLoc(dog.getSkinHash()));
        this.textureIndex = this.textureIndex >= 0 ? this.textureIndex : 0;
    }

    public static void open(DogEntity dog, StatsTracker statsTracker) {
        ChopinLogger.LOGGER.info(dog.getName().getString() + " 's roar cooldown from client: " + Integer.toString( (Integer) dog.getDataOrDefault(RoaringGaleTalent.COOLDOWN, 0) )); 
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new DogInfoScreen(dog, mc.player, statsTracker));
    }

    @Override
    public void init() {
        super.init();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        int topX = this.width / 2;
        int topY = this.height / 2;

        TextFieldWidget nameTextField = new TextFieldWidget(this.font, topX - 100, topY + 50, 200, 20,  new TranslationTextComponent("dogInfo.enterName"));
        nameTextField.setResponder(text ->  {
            PacketHandler.send(PacketDistributor.SERVER.noArg(), new DogNameData(DogInfoScreen.this.dog.getId(), text));
        });
        nameTextField.setFocus(false);
        nameTextField.setMaxLength(128);

        if (this.dog.hasCustomName()) {
            nameTextField.setValue(this.dog.getCustomName().getContents());
        }

        this.addButton(nameTextField);



        if (this.dog.isOwnedBy(this.player)) {
            Button obeyBtn = new Button(this.width - 64, topY + 77, 42, 20, new StringTextComponent(String.valueOf(this.dog.willObeyOthers())), (btn) -> {
                btn.setMessage(new StringTextComponent(String.valueOf(!this.dog.willObeyOthers())));
                PacketHandler.send(PacketDistributor.SERVER.noArg(), new DogObeyData(this.dog.getId(), !this.dog.willObeyOthers()));
            });

            this.addButton(obeyBtn);
        }

        Button attackPlayerBtn = new Button(this.width - 64, topY - 5, 42, 20, new StringTextComponent(String.valueOf(this.dog.canPlayersAttack())), button -> {
            button.setMessage(new StringTextComponent(String.valueOf(!this.dog.canPlayersAttack())));
            PacketHandler.send(PacketDistributor.SERVER.noArg(), new FriendlyFireData(this.dog.getId(), !this.dog.canPlayersAttack()));
        });

        this.addButton(attackPlayerBtn);

        if (ConfigValues.USE_DT_TEXTURES) {
            Button addBtn = new Button(this.width - 42, topY + 30, 20, 20, new StringTextComponent("+"), (btn) -> {
                this.textureIndex += 1;
                this.textureIndex %= this.customSkinList.size();
                ResourceLocation rl = this.customSkinList.get(this.textureIndex);

                this.setDogTexture(rl);
            });
            Button lessBtn = new Button(this.width - 64, topY + 30, 20, 20, new StringTextComponent("-"), (btn) -> {
                this.textureIndex += this.customSkinList.size() - 1;
                this.textureIndex %= this.customSkinList.size();
                ResourceLocation rl = this.customSkinList.get(this.textureIndex);
                this.setDogTexture(rl);
            });

            this.addButton(addBtn);
            this.addButton(lessBtn);
        }


        Button modeBtn = new Button(topX + 40, topY + 25, 60, 20, new TranslationTextComponent(this.dog.getMode().getUnlocalisedName()), button -> {
            EnumMode mode = DogInfoScreen.this.dog.getMode().nextMode();

            if (mode == EnumMode.WANDERING && !DogInfoScreen.this.dog.getBowlPos().isPresent()) {
                button.setMessage(new TranslationTextComponent(mode.getUnlocalisedName()).withStyle(TextFormatting.RED));
            } else {
                button.setMessage(new TranslationTextComponent(mode.getUnlocalisedName()));
            }
            //dog.getName();

            PacketHandler.send(PacketDistributor.SERVER.noArg(), new DogModeData(DogInfoScreen.this.dog.getId(), mode));
        }) {
            @Override
            public void renderToolTip(MatrixStack stack, int mouseX, int mouseY) {
                List<ITextComponent> list = new ArrayList<>();
                String str = I18n.get(dog.getMode().getUnlocalisedInfo());
                list.addAll(ScreenUtil.splitInto(str, 150, DogInfoScreen.this.font));
                if (DogInfoScreen.this.dog.getMode() == EnumMode.WANDERING) {


                    if (DogInfoScreen.this.dog.getBowlPos().isPresent()) {
                        double distance = DogInfoScreen.this.dog.blockPosition().distSqr(DogInfoScreen.this.dog.getBowlPos().get());

                        if (distance > 256D) {
                            list.add(new TranslationTextComponent("dog.mode.docile.distance", (int) Math.sqrt(distance)).withStyle(TextFormatting.RED));
                        } else {
                            list.add(new TranslationTextComponent("dog.mode.docile.bowl", (int) Math.sqrt(distance)).withStyle(TextFormatting.GREEN));
                        }
                    } else {
                        list.add(new TranslationTextComponent("dog.mode.docile.nobowl").withStyle(TextFormatting.RED));
                    }
                }

                DogInfoScreen.this.renderComponentTooltip(stack, list, mouseX, mouseY);
            }
        };

        this.addButton(modeBtn);

        // Talent level-up buttons
        int size = DoggyTalentsAPI.TALENTS.getKeys().size();
        int perPage = Math.max(MathHelper.floor((this.height - 10) / (double) 21) - 2, 1);
        this.currentPage = 0;
        this.recalculatePage(perPage);


        if (perPage < size) {
            this.leftBtn = new Button(25, perPage * 21 + 10, 20, 20, new StringTextComponent("<"), (btn) -> {
                this.currentPage = Math.max(0, this.currentPage - 1);
                btn.active = this.currentPage > 0;
                this.rightBtn.active = true;
                this.recalculatePage(perPage);
            }) {
                @Override
                public void renderToolTip(MatrixStack stack, int mouseX, int mouseY) {
                    DogInfoScreen.this.renderTooltip(stack, new TranslationTextComponent("doggui.prevpage").withStyle(TextFormatting.ITALIC), mouseX, mouseY);
                }
            };
            this.leftBtn.active = false;

            this.rightBtn = new Button(48, perPage * 21 + 10, 20, 20, new StringTextComponent(">"), (btn) -> {
                this.currentPage = Math.min(this.maxPages - 1, this.currentPage + 1);
                btn.active = this.currentPage < this.maxPages - 1;
                this.leftBtn.active = true;
                this.recalculatePage(perPage);
            }) {
                @Override
                public void renderToolTip(MatrixStack stack, int mouseX, int mouseY) {
                    DogInfoScreen.this.renderTooltip(stack, new TranslationTextComponent("doggui.nextpage").withStyle(TextFormatting.ITALIC), mouseX, mouseY);
                }
            };

            this.addButton(this.leftBtn);
            this.addButton(this.rightBtn);
        }
        this.PrevPage = new Button(2, this.height-22, 20, 20, new StringTextComponent("<"), btn -> {   }  );
        this.NextPage = new Button(this.width-22, this.height-22, 20, 20, new StringTextComponent(">"), 
            btn -> {
                PacketHandler.send(PacketDistributor.SERVER.noArg(), new StatsRequestData(dog.getId()));
                Minecraft.getInstance().setScreen(new DogStatsScreen(this.dog, this.player, this.dogStat)); 
            }    
        );

        this.addButton(this.PrevPage);
        this.addButton(this.NextPage);
    }

    private void setDogTexture(ResourceLocation rl) {
        if (ConfigValues.SEND_SKIN) {
            try {
                byte[] data = DogTextureManager.INSTANCE.getResourceBytes(rl);
                PacketHandler.send(PacketDistributor.SERVER.noArg(), new SendSkinData(this.dog.getId(), data));
            } catch (IOException e) {
                DoggyTalents2.LOGGER.error("Was unable to get resource data for {}, {}", rl, e);
            }
        } else {
            PacketHandler.send(PacketDistributor.SERVER.noArg(), new DogTextureData(this.dog.getId(), DogTextureManager.INSTANCE.getTextureHash(rl)));
        }
    }

    private void recalculatePage(int perPage) {
        this.talentWidgets.forEach(this::removeWidget);
        this.talentWidgets.clear();

        this.maxPages = MathHelper.ceil(this.talentList.size() / (double) perPage);

        for (int i = 0; i < perPage; ++i) {

            int index = this.currentPage * perPage + i;
            if (index >= this.talentList.size()) break;
            Talent talent = this.talentList.get(index);

            Button button = new TalentButton(25, 10 + i * 21, 20, 20, new StringTextComponent("+"), talent, (btn) -> {
                int level = DogInfoScreen.this.dog.getLevel(talent);
                if (level < talent.getMaxLevel() && DogInfoScreen.this.dog.canSpendPoints(talent.getLevelCost(level + 1))) {
                    PacketHandler.send(PacketDistributor.SERVER.noArg(), new DogTalentData(DogInfoScreen.this.dog.getId(), talent));
                }

            }) {
                @Override
                public void renderToolTip(MatrixStack stack, int mouseX, int mouseY) {
                    List<ITextComponent> list = new ArrayList<>();

                    list.add(new TranslationTextComponent(talent.getTranslationKey()).withStyle(TextFormatting.GREEN));
                    if (this.active) {
                        list.add(new StringTextComponent("Level: " + DogInfoScreen.this.dog.getLevel(talent)));
                        list.add(new StringTextComponent("--------------------------------").withStyle(TextFormatting.GRAY));
                        list.addAll(ScreenUtil.splitInto(I18n.get(talent.getInfoTranslationKey()), 200, DogInfoScreen.this.font));
                    } else {
                        list.add(new StringTextComponent("Talent disabled").withStyle(TextFormatting.RED));
                    }

                    DogInfoScreen.this.renderComponentTooltip(stack, list, mouseX, mouseY);
                }
            };
            button.active = !ConfigValues.DISABLED_TALENTS.contains(talent);

            this.talentWidgets.add(button);
            this.addButton(button);
        }
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        //Background
        int topX = this.width / 2;
        int topY = this.height / 2;
        this.renderBackground(stack);

        // Background
        String health = Util.format1DP(this.dog.getHealth());
        String healthMax = Util.format1DP(this.dog.getMaxHealth());
        String speedValue = Util.format2DP(this.dog.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
        String armorValue = Util.format2DP(this.dog.getAttribute(Attributes.ARMOR).getValue());
        String ageValue = Util.format2DP(this.dog.getAge());
        String ageRel = I18n.get(this.dog.isBaby() ? "doggui.age.baby" : "doggui.age.adult");

        String ageString = ageValue + " " + ageRel;

        String tamedString = "";
        if (this.dog.isTame()) {
            if (this.dog.isOwnedBy(this.player)) {
                tamedString = I18n.get("doggui.owner.you");
            } else if (this.dog.getOwnersName().isPresent()) {
                tamedString = this.dog.getOwnersName().get().getString();
            }
        }

        //this.font.drawString(I18n.format("doggui.health") + healthState, this.width - 160, topY - 110, 0xFFFFFF);
        this.renderHealthBar(stack, this.dog, this.width - 160, topY - 110); 
        this.font.draw(stack, I18n.get("doggui.speed") + " " + speedValue, this.width - 160, topY - 100, 0xFFFFFF);
        this.font.draw(stack, I18n.get("doggui.owner") + " " + tamedString, this.width - 160, topY - 90, 0xFFFFFF);
        this.font.draw(stack, I18n.get("doggui.age") + " " + ageString, this.width - 160, topY - 80, 0xFFFFFF);
        this.font.draw(stack, I18n.get("doggui.armor") + " " + armorValue, this.width - 160, topY - 70, 0xFFFFFF);
        if (ConfigValues.DOG_GENDER) {
            this.font.draw(stack, I18n.get("doggui.gender") + " "+ I18n.get(this.dog.getGender().getUnlocalisedName()), this.width - 160, topY - 60, 0xFFFFFF);
        }
        this.font.draw(stack, "Xp : " + Integer.toString( this.dog.getDogExperiencePoint() ), this.width - 160, topY - 50, 0x07ad02);

        this.font.draw(stack, I18n.get("doggui.newname"), topX - 100, topY + 38, 4210752);
        this.font.draw(stack, I18n.get("doggui.level") + " " + this.dog.getLevel().getLevel(Type.NORMAL), topX - 65, topY + 75, 0xFF10F9);
        this.font.draw(stack, I18n.get("doggui.leveldire") + " " + this.dog.getLevel().getLevel(Type.DIRE), topX, topY + 75, 0xFF10F9);
        if (this.dog.getAccessory(DoggyAccessories.GOLDEN_COLLAR.get()).isPresent()) {
            this.font.draw(stack, TextFormatting.GOLD + "Unlimited Points", topX - 38, topY + 89, 0xFFFFFF); //TODO translation
        } else {
            this.font.draw(stack, I18n.get("doggui.pointsleft") + " " + this.dog.getSpendablePoints(), topX - 38, topY + 89, 0xFFFFFF);
        }
       // if (ConfigValues.USE_DT_TEXTURES) {
            this.font.draw(stack, I18n.get("doggui.textureindex"), this.width - 80, topY + 20, 0xFFFFFF);
            this.font.draw(stack, this.dog.getSkinHash().substring(0, Math.min(this.dog.getSkinHash().length(), 10)), this.width - 73, topY + 54, 0xFFFFFF);
       // }

        if (this.dog.isOwnedBy(this.player)) {
            this.font.draw(stack, I18n.get("doggui.obeyothers"), this.width - 76, topY + 67, 0xFFFFFF);
        }

        this.font.draw(stack, I18n.get("doggui.friendlyfire"), this.width - 76, topY - 15, 0xFFFFFF);


        this.buttons.forEach(widget -> {
            if (widget instanceof TalentButton) {
                TalentButton talBut = (TalentButton)widget;
                this.font.draw(stack, I18n.get(talBut.talent.getTranslationKey()), talBut.x + 25, talBut.y + 7, 0xFFFFFF);
            }
        });

        super.render(stack, mouseX, mouseY, partialTicks);
        //RenderHelper.disableStandardItemLighting(); // 1.14 enableGUIStandardItemLighting

        for (Widget widget : this.buttons) {
            if (widget.isHovered()) {
               widget.renderToolTip(stack, mouseX, mouseY);
               break;
            }
         }

       // RenderHelper.enableStandardItemLighting();
    }

    
    private Random random = new Random(); 
    private void renderHealthBar(MatrixStack stack, DogEntity dog, int ati, int atj) {
        this.random.setSeed((long)(dog.tickCount * 312871));
        int i = MathHelper.ceil(dog.getHealth());
        int i1 = ati;
        int k1 = atj;
        float f = (float)dog.getAttributeValue(Attributes.MAX_HEALTH);
        int l1 = MathHelper.ceil(dog.getAbsorptionAmount());
        int i2 = MathHelper.ceil((f + (float)l1) / 2.0F / 10.0F);
        int j2 = Math.max(10 - (i2 - 2), 3);
        int k2 = k1 - (i2 - 1) * j2 - 10;
        int l2 = k1 - 10;
        int i3 = l1;
        int j3 = dog.getArmorValue();
        int k3 = -1;
        if (dog.hasEffect(Effects.REGENERATION)) {
           k3 = dog.tickCount % MathHelper.ceil(f + 5.0F);
        }
        this.minecraft.getTextureManager().bind(GUI_ICONS_LOCATION);
        //this.minecraft.getProfiler().push("health");
        //not gonna display effect now becuz there is an client entity effect sync problem

         for(int l5 = MathHelper.ceil((f + (float)l1) / 2.0F) - 1; l5 >= 0; --l5) {
            int i6 = 16;
            if (dog.hasEffect(Effects.POISON)) {
               i6 += 36;
            } else if (dog.hasEffect(Effects.WITHER)) {
               i6 += 72;
            }

            int j4 = 0;

            int k4 = MathHelper.ceil((float)(l5 + 1) / 10.0F) - 1;
            int l4 = i1 + l5 % 10 * 8;
            int i5 = k1 - k4 * j2;
            if (i <= 4) {
                i5 += this.random.nextInt(2);
            }

            if (i3 <= 0 && l5 == k3) {
               i5 -= 2;
            }

            int j5 = 0;

            this.blit(stack, l4, i5, 16, 0, 9, 9);
            
            if (i3 > 0) {
               if (i3 == l1 && l1 % 2 == 1) {
                  this.blit(stack, l4, i5, i6 + 153, 9 * j5, 9, 9);
                  --i3;
               } else {
                  this.blit(stack, l4, i5, i6 + 144, 9 * j5, 9, 9);
                  i3 -= 2;
               }
            } else {
               if (l5 * 2 + 1 < i) {
                  this.blit(stack, l4, i5, i6 + 36, 9 * j5, 9, 9);
               } 
               
               if (l5 * 2 + 1 == i) {
                  this.blit(stack, l4, i5, i6 + 45, 9 * j5, 9, 9);
               }
            }
        
         }
         
         //this.minecraft.getProfiler().pop();
    }
    
    /*
    public void renderDoggyExperienceBar(MatrixStack p_238454_1_, int x, int y) {
        this.minecraft.getProfiler().push("expBar");
        this.minecraft.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
        int i = this.minecraft.player.getXpNeededForNextLevel();
        if (i > 0) {
           int j = 182;
           int k = (int)(this.minecraft.player.experienceProgress * 183.0F);
           this.blit(p_238454_1_, x, y, 0, 64, 182, 5);
           if (k > 0) {
              this.blit(p_238454_1_, x, y, 0, 69, k, 5);
           }
        }
  
        this.minecraft.getProfiler().pop();
        if (this.minecraft.player.experienceLevel > 0) {
           this.minecraft.getProfiler().push("expLevel");
           String s = "" + this.minecraft.player.experienceLevel;
           int i1 = (this.screenWidth - this.getFont().width(s)) / 2;
           int j1 = this.screenHeight - 31 - 4;
           this.getFont().draw(p_238454_1_, s, (float)(i1 + 1), (float)j1, 0);+
           this.getFont().draw(p_238454_1_, s, (float)(i1 - 1), (float)j1, 0);
           this.getFont().draw(p_238454_1_, s, (float)i1, (float)(j1 + 1), 0);
           this.getFont().draw(p_238454_1_, s, (float)i1, (float)(j1 - 1), 0);
           this.getFont().draw(p_238454_1_, s, (float)i1, (float)j1, 8453920);
           this.minecraft.getProfiler().pop();
        }
  
     }
     */

    @Override
    public void removed() {
        super.removed();
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected <T extends Widget> T removeWidget(T widgetIn) {
        this.buttons.remove(widgetIn);
        this.children.remove(widgetIn);
        return widgetIn;
    }

    private static class TalentButton extends Button {

        protected Talent talent;
        private TalentButton(int x, int y, int widthIn, int heightIn, ITextComponent buttonText, Talent talent, Consumer<TalentButton> onPress) {
            super(x, y, widthIn, heightIn, buttonText, button -> onPress.accept((TalentButton) button));
            this.talent = talent;
        }

    }

    private class DogStatsScreen extends Screen {
        private Button PrevPage, NextPage;
        private DogEntity dog; 
        private PlayerEntity player;
        private StatsTracker dogStat;
        public DogStatsScreen(DogEntity dog, PlayerEntity player, StatsTracker statsTracker) {
            super(new TranslationTextComponent("doggytalents.screen.dog.stats.title"));
            this.dog = dog; 
            this.player = player; 
            this.dogStat = statsTracker;
        }
        @Override 
        public void init() {
            this.PrevPage = new Button(2, this.height-22, 20, 20, new StringTextComponent("<"), 
                btn -> Minecraft.getInstance().setScreen(new DogInfoScreen(this.dog, this.player, this.dogStat) )
            );
            this.NextPage = new Button(this.width-22, this.height-22, 20, 20, new StringTextComponent(">"), 
                btn -> Minecraft.getInstance().setScreen(new DogAmourEquipScreen(this.dog, this.player, this.dogStat) )
            );

            this.addButton(this.PrevPage);
            this.addButton(this.NextPage);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
            //Background
            this.renderBackground(stack);
            super.render(stack, mouseX, mouseY, partialTicks);
            StringTextComponent title_stats = new StringTextComponent("Statistics");
            
            title_stats.setStyle(
                Style.EMPTY
                .withBold(true)
                .withUnderlined(true)
            );
            
            this.font.draw(stack, title_stats , 2, 2, 0xFF10F9);

            /*
                private float damageDealt = 0;
                private int distanceOnWater = 0;
                private int distanceInWater = 0;
                private int distanceSprinting = 0;
                private int distanceSneaking = 0;
                private int distanceWalking = 0;
                private int distanceRidden = 0;
            */
            
            this.font.draw(stack, "Damage dealt : " + Float.toString(
                this.dog.statsTracker.getDamageDealt()
            ), 2, 13, 0xFFFFFF );
            try {
            this.font.draw(stack, "Distance on water : " + Integer.toString(
                this.dogStat.getDistanceOnWater()
            ), 2, 24, 0xFFFFFF );
            this.font.draw(stack, "Distance in water : " + Integer.toString(
                this.dogStat.getDistanceInWater()
            ), 2, 35, 0xFFFFFF );
            this.font.draw(stack, "Distance ridden : " + Integer.toString(
                this.dogStat.getDistanceRidden()
            ), 2, 46, 0xFFFFFF );
            this.font.draw(stack, "Distance walking : " + Integer.toString(
                this.dogStat.getDistanceWalking()
            ), 2, 57, 0xFFFFFF );
            this.font.draw(stack, "Death counts : " + Integer.toString(
                this.dogStat.getDeathCounts()
            ), 2, 68, 0xff0000 );
            } catch (Exception e) {
                ChopinLogger.LOGGER.info(e.toString()); 
            }
            /*
            this.font.draw(stack, "Damage dealt : " + Float.toString(
                this.dog.statsTracker.getDamageDealt()
            ), 2, 68, 0xFFFFFF );
            this.font.draw(stack, "Damage dealt : " + Float.toString(
                this.dog.statsTracker.getDamageDealt()
            ), 2, 79, 0xFFFFFF );
            */
        }
    }

    private class DogAmourEquipScreen extends Screen {
        private Button PrevPage, NextPage;
        private DogEntity dog; 
        private PlayerEntity player;
        private StatsTracker dogStat;
        public DogAmourEquipScreen(DogEntity dog, PlayerEntity player, StatsTracker statsTracker) {
            super(new TranslationTextComponent("doggytalents.screen.dog.armor.title"));
            this.dog = dog; 
            this.player = player; 
            this.dogStat = statsTracker;
        }
        @Override 
        public void init() {
            this.PrevPage = new Button(2, this.height-22, 20, 20, new StringTextComponent("<"), 
                btn -> Minecraft.getInstance().setScreen(new DogInfoScreen(this.dog, this.player, this.dogStat) )
            );
            this.NextPage = new Button(this.width-22, this.height-22, 20, 20, new StringTextComponent(">"), 
                btn -> {} 
            );

            this.addButton(this.PrevPage);
            this.addButton(this.NextPage);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
            //Background
            this.renderBackground(stack);
            super.render(stack, mouseX, mouseY, partialTicks);
            InventoryScreen.renderEntityInInventory(this.width/2, this.height/2, 100, this.width/2 - mouseX , this.height/2 - mouseY, this.dog);
        }
    }
}
