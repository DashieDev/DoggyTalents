package doggytalents.common.item;

import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.inferface.IDogItem;
import doggytalents.common.lib.Constants;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ChangeOwnerItem extends Item implements IDogItem {

    public ChangeOwnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType processInteract(AbstractDogEntity dogIn, World worldIn, PlayerEntity playerIn, Hand handIn) {
        if(!dogIn.isOwner(playerIn)) {

            if(!worldIn.isRemote) {
                dogIn.setTamedBy(playerIn);
                dogIn.getNavigator().clearPath();
                dogIn.setAttackTarget((LivingEntity) null);
                dogIn.getAISit().setSitting(true);
                dogIn.world.setEntityState(dogIn, Constants.EntityState.WOLF_HEARTS);
            }

            return ActionResultType.SUCCESS;
        }

        return ActionResultType.CONSUME;
    }
}
