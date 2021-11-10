package doggytalents.common.storage;

import static net.minecraftforge.common.util.Constants.NBT.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import doggytalents.DoggyTalents2;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.lib.Constants;
import doggytalents.common.util.NBTUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

public class  DogLiveStorage extends WorldSavedData {

    private Map<UUID, DogLiveData> liveDataMap = Maps.newConcurrentMap();

    public DogLiveStorage() {
        super(Constants.STORAGE_DOG_LIVE);
    }

    public static DogLiveStorage get(World world) {
        if (!(world instanceof ServerWorld)) {
            throw new RuntimeException("Tried to access dog respawn data from the client. This should not happen...");
        }

        ServerWorld overworld = world.getServer().getLevel(World.OVERWORLD);

        DimensionSavedDataManager storage = overworld.getDataStorage();
        return storage.computeIfAbsent(DogLiveStorage::new, Constants.STORAGE_DOG_LIVE);
    }

    public Stream<DogLiveData> getDogs(@Nonnull UUID ownerId) {
        return this.liveDataMap.values().stream()
                .filter(data -> ownerId.equals(data.getOwnerId()));
    }

    public Stream<DogLiveData> getDogs(@Nonnull String ownerName) {
        return this.liveDataMap.values().stream()
                .filter(data -> ownerName.equals(data.getOwnerName()));
    }

    @Nullable
    public DogLiveData getData(UUID uuid) {
        if (this.liveDataMap.containsKey(uuid)) {
            return this.liveDataMap.get(uuid);
        }

        return null;
    }

    @Nullable
    public DogLiveData remove(UUID uuid) {
        if (this.liveDataMap.containsKey(uuid)) {
            DogLiveData storage = this.liveDataMap.remove(uuid);

            // Mark dirty so changes are saved
            this.setDirty();
            return storage;
        }

        return null;
    }

    @Nullable
    public DogLiveData putData(DogEntity dogIn) {
        UUID uuid = dogIn.getUUID();

        DogLiveData storage = new DogLiveData(this, uuid);
        storage.populate(dogIn);

        this.liveDataMap.put(uuid, storage);
        // Mark dirty so changes are saved
        this.setDirty();
        return storage;
    }

    public Set<UUID> getAllUUID() {
        return Collections.unmodifiableSet(this.liveDataMap.keySet());
    }

    public Collection<DogLiveData> getAll() {
        return Collections.unmodifiableCollection(this.liveDataMap.values());
    }

    @Override
    public void load(CompoundNBT nbt) {
        this.liveDataMap.clear();

        ListNBT list = nbt.getList("liveData", TAG_COMPOUND);

        for (int i = 0; i < list.size(); ++i) {
            CompoundNBT liveCompound = list.getCompound(i);

            UUID uuid = NBTUtil.getUniqueId(liveCompound, "uuid");
            DogLiveData respawnData = new DogLiveData(this, uuid);
            respawnData.read(liveCompound);

            if (uuid == null) {
                DoggyTalents2.LOGGER.info("Failed to load dog respawn data. Please report to mod author...");
                DoggyTalents2.LOGGER.info(respawnData);
                continue;
            }

            this.liveDataMap.put(uuid, respawnData);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        ListNBT list = new ListNBT();

        for (Map.Entry<UUID, DogLiveData> entry : this.liveDataMap.entrySet()) {
            CompoundNBT respawnCompound = new CompoundNBT();

            DogLiveData respawnData = entry.getValue();
            NBTUtil.putUniqueId(respawnCompound, "uuid", entry.getKey());
            respawnData.write(respawnCompound);

            list.add(respawnCompound);
        }

        compound.put("liveData", list);

        return compound;
    }

}
