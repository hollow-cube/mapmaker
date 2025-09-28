package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.setting.SavedMapSettings;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.Codec;

public class Attachments {

    public static final PlayState.Attachment<Integer> PROGRESS_INDEX = PlayState.attachment(
            Key.key("mapmaker:progress_index"), Codec.INT);
    public static final PlayState.Attachment<Integer> RESET_HEIGHT = PlayState.attachment(
            Key.key("mapmaker:reset_height"), Codec.INT);
    public static final PlayState.Attachment<PotionEffectList> POTION_EFFECTS = PlayState.attachment(
            Key.key("mapmaker:potion_effects"), PotionEffectList.CODEC, PotionEffectList::copy);
    public static final PlayState.Attachment<Boolean> ELYTRA = PlayState.attachment(
            Key.key("mapmaker:elytra"), Codec.BOOLEAN);
    public static final PlayState.Attachment<HotbarItems> HOTBAR_ITEMS = PlayState.attachment(
            Key.key("mapmaker:hotbar_items"), HotbarItems.CODEC);
    public static final PlayState.Attachment<SavedMapSettings> SETTINGS = PlayState.attachment(
            Key.key("mapmaker:settings"), SavedMapSettings.CODEC);

    // If set, indicates that the start plate actions have been applied.
    public static final PlayState.Attachment<Boolean> START_ACTIONS_APPLIED = PlayState.attachment(
            Key.key("mapmaker:start_actions_applied"), Codec.BOOLEAN);

}
