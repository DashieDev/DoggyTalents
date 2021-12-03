package doggytalents.common.talent;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.base.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import doggytalents.ChopinLogger;
import doggytalents.DoggyTalents;
import doggytalents.api.feature.EnumMode;
import doggytalents.api.inferface.AbstractDogEntity;
import doggytalents.api.registry.Talent;
import doggytalents.api.registry.TalentInstance;
import doggytalents.common.inventory.DogHotSlotItemHandler;
import doggytalents.common.inventory.PackPuppyItemHandler;
import doggytalents.common.util.InventoryUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.SoundType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.FarmTask;
import net.minecraft.entity.ai.goal.BreakBlockGoal;
import net.minecraft.entity.ai.goal.BreakDoorGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

/** 
*    @author DashieDev
*/
public class FarmerDoggoTalent extends TalentInstance {

    private final int PLACE_WHEAT_INTERVAL = 5;
    private final int HARVEST_AND_PLANT_INTERVAL = 0;

    private SearchForFarmBlockAndFarmAi goal;
    

    public FarmerDoggoTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    @Override
    public void init(AbstractDogEntity dogIn) {
        this.goal = new SearchForFarmBlockAndFarmAi(dogIn, this);
        dogIn.goalSelector.addGoal(4, this.goal);
   
    }

    @Override
    public void remove(AbstractDogEntity dogIn) {
        dogIn.targetSelector.removeGoal(this.goal);
    }

    @Override
    public ActionResult<Integer> hungerTick(AbstractDogEntity dogIn, int hungerTick) {
        if (this.goal.getRunning()) {
            hungerTick += this.level() < 5 ? 3 : 1;
            return ActionResult.success(hungerTick);
        }

        return ActionResult.pass(hungerTick);
    }


    @Override
    public ActionResultType processInteract(AbstractDogEntity dogIn, World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (!dogIn.level.isClientSide) {
            ChopinLogger.l("Debug here");
        }
        if (stack.getItem() == Items.STONE_SHOVEL) {
            BlockState s = dogIn.level.getBlockState(dogIn.blockPosition().below());
            BlockState s2 = dogIn.level.getBlockState(dogIn.blockPosition());
            BlockState s1 = dogIn.level.getBlockState(dogIn.blockPosition().above());
            ChopinLogger.l("block below : " + s);
            ChopinLogger.l("block above : " + s1);
            ChopinLogger.l("block there : " + s2);
            
            //ChopinLogger.l("Hello?");
            return ActionResultType.SUCCESS;
        }
        
        return ActionResultType.PASS;
    }

    public static class SearchForFarmBlockAndFarmAi extends Goal {

        private static final int FIND_RADIUS = 5;

        protected final AbstractDogEntity dog;
        private final World world;
        private final PathNavigator dogPathfinder;
        private ToolUtilizerTalent toolUtil;
        private FarmerDoggoTalent farmer;

        private LivingEntity owner;
        private BlockPos nextFarmBlock = null;
        private int timetorecalcpath = 10;
        private int cooldown = 10;
        private boolean running = false;

        public SearchForFarmBlockAndFarmAi(AbstractDogEntity dogIn, FarmerDoggoTalent talent) {
            this.dog = dogIn;
            this.world = dogIn.level;
            this.dogPathfinder = dogIn.getNavigation();
            this.farmer = talent;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.dog.isInSittingPose()) return false;
            if (this.dog.getMode() != EnumMode.DOCILE) return false;
            if (this.dog.getLevel(DoggyTalents.TOOL_UTILIZER) <= 0) return false;
            if (this.dog.getLevel(DoggyTalents.FARMER_DOGGO) <= 0) return false;
            if (this.toolUtil == null) {
                this.toolUtil = (ToolUtilizerTalent) this.dog.getTalent(DoggyTalents.TOOL_UTILIZER).orElse(null);
                if (this.toolUtil == null) return false;
            }
            if (toolUtil.inventory().getItemSlotIfExitst(HoeItem.class) < 0) return false;
            this.nextFarmBlock = this.findNextFarmBlock();
            if (this.nextFarmBlock == null) return false;
            

