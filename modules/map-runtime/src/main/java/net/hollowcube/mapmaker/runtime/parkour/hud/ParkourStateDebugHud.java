package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditAttributeAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Debug overlay for the current play state, shown whenever the player is in an
/// [ParkourState.AnyPlaying] state backed by a [PlayState].
///
/// Compact by design (short codes, single-row settings/attributes) to leave room for
/// variables: anchor markers cap the vertical offset at 127, so ~14 rows fit.
public class ParkourStateDebugHud implements PlayerHud.Module {
    private static final int OFFSET_X = 5;
    private static final int OFFSET_Y = 5;
    // Rows are DEFAULT_HEIGHT (9px) apart; the last one (plus its +1 shadow pass) must stay
    // within the marker's +127. 14 rows end at y=122, so there is 4px of slack: the group
    // separator below is free even at full capacity.
    private static final int MAX_ROWS = 1 + (126 - OFFSET_Y) / 9;
    private static final int GROUP_GAP = 4;

    // Shadowless with no background, so bright colors only.
    private static final TextColor LABEL = NamedTextColor.YELLOW;
    private static final TextColor VALUE = NamedTextColor.WHITE;
    private static final TextColor NAME = NamedTextColor.AQUA;
    private static final TextColor ON = NamedTextColor.GREEN;
    private static final TextColor OFF = NamedTextColor.RED;

    private static final List<Map.Entry<MapSetting<Boolean>, String>> SETTING_CODES = List.of(
        Map.entry(MapSettings.ONLY_SPRINT, "os"),
        Map.entry(MapSettings.NO_SPRINT, "ns"),
        Map.entry(MapSettings.NO_JUMP, "nj"),
        Map.entry(MapSettings.NO_SNEAK, "nk"),
        Map.entry(MapSettings.RESET_IN_WATER, "rw"),
        Map.entry(MapSettings.RESET_IN_LAVA, "rl"),
        Map.entry(MapSettings.NO_TURN, "nt"));

    private static final Map<Attribute, String> ATTRIBUTE_CODES = Map.of(
        Attribute.SCALE, "s",
        Attribute.BLOCK_INTERACTION_RANGE, "ir",
        Attribute.STEP_HEIGHT, "sh",
        Attribute.GRAVITY, "g");

    public static final ParkourStateDebugHud INSTANCE = new ParkourStateDebugHud();

    private ParkourStateDebugHud() {
    }

    @Override
    public @Nullable HudNode.Anchored render(Player player) {
        var playerData = PlayerData.fromPlayer(player);
        if (!playerData.getSetting(PlayerSettings.PARKOUR_DEBUG_HUD))
            return null;

        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || !(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return null;
        // Staff only during gameplay for now, may allow everyone later.
        var isMapTesting = p instanceof ParkourState.Testing testing && testing.parent() == null;
        if (!isMapTesting && !playerData.has(Permission.GENERIC_STAFF))
            return null;
        if (!(p.saveState().state instanceof PlayState state))
            return null;

        var lines = new ArrayList<HudNode>();

        var stats = Component.text();
        var progressIndex = state.get(Attachments.PROGRESS_INDEX);
        if (progressIndex != null)
            stats.append(Component.text("PI: ", LABEL)).append(Component.text(progressIndex, VALUE))
                .append(Component.text(" "));
        // Always present so the hud is never fully empty.
        int resetHeight = state.get(Attachments.RESET_HEIGHT, world.defaultResetHeight());
        stats.append(Component.text("R: ", LABEL)).append(Component.text(resetHeight, VALUE));
        lines.add(HudNode.text(stats.build()));

        var counts = Component.text();
        // Ghost blocks are often present for other reasons, only worth showing when non-empty.
        int ghostBlocks = state.ghostBlocks().size();
        if (ghostBlocks > 0)
            counts.append(Component.text("B: ", LABEL)).append(Component.text(ghostBlocks, VALUE));
        var entities = state.get(Attachments.OWNED_ENTITIES);
        if (entities != null) {
            if (ghostBlocks > 0) counts.append(Component.text(" "));
            counts.append(Component.text("E: ", LABEL)).append(Component.text(entities.entities().size(), VALUE));
        }
        if (ghostBlocks > 0 || entities != null) lines.add(HudNode.text(counts.build()));

        var settings = state.get(Attachments.SETTINGS);
        if (settings != null) {
            var row = Component.text().append(Component.text("S: ", LABEL));
            boolean any = false;
            for (var entry : SETTING_CODES) {
                var value = settings.getOrNull(entry.getKey());
                if (value == null) continue;
                if (any) row.append(Component.text(" "));
                row.append(Component.text((value ? "+" : "-") + entry.getValue(), value ? ON : OFF));
                any = true;
            }
            if (any) lines.add(HudNode.text(row.build()));
        }

        var attributes = state.get(EditAttributeAction.SAVE_DATA);
        if (attributes != null && !attributes.view().isEmpty()) {
            var row = Component.text().append(Component.text("A: ", LABEL));
            boolean first = true;
            for (var entry : attributes.view().entrySet()) {
                if (!first) row.append(Component.text(" "));
                var code = ATTRIBUTE_CODES.getOrDefault(entry.getKey(), entry.getKey().key().value());
                row.append(Component.text(code, NAME)).append(Component.text("=" + fmt(entry.getValue()), VALUE));
                first = false;
            }
            lines.add(HudNode.text(row.build()));
        }

        var variables = state.get(Attachments.VARIABLES);
        if (variables != null) {
            var view = variables.view();
            var names = view.keySet().stream().sorted().toList();
            var varLines = new ArrayList<HudNode>();
            for (int i = 0; i < names.size(); i++) {
                if (lines.size() + varLines.size() == MAX_ROWS - 1 && i < names.size() - 1) {
                    varLines.add(HudNode.text(Component.text("+" + (names.size() - i) + " more", VALUE)));
                    break;
                }
                varLines.add(HudNode.text(Component.text()
                    .append(Component.text(FontUtil.stripInvalidChars(names.get(i)), NAME))
                    .append(Component.text(" = " + fmt(view.get(names.get(i))), VALUE))
                    .build()));
            }
            if (!varLines.isEmpty()) {
                var block = HudNode.vstack(0, HudNode.Align.LEFT, varLines.toArray(HudNode[]::new));
                // Render-only offset: separates the group without consuming a row.
                lines.add(lines.isEmpty() ? block : block.offset(0, GROUP_GAP));
            }
        }

        return HudNode.vstack(0, HudNode.Align.LEFT, lines)
            .shadow()
            .offset(OFFSET_X, OFFSET_Y)
            .anchored(HudAnchor.TOP_LEFT);
    }

    private static String fmt(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ParkourStateDebugHud;
    }

    @Override
    public int hashCode() {
        return 31 * ParkourStateDebugHud.class.hashCode();
    }
}
