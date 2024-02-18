package net.hollowcube.mapmaker.map.gui.biome;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.biome.BiomeInfo;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BiomeEditorView extends View {
    private static final TextColor COLOR_MISSING_BG = TextColor.color(0x000000);
    private static final TextColor COLOR_MISSING_FG = getForegroundColor(COLOR_MISSING_BG);

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
        pushView(c -> new HexInputAnvil(c, "sky_color_rename", biomeInfo.getSkyColor()));
    }

    @Signal("sky_color_rename")
    public void handleSkyColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setSkyColor);
    }

    @Action("fog_color")
    public void handleFogColorButton() {
        pushView(c -> new HexInputAnvil(c, "fog_color_rename", biomeInfo.getFogColor()));
    }

    @Signal("fog_color_rename")
    public void handleFogColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setFogColor);
    }

    @Action("water_color")
    public void handleWaterColorButton() {
        pushView(c -> new HexInputAnvil(c, "water_color_rename", biomeInfo.getWaterColor()));
    }

    @Signal("water_color_rename")
    public void handleWaterColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setWaterColor);
    }

    @Action("water_fog_color")
    public void handleWaterFogColorButton() {
        pushView(c -> new HexInputAnvil(c, "water_fog_color_rename", biomeInfo.getWaterFogColor()));
    }

    @Signal("water_fog_color_rename")
    public void handleWaterFogColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setWaterFogColor);
    }

    @Action("grass_color")
    public void handleGrassColorButton() {
        pushView(c -> new HexInputAnvil(c, "grass_color_rename", biomeInfo.getGrassColor()));
    }

    @Signal("grass_color_rename")
    public void handleGreenColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setGrassColor);
    }

    @Action("foliage_color")
    public void handleFoliageColorButton() {
        pushView(c -> new HexInputAnvil(c, "foliage_color_rename", biomeInfo.getFoliageColor()));
    }

    @Signal("foliage_color_rename")
    public void handleFoliageColorRename(@NotNull String color) {
        updateColorField(color, biomeInfo::setFoliageColor);
    }

    private void updateColorField(@NotNull String newColor, @NotNull Consumer<String> setter) {
        var parsed = TextColor.fromCSSHexString(newColor);
        if (parsed == null) return;
        setter.accept(newColor);
        updateContents();
    }

    private void updateColorText(@NotNull Text text, @Nullable String colorHex) {
        if (colorHex == null) {
            text.setSpriteColorModifier(COLOR_MISSING_BG);
            text.setText("None", COLOR_MISSING_FG);
        } else {
            var color1 = TextColor.fromCSSHexString(colorHex);
            if (color1 == null) return;
            text.setSpriteColorModifier(color1);
            text.setText(colorHex, getForegroundColor(color1));
        }
    }

    public static TextColor getForegroundColor(TextColor backgroundColor) {
        return isLight(backgroundColor) ? NamedTextColor.BLACK : NamedTextColor.WHITE;
    }

    public static boolean isLight(TextColor color) {
        double luminance = (0.299 * color.red() + 0.587 * color.green() + 0.114 * color.blue()) / 255;
        return luminance > 0.5;
    }

}
