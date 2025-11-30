package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.List;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerItemModel;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.MathUtils;

public final class Crane implements ConveyerPart {
    private static final Vec towerScale = new Vec(10, 10, 10);
    private static final Vec forkScale = new Vec(13, 13, 13);

    private static final float MAX_HEIGHT = 7.98f;
    private static final float MIN_HEIGHT = 3.125f;
    private static final float HEIGHT_DELTA = MAX_HEIGHT - MIN_HEIGHT;

    private final List<ConveyerPart> children;
    private final ConveyerPart parent;
    private final List<ConveyerEnd> destinations;
    private final Direction rotation;
    private final Point pickupPoint;
    private final Point craneCenter;
    private final Quaternion from;
    private final Direction fromDirection;
    private final Quaternion to;
    private final Direction toDirection;
    private final NpcItemModel tower;
    private final NpcItemModel fork;
    private CraneState state = CraneState.IDLE;
    private int currentCooldown = state.cooldown;
    private int currentAnimation = state.animationTime;
    private WrappedGood currentGood = null;

    public Crane(
            List<ConveyerPart> children,
            ConveyerPart parent,
            Direction from,
            Direction to,
            Direction rotation,
            Point pickupPoint,
            Point craneCenter,
            HubMapWorld server
    ) {
        this.rotation = rotation;
        this.pickupPoint = pickupPoint;
        parent.children().add(this);
        this.children = children;
        this.parent = parent;
        this.destinations = this.children.stream().map(ConveyerPart::getDestinations).flatMap(List::stream).toList();
        this.from = Quaternion.fromEulerAngles(getRotationFromStructureNorth(from)).normalizeThis();
        this.fromDirection = from;
        this.to = Quaternion.fromEulerAngles(getRotationFromStructureNorth(to)).normalizeThis();
        this.toDirection = to;
        this.craneCenter = craneCenter;

        this.tower = new NpcItemModel();
        this.tower.setStatic(true);
        this.tower.setModel(Material.LIGHT, BadSprite.require("hub/extra/crane_2"));
        this.tower.getEntityMeta().setTransformationInterpolationDuration(2);
        this.tower.getEntityMeta().setPosRotInterpolationDuration(1);
        this.tower.getEntityMeta().setTranslation(new Pos(0, 5, 0));
        this.tower.getEntityMeta().setScale(towerScale);
        this.tower.setInstance(server.instance(), craneCenter.add(0.5, 0, 0.5));
        this.tower.getEntityMeta().setLeftRotation(this.from.into());

        this.fork = new NpcItemModel();
        this.fork.setStatic(true);
        this.fork.setModel(Material.LIGHT, BadSprite.require("hub/extra/crane_fork_2"));
        this.fork.getEntityMeta().setTransformationInterpolationDuration(2);
        this.fork.getEntityMeta().setPosRotInterpolationDuration(1);
        this.fork.getEntityMeta().setTranslation(new Pos(0, 7.98, 0));
        this.fork.getEntityMeta().setScale(forkScale);
        this.fork.setInstance(server.instance(), craneCenter.add(0.5, 0, 0.5));
        this.fork.getEntityMeta().setLeftRotation(this.from.into());
    }

    static Vec getRotationFromStructureNorth(Direction direction) {
        return switch (direction) {
            case Direction.SOUTH -> new Vec(0, -90, 0);
            case Direction.WEST -> new Vec(0, 180, 0);
            case Direction.NORTH -> new Vec(0, 90, 0);
            default -> Vec.ZERO;
        };
    }

    static Vec getRotationFromTrueNorth(Direction direction) {
        return switch (direction) {
            case Direction.SOUTH -> new Vec(0, -90, 0);
            case Direction.WEST -> new Vec(0, 180, 0);
            case Direction.NORTH -> new Vec(0, 270, 0);
            default -> Vec.ZERO;
        };
    }

    public List<ConveyerPart> children() {
        return this.children;
    }

    @Override
    public Point handOverPoint() {
        return this.pickupPoint;
    }

    private void swapToNextState() {
        this.swapToState(this.state.getNextState());
    }

    private void swapToState(CraneState state) {
        this.state = state;
        this.currentCooldown = this.state.cooldown;
        this.currentAnimation = this.state.animationTime;
    }

    @Override public HandOverResult handOver(Point point, ConveyerGood good) {
        if (state == CraneState.IDLE) {
            this.swapToState(CraneState.GOING_DOWN);
            this.currentGood = new WrappedGood(
                    good,
                    new Quaternion(good.good().getEntityMeta().getLeftRotation())
            );
            var translation = this.currentGood.good()
                    .getEntityMeta()
                    .getTranslation()
                    .add(this.fromDirection.vec().mul(7).add(0, 0.5, 0));
            this.currentGood.good().sync(model -> {
                var meta = model.getEntityMeta();
                meta.setPosRotInterpolationDuration(2);
                meta.setTransformationInterpolationDuration(2);
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTranslation(translation);

                model.updatePosition(this.craneCenter.asPos().add(0.5, 0, 0.5));
            });
            return HandOverResult.ACCEPT;
        }
        return HandOverResult.REJECT;
    }

