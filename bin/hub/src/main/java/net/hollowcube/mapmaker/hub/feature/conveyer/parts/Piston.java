package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.MathUtils;

public class Piston implements ConveyerPart {
    private static final Vec pistonScale = new Vec(10.1, 10.1, 10.1);
    private final Point from;
    private final Direction direction;
    private final ArrayList<ConveyerPart> children;
    private final ConveyerPart parent;
    private final NpcItemModel piston;
    private PistonState state = PistonState.IDLE;
    private int currentCooldown = state.cooldown;
    private int currentAnimation = state.animationTime;
    private ConveyerGood currentGood;
    private final Vec directionVector;
    private final Quaternion defaultRotation = Quaternion.fromEulerAngles(new Pos(0, 0, 90));
    private final Vec idleTranslation;

    public Piston(
            Direction direction,
            Point handOverPoint,
            Point pistonCenter,
            ConveyerPart parent,
            HubMapWorld server,
            ConveyerPart... children
    ) {
        parent.children().add(this);
        this.from = handOverPoint;
        this.direction = direction;
        this.children = new ArrayList<>(Arrays.asList(children));
        this.parent = parent;
        this.directionVector = direction.vec().mul(6).div(PistonState.PUSHING.animationTime);
        this.idleTranslation = this.direction.opposite().mul(7.3);

        this.piston = new NpcItemModel();
        this.piston.setStatic(true);
        this.piston.setModel(Material.LIGHT, BadSprite.require("hub/extra/crusher"));
        this.piston.getEntityMeta().setTransformationInterpolationDuration(5);
        this.piston.getEntityMeta().setPosRotInterpolationDuration(1);
        this.piston.getEntityMeta().setScale(pistonScale);
        this.piston.getEntityMeta().setTranslation(this.idleTranslation);
        this.piston.setInstance(server.instance(), pistonCenter.add(0.5, 2.5, 0.5));
        this.piston.getEntityMeta().setLeftRotation(defaultRotation.mulThis(Quaternion.fromEulerAngles(Crane.getRotationFromStructureNorth(this.direction))).normalizeThis().into());
    }

    @Override
    public HandOverResult handOver(Point point, ConveyerGood good) {
        this.currentGood = good;
        swapToState(PistonState.PUSHING);
        return HandOverResult.ACCEPT;
    }

    @Override
    public boolean shouldBePaused() {
        return this.state != PistonState.IDLE || ConveyerPart.super.shouldBePaused();
    }

    private void swapToNextState() {
        this.swapToState(this.state.getNextState());
    }

    private void swapToState(Piston.PistonState state) {
        this.state = state;
        this.currentCooldown = this.state.cooldown;
        this.currentAnimation = this.state.animationTime;
    }

    @Override
    public void tick(MapInstance instance) {
        currentAnimation--;
        if (currentCooldown-- <= 0) {
            this.swapToNextState();
        }
        float delta = MathUtils.clamp(1 - ((float) this.currentAnimation) / this.state.animationTime, 0, 1);
        switch (this.state) {
            case PUSHING -> {
                var meta = this.piston.getEntityMeta();
                meta.setTranslation(meta.getTranslation().add(directionVector));
                if (this.currentGood != null) {
                    this.currentGood.good().teleport(this.currentGood.good().getPosition().add(this.directionVector));
                    if (delta == 1) {
                        this.children.getFirst().handOver(currentGood);
                        this.currentGood = null;
                    }
                }
            }
            case RETRACTING -> {
                var meta = this.piston.getEntityMeta();
                meta.setTranslation(meta.getTranslation().add(directionVector.mul(-1)));
                if (delta == 1) {
                    meta.setTranslation(this.idleTranslation);
                }
            }
        }
    }

    @Override
    public List<ConveyerPart> children() {
        return this.children;
    }

    @Override
    public Point handOverPoint() {
        return this.from;
    }

    enum PistonState {
        IDLE(0),
        PUSHING(20),
        RETRACTING(20),
        ;

        private final int cooldown;
        private final int animationTime;

        PistonState(int cooldown) {
            this(cooldown, cooldown);
        }

        PistonState(int cooldown, int animationTime) {
            this.cooldown = cooldown;
            this.animationTime = animationTime;
        }

        Piston.PistonState getNextState() {
            return switch (this) {
                case IDLE, RETRACTING -> IDLE;
                case PUSHING -> RETRACTING;
            };
        }
    }
}
