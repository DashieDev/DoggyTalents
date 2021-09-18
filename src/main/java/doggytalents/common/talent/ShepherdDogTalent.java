package doggytalents.common.talent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import doggytalents.DoggyItems;
import doggytalents.DoggyTalents;
import doggytalents.api.feature.DataKey;
import doggytalents.api.feature.EnumMode;
import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import doggytalents.common.util.EntityUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class ShepherdDogTalent extends TalentInstance {

    private static DataKey<EntityAIShepherdDog> SHEPHERD_AI = DataKey.make();

    public ShepherdDogTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    @Override
    public void init(AbstractDogEntity dogIn) {
        if (!dogIn.hasData(SHEPHERD_AI)) {
            EntityAIShepherdDog shepherdAI = new EntityAIShepherdDog(dogIn, 1.0D, 8F, entity -> !(entity instanceof TameableEntity));
            dogIn.goalSelector.addGoal(7, shepherdAI);
            dogIn.setData(SHEPHERD_AI, shepherdAI);
        }
    }

    public static int getMaxFollowers(int level) {
        switch(level) {
        case 1:
            return 1;
        case 2:
            return 2;
        case 3:
            return 4;
        case 4:
            return 8;
        case 5:
            return 16;
        default:
            return 0;
        }
    }

    public static class EntityAIShepherdDog extends Goal {

        protected final AbstractDogEntity dog;
        private final World world;
        private final double followSpeed;
        private final float maxDist;
        private final PathNavigator dogPathfinder;
        private final Predicate<ItemStack> holdingPred;
        private final Predicate<AnimalEntity> predicate;
        private final Comparator<Entity> sorter;


        private int timeToRecalcPath;
        private LivingEntity owner;
        protected List<AnimalEntity> targets;
        private float oldWaterCost;

        private int MAX_FOLLOW = 5;

        public EntityAIShepherdDog(AbstractDogEntity dogIn, double speedIn, float range, @Nullable Predicate<AnimalEntity> targetSelector) {
            this.dog = dogIn;
            this.world = dogIn.level;
            this.dogPathfinder = dogIn.getNavigation();
            this.followSpeed = speedIn;
            this.maxDist = range;
            this.predicate = (entity) -> {
                double d0 = EntityUtil.getFollowRange(this.dog);
                if (entity.isInvisible()) {
                    return false;
                }
                else if (targetSelector != null && !targetSelector.test(entity)) {
                    return false;
                } else {
                    return entity.distanceTo(this.dog) > d0 ? false : entity.canSee(this.dog);
                }
            };
            this.holdingPred = (stack) -> {
                return stack.getItem() == DoggyItems.WHISTLE.get(); // TODO
            };

            this.sorter = new EntityUtil.Sorter(dogIn);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.dog.getMode() != EnumMode.DOCILE) {
                return false;
            } else if (this.dog.getLevel(DoggyTalents.SHEPHERD_DOG) <= 0) {
                return false;
            } else {
                LivingEntity owner = this.dog.getOwner();
                if (owner == null) {
                   return false;
                } else if (owner instanceof PlayerEntity && ((PlayerEntity) owner).isSpectator()) {
                    return false;
                } else if (!EntityUtil.isHolding(owner, DoggyItems.WHISTLE.get(), (nbt) -> nbt.contains("mode") && nbt.getInt("mode") == 4)) {
                    return false;
                } else {
                    List<AnimalEntity> list = this.world.getEntitiesOfClass(AnimalEntity.class, this.dog.getBoundingBox().inflate(12D, 4.0D, 12D), this.predicate);
                    Collections.sort(list, this.sorter);
                    if (list.isEmpty()) {
                        return false;
                    }
                    else {



                        this.MAX_FOLLOW = ShepherdDogTalent.getMaxFollowers(this.dog.getLevel(DoggyTalents.SHEPHERD_DOG));
                        this.targets = list.subList(0, Math.min(MAX_FOLLOW, list.size()));
                        this.owner = owner;
                        return true;
                    }
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (this.dog.getMode() != EnumMode.DOCILE) {
                return false;
            } else if (this.dog.getLevel(DoggyTalents.SHEPHERD_DOG) <= 0) {
                return false;
            } else if (!EntityUtil.isHolding(owner, DoggyItems.WHISTLE.get(), (nbt) -> nbt.contains("mode") && nbt.getInt("mode") == 4)) {
                return false;
            } else if (this.targets.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.dog.getPathfindingMalus(PathNodeType.WATER);
            this.dog.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        }

        @Override
        public void tick() {
            if (!this.dog.isInSittingPose()) {

                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = 10;

                    // Pick up more animals
                    if (this.targets.size() < MAX_FOLLOW) {
                        List<AnimalEntity> list = this.world.getEntitiesOfClass(AnimalEntity.class,
                                this.dog.getBoundingBox().inflate(16, 4.0D, 16), this.predicate);
                        list.removeAll(this.targets);
                        Collections.sort(list, this.sorter);

                        this.targets.addAll(list.subList(0, Math.min(MAX_FOLLOW - this.targets.size(), list.size())));
                    }

                    Collections.sort(this.targets, this.sorter);
                    boolean teleport = this.owner.distanceTo(this.targets.get(0)) > 16;

                    for (AnimalEntity target : this.targets) {
                        //target.goalSelector.addGoal(0, new );

                        double distanceAway = target.distanceTo(this.owner);
                        target.getLookControl().setLookAt(this.owner, 10.0F, target.getMaxHeadXRot());
                        if (teleport) {
                            if (!target.isLeashed() && !target.isPassenger()) {
                                EntityUtil.tryToTeleportNearEntity(target, target.getNavigation(), this.owner, 4);
                            }
                        }
                        else if (distanceAway >= 5) {
                            if (!target.getNavigation().moveTo(this.owner, 1.2D)) {
                                if (!target.isLeashed() && !target.isPassenger() && distanceAway >= 20) {
                                    EntityUtil.tryToTeleportNearEntity(target, target.getNavigation(), this.owner, 4);
                                }
                            }
                        }
                        else {
                            target.getNavigation().stop();
                        }
                    }

                    Vector3d vec = Vector3d.ZERO;

                    // Calculate average pos of targets
                    for (AnimalEntity target : this.targets) {
                        vec = vec.add(target.position());
                    }

                    vec = vec.scale(1D / this.targets.size());

                    double dPosX = vec.x - this.owner.getX();
                    double dPosZ = vec.z - this.owner.getZ();
                    double size = Math.sqrt(dPosX * dPosX + dPosZ * dPosZ);
                    double j3 = vec.x + dPosX / size * (2 + this.targets.size() / 16);
                    double k3 = vec.z + dPosZ / size * (2 + this.targets.size() / 16);

                    if (teleport) {
                        EntityUtil.tryToTeleportNearEntity(this.dog, this.dogPathfinder, new BlockPos(j3, this.dog.getY(), k3), 1);
                    }

                    this.dog.getLookControl().setLookAt(this.owner, 10.0F, this.dog.getMaxHeadXRot());
                    if (!this.dogPathfinder.moveTo(j3, this.owner.getBoundingBox().minY, k3, this.followSpeed)) {
                        if (this.dog.distanceToSqr(j3, this.owner.getBoundingBox().minY, k3) > 144D) {
                            if (!this.dog.isLeashed() && !this.dog.isPassenger()) {
                                EntityUtil.tryToTeleportNearEntity(this.dog, this.dogPathfinder, new BlockPos(j3, this.dog.getY(), k3), 4);
                            }
                        }
                    }

                    if (this.dog.distanceTo(this.owner) > 40) {
                        EntityUtil.tryToTeleportNearEntity(this.dog, this.dogPathfinder, this.owner, 2);
                    }
                    // Play woof sound
                    if (this.dog.getRandom().nextFloat() < 0.15F) {
                        this.dog.playSound(SoundEvents.WOLF_AMBIENT, this.dog.getSoundVolume() + 1.0F, (this.dog.getRandom().nextFloat() - this.dog.getRandom().nextFloat()) * 0.1F + 0.9F);
                    }

                    // Remove dead or faraway entities
                    List<AnimalEntity> toRemove = new ArrayList<>();
                    for (AnimalEntity target : this.targets) {
                        if (!target.isAlive() || target.distanceTo(this.dog) > 25D)
                            toRemove.add(target);
                    }
                    this.targets.removeAll(toRemove);
                }
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            for (AnimalEntity target : this.targets) {
                target.getNavigation().stop();
            }
            this.dogPathfinder.stop();
            this.dog.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
        }
    }
}
