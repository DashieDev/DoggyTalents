package doggytalents.common.talent;


import java.util.EnumSet;
import java.util.List;

import doggytalents.ChopinLogger;
import doggytalents.DoggyTalents;
import doggytalents.api.enu.WetSource;
import doggytalents.api.feature.EnumMode;
import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import doggytalents.common.entity.DogEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** 
*    @author DashieDev
*/
public class WaterStorageTalent extends TalentInstance {

    //Consts
    private static final int STORE_WATER_INVTERVAL = 1;
    private static final int WATER_CONSUME = 20; // for each successful extinguish attempt
    private static final int COOLDOWN_INTERVAL = 60;
    private static final int SEARCH_RADIUS = 12;
    private static final int EFFECT_RADIUS_SQR = 4;
    private static final int MAX_WATER_STORAGE = 100;

    private static final int DECREASE_FACTOR_SIZE = 5; 
    private static final float[] decreaseFactorLevelMap = { 1.0f, 0.5f, 0.25f, 0.15f, 0.1f, 0.0f}; 

    public WaterStorageTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    private int WaterLeft = 0; //Server
    
    private int cooldownDeadline;

    //private int ActivationTick = 0;

    //private int BeginShakingTick = 0;

    private TryToGoNearDogOnFireAi dgoal;

    @Override
    public void init(AbstractDogEntity dogIn) {
        this.dgoal = new TryToGoNearDogOnFireAi(dogIn, this);
        dogIn.goalSelector.addGoal(4, dgoal);
    }

    @Override
    public void remove(AbstractDogEntity dogIn) {
        dogIn.goalSelector.removeGoal(this.dgoal);
    }

    @Override 
    public void livingTick(AbstractDogEntity dogIn) {

        //Absorb Water if in Water or Rain
        if(
            dogIn.tickCount%STORE_WATER_INVTERVAL == 0 
            && !dogIn.level.isClientSide 
            && dogIn.isInWaterOrRain()
        ) {
            if (WaterLeft < MAX_WATER_STORAGE) ++WaterLeft;
        }


    }

    

    @Override
    public ActionResultType processInteract(AbstractDogEntity dogIn, World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (stack.getItem() == Items.WATER_BUCKET) {

            this.WaterLeft = MAX_WATER_STORAGE;

            if (!playerIn.abilities.instabuild) playerIn.setItemInHand(handIn, new ItemStack(Items.BUCKET) );
            dogIn.playSound(SoundEvents.BUCKET_FILL, 1.0f, 1.0f);

            this.doClientSplash(dogIn);
            return ActionResultType.SUCCESS;
            
            /*
            ChopinLogger.l("in Water Storage Doggo");
            if ( !((DogEntity) dogIn).isDogWet() ) {
                ((DogEntity) dogIn).setDogWet(WetSource.of(true, false, false));
            }
            Vector3d vec3d = dogIn.getDeltaMovement();
            for (int j = 0; j < 49; ++j) {
                float f1 = (dogIn.getRandom().nextFloat() * 2.0F - 1.0F) * EFFECT_RADIUS;
                float f2 = (dogIn.getRandom().nextFloat() * 2.0F - 1.0F) * EFFECT_RADIUS;
                dogIn.level.addParticle(ParticleTypes.SPLASH, dogIn.getX() + f1, dogIn.getY() + 0.8F, dogIn.getZ() + f2, vec3d.x, vec3d.y, vec3d.z);
            }
            ChopinLogger.l("BIG SPLASH !!!!");
            ChopinLogger.l("Water currently absorbing : " + this.WaterLeft);
            return ActionResultType.SUCCESS;    
            */
            
        } 
        
        return ActionResultType.PASS;
    }

    private void extinguishTarget(LivingEntity e, int dlevel) {
        e.setRemainingFireTicks((int) (e.getRemainingFireTicks()* decreaseFactorLevelMap[dlevel]) );
        e.playSound( SoundEvents.FIRE_EXTINGUISH,  0.5F, 2.6F + e.getRandom().nextFloat() - e.getRandom().nextFloat() * 0.8F);
    }

    private void doClientSplash(LivingEntity e) {
        Vector3d vec3d = e.getDeltaMovement();
        for (int j = 0; j < 32; ++j) {
            float f1 = (e.getRandom().nextFloat() * 2.0F - 1.0F) * 2 ;
            float f2 = (e.getRandom().nextFloat() * 2.0F - 1.0F) * 2;
            e.level.addParticle(ParticleTypes.SPLASH, e.getX() + f1, e.getY() + 0.8F, e.getZ() + f2, vec3d.x, vec3d.y, vec3d.z);
        }
        ChopinLogger.l("Splash!");
    }

