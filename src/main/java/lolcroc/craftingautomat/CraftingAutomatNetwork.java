package lolcroc.craftingautomat;

import com.google.common.collect.Lists;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CraftingAutomatNetwork {

    private static int messageID;

    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation REGISTRY_NAME = new ResourceLocation(CraftingAutomat.MODID, "network");

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            REGISTRY_NAME, () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    public static int nextID() {
        return messageID++;
    }

    public static void registerMessages() {
        INSTANCE.registerMessage(nextID(), SOverrideConfigPacket.class,
                SOverrideConfigPacket::toBytes, SOverrideConfigPacket::new, SOverrideConfigPacket::handle);
    }

    public static void overrideClientConfigs(ForgeConfigSpec.IntValue ... vals) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), new SOverrideConfigPacket(vals));
    }

    private static class SOverrideConfigPacket {

        private Map<List<String>, Integer> values = new HashMap<>();

        public SOverrideConfigPacket(PacketBuffer buf) {
            int size = buf.readInt();

            for (int i = 0; i < size; i++) {
                int val = buf.readInt();
                int pathsize = buf.readInt();
                List<String> path = Lists.newArrayList();

                for (int j = 0; j < pathsize; j++) {
                    path.add(buf.readString());
                }

                values.put(path, val);
            }
        }

        public SOverrideConfigPacket(ForgeConfigSpec.IntValue ... vals) {
            for (int i = 0; i < vals.length; i++) {
                values.put(vals[i].getPath(), vals[i].get());
            }
        }

        public void toBytes(PacketBuffer buf) {
            buf.writeInt(values.size()); // Number of values to sync

            for (Map.Entry<List<String>, Integer> e : values.entrySet()) {
                buf.writeInt(e.getValue()); // The value itself
                buf.writeInt(e.getKey().size());  // Number of elements in the path
                e.getKey().forEach(buf::writeString); // The path itself
            }
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> CraftingAutomatConfig.Client.putAll(values));
            ctx.get().setPacketHandled(true);
        }
    }
}
