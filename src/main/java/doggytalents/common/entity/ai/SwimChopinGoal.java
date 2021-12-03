package doggytalents.common.entity.ai;

import java.util.EnumSet;

import doggytalents.common.entity.DogEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.MathHelper;

public class SwimChopinGoal extends Goal {
    private DogEntity dog;
    private LivingEntity owner;
    private PathNavigator old_nav;
    private int pathtimeout;
    private

    public SwimChopinGoal(DogEntity d) {
        this.dog = d;
        this.owner = d.getOwner();
        this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.dog.isInWater();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dog.isInWater();
    }

    @Override
    public void start() {
        this.old_nav = this.dog.getNavigation();
        this.dog.setNavigation(new SwimmerPathNavigator(this.dog, this.dog.level));
        
    }

    @Override
    public void tick() {
        if (this.owner != null) {
            if (--this.pathtimeout <= 0 && this.dog.distanceToSqr(this.owner) > 8) {
                this.dog.getNavigation().moveTo(this.owner, 1.2f);
                this.pathtimeout = 10;
            }
        }
    }

    @Override
    public void stop() {
        this.dog.setNavigation(this.old_nav);
    }

    static class ChopinSwimMovement extends MovementController {
        private final DogEntity dog;
  
        public ChopinSwimMovement(DogEntity d) {
           super(d);
           this.dog = d;
        }
  
        public void tick() {
           if (this.dog.isEyeInFluid(FluidTags.WATER)) {
              this.dog.setDeltaMovement(this.dog.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
           }
  
           if (this.operation == MovementController.Action.MOVE_TO && !this.dog.getNavigation().isDone()) {
              float f = (float)(this.speedModifier * this.dog.getAttributeValue(Attributes.MOVEMENT_SPEED));
              this.dog.setSpeed(MathHelper.lerp(0.125F, this.dog.getSpeed(), f));
              double d0 = this.wantedX - this.dog.getX();
              double d1 = this.wantedY - this.dog.getY();
              double d2 = this.wantedZ - this.dog.getZ();
              if (d1 != 0.0D) {
                 double d3 = (double)MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                 this.dog.setDeltaMovement(this.dog.getDeltaMovement().add(0.0D, (double)this.dog.getSpeed() * (d1 / d3) * 0.1D, 0.0D));
              }
  
              if (d0 != 0.0D || d2 != 0.0D) {
                 float f1 = (float)(MathHelper.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                 this.dog.yRot = this.rotlerp(this.dog.yRot, f1, 90.0F);
                 this.dog.yBodyRot = this.dog.yRot;
              }
  
           } else {
              this.dog.setSpeed(0.0F);
           }
        }
    }
}
