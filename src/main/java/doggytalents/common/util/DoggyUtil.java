package doggytalents.common.util;

import doggytalents.common.entity.DogEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;

public class DoggyUtil {
    public static boolean tryToTeleportNearEntity(DogEntity d, PathNavigator navigator, LivingEntity target, int radius) {
        return tryToTeleportNearEntity(d, navigator, target.blockPosition(), radius);
    }

    public static boolean tryToTeleportNearEntity(DogEntity d, PathNavigator navigator, BlockPos targetPos, int radius) {
        for (int i = 0; i < 10; ++i) {
            int j = EntityUtil.getRandomNumber(d, -radius, radius);
            int k = EntityUtil.getRandomNumber(d, -1, 1);
            int l = EntityUtil.getRandomNumber(d, -radius, radius);
            boolean flag = tryToTeleportToLocation(d, navigator, targetPos, targetPos.getX() + j, targetPos.getY() + k, targetPos.getZ() + l);
            if (flag) {
                return true;
            }
        }

        return false;
    }

    public static boolean tryToTeleportToLocation(DogEntity d, PathNavigator navigator, BlockPos targetPos, int x, int y, int z) {
        if (Math.abs(x - targetPos.getX()) < 2.0D && Math.abs(z - targetPos.getZ()) < 2.0D) {
            return false;
        } else if (!isTeleportFriendlyBlock(d, new BlockPos(x, y, z), false)) {
            return false;
        } else {
            d.moveTo(x + 0.5F, y, z + 0.5F, d.yRot, d.xRot);
            navigator.stop();
            return true;
        }
    }

    public static boolean isTeleportFriendlyBlock(DogEntity d, BlockPos pos, boolean teleportToLeaves) {
        if (!d.isBlockSafe(pos)) {
            return false;
        } else {
            BlockState blockstate = d.level.getBlockState(pos.below());
            if (!teleportToLeaves && blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pos.subtract(d.blockPosition());
                return d.level.noCollision(d, d.getBoundingBox().move(blockpos));
            }
        }
    }

    public static boolean isBlockSafeDefault(DogEntity entityIn, BlockPos pos, boolean teleportToLeaves) {
        PathNodeType pathnodetype = WalkNodeProcessor.getBlockPathTypeStatic(entityIn.level, pos.mutable());
        return pathnodetype == PathNodeType.WALKABLE; 
    }
}
