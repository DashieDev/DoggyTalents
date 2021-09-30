package doggytalents.common.network.packet;
import doggytalents.common.network.PacketHandler;

import java.util.function.Supplier;

import doggytalents.DoggyTalents2;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.entity.texture.DogTextureServer;
import doggytalents.common.network.IPacket;
import doggytalents.common.network.packet.data.DogData;
import doggytalents.common.network.packet.data.RequestSkinData;
import doggytalents.common.network.packet.data.SendSkinData;
import doggytalents.common.network.packet.data.StatsData;
import doggytalents.common.network.packet.data.StatsRequestData;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraft.entity.Entity;

public class RequestUpdateStats implements IPacket<StatsRequestData> {
    //Test with damagae dealt
    @Override
    public void encode(StatsRequestData data, PacketBuffer buf) {
        buf.writeInt(data.entityId);
    }

    @Override
    public StatsRequestData decode(PacketBuffer buf) {
        return new StatsRequestData(buf.readInt());
    }

    @Override
    public void handle(StatsRequestData data, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            //Only on server thread
            Entity target = ctx.get().getSender().level.getEntity(data.entityId);
            if (!(target instanceof DogEntity)) {
                return;
            }
            DogEntity dog = (DogEntity) target;
            PacketHandler.send(
                PacketDistributor.PLAYER.with( () -> ctx.get().getSender() ), 
                new StatsData( data.entityId, dog.statsTracker.getStatsObject() )
            );
        });

        ctx.get().setPacketHandled(true);
    }

}