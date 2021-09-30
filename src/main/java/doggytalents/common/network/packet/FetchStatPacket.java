package doggytalents.common.network.packet;

import java.util.function.Supplier;

import doggytalents.ChopinLogger;
import doggytalents.DoggyTalents;
import doggytalents.api.DoggyTalentsAPI;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.entity.stats.StatsObject;
import doggytalents.common.entity.stats.StatsTracker;
import doggytalents.common.network.IPacket;
import doggytalents.common.network.packet.data.DogData;
import doggytalents.common.network.packet.data.StatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class FetchStatPacket implements IPacket<StatsData> {
    //Test with damagae dealt
    @Override
    public void encode(StatsData data, PacketBuffer buf) {
        buf.writeInt(data.entityId);
        buf.writeFloat(data.stats.damageDealt); 
        buf.writeInt(data.stats.distanceOnWater);
        buf.writeInt(data.stats.distanceInWater);
        buf.writeInt(data.stats.distanceSprinting);
        buf.writeInt(data.stats.distanceSneaking);
        buf.writeInt(data.stats.distanceWalking);
        buf.writeInt(data.stats.distanceRidden);
        buf.writeInt(data.stats.deathCounts);
    }

    @Override
    public StatsData decode(PacketBuffer buf) {
        StatsData s = new StatsData(buf.readInt(), new StatsObject());
        s.stats.damageDealt = buf.readFloat();
        s.stats.distanceOnWater=buf.readInt();
        s.stats.distanceInWater=buf.readInt();
        s.stats.distanceSprinting=buf.readInt();
        s.stats.distanceSneaking=buf.readInt();
        s.stats.distanceWalking=buf.readInt();
        s.stats.distanceRidden=buf.readInt();
        s.stats.deathCounts=buf.readInt();
        return s;
    }

    @Override
    public final void handle(StatsData data, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            //Only On Dist.CLIENT
            Entity target = Minecraft.getInstance().level.getEntity(data.entityId);

            if (!(target instanceof DogEntity)) {
                return;
            }
            DogEntity dog = (DogEntity) target;
            dog.statsTracker.updateStats(data.stats);
        });

        ctx.get().setPacketHandled(true);
    }


}