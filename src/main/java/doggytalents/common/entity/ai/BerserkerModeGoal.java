package doggytalents.common.entity.ai;

import doggytalents.api.feature.EnumMode;
import doggytalents.common.entity.DogEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.CreeperEntity;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import doggytalents.ChopinLogger;
import doggytalents.DoggyTalents;

public class BerserkerModeGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    private final DogEntity dog;

    public BerserkerModeGoal(DogEntity dogIn, Class<T> targetClassIn, boolean checkSight) {
        super(dogIn, targetClassIn, 10, checkSight, false, //Set the third param to 10 because it is the default param if not specified
            (target) -> dogIn.wantsToAttack(target, dogIn.getOwner()) //Add predicate whick use the DogEntity::alterations dependent DogEntity::wantsToAttack method to filter out the target
        );
        this.dog = dogIn;
        ChopinLogger.l("in BerserkerMode goal constructor. ");
        if (dogIn == null) ChopinLogger.l("OoOoOoO Nuuullll");
    }

    @Override
    public boolean canUse() {
        return this.dog.isMode(EnumMode.BERSERKER) && super.canUse();
    }   

    /*
    public void setNewPredicate(Predicate<LivingEntity> ep) {
        this.targetConditions.selector(ep);
        ChopinLogger.l("Target COndition set ! ");
    }
    */
}