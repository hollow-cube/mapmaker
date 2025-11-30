package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.List;

import java.util.concurrent.ThreadLocalRandom;

import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.Material;
import net.minestom.server.utils.MathUtils;

public class Crusher implements ConveyerPart {
    private static final Vec pistonScale = new Vec(10.1, 10.1, 10.1);

    private final Point position;
    private final int distanceToFloor;
    private final ConveyerEnd end;
    private final ConveyerBelt parent;
    private Crusher.CrusherState state = Crusher.CrusherState.IDLE;
    private int currentCooldown = state.cooldown;
    private int currentAnimation = state.animationTime;
    private ConveyerGood currentGood;
    private NpcItemModel crusher;
    private final Vec idleTranslation = new Vec(0, 7.5, 0);


    public Crusher(
            Point position,
            int distanceToFloor,
            ConveyerEnd end,
            ConveyerBelt parent,
            HubMapWorld server
    ) {
        this.position = position;
        this.distanceToFloor = distanceToFloor;
        this.end = end;
        this.parent = parent;


        this.crusher = new NpcItemModel();
        this.crusher.setStatic(true);
        this.crusher.setModel(Material.LIGHT, BadSprite.require("hub/extra/crusher"));
        this.crusher.getEntityMeta().setTransformationInterpolationDuration(5);
        this.crusher.getEntityMeta().setPosRotInterpolationDuration(1);
        this.crusher.getEntityMeta().setScale(pistonScale);
        this.crusher.getEntityMeta().setTranslation(this.idleTranslation);
        this.crusher.getEntityMeta().setDisplayContext(ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND);
        this.crusher.setInstance(server.instance(), position.add(0.5, 2.5, 0.5));
    }

    private void swapToNextState() {
        this.swapToState(this.state.getNextState());
    }

    private void swapToState(Crusher.CrusherState state) {
        this.state = state;
        this.currentCooldown = this.state.cooldown;
        this.currentAnimation = this.state.animationTime;
    }

    @Override
    public HandOverResult handOver(Point point, ConveyerGood good) {
        if (ThreadLocalRandom.current().nextInt(1, 10) != 1) {
            return HandOverResult.REJECT;
        }
        this.currentGood = good;
        swapToState(CrusherState.CRUSHING);
        return HandOverResult.ACCEPT;
    }

    @Override
    public boolean shouldBePaused() {
        return this.state == CrusherState.CRUSHING;
    }

    @Override
    public void tick(MapInstance instance) {
        currentAnimation--;
        if (currentCooldown-- <= 0) {
            this.swapToNextState();
        }
        float delta = MathUtils.clamp(1 - ((float) this.currentAnimation) / this.state.animationTime, 0, 1);
        switch (this.state) {
            case CRUSHING -> {
                //var meta = this.crusher.getEntityMeta();
                //meta.setTranslation(meta.getTranslation().add(directionVector));
                //if (this.currentGood != null) {
                //    this.currentGood.good().teleport(this.currentGood.good().getPosition().add(this.directionVector));
                //    if (delta == 1) {
                //        this.children.getFirst().handOver(currentGood);
                //        this.currentGood = null;
                //    }
                //}
            }
            case RETRACTING -> {
                //var meta = this.crusher.getEntityMeta();
                //meta.setTranslation(meta.getTranslation().add(directionVector.mul(-1)));
                //if (delta == 1) {
                //    meta.setTranslation(this.idleTranslation);
                //}
            }
        }
    }

    @Override
    public List<ConveyerPart> children() {
        return List.of(end);
    }

    @Override
    public Point handOverPoint() {
        return position.sub(0, distanceToFloor, 0);
    }


    enum CrusherState {
        IDLE(0),
        CRUSHING(5, 3),
        RETRACTING(10),
        ;

        private final int cooldown;
        private final int animationTime;

        CrusherState(int cooldown) {
            this(cooldown, cooldown);
        }

        CrusherState(int cooldown, int animationTime) {
            this.cooldown = cooldown;
            this.animationTime = animationTime;
        }

        Crusher.CrusherState getNextState() {
            return switch (this) {
                case IDLE, RETRACTING -> IDLE;
                case CRUSHING -> RETRACTING;
            };
        }
    }
}
