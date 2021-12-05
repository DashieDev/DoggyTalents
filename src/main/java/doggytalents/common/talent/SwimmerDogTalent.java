package doggytalents.common.talent;

import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.MathHelper;

public class SwimmerDogTalent extends TalentInstance {

    SwimmerDogMovementController mv_ctrl;
    SwimmerPathNavigator navigator;
    //TODO add ai goal to follow owner through water
    //I think the owner of the alteration should be passed in the constructor and set as a field for
    //easier access, the alteration must have an owner, right ?

    public SwimmerDogTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    @Override
    public void livingTick(AbstractDogEntity dogIn) {
        if (this.level() >= 5 && dogIn.isVehicle() && dogIn.canBeControlledByRider()) {
            // canBeSteered checks entity is LivingEntity
            LivingEntity rider = (LivingEntity) dogIn.getControllingPassenger();
            if (rider.isInWater()) {
                rider.addEffect(new EffectInstance(Effects.NIGHT_VISION, 80, 1, true, false));
            }
        }
    }

    @Override
    public ActionResultType canBeRiddenInWater(AbstractDogEntity dogIn, Entity rider) {
        return this.level() >= 5 ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public ActionResultType canBreatheUnderwater(AbstractDogEntity dogIn) {
        return this.level() >= 5 ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public ActionResult<Integer> decreaseAirSupply(AbstractDogEntity dogIn, int air) {
        if (this.level() > 0 && dogIn.getRandom().nextInt(this.level() + 1) > 0) {
            return ActionResult.success(air);
        }

        return ActionResult.pass(air);
    }

    @Override
    public ActionResult<Integer> determineNextAir(AbstractDogEntity dogIn, int currentAir) {
        if (this.level() > 0) {
            return ActionResult.pass(currentAir + this.level());
        }

        return ActionResult.pass(currentAir);
    }

    @Override
    public ActionResult<MovementController> getMoveControl(AbstractDogEntity dogIn) {
        if (dogIn.isInWater()) {
            if (this.mv_ctrl == null) {
                this.mv_ctrl = new SwimmerDogMovementController(dogIn);
            }
            return ActionResult.success(this.mv_ctrl);
        }
        return ActionResult.pass(null);
    }

    @Override
    public ActionResultType canSwim(AbstractDogEntity dogIn) {
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResult<PathNavigator> getNavigation(AbstractDogEntity dogIn) {
        if (dogIn.isInWater()) {
            if (this.navigator == null) {
                this.navigator = new SwimmerPathNavigator(dogIn, dogIn.level);
            }
            return ActionResult.success(this.navigator);
        }
        return ActionResult.pass(null);
    }

    static class SwimmerDogMovementController extends MovementController {
        private final AbstractDogEntity dog;
  
        public SwimmerDogMovementController(AbstractDogEntity d) {
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
