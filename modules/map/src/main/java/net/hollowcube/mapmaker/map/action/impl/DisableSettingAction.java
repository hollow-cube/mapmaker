package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.impl.EnableSettingAction.Data;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.kyori.adventure.text.Component.translatable;

/// Only supports boolean settings for now, should revisit later.
///
/// Note that the data type is shared with enable setting currently.
public class DisableSettingAction extends AbstractAction<Data> {
    public static final DisableSettingAction INSTANCE = new DisableSettingAction();

    private static final Sprite SPRITE = new Sprite("action/icon/setting_subtract", 2, 3);

    public DisableSettingAction() {
        super("mapmaker:disable_setting", Data.CODEC, new Data(null));
    }

    @Override
    public @NotNull Sprite sprite(@Nullable Data data) {
        return SPRITE;
    }

    @Override
    public @NotNull TranslatableComponent thumbnail(@Nullable Data data) {
        if (data == null || data.setting() == null)
            return translatable("gui.action.disable_setting.thumbnail.empty");
        return translatable("gui.action.disable_setting.thumbnail", List.of(
                translatable(EnableSettingAction.SETTINGS_TRANSLATION_KEYS.get(data.setting()))
        ));
    }


    @Override
    public @NotNull AbstractActionEditorPanel<Data> createEditor(@NotNull ActionList.ActionData<Data> actionData) {
        return new EnableSettingAction.Editor(actionData);
    }

}
