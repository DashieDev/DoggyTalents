package doggytalents.common.entity.ai;

import java.util.EnumSet;
import java.util.UUID;

import doggytalents.ChopinLogger;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.util.EntityUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeMod;

public class SwimChopinGoal extends Goal {
    private DogEntity dog;
    private LivingEntity owner;
    private PathNavigator old_nav;
    private int pathtimeout;
    private MovementController old_mvctrl;
    private static final UUID SWIM_BOOST_ID = UUID.fromString("50671e42-1ded-4f97-9e2b-78bbeb1e8772");

    public SwimChopinGoal(DogEntity d) {
        this.dog = d;
        this.owner = d.getOwner();
        this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE, Goal.Flag.JUMP));
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
        this.old_mvctrl = this.dog.getMoveControl();
        this.dog.setMovementController(new ChopinSwimMovement(this.dog));
        ChopinLogger.lwn(this.dog, "chopin swimming starting"); 
        this.dog.setAttributeModifier(ForgeMod.SWIM_SPEED.get(), SWIM_BOOST_ID, (d, u) -> 
            new AttributeModifier(u, "Swim Boost", 3f, Operation.ADDITION)
        );
    }

    @Override
    public void tick() {
        if (this.owner != null) {
            this.dog.getLookControl().setLookAt(this.owner, 10.0F, this.dog.getMaxHeadXRot());

            if (--this.pathtimeout <= 0 && this.dog.distanceToSqr(this.owner) > 16) {
                this.pathtimeout = 10;
                if (this.dog.distanceToSqr(this.owner) > 144) {
                    EntityUtil.tryToTeleportNearEntityInWater(this.dog, this.dog.getNavigation(), this.owner.blockPosition(), 4);
                } else {
                    this.dog.getNavigation().moveTo(this.owner, 2f);
                }
            }
        }
    }

    @Override
    public void stop() {
        this.dog.setNavigation(this.old_nav);
        this.dog.setMovementController(this.old_mvctrl);
        this.dog.removeAttributeModifier(ForgeMod.SWIM_SPEED.get(), SWIM_BOOST_ID);
    }

    static class ChopinSwimMovement extends MovementController {
        private final DogEntity dog;
  
        public ChopinSwimMovement(DogEntity d) {
           super(d);
           this.dog = d;
        }
        
        @Override
        public void tick() {
           if (this.dog.isEyeInFluid(FluidTags.WATER)) {
              this.dog.setDeltaMovement(this.dog.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
           }
           
           //When dog has target pos and wants to move to it
           if (this.operation == MovementController.Action.MOVE_TO && !this.dog.getNavigation().isDone()) {

                //set the scale of the movement vector
                float s = (float)(this.speedModifier * this.dog.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.dog.setSpeed(MathHelper.lerp(0.125F, this.dog.getSpeed(), s));

                //set the direction of the movement vector
                double dx = this.wantedX - this.dog.getX();
                double dy = this.wantedY - this.dog.getY();
                double dz = this.wantedZ - this.dog.getZ();
                if (dy != 0.0D) {
                    double dl = (double)MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
                    this.dog.setDeltaMovement(this.dog.getDeltaMovement().add(0.0D, (double)this.dog.getSpeed() * (dy / dl) * 0.1D, 0.0D));
                }

                // Body&Head Rotation Processing 
                float f1 = (float)(MathHelper.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.dog.yRot = this.rotlerp(this.dog.yRot, f1, 90.0F);
                this.dog.yBodyRot = this.dog.yRot;
    
            } else {
                this.dog.setSpeed(0.0F);
                this.dog.setXxa(0.0F);
                this.dog.setZza(0.0F);
                this.dog.setYya(0.0F);
            }        
        }
    }
}
