package net.hollowcube.aj.entity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.hollowcube.aj.Model;
import net.hollowcube.aj.Node;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelEntity extends Entity {
    public static void main(String[] args) throws Exception {
        var server = MinecraftServer.init();

        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/qbdg/ajmodel/checkpoint.json");
        var json = new Gson().fromJson(Files.readString(path), JsonElement.class);
        var model = Model.CODEC.decode(new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process()), json).orElseThrow();

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    event.setSpawningInstance(instance);
                    event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    event.getPlayer().setGameMode(GameMode.CREATIVE);
                })
                .addListener(PlayerChatEvent.class, event -> {
                    var entity = new ModelEntity(model, null);
                    entity.setInstance(instance, event.getPlayer().getPosition().withView(0, 0));
                });
        server.start("0.0.0.0", 25565);
    }

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(-50_000_000);

    private final List<Bone> rootBones = new ArrayList<>();
    private final List<Integer> leafEntityIds = new ArrayList<>();

    public ModelEntity(@NotNull Model model, @Nullable UUID uuid) {
        super(EntityType.INTERACTION, Objects.requireNonNullElseGet(uuid, UUID::randomUUID));

        setNoGravity(true);
        hasPhysics = false;
        collidesWithEntities = false;

        Map<UUID, Bone> bones = new HashMap<>();
        for (var entry : model.nodes().entrySet()) {
            bones.put(entry.getKey(), switch (entry.getValue()) {
                case Node.TextDisplay node -> new Bone.EntityBone(node);
                case Node.Struct node -> new Bone.ParentBone(node);
            });
        }

        for (var bone : bones.values()) {
            if (bone.node().parent() != null) {
                if (!(bones.get(bone.node().parent()) instanceof Bone.ParentBone parent))
                    throw new IllegalStateException("Parent node must be a ParentBone");
                parent.addChild(bone);
            } else {
                this.rootBones.add(bone);
            }

            if (bone instanceof Bone.EntityBone entity) {
                this.leafEntityIds.add(entity.entityId());
            }
        }

        var animation = model.animations().values().stream().findFirst().orElseThrow();
        for (var entry : animation.animators().entrySet()) {
            var bone = Objects.requireNonNull(bones.get(entry.getKey()));
            bone.setAnimator(new Animator.AnimatorImpl(entry.getValue()));
        }
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);

        for (var bone : this.rootBones) bone.spawn(player);
        player.sendPacket(new SetPassengersPacket(getEntityId(), this.leafEntityIds));

    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        player.sendPacket(new DestroyEntitiesPacket(this.leafEntityIds));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

//        teleport(getPosition().withX(x -> x + 0.1f));

        if (getAliveTicks() < 40) return;

        for (var bone : this.rootBones) {
            bone.update(this, Vec.ZERO, Vec.ONE);
        }
    }
}
