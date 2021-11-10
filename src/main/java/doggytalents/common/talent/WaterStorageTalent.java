package doggytalents.common.talent;


import java.util.List;

import doggytalents.ChopinLogger;
import doggytalents.api.enu.WetSource;
import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import doggytalents.common.entity.DogEntity;
import net.minecraft.block.LadderBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/** 
*    @author DashieDev
*/
public class WaterStorageTalent extends TalentInstance {

    private final int STORE_WATER_INVTERVAL = 1;
    private final int WATER_CONSUME = 35;
    private final int COOLDOWN_INTERVAL = 40;
    private final int EFFECT_RADIUS = 5;

    public WaterStorageTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    private int MAX_WATER_STORAGE = 100;

    private int WaterLeft = 0; //Server
    
    private int cooldownDeadline;

    private int ActivationTime;



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

        LivingEntity owner = dogIn.getOwner();
        //Extinguish player and teammate
        if (
            !dogIn.level.isClientSide
            && owner != null
            && dogIn.isAlive() 
            && dogIn.tickCount > cooldownDeadline 
            && !((DogEntity)dogIn).isDogWet() 
            && this.WaterLeft >= WATER_CONSUME
        ) {
        
            List<DogEntity> otherDogs = dogIn.level.getEntitiesOfClass(
                DogEntity.class, dogIn.getBoundingBox().inflate((double)EFFECT_RADIUS, (double)EFFECT_RADIUS, (double)EFFECT_RADIUS), 
                dog -> (
                        dog.isOwnedBy(owner) 
                        && dog != dogIn
                        && dog.isOnFire()
                    )
            );
            //ChopinLogger.l(""+otherDogs.size()); 

            if ((owner.isOnFire() && dogIn.distanceToSqr(owner) <= EFFECT_RADIUS*EFFECT_RADIUS) || otherDogs.size() > 0) {
                ((DogEntity) dogIn).setDogWet(WetSource.of(true, false, false));
                if (owner.isOnFire()) {
                    owner.clearFire();
                    //owner.playSound( SoundEvents.FIRE_EXTINGUISH,  0.5F, 2.6F + dogIn.getRandom().nextFloat() - ((DogEntity) dogIn).getRandom().nextFloat() * 0.8F);
                    dogIn.level.playSound(null, owner.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundCategory.PLAYERS,  0.5F, 2.6F + dogIn.getRandom().nextFloat() - ((DogEntity) dogIn).getRandom().nextFloat() * 0.8F);
                    this.WaterLeft -= WATER_CONSUME;
                    ChopinLogger.l(dogIn.getName().getString() + " extinguished you!");
                    //this.level.playLocalSound(p_180439_3_,, SoundCategory.BLOCKS,, false);
                }

                if (otherDogs.size() > 0) {
                    for (DogEntity d : otherDogs) {
                        if (this.WaterLeft >= WATER_CONSUME) {
                            d.clearFire();
                            d.playSound( SoundEvents.FIRE_EXTINGUISH,  0.5F, 2.6F + dogIn.getRandom().nextFloat() - ((DogEntity) dogIn).getRandom().nextFloat() * 0.8F);
                            this.WaterLeft -= WATER_CONSUME; 
                            ChopinLogger.l(dogIn.getName().getString() + " extinguished " + d.getName().getString()+  "!");
                        }
                    }
                }
                this.cooldownDeadline = dogIn.tickCount + COOLDOWN_INTERVAL;

            } 
            
        
        }


    }

    

    @Override
    public ActionResultType processInteract(AbstractDogEntity dogIn, World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (stack.getItem() == Items.WATER_BUCKET) {
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
        }
        
        return ActionResultType.PASS;
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

}
