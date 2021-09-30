package doggytalents.common.entity.stats;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.google.common.collect.Maps;

import doggytalents.ChopinLogger;
import doggytalents.common.util.Cache;
import doggytalents.common.util.NBTUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;

public class StatsTracker {

    private Map<EntityType<?>, Integer> ENTITY_KILLS = Maps.newHashMap();
    private float damageDealt = 0;
    private int distanceOnWater = 0;
    private int distanceInWater = 0;
    private int distanceSprinting = 0;
    private int distanceSneaking = 0;
    private int distanceWalking = 0;
    private int distanceRidden = 0;
    private int deathCounts = 0;

    // Cache
    private final Cache<Integer> killCount = Cache.make(this::getTotalKillCountInternal);

    public void writeAdditional(CompoundNBT compound) {
        ListNBT killList = new ListNBT();
        for (Entry<EntityType<?>, Integer> entry : this.ENTITY_KILLS.entrySet()) {
            CompoundNBT stats = new CompoundNBT();
            NBTUtil.putRegistryValue(stats, "type", entry.getKey());
            stats.putInt("count", entry.getValue());
            killList.add(stats);
        }
        compound.put("entityKills", killList);
        compound.putDouble("damageDealt", this.damageDealt);
        compound.putInt("distanceOnWater", this.distanceOnWater);
        compound.putInt("distanceInWater", this.distanceInWater);
        compound.putInt("distanceSprinting", this.distanceSprinting);
        compound.putInt("distanceSneaking", this.distanceSneaking);
        compound.putInt("distanceWalking", this.distanceWalking);
        compound.putInt("distanceRidden", this.distanceRidden);
        compound.putInt("deathCounts", this.deathCounts);
    }

    public void readAdditional(CompoundNBT compound) {
        ListNBT killList = compound.getList("entityKills", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < killList.size(); i++) {
            CompoundNBT stats = killList.getCompound(i);
            EntityType<?> type = NBTUtil.getRegistryValue(stats, "type", ForgeRegistries.ENTITIES);
            this.ENTITY_KILLS.put(type, stats.getInt("count"));
        }
        this.damageDealt = compound.getFloat("damageDealt");
        this.distanceOnWater = compound.getInt("distanceOnWater");
        this.distanceInWater = compound.getInt("distanceInWater");
        this.distanceSprinting = compound.getInt("distanceSprinting");
        this.distanceSneaking = compound.getInt("distanceSneaking");
        this.distanceWalking = compound.getInt("distanceWalking");
        this.distanceRidden = compound.getInt("distanceRidden");
        this.deathCounts = compound.getInt("deathCounts");
    }

    public int getKillCountFor(EntityType<?> type) {
        return this.ENTITY_KILLS.getOrDefault(type, 0);
    }

    public int getKillCountFor(Predicate<EntityClassification> classification) {
        int total = 0;
        for (Entry<EntityType<?>, Integer> entry : this.ENTITY_KILLS.entrySet()) {
            if (classification.test(entry.getKey().getCategory())) {
                total += entry.getValue();
            }
        }
        return total;
    }

    private int getTotalKillCountInternal() {
        int total = 0;
        for (Entry<EntityType<?>, Integer> entry : this.ENTITY_KILLS.entrySet()) {
            total += entry.getValue();
        }
        return total;
    }

    public int getTotalKillCount() {
        return this.killCount.get();
    }

    public void incrementKillCount(Entity entity) {
        this.incrementKillCount(entity.getType());
    }

    private void incrementKillCount(EntityType<?> type) {
        this.ENTITY_KILLS.compute(type, (k, v) -> (v == null ? 0 : v) + 1);
    }

    public void increaseDamageDealt(float damage) {
        this.damageDealt += damage;
    }

    public void increaseDistanceOnWater(int distance) {
        this.distanceOnWater += distance;
    }

    public void increaseDistanceInWater(int distance) {
        this.distanceInWater += distance;
    }

    public void increaseDistanceSprint(int distance) {
        this.distanceSprinting += distance;
    }

    public void increaseDistanceSneaking(int distance) {
        this.distanceSneaking += distance;
    }

    public void increaseDistanceWalk(int distance) {
        this.distanceWalking += distance;
    }

    public void increaseDistanceRidden(int distance) {
        this.distanceRidden += distance;
    }
    public void increaseDeathCounts() {
        ++this.deathCounts;
    } 


    public void updateStats(StatsObject s) {
        this.damageDealt = s.damageDealt; 
        this.distanceOnWater = s.distanceOnWater;
        this.distanceInWater = s.distanceInWater;
        this.distanceSprinting = s.distanceSprinting;
        this.distanceSneaking = s.distanceSneaking;
        this.distanceWalking = s.distanceWalking;
        this.distanceRidden = s.distanceRidden;
        this.deathCounts = s.deathCounts; 
    }
    public StatsObject getStatsObject() {
        StatsObject s = new StatsObject();
        s.damageDealt = this.damageDealt; 
        s.distanceOnWater = this.distanceOnWater;
        s.distanceInWater = this.distanceInWater;
        s.distanceSprinting = this.distanceSprinting;
        s.distanceSneaking = this.distanceSneaking;
        s.distanceWalking = this.distanceWalking;
        s.distanceRidden = this.distanceRidden;
        s.deathCounts = this.deathCounts;
        return s;
    }
    public float getDamageDealt() {
        return this.damageDealt;
    }
    public int getDistanceOnWater() {
        return this.distanceOnWater;
    }
    public int getDistanceInWater() {
        return this.distanceInWater;
    }
    public int getDistanceSprinting() {
        return this.distanceSprinting;
    }
    public int getDistanceSneaking() {
        return this.distanceSneaking;
    }
    public int getDistanceWalking() {
        return this.distanceWalking;
    }
    public int getDistanceRidden() {
        return this.distanceRidden;
    }
    public int getDeathCounts() {
        return this.deathCounts;
    }
}
