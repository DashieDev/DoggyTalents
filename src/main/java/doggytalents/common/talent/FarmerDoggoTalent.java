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
import doggytalents.common.inventory.PackPuppyItemHandler;
import doggytalents.common.util.InventoryUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropsBlock;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.BreakBlockGoal;
import net.minecraft.entity.ai.goal.BreakDoorGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
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

    public FarmerDoggoTalent(Talent talentIn, int levelIn) {
        super(talentIn, levelIn);
    }

    @Override
    public void livingTick(AbstractDogEntity dogIn) {
        if(dogIn.tickCount % PLACE_WHEAT_INTERVAL == 0) { 
            BlockPos bp = dogIn.blockPosition();
            BlockState bs = dogIn.level.getBlockState(bp);
            BlockState bs1 = dogIn.level.getBlockState(bp.above());
            if (bs.getBlock() == Blocks.FARMLAND) {
                if(bs1.getBlock() == Blocks.AIR) {  
                    if (!dogIn.level.isClientSide) { 
                        BlockState bsw = Blocks.WHEAT.defaultBlockState();
                        dogIn.level.setBlockAndUpdate(bp.above(), bsw);
                    }
                } 
            }
        } 
        if (dogIn.tickCount % (PLACE_WHEAT_INTERVAL - HARVEST_AND_PLANT_INTERVAL)  == 0) {
            BlockPos bp = dogIn.blockPosition();
            BlockState bs1 = dogIn.level.getBlockState(bp.above());
            if (
                    bs1.getBlock() == Blocks.WHEAT 
                    && ((CropsBlock) Blocks.WHEAT).isMaxAge(bs1) 
                    && !dogIn.level.isClientSide
                ) {
                    dogIn.level.levelEvent(2001, bp.above(), Block.getId(dogIn.level.getBlockState(bp.above())));
                    ChopinLogger.l(dogIn.getName().getString() + " played sound !");
                    bs1.getBlock().dropResources(bs1, dogIn.level, bp.above());
                    dogIn.level.removeBlock(bp.above(), false);
                    ChopinLogger.l(dogIn.getName().getString() + "harvested crop");
                }
        } 
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

        protected final AbstractDogEntity dog;
        private final World world;
        private final PathNavigator dogPathfinder;
        private ToolUtilizerTalent toolUtil;


        private int timeToRecalcPath;
        private LivingEntity owner;
        private float oldWaterCost;
        private List<BlockState> targets;

        private int MAX_FOLLOW = 5;

        public SearchForFarmBlockAndFarmAi(AbstractDogEntity dogIn, ToolUtilizerTalent talentIn) {
            this.dog = dogIn;
            this.world = dogIn.level;
            this.dogPathfinder = dogIn.getNavigation();
            this.toolUtil = talentIn;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (
                this.dog.getMode() == EnumMode.DOCILE
                && this.dog.getLevel(DoggyTalents.TOOL_UTILIZER) > 0
                && this.dog.getLevel(DoggyTalents.FARMER_DOGGO) > 0
            ) {
                //if got a Hoe in inventory
                this.toolUtil = (ToolUtilizerTalent) this.dog.getTalent(DoggyTalents.TOOL_UTILIZER).get();
                if (this.toolUtil.inventory().getItemSlotIfExitst(HoeItem.class) >= 0) {
                    List<BlockState> targets = this.dog.level.getBlockStates(this.dog.getBoundingBox().inflate(5.0D))
                        .filter((blck) -> 
                            blck.getBlock() == Blocks.FARMLAND
                            ).collect(Collectors.toList()
                        );
                    BlockState x;
                    
                    if (targets.size() > 0) {
                        this.targets = targets;
                        return true;
                    }
                } 
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (
                this.dog.getMode() == EnumMode.DOCILE
                && this.dog.getLevel(DoggyTalents.TOOL_UTILIZER) > 0
                && this.dog.getLevel(DoggyTalents.FARMER_DOGGO) > 0
            ) {
                //if got Seed in inventory
                this.toolUtil = (ToolUtilizerTalent) this.dog.getTalent(DoggyTalents.TOOL_UTILIZER).get();
                if (this.toolUtil.getSelectedSlot() >= 3) return false;
                if (this.toolUtil.inventory().getStackInSlot(
                    (int) this.toolUtil.getSelectedSlot()
                ).getItem() == Items.WHEAT_SEEDS ) {
                    if (!targets.isEmpty()) return true;
                } 
            }
            return false;
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.toolUtil = (ToolUtilizerTalent) this.dog.getTalent(DoggyTalents.TOOL_UTILIZER).get();
            this.toolUtil.selectItemIfExist(Items.WHEAT_SEEDS);
        }

        @Override
        public void tick() {
            if (!this.dog.isInSittingPose()) {

                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = 10;
                                
                                                
                }
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            this.dogPathfinder.stop();
            this.dog.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
        }
    }
}
