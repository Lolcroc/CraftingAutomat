package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftingAutomatNetwork {

    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(CraftingAutomat.MODID, "network");
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(1)
            .acceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    public static void registerMessages() {
        CHANNEL.messageBuilder(SOverrideConfigPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SOverrideConfigPacket::toBytes)
                .decoder(SOverrideConfigPacket::new)
                .consumerMainThread(SOverrideConfigPacket::handle)
                .add();
    }

    public static void overrideClientConfigs(ForgeConfigSpec.IntValue... vals) {
        CHANNEL.send(new SOverrideConfigPacket(vals), PacketDistributor.ALL.noArg());
    }

    private static class SOverrideConfigPacket {

        private Map<List<String>, Integer> values = new HashMap<>();

        public SOverrideConfigPacket(FriendlyByteBuf buf) {
            int size = buf.readInt();

            for (int i = 0; i < size; i++) {
                int val = buf.readInt();
                int pathsize = buf.readInt();
                List<String> path = Lists.newArrayList();

                for (int j = 0; j < pathsize; j++) {
                    path.add(buf.readUtf());
                }

                values.put(path, val);
            }
        }

        public SOverrideConfigPacket(ForgeConfigSpec.IntValue ... vals) {
            for (ForgeConfigSpec.IntValue val : vals) {
                values.put(val.getPath(), val.get());
            }
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeInt(values.size()); // Number of values to sync

            for (Map.Entry<List<String>, Integer> e : values.entrySet()) {
                buf.writeInt(e.getValue()); // The value itself
                buf.writeInt(e.getKey().size());  // Number of elements in the path
                e.getKey().forEach(buf::writeUtf); // The path itself
            }
        }

        public void handle(CustomPayloadEvent.Context ctx) {
            CraftingAutomatConfig.Client.putAll(values);
        }
    }
}
