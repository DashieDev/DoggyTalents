package doggytalents.common.entity.ai;

import java.util.EnumSet;

import doggytalents.common.block.tileentity.DogBedTileEntity;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.lib.Constants;
import doggytalents.common.util.WorldUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

public class MoveToBlockGoal extends Goal {

    protected final DogEntity dog;

    public MoveToBlockGoal(DogEntity dogIn) {
        this.dog = dogIn;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.dog.getTargetBlock() != null && !this.dog.isOrderedToSit();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dog.isPathFinding() && !this.dog.getTargetBlock().closerThan(this.dog.position(), 0.5);
    }

    @Override
    public void stop() {
        BlockPos target = this.dog.getTargetBlock();

        DogBedTileEntity dogBedTileEntity = WorldUtil.getTileEntity(dog.level, target, DogBedTileEntity.class);

        if (dogBedTileEntity != null) {
            // Double check the bed still has no owner
            if (dogBedTileEntity.getOwnerUUID() == null) {
                dogBedTileEntity.setOwner(this.dog);
                this.dog.setBedPos(this.dog.level.dimension(), target);
            }
        }

        this.dog.setTargetBlock(null);
        this.dog.setOrderedToSit(true);

        this.dog.level.broadcastEntityEvent(this.dog, Constants.EntityState.WOLF_HEARTS);
    }

    @Override
    public void start() {
        BlockPos target = this.dog.getTargetBlock();
        this.dog.getNavigation().moveTo((target.getX()) + 0.5D, target.getY() + 1, (target.getZ()) + 0.5D, 1.0D);
    }

}
