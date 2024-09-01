package net.hollowcube.aj;

import net.hollowcube.aj.entity.AnimEntityV2;
import net.hollowcube.aj.entity.AnimQuery;
import net.hollowcube.aj.model.ExportedModel;
import net.hollowcube.aj.model.ModelNode;
import net.hollowcube.mql.MqlModule;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SpawnedModelImpl implements SpawnedModel {
    private final Set<Player> viewers = new HashSet<>();

    private UUID activeVariant;

    private final Entity root;
    private final Map<UUID, AnimEntityV2> allBones = new HashMap<>();
    private final Map<UUID, AnimEntityV2> rootBones = new HashMap<>();

    private final ExportedModel config;
    private final MqlModule.Instance scriptInstance;

    public SpawnedModelImpl(@NotNull ExportedModel config, @NotNull MqlModule.Instance scriptInstance, @NotNull Instance instance) {
        this.config = config;
        this.scriptInstance = scriptInstance;

        activeVariant = config.variants().entrySet().stream()
                .filter(e -> e.getValue().isDefault())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();

        var baseItemStack = ItemStack.of(config.settings().displayItem());
        var variant = config.variants().get(activeVariant);

        root = new Entity(EntityType.MARKER);
        for (var entry : config.nodes().entrySet()) {
            var node = entry.getValue();
            switch (node) {
                case ModelNode.Bone bone -> {
                    var entity = new AnimEntityV2(EntityType.ITEM_DISPLAY, bone.props());

                    var meta = (ItemDisplayMeta) entity.getEntityMeta();
                    meta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
                    meta.setItemStack(baseItemStack.with(ItemComponent.CUSTOM_MODEL_DATA,
                            variant.models().get(bone.uuid()).customModelData()));

                    rootBones.put(entry.getKey(), entity);
                }
                case ModelNode.ItemDisplay itemDisplay -> {
                    var entity = new AnimEntityV2(EntityType.ITEM_DISPLAY, itemDisplay.props());
                    var meta = (ItemDisplayMeta) entity.getEntityMeta();
                    meta.setItemStack(ItemStack.of(itemDisplay.item()));
                    rootBones.put(entry.getKey(), entity);
                }
                case ModelNode.BlockDisplay blockDisplay -> {
                    var entity = new AnimEntityV2(EntityType.BLOCK_DISPLAY, blockDisplay.props());
                    var meta = (BlockDisplayMeta) entity.getEntityMeta();
                    meta.setBlockState(blockDisplay.block());
                    rootBones.put(entry.getKey(), entity);
                }
                case ModelNode.TextDisplay textDisplay -> {
                    var entity = new AnimEntityV2(EntityType.TEXT_DISPLAY, textDisplay.props());
                    var meta = (TextDisplayMeta) entity.getEntityMeta();
                    meta.setText(textDisplay.text());
                    rootBones.put(entry.getKey(), entity);
                }
                case ModelNode.Camera camera -> {
                    // Do nothing
                }
                case ModelNode.Locator locator -> {
                    // Do nothing
                }
                case ModelNode.Struct struct -> {
                    // Do nothing

                    var entity = new AnimEntityV2(EntityType.BLOCK_DISPLAY, struct.props());
                    var meta = (BlockDisplayMeta) entity.getEntityMeta();
                    meta.setBlockState(Block.AIR);
                    rootBones.put(entry.getKey(), entity);
                }
            }
        }

        var spawnFutures = new ArrayList<CompletableFuture<Void>>();
        spawnFutures.add(root.setInstance(instance));
        for (var entity : rootBones.values())
            spawnFutures.add(entity.setInstance(instance));
        CompletableFuture.allOf(spawnFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                    for (var entity : rootBones.values()) root.addPassenger(entity);
                    allBones.putAll(rootBones);

                    var withParent = new HashSet<UUID>();

                    // For each entity, if it has a parent then add it to the parent
                    for (var id : Set.copyOf(rootBones.keySet())) {
                        var node = config.nodes().get(id);
                        if (node.parent() != null && !node.parent().isEmpty()) {
                            withParent.add(id);
                            var parent = rootBones.get(UUID.fromString(node.parent()));
                            if (parent == null) {
                                System.err.println("Parent not found for " + id + " (name = " + node.parent() + ")");
                                continue;
                            }
                            parent.addChild(rootBones.get(id));
                        }
                    }

                    // For each entity, if it has a parent then remove it from the root
                    for (var id : withParent) {
                        rootBones.remove(id);
                    }

                    root.scheduler().submitTask(this::tick);
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private double lifetime = 0;

    private TaskSchedule tick() {
        var animators = config.animations().get(UUID.fromString("5b870340-d35e-81ae-89a0-66d353b2286c"));
//        var animators = config.animations().get(UUID.fromString("d855eb0f-db42-ac97-b4eb-6dca41f6ebbc"));
        if (lifetime > animators.duration()) {
            lifetime -= animators.duration();
        }

        var query = new AnimQuery(lifetime);

        for (var entry : rootBones.entrySet()) {


            entry.getValue().doFrame(
                    animators.animators(),
                    scriptInstance,
                    query,
                    Vec.ZERO,
                    new float[4],
                    Vec.ZERO
            );
        }

//        for (var animator : animators.animators().entrySet()) {
//            if (animator.getKey().equals("effects")) continue;
//
//            var bone = rootBones.get(UUID.fromString(animator.getKey()));
//            if (bone == null) continue;
//
//
//            bone.applyFrame(animators.duration(), scriptInstance, query, animator.getValue());
//        }

        lifetime += (1 / 20.);
        return TaskSchedule.nextTick();
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        if (!viewers.add(player)) return false;

        root.addViewer(player);
        for (var entity : allBones.values()) {
            entity.addViewer(player);
        }

        return true;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        if (!viewers.remove(player)) return false;

        root.removeViewer(player);
        for (var entity : allBones.values()) {
            entity.removeViewer(player);
        }

        return false;
    }

    @Override
    public @NotNull Set<@NotNull Player> getViewers() {
        return viewers;
    }

    @Override
    public @NotNull Pos position() {
        return root.getPosition();
    }

    @Override
    public void setPosition(@NotNull Pos position) {
        root.teleport(position);
        for (var entity : allBones.values()) {
            entity.teleport(position);
        }
    }

    @Override
    public void play(@NotNull String animationName) {

    }

    @Override
    public void stop(@NotNull String animationName) {

    }

    @Override
    public boolean isPlaying(@NotNull String animationName) {
        return false;
    }

    @Override
    public @NotNull String variant() {
        return "";
    }

    @Override
    public void setVariant(@NotNull String variantName) {

    }
}
