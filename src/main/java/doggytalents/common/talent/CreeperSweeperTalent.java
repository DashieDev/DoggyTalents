package doggytalents.common.talent;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.entity.ai.BerserkerModeGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundEvents;

public class CreeperSweeperTalent extends TalentInstance {

    private int cooldown;

    public CreeperSweeperTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    @Override
    public void init(AbstractDogEntity dogIn) {
        this.cooldown = dogIn.tickCount;
        if (!dogIn.level.isClientSide && this.level >= 5) {
            //dogIn.targetSelector.removeGoal(new BerserkerModeGoal<>(this, MonsterEntity.class, false));
            //((DogEntity) dogIn).getBerserkerGoal().setNewPredicate(null);
        }
    }
    //this.targetSelector.addGoal(6, new BerserkerModeGoal<>(this, MonsterEntity.class, false));

    @Override
    public void tick(AbstractDogEntity dogIn) {
        if (this.level() > 0) {
            int timeLeft = this.cooldown - dogIn.tickCount;

            if (timeLeft <= 0 && !dogIn.isInSittingPose()) {
                List<CreeperEntity> list = dogIn.level.getEntitiesOfClass(CreeperEntity.class, dogIn.getBoundingBox().inflate(this.level() * 5,this.level() * 2, this.level() * 5));

                if (!list.isEmpty()) {
                    dogIn.playSound(SoundEvents.WOLF_GROWL, dogIn.getSoundVolume(), (dogIn.getRandom().nextFloat() - dogIn.getRandom().nextFloat()) * 0.2F + 1.0F);
                    this.cooldown = dogIn.tickCount + 60 + dogIn.getRandom().nextInt(40);
                }
            }

            if (dogIn.getTarget() instanceof CreeperEntity) {
                CreeperEntity creeper = (CreeperEntity) dogIn.getTarget();
                creeper.setSwellDir(0);
            }
        }
    }

    @Override
    public ActionResultType canAttack(AbstractDogEntity dog, EntityType<?> entityType) {
        return entityType == EntityType.CREEPER && this.level() >= 5 ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public ActionResultType canAttack(AbstractDogEntity dog, LivingEntity entity) {
        return entity instanceof CreeperEntity && this.level() >= 5 ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public ActionResultType shouldAttackEntity(AbstractDogEntity dog, LivingEntity target, LivingEntity owner) {
        return target instanceof CreeperEntity && this.level() >= 5 ? ActionResultType.SUCCESS : ActionResultType.PASS;
     }
}