    @Override public void tick(MapInstance instance) {
        currentAnimation--;
        if (currentCooldown-- <= 0) {
            this.swapToNextState();
        }
        float delta = MathUtils.clamp(1 - ((float) this.currentAnimation) / this.state.animationTime, 0, 1);
        switch (state) {
            case GOING_DOWN, PUTTING_DOWN -> {
                this.fork.getEntityMeta().setTranslation(new Vec(0, interpolateY(1 - delta), 0));

                if (this.currentGood != null && state == CraneState.PUTTING_DOWN) {
                    var currentTranslation = this.currentGood.good().getEntityMeta().getTranslation();
                    this.currentGood.good().getEntityMeta().setTranslation(
                            currentTranslation.withY(5 - delta * 5 + 0.5)
                    );

                    if (delta == 1) {
                        this.currentGood.good().sync(model -> {
                            var meta = model.getEntityMeta();
                            meta.setTransformationInterpolationStartDelta(-4);
                            meta.setTransformationInterpolationDuration(0);
                            meta.setPosRotInterpolationDuration(0);
                            meta.setTranslation(Vec.ZERO);

                            model.updatePosition(children.getFirst().handOverPoint().asPos().add(0.5));
                        });
                    }
                }
            }
            case PICKING_UP, GOING_UP -> {
                this.fork.getEntityMeta().setTranslation(new Vec(0, interpolateY(delta), 0));


                if (currentCooldown == 0 && this.state == CraneState.GOING_UP && this.currentGood != null) {
                    var nextPart = children.getFirst();
                    this.currentGood.good().getEntityMeta().setTransformationInterpolationDuration(2);
                    this.currentGood.good().getEntityMeta().setPosRotInterpolationDuration(2);
                    nextPart.handOver(this.currentGood.conveyerGood);
                    this.currentGood = null;
                }
                if (this.currentGood != null && this.state != CraneState.GOING_UP) {
                    var currentTranslation = this.currentGood.good().getEntityMeta().getTranslation();
                    this.currentGood.good().getEntityMeta().setTranslation(
                            currentTranslation.withY(delta * 5 + 0.5)
                    );
                }
            }
            case TURNING -> {
                var newQuaternion = from.interpolate(to, delta);
                var newRotation = newQuaternion.into();


                this.fork.getEntityMeta().setLeftRotation(newRotation);
                this.tower.getEntityMeta().setLeftRotation(newRotation);


                if (this.currentGood != null) {
                    var fromVec = rotation.vec();
                    var offset = Quaternion.fromEulerAngles(getRotationFromTrueNorth(fromDirection));

                    var newVec = rotate(fromVec, newQuaternion.toRotationMatrix());

                    this.currentGood.good().getEntityMeta().setLeftRotation(
                           newQuaternion
                                   .mulThis(offset)
                                   .mulThis(currentGood.rotation)
                                    .normalizeThis().into());
                    this.currentGood.good().getEntityMeta().setTranslation(newVec.mul(7).add(0, 5.5, 0));
                }
            }
            case RETURNING -> {
                var newRotation = to.interpolate(from, delta).into();
                this.fork.getEntityMeta().setLeftRotation(newRotation);
                this.tower.getEntityMeta().setLeftRotation(newRotation);
            }
        }
    }

    @Override
    public boolean shouldBePaused() {
        return this.state == CraneState.GOING_DOWN || this.state == CraneState.PICKING_UP;
    }

    private Vec rotate(Vec input, double[] rotationMatrix) {
        return new Vec(
                input.x() * rotationMatrix[0] + input.y() * rotationMatrix[1] + input.z() * rotationMatrix[2],
                input.x() * rotationMatrix[3] + input.y() * rotationMatrix[4] + input.z() * rotationMatrix[5],
                input.x() * rotationMatrix[6] + input.y() * rotationMatrix[7] + input.z() * rotationMatrix[8]
        );
    }

    private double interpolateY(float delta) {
        return MIN_HEIGHT + HEIGHT_DELTA * delta;
    }

    @Override public String toString() {
        return "Crane[" + "children=" + children + ']';
    }


    enum CraneState {
        IDLE(0),
        GOING_DOWN(20, 18),
        PICKING_UP(20, 18),
        TURNING(20),
        PUTTING_DOWN(20, 18),
        GOING_UP(15, 15),
        RETURNING(15),
        ;

        final int cooldown;
        final int animationTime;


        CraneState(int cooldown) {
            this(cooldown, cooldown);
        }

        CraneState(int cooldown, int animationTime) {
            this.cooldown = cooldown;
            this.animationTime = animationTime;
        }

        CraneState getNextState() {
            return switch (this) {
                case IDLE, RETURNING -> IDLE;
                case GOING_DOWN -> PICKING_UP;
                case PICKING_UP -> TURNING;
                case TURNING -> PUTTING_DOWN;
                case PUTTING_DOWN -> GOING_UP;
                case GOING_UP -> RETURNING;
            };
        }
    }

    record WrappedGood(
            ConveyerGood conveyerGood,
            Quaternion rotation
    ) {
        public ConveyerItemModel good() {
            return conveyerGood.good();
        }

    }
}
