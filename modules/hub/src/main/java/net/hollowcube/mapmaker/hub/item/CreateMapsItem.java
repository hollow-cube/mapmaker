package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class CreateMapsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hammer"), "hammer");
    public static final String ID = "mapmaker:create_maps";

    private final Controller guiController;

    public CreateMapsItem(@NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY | LEFT_CLICK_ENTITY);
        this.guiController = guiController;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void leftClicked(@NotNull Click click) {
        if (click.hand() != PlayerHand.MAIN) return;

        var player = click.player();
        var target = click.entity();
        if (target.getEntityType() != EntityType.PLAYER || !(target instanceof Player bonkee)) return;

        player.playSound(Sound.sound(Key.key("item.toy.squeak"), Sound.Source.MASTER, 1f, ThreadLocalRandom.current().nextFloat(0.9f, 1.1f)), Sound.Emitter.self());
        spawnBonkEntity(click.instance(), target.getPosition(), player, bonkee);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        guiController.show(player, CreateMaps::new);
    }

    private static void spawnBonkEntity(Instance instance, Point position, Player bonker, Player bonkee) {
        var random = ThreadLocalRandom.current();
        var bonkEntity = new Entity(EntityType.TEXT_DISPLAY) {{
            hasPhysics = false;
        }};
        bonkEntity.setNoGravity(true);
        bonkEntity.setAutoViewable(false);
        var meta = (TextDisplayMeta) bonkEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setBackgroundColor(0);

        double bonkeeScale = bonkee.getAttributeValue(Attribute.SCALE);
        double scale = 0.75 * bonkeeScale;
        meta.setScale(new Vec(scale, scale, 1));
        meta.setNotifyAboutChanges(true);
        var offsetPosition = new Vec(random.nextDouble(-1, 1), random.nextDouble(2, 2.5), random.nextDouble(-1, 1))
                .mul(bonkeeScale); // Tiny bonk for tiny person
        bonkEntity.setInstance(instance, position.add(offsetPosition));
        bonkEntity.addViewer(bonker);

        bonkEntity.scheduler().buildTask(() -> doBonkAnimation(meta, scale, bonkeeScale))
                .delay(TaskSchedule.tick(3))
                .repeat(TaskSchedule.tick(4))
                .schedule();
        bonkEntity.scheduleRemove(10, TimeUnit.SERVER_TICK);
    }

    private static void doBonkAnimation(TextDisplayMeta meta, double startScale, double bonkeeScale) {
        meta.setNotifyAboutChanges(false);
        meta.setText(Component.text("BONK!", NamedTextColor.RED));
        meta.setTransformationInterpolationDuration(3);
        meta.setTransformationInterpolationStartDelta(0);
        if (meta.getScale().x() == startScale) {
            meta.setScale(new Vec(1.5 * bonkeeScale, 1.5 * bonkeeScale, 1));
        } else {
            meta.setScale(new Vec(1.2 * bonkeeScale, 1.2 * bonkeeScale, 1));
        }
        meta.setNotifyAboutChanges(true);
    }

}
