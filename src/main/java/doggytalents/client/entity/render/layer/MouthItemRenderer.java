package doggytalents.client.entity.render.layer;

import com.mojang.blaze3d.matrix.MatrixStack;

import doggytalents.DoggyTalents;
import doggytalents.api.client.render.ITalentRenderer;
import doggytalents.api.registry.TalentInstance;
import doggytalents.client.entity.model.DogBackpackModel;
import doggytalents.client.entity.model.DogModel;
import doggytalents.client.entity.render.DogRenderer;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.lib.Resources;
import doggytalents.common.talent.ToolUtilizerTalent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.util.math.vector.Vector3f;

public class MouthItemRenderer implements ITalentRenderer<DogEntity> {

    public MouthItemRenderer() {
    }

    @Override
    public void render(LayerRenderer<DogEntity, EntityModel<DogEntity>> layer, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, DogEntity dogIn, TalentInstance inst, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!dogIn.hasBone()) {

            matrixStackIn.pushPose();
            DogModel<DogEntity> model = (DogModel<DogEntity>) layer.getParentModel();
            if (model.young) {
                // derived from AgeableModel head offset
                matrixStackIn.translate(0.0F, 5.0F / 16.0F, 2.0F / 16.0F);
            }

            model.head.translateAndRotate(matrixStackIn); //Make the matrix rotate according to the head

            matrixStackIn.translate(-0.025F, 0.125F, -0.32F);
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(45.0F));
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(90.0F));

            ToolUtilizerTalent t;
            if (inst instanceof ToolUtilizerTalent) {
                t = (ToolUtilizerTalent) inst;
                int sel_slot = t.getSelectedSlot();
                if ( 0 <= sel_slot && sel_slot < 3) 
                Minecraft.getInstance().getItemInHandRenderer().renderItem(dogIn, t.getSelectedStack() , ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            } 
            
            matrixStackIn.popPose();
        }
    }
}
