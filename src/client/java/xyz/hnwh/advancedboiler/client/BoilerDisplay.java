package xyz.hnwh.advancedboiler.client;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class BoilerDisplay {

    private int id;
    private Vec3i pos1;
    private Vec3i pos2;
    private int width;
    private int height;
    private int direction;
    private int rotation;
    private int rootMapId;
    private int lastMapId;
    private int[] mapIds;
    private StreamState streamState;
    private List<Vec3d> speakers;

    private String streamUrl;
    private DisplayRenderer renderer;

    private Vec3d center;
    private ItemFrameEntity rootEntity;

    public BoilerDisplay(int width, int height, int rootMapId, int lastMapId, int[] mapIds, String streamUrl) {
        this.width = width;
        this.height = height;
        this.rootMapId = rootMapId;
        this.lastMapId = lastMapId;
        this.mapIds = mapIds;
        this.streamUrl = streamUrl;
    }

    public BoilerDisplay(int id, Vec3i pos1, Vec3i pos2, int width, int height, int direction, int rotation, List<Vec3d> speakers, String streamUrl) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.rotation = rotation;
        this.streamUrl = streamUrl;
        this.streamState = StreamState.UNKNOWN;

        center = new Vec3d(
                (pos1.getX() + pos2.getX()) / 2.0,
                (pos1.getY() + pos2.getY()) / 2.0,
                (pos1.getZ() + pos2.getZ()) / 2.0
        );

        if (speakers == null || speakers.isEmpty()) {
            this.speakers = new ArrayList<>();
            this.speakers.add(center);
        } else {
            this.speakers = speakers;
        }

        if(!streamUrl.isEmpty()) renderer = new DisplayRenderer(streamUrl, this.speakers, width, height);
    }

    public void setSource(String url) {
        this.streamUrl = url;

        if (this.renderer == null) {
            renderer = new DisplayRenderer(url, speakers, width, height);
        } else {
            renderer.play(url);
        }
    }

    public int getId() {
        return id;
    }

    public int getRotation() {
        return rotation;
    }

    public StreamState getStreamState() {
        return streamState;
    }

    public void setStreamState(StreamState streamState) {
        this.streamState = streamState;
    }

    public int getDirection() {
        return direction;
    }

    public Vec3i getPos1() {
        return pos1;
    }

    public Vec3i getPos2() {
        return pos2;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRootMapId() {
        return rootMapId;
    }

    public int getLastMapId() {
        return lastMapId;
    }

    public int[] getMapIds() {
        return mapIds;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public Vec3d getCenter() {
        return center;
    }

    public ItemFrameEntity getRootEntity() {
        return rootEntity;
    }

    public void setRootEntity(ItemFrameEntity rootEntity) {
        this.rootEntity = rootEntity;
    }

    public void setCenter(Vec3d center) {
        this.center = center;
    }

    public DisplayRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(DisplayRenderer renderer) {
        this.renderer = renderer;
    }
}