    private void doServerSplash(LivingEntity e) {
        if (!(e.level instanceof ServerWorld)) return;
        ((ServerWorld) e.level).sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(), e.getZ(), 32, e.getBbWidth() * 0.5F, 0.8f, e.getBbWidth() * 0.5F, 0.15);
    }

    @Override
    public void writeToNBT(AbstractDogEntity dogIn, CompoundNBT compound) {
        super.writeToNBT(dogIn, compound);
        compound.putInt("water_amount", this.WaterLeft);
    }

    @Override
    public void readFromNBT(AbstractDogEntity dogIn, CompoundNBT compound) {
        super.readFromNBT(dogIn, compound);
        this.WaterLeft = compound.getInt("water_amount");
    }

    public static class TryToGoNearDogOnFireAi extends Goal {

        
        protected final AbstractDogEntity dog;
        protected final WaterStorageTalent talentInst;
        protected List<LivingEntity> onfiretargets = null;
        protected LivingEntity nextfartarget = null; 

        protected int timetorecalcpath = 10;

        public TryToGoNearDogOnFireAi(AbstractDogEntity dogIn, WaterStorageTalent talentInst) {
            this.dog = dogIn;
            this.talentInst = talentInst;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.dog.isInSittingPose()) return false;
            if (this.dog.getMode() != EnumMode.DOCILE) return false;        
            if (talentInst.WaterLeft < WATER_CONSUME) return false;
            this.onfiretargets = this.getEntityOnFire();
            if (this.onfiretargets.isEmpty()) return false;

            
            return true;
        }

        @Override
        public boolean canContinueToUse() {

            if (this.dog.isInSittingPose()) return false;
            if (this.dog.getMode() != EnumMode.DOCILE) return false;
            if (talentInst.WaterLeft < WATER_CONSUME) return false;
            if (this.nextfartarget != null) return true;
            this.onfiretargets = this.getEntityOnFire();
            if (this.onfiretargets.isEmpty()) return false;


            return true;
        }

        @Override
        public void tick() {

            if (dog.tickCount > talentInst.cooldownDeadline && !((DogEntity)dog).isDogWet() ) {
                
                if (!this.onfiretargets.isEmpty()) { 
                    this.nextfartarget = null;
                    dog.getNavigation().stop();
                    LivingEntity mvto = null; float mndis = Float.MAX_VALUE;
                    boolean alrw = false; boolean only_far = true;
                    
                    for (LivingEntity e : this.onfiretargets) {
                        float distance = (float) dog.distanceToSqr(e);
                        if (distance > EFFECT_RADIUS_SQR) {
                            if (distance < mndis) {
                                mndis = distance; mvto = e;
                            }
                            continue;
                        } else if (!alrw){
                            ((DogEntity) dog).setDogWet(WetSource.of(true, false, false));
                            alrw = true;
                        }
                        only_far = false;
                        if (talentInst.WaterLeft < WATER_CONSUME) break;
                        talentInst.extinguishTarget(e, MathHelper.clamp(talentInst.level, 0, DECREASE_FACTOR_SIZE) );
                        
                        BlockPos x = e.blockPosition();
                        BlockPos.Mutable bp = new BlockPos.Mutable(x.getX(), x.getY(), x.getZ());
    
                        for (int i = -1; i < 2; ++i) {
                            for(int j = -1; j < 2; ++j) {
                                bp.setX(bp.getX() + i); bp.setZ(bp.getZ()+j);
                                if (e.level.getBlockState(bp).getBlock() == Blocks.FIRE ) {
                                    e.level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                                }
                                bp.setX(x.getX()); bp.setZ(x.getZ());
                            }
                        }
                        talentInst.WaterLeft -= WATER_CONSUME;
                        talentInst.doServerSplash(e);
    
                    }
                    if (mvto != null) {
                        this.nextfartarget = mvto;
                    }
                    if (!only_far) talentInst.cooldownDeadline = dog.tickCount + COOLDOWN_INTERVAL;
                } else if ( --this.timetorecalcpath <= 0 && this.nextfartarget != null) {
                    this.timetorecalcpath = 10;
                    dog.getLookControl().setLookAt(this.nextfartarget, 10.0f, dog.getMaxHeadXRot());
                    this.dog.getNavigation().moveTo(this.nextfartarget, 1.25f);
                }
    
            }
        }

        private List<LivingEntity> getEntityOnFire() {
            return dog.level.getEntitiesOfClass(LivingEntity.class, dog.getBoundingBox().inflate(SEARCH_RADIUS), 
                e -> 
                    (
                        e.isOnFire() && !e.isInLava() && dog.canSee(e)
                    ) && ((
                        e instanceof DogEntity 
                        && dog.getOwner() != null && ((DogEntity)e).getOwner() == dog.getOwner()
                    ) || (
                        dog.getOwner() != null && e == dog.getOwner()
                    )) 
            );
        }
        
    }

}
