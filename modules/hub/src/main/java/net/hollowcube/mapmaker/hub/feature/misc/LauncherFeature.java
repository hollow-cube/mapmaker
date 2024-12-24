package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.physics.BoundingBox;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@AutoService(HubFeature.class)
public class LauncherFeature implements HubFeature {
    private static final SoundEvent EMPTY_SOUND = SoundEvent.of(NamespaceID.from("not.a.real.sound"), 0f);

    // From the perspective of the player when they spawn in the world.
    private final LauncherEntity left = new LauncherEntity();
    private final LauncherEntity right = new LauncherEntity();

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        left.setInstance(world.instance(), new Pos(-14.5, 37.5, 8.5, 180, 0));
        right.setInstance(world.instance(), new Pos(-14.5, 37.5, -7.5, 180, 0));
    }

    private static class LauncherEntity extends NpcItemModel {

        private enum State {
            IDLE(0),
            LAUNCHING(3),
            LAUNCHED(40),
            RETURNING(20),
            COOLDOWN(20);

            private final int duration;

            State(int duration) {
                this.duration = duration;
            }
        }

        private State state = State.IDLE;
        private int remaining = 0; // Tick countdown to next state change

        private static final Vec MIN = new Vec(-12, 37, 7).sub(new Pos(-14.5, 37.5, 8.5));
        private static final Vec MAX = new Vec(-9, 39, 10).sub(new Pos(-14.5, 37.5, 8.5));
        private static final Vec SIZE = MAX.sub(MIN);

        private BoundingBox bb = BoundingBox.ZERO;

        public LauncherEntity() {
            setStatic(true);
            setModel(Material.STICK, 12);
            getEntityMeta().setScale(new Vec(16));
        }

        @Override
        public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
            this.bb = new BoundingBox(SIZE.x(), SIZE.y(), SIZE.z(), MIN.add(spawnPosition));
            return super.setInstance(instance, spawnPosition);
        }

        @Override
        public void update(long time) {
//            if (this.state == State.COOLDOWN) {
//                var sound = remaining % 2 == 0 ? SoundEvent.BLOCK_STONE_BUTTON_CLICK_OFF : SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF;
//                getViewersAsAudience().playSound(Sound.sound(sound, Sound.Source.BLOCK, 0.5f, 0.3f),
//                        getPosition().x(), getPosition().y(), getPosition().z());
//            }
//
//            if (remaining > 0) {
//                remaining--;
//                return;
//            }
//
//            switch (this.state) {
//                case IDLE -> {
//                    // Try to find players to launch
//
//                    int launched = 0;
//                    for (var player : getInstance().getPlayers()) {
//                        var isInBox = this.bb.intersectBox(player.getPosition().mul(-1), player.getBoundingBox());
//                        if (!isInBox) continue;
//
//                        Vec motion;
//                        if (player.getPosition().yaw() > 90 || player.getPosition().yaw() < -90) {
//                            motion = new Vec(-16.5, 3, getPosition().x() < 0 ? -1f : 2f); // Send to middle
//                        } else {
//                            motion = new Vec(-16, 3, getPosition().x() < 0 ? 2f : -1f); // Send to edge
//                        }
//                        player.setVelocity(Vec.ZERO);
//                        player.sendPacket(makeExplosion(player.getPosition(), motion));
//                        launched++;
//                    }
//
//                    if (launched > 0) setState(State.LAUNCHING);
//                    else this.remaining = 5;
//                }
//                case LAUNCHING -> {
//                    editEntityMeta(ItemDisplayMeta.class, meta -> {
//                        meta.setTransformationInterpolationStartDelta(0);
//                        meta.setTransformationInterpolationDuration(3);
//                        meta.setLeftRotation(new Quaternion(new Vec(0, 0, 1), Math.toRadians(-90)).into());
//                    });
//
//                    this.remaining = state.duration;
//                    this.state = State.LAUNCHED;
//                }
//                case LAUNCHED -> {
//                    this.remaining = state.duration;
//                    this.state = State.RETURNING;
//                }
//                case RETURNING -> {
//                    editEntityMeta(ItemDisplayMeta.class, meta -> {
//                        meta.setTransformationInterpolationStartDelta(0);
//                        meta.setTransformationInterpolationDuration(20);
//                        meta.setLeftRotation(new Quaternion(new Vec(0, 0, 1), Math.toRadians(0)).into());
//                    });
//
//                    this.remaining = state.duration;
//                    this.state = State.COOLDOWN;
//                }
//                case COOLDOWN -> {
//                    this.remaining = state.duration;
//                    this.state = State.IDLE;
//                }
//            }
        }

        private void setState(@NotNull State state) {
            this.state = state;
        }

        private static @NotNull ExplosionPacket makeExplosion(@NotNull Point position, @NotNull Vec motion) {
            return new ExplosionPacket(position, motion, Particle.BLOCK.withBlock(Block.AIR), EMPTY_SOUND);
        }
    }
}
