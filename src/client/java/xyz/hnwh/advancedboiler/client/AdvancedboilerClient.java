package xyz.hnwh.advancedboiler.client;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Environment(EnvType.CLIENT)
public class AdvancedboilerClient implements ClientModInitializer {

    public static final String MOD_ID = "boiler_mod";

    public static BoilerDisplayManager displayManager;
    public static final BoilerClientConfig CONFIG = BoilerClientConfig.createAndLoad();

    private static HashMap<Integer, StreamState> streamStates = new HashMap<>();
    private static Timer streamStateTimer;

    @Override
    public void onInitializeClient() {
        displayManager = new BoilerDisplayManager();

        WorldRenderEvents.END.register(context -> {
            DisplayRenderHelper.renderDisplays(context);
        });

        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler, packetSender, minecraftClient) -> {
            streamStateTimer = new Timer();
            streamStateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    for(BoilerDisplay display : displayManager.getDisplays()) {
                        ClientPlayNetworking.send(new BoilerStreamStateRequest(display.getId()));
                    }
                }
            }, 1000, 1000);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> {
            streamStateTimer.cancel();
            streamStates.clear();
            displayManager.removeAll();
        });

        // Server -> Client
        // Sent by server if boiler plugin is installed
        PayloadTypeRegistry.playS2C().register(BoilerHandshakeRequest.ID, BoilerHandshakeRequest.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BoilerHandshakeRequest.ID, (payload, context) -> {
            System.out.println("Handshake recieved");
            ClientPlayNetworking.send(new BoilerHandshakeResponse(true));
        });

        // Sent on display remove/unload
        PayloadTypeRegistry.playS2C().register(BoilerDisplayRemovePayload.ID, BoilerDisplayRemovePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BoilerDisplayRemovePayload.ID, (payload, context) -> {
            System.out.println("Display removed");
            displayManager.removeDisplay(payload.displayId);
        });

        PayloadTypeRegistry.playS2C().register(BoilerDisplayCreatePayload.ID, BoilerDisplayCreatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BoilerDisplayCreatePayload.ID, (payload, context) -> {
            System.out.println("Display created");
            int displayId = payload.displayId;
            Vec3i pos1 = new Vec3i(payload.pos[0], payload.pos[1], payload.pos[2]);
            Vec3i pos2 = new Vec3i(payload.pos[3], payload.pos[4], payload.pos[5]);
            int width = payload.width;
            int height = payload.height;
            int face = payload.facing;
            int rotation = payload.rotation;
            String streamUrl = payload.streamUrl;

            int[] speakers = payload.speakers;

            List<Vec3d> speakerList = new ArrayList<>();
            for (int i = 0; i < speakers.length; i+=3) {
                Vec3d pos = new Vec3d(speakers[i], speakers[i + 1], speakers[i + 2]);
                speakerList.add(pos);
            }

            streamStates.put(displayId, StreamState.UNKNOWN);
            BoilerDisplay d = displayManager.getDisplay(displayId);
            if(d == null) {
                d = new BoilerDisplay(displayId, pos1, pos2, width, height, face, rotation, speakerList, streamUrl);
                displayManager.registerDisplay(d);
            } else {
                d.setSource(streamUrl);
            }
        });

        PayloadTypeRegistry.playS2C().register(BoilerStreamStateResponse.ID, BoilerStreamStateResponse.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BoilerStreamStateResponse.ID, (payload, context) -> {

            int displayId = payload.displayId;
            StreamState currentState = streamStates.get(displayId);
            StreamState reportedState = StreamState.values()[payload.streamState];

            BoilerDisplay display = displayManager.getDisplay(displayId);

            if(!currentState.equals(reportedState) && reportedState.equals(StreamState.ONLINE)) {
                display.getRenderer().startPlayback();
            }

            display.setStreamState(reportedState);



            streamStates.put(displayId, reportedState);
        });

        // Client -> Server
        PayloadTypeRegistry.playC2S().register(BoilerHandshakeResponse.ID, BoilerHandshakeResponse.CODEC);
        PayloadTypeRegistry.playC2S().register(BoilerStreamStateRequest.ID, BoilerStreamStateRequest.CODEC);



    }

    public record TestRequest(int value, String text) implements CustomPayload {
        public static final CustomPayload.Id<TestRequest> ID = new CustomPayload.Id<>(Identifier.of("mymod", "test"));
        public static final PacketCodec<RegistryByteBuf, TestRequest> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, TestRequest::value,
                UTF_PACKET_CODEC, TestRequest::text,
                TestRequest::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BoilerHandshakeRequest(boolean installed) implements CustomPayload {
        public static final CustomPayload.Id<BoilerHandshakeRequest> ID = new CustomPayload.Id<>(Identifier.of("boiler", "handshake_request"));
        public static final PacketCodec<RegistryByteBuf, BoilerHandshakeRequest> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOLEAN, BoilerHandshakeRequest::installed,
                BoilerHandshakeRequest::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BoilerHandshakeResponse(boolean installed) implements CustomPayload {
        public static final CustomPayload.Id<BoilerHandshakeResponse> ID = new CustomPayload.Id<>(Identifier.of("boiler", "handshake_response"));
        public static final PacketCodec<RegistryByteBuf, BoilerHandshakeResponse> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOLEAN, BoilerHandshakeResponse::installed,
                BoilerHandshakeResponse::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BoilerStreamStateRequest(int displayId) implements CustomPayload {
        public static final CustomPayload.Id<BoilerStreamStateRequest> ID = new CustomPayload.Id<>(Identifier.of("boiler", "stream_state_request"));
        public static final PacketCodec<RegistryByteBuf, BoilerStreamStateRequest> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, BoilerStreamStateRequest::displayId,
                BoilerStreamStateRequest::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BoilerStreamStateResponse(int displayId, int streamState) implements CustomPayload {
        public static final CustomPayload.Id<BoilerStreamStateResponse> ID = new CustomPayload.Id<>(Identifier.of("boiler", "stream_state_response"));
        public static final PacketCodec<RegistryByteBuf, BoilerStreamStateResponse> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, BoilerStreamStateResponse::displayId,
                PacketCodecs.INTEGER, BoilerStreamStateResponse::streamState,
                BoilerStreamStateResponse::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BoilerDisplayRemovePayload(int displayId) implements CustomPayload {
        public static final CustomPayload.Id<BoilerDisplayRemovePayload> ID = new CustomPayload.Id<>(Identifier.of("boiler", "display_remove"));
        public static final PacketCodec<RegistryByteBuf, BoilerDisplayRemovePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, BoilerDisplayRemovePayload::displayId,
                BoilerDisplayRemovePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BoilerDisplayCreatePayload(int displayId, int[] pos, int width, int height, int facing, int rotation, int[] speakers, String streamUrl) implements CustomPayload {
        public static final CustomPayload.Id<BoilerDisplayCreatePayload> ID = new CustomPayload.Id<>(Identifier.of("boiler", "display_create"));
        public static final PacketCodec<RegistryByteBuf, BoilerDisplayCreatePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, BoilerDisplayCreatePayload::displayId,
                INT_LIST_PACKET_CODEC, BoilerDisplayCreatePayload::pos,
                PacketCodecs.INTEGER, BoilerDisplayCreatePayload::width,
                PacketCodecs.INTEGER, BoilerDisplayCreatePayload::height,
                PacketCodecs.INTEGER, BoilerDisplayCreatePayload::facing,
                PacketCodecs.INTEGER, BoilerDisplayCreatePayload::rotation,
                INT_LIST_PACKET_CODEC, BoilerDisplayCreatePayload::speakers,
                UTF_PACKET_CODEC, BoilerDisplayCreatePayload::streamUrl,
                BoilerDisplayCreatePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static final PacketCodec<ByteBuf, String> UTF_PACKET_CODEC = new PacketCodec<ByteBuf, String>() {
        @Override
        public String decode(ByteBuf buf) {
            // remove first two length info bytes
            buf.readByte();
            buf.readByte();

            byte[] data = new byte[buf.readableBytes()];

            int i = 0;
            while (buf.readableBytes() > 0) {
                data[i] = buf.readByte();
                i++;
            }

            return new String(data, StandardCharsets.UTF_8);
        }

        @Override
        public void encode(ByteBuf buf, String value) {

        }
    };

    public static final PacketCodec<ByteBuf, int[]> INT_LIST_PACKET_CODEC = new PacketCodec<ByteBuf, int[]>() {
        @Override
        public int[] decode(ByteBuf buf) {
            // first byte defines length
            int length = buf.readInt();

            int[] data = new int[length];

            for(int i = 0; i < length; i++) {
                data[i] = buf.readInt();
            }

            return data;
        }

        @Override
        public void encode(ByteBuf buf, int[] value) {

        }
    };

    public static final PacketCodec<ByteBuf, byte[]> BYTE_LIST_PACKET_CODEC = new PacketCodec<ByteBuf, byte[]>() {
        @Override
        public byte[] decode(ByteBuf buf) {
            // first byte defines length
            byte[] data = new byte[buf.readableBytes()];

            for(int i = 0; i < data.length; i++) {
                data[i] = buf.readByte();
            }

            return data;
        }

        @Override
        public void encode(ByteBuf buf, byte[] value) {

        }
    };

}
