package doggytalents.common.network.packet;

import java.util.List;
import java.util.function.Supplier;

import doggytalents.ChopinLogger;
import doggytalents.common.Screens;
import doggytalents.common.entity.DogEntity;
import doggytalents.common.network.IPacket;
import doggytalents.common.network.packet.data.DogHotSlotScreenData;
import doggytalents.common.network.packet.data.OpenDogScreenData;
import doggytalents.common.talent.PackPuppyTalent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class OpenDogHotSlotScreenPacket implements IPacket<DogHotSlotScreenData>  {

    @Override
    public DogHotSlotScreenData decode(PacketBuffer buf) {
        return new DogHotSlotScreenData(buf.readInt());
    }


    @Override
    public void encode(DogHotSlotScreenData data, PacketBuffer buf) {
        buf.writeInt(data.entityId);
    }

    @Override
    public void handle(DogHotSlotScreenData data, Supplier<Context> ctx) {
        ChopinLogger.l("server dog hot slot data");
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide() == LogicalSide.SERVER) {
                ServerPlayerEntity player = ctx.get().getSender();
                Entity e = player.level.getEntity(data.entityId);
                if (e instanceof DogEntity) {
                    DogEntity dogIn = (DogEntity)e;
                    if (dogIn.canInteract(player))
                    Screens.openDogHotSlotScreen(player, dogIn);
                }
                
            }
        });

        ctx.get().setPacketHandled(true);
    }
}
