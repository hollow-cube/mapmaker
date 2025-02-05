package net.hollowcube.mapmaker.map.feature.edit.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EnterTestModeItem extends ItemHandler {

    public static final String ID = "mapmaker:enter_test_mode";
    public static final EnterTestModeItem INSTANCE = new EnterTestModeItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/enter_test_mode"));

    private EnterTestModeItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    public int customModelData() {
        return SPRITE.cmd();
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld buildMode)) return;

        if (world.map().settings().getVariant() != MapVariant.PARKOUR) {
            player.sendMessage(Component.translatable("item.test.enter.not_in_parkour"));
            return;
        }

        FutureUtil.submitVirtual(() -> {
            SpectateHandler.setCheckpoint(player, null);
            buildMode.enterTestMode(player);
        });
    }

}