            return true;
        }

        @Override
        public boolean canContinueToUse() {

            if (this.dog.isInSittingPose()) return false;
            if (this.dog.getMode() != EnumMode.DOCILE) return false;
            if (this.dog.getLevel(DoggyTalents.TOOL_UTILIZER) <= 0) return false;
            if (this.dog.getLevel(DoggyTalents.FARMER_DOGGO) <= 0) return false;
            if (toolUtil.inventory().getItemSlotIfExitst(HoeItem.class) < 0) return false;
            if (this.nextFarmBlock == null || this.getFarnState(this.nextFarmBlock) <= 0 ) this.nextFarmBlock = this.findNextFarmBlock();
            if (this.nextFarmBlock == null || this.getFarnState(this.nextFarmBlock) <= 0 ) return false;

            return true;
        }

        @Override
        public void start() {
            this.toolUtil.selectItemIfExist(HoeItem.class); 
            this.running = true;
        }

        @Override
        public void tick() {
            --this.cooldown;
            
            if (this.nextFarmBlock != null) {
                this.dog.getLookControl().setLookAt(this.nextFarmBlock.getX(), this.nextFarmBlock.getY(),this.nextFarmBlock.getZ() );
                BlockPos bp = this.dog.blockPosition();
                if (!(bp.closerThan(this.nextFarmBlock.above(), 2.25F))) {
                    if (--this.timetorecalcpath <= 0) { 
                        this.timetorecalcpath = 10;
                        this.dogPathfinder.moveTo(this.nextFarmBlock.getX(), this.nextFarmBlock.getY(), this.nextFarmBlock.getZ(), 1);
                    }
                } else {
                    if (this.cooldown > 0) return;
                    else this.cooldown = 10;
                    switch(this.getFarnState(this.nextFarmBlock)) {
                        case 1 : { 
                            BlockState wht = Blocks.WHEAT.defaultBlockState();
                            this.world.setBlockAndUpdate(this.nextFarmBlock.above(), wht);
                            SoundType soundtype = wht.getSoundType(this.world, this.nextFarmBlock.above(), this.dog);
                            this.dog.playSound(soundtype.getPlaceSound(), (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                            ChopinLogger.lwn(this.dog,"planted crop!");
                            this.nextFarmBlock = this.findNextFarmBlock();
                            break;
                        }
                        case 2 : {
                            this.dog.level.destroyBlock(this.nextFarmBlock.above(), true);
                            ChopinLogger.lwn( this.dog, "harvested crop" );
                            this.nextFarmBlock = this.findNextFarmBlock();
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void stop() {
            this.dogPathfinder.stop();
            this.running = false;
        }

        public boolean getRunning() {
            return this.running;
        }

        private BlockPos findNextFarmBlock() {
            BlockPos bp = this.dog.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(bp.offset(-this.FIND_RADIUS, -4, -this.FIND_RADIUS), bp.offset(this.FIND_RADIUS, 4, this.FIND_RADIUS))) {
                if (this.getFarnState(pos) > 0) { ChopinLogger.lwn(this.dog, pos.toString()); return pos;}
            }
            return null;
        }

        //0: default
        //1: is a farm block with air on top
        //2: is a farm block with max age crop on top
        private byte getFarnState(BlockPos bp) { 
            BlockState bs = this.world.getBlockState(bp);
            if(bs.getBlock() == Blocks.FARMLAND) {
                if(this.world.getBlockState(bp.above()).getBlock() == Blocks.AIR) {
                    return 1;
                } else {
                    BlockState bs1 = this.world.getBlockState(bp.above());
                    if( bs1.getBlock() == Blocks.WHEAT && ((CropsBlock) Blocks.WHEAT).isMaxAge(bs1)) {
                        return 2;
                    }
                }
            } 
            return 0;
        }
    }
}
