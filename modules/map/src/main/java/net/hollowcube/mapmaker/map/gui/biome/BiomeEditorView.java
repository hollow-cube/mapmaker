package net.hollowcube.mapmaker.map.gui.biome;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.ColorUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.biome.BiomeInfo;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BiomeEditorView extends View {
    private static final TextColor COLOR_MISSING_BG = TextColor.color(0x000000);
    private static final TextColor COLOR_MISSING_FG = getForegroundColor(new Color(0x000000));

    private @Outlet("gui_title") Text guiTitleText;
    private @Outlet("biome_name") Text biomeNameText;

    private @Outlet("sky_color") Text skyColorText;
    private @Outlet("fog_color") Text fogColorText;
    private @Outlet("water_color") Text waterColorText;
    private @Outlet("water_fog_color") Text waterFogColorText;
    private @Outlet("grass_color") Text grassColorText;
    private @Outlet("foliage_color") Text foliageColorText;

    private final BiomeContainer container;
    private final BiomeInfo biomeInfo;

    public BiomeEditorView(@NotNull Context context, @NotNull BiomeContainer container, @NotNull BiomeInfo info) {
        super(context);
        this.container = container;
        this.biomeInfo = info;

        guiTitleText.setText("Biome Editor");

        updateContents();
    }

    private void updateContents() {
        if (biomeInfo.getName().isEmpty()) {
            biomeNameText.setText("Unnamed Biome", TextColor.color(0xB0B0B0));
        } else {
            biomeNameText.setText(biomeInfo.getName());
        }

        updateColorText(skyColorText, biomeInfo.getSkyColor());
        updateColorText(fogColorText, biomeInfo.getFogColor());
        updateColorText(waterColorText, biomeInfo.getWaterColor());
        updateColorText(waterFogColorText, biomeInfo.getWaterFogColor());
        updateColorText(grassColorText, biomeInfo.getGrassColor());
        updateColorText(foliageColorText, biomeInfo.getFoliageColor());
    }

    @Action("biome_name")
    public void handleNameButton() {
        pushView(c -> new BiomeNameInputAnvil(c, biomeInfo.getName()));
    }

    @Signal(BiomeNameInputAnvil.SIG_UPDATE_NAME)
    public void handleNameChange(@NotNull String newName) {
        if (newName.isEmpty() || container.hasCustomBiome(newName)) return;

        biomeInfo.setName(newName);
        updateContents();
    }

    @Action("sky_color")
    public void handleSkyColorButton() {
        pushView(c -> new HexInputAnvil(c, "sky_color_rename", ColorUtil.toHex(biomeInfo.getSkyColor())));
    }

    @Signal("sky_color_rename")
    public void handleSkyColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setSkyColor);
    }

    @Action("fog_color")
    public void handleFogColorButton() {
        pushView(c -> new HexInputAnvil(c, "fog_color_rename", ColorUtil.toHex(biomeInfo.getFogColor())));
    }

    @Signal("fog_color_rename")
    public void handleFogColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setFogColor);
    }

    @Action("water_color")
    public void handleWaterColorButton() {
        pushView(c -> new HexInputAnvil(c, "water_color_rename", ColorUtil.toHex(biomeInfo.getWaterColor())));
    }

    @Signal("water_color_rename")
    public void handleWaterColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setWaterColor);
    }

    @Action("water_fog_color")
    public void handleWaterFogColorButton() {
        pushView(c -> new HexInputAnvil(c, "water_fog_color_rename", ColorUtil.toHex(biomeInfo.getWaterFogColor())));
    }

    @Signal("water_fog_color_rename")
    public void handleWaterFogColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setWaterFogColor);
    }

    @Action("grass_color")
    public void handleGrassColorButton() {
        pushView(c -> new HexInputAnvil(c, "grass_color_rename", OpUtils.map(biomeInfo.getGrassColor(), ColorUtil::toHex)));
    }

    @Signal("grass_color_rename")
    public void handleGreenColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setGrassColor);
    }

    @Action("foliage_color")
    public void handleFoliageColorButton() {
        pushView(c -> new HexInputAnvil(c, "foliage_color_rename", OpUtils.map(biomeInfo.getFoliageColor(), ColorUtil::toHex)));
    }

    @Signal("foliage_color_rename")
    public void handleFoliageColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setFoliageColor);
    }

    private void updateColorField(@NotNull String newColor, @NotNull Consumer<RGBLike> setter) {
        var parsed = ColorUtil.fromHex(newColor);
        if (parsed == null) return;
        setter.accept(parsed);
        updateContents();
    }

    private void updateColorText(@NotNull Text text, @Nullable RGBLike color) {
        if (color == null) {
            text.setSpriteColorModifier(COLOR_MISSING_BG);
            text.setText("None", COLOR_MISSING_FG);
        } else {
            text.setSpriteColorModifier(TextColor.color(color));
            text.setText(ColorUtil.toHex(color), getForegroundColor(color));
        }
    }

    public static TextColor getForegroundColor(RGBLike backgroundColor) {
        return isLight(backgroundColor) ? NamedTextColor.BLACK : NamedTextColor.WHITE;
    }

    public static boolean isLight(RGBLike color) {
        double luminance = (0.299 * color.red() + 0.587 * color.green() + 0.114 * color.blue()) / 255;
        return luminance > 0.5;
    }

}
