package doggytalents.common.network.packet;

import java.util.function.Supplier;

import doggytalents.ChopinLogger;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.network.IPacket;
import doggytalents.common.network.PacketHandler;
import doggytalents.common.network.packet.data.DogData;
import doggytalents.common.network.packet.data.RequestHeelData;
import doggytalents.common.network.packet.data.ParticleData.CritEmitterData;
import doggytalents.common.util.EntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ParticleTypes;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.PacketDistributor.TargetPoint;

 public class RequestHeelPacket extends DogPacket<RequestHeelData> {

    @Override
    public void encode(RequestHeelData data, PacketBuffer buf) {
        buf.writeInt(data.entityId);
    }

    @Override
    public RequestHeelData decode(PacketBuffer buf) {
        return new RequestHeelData(buf.readInt());
    }

    @Override
    public void handleDog(DogEntity dogIn, RequestHeelData data, Supplier<Context> ctx) {
        EntityUtil.tryToTeleportNearEntity(dogIn, dogIn.getNavigation(), ctx.get().getSender(), 4);
    }
    

}
