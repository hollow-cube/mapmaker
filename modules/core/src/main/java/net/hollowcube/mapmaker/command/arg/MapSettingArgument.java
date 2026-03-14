package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.minestom.server.command.CommandSender;

public class MapSettingArgument extends Argument<MapSetting<?>> {

    protected MapSettingArgument(String id) {
        super(id);
    }

    @Override
    public ParseResult<MapSetting<?>> parse(CommandSender sender, StringReader reader) {
        var input = reader.readWord(WordType.BRIGADIER);
        for (var entry : MapSetting.ID_MAP.entrySet()) {
            var key = entry.getKey();
            if (key.equals(input)) {
                return success(entry.getValue());
            } else if (key.startsWith(input)) {
                return partial();
            }
        }
        return syntaxError(-1);
    }

    @Override
    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        for (var key : MapSetting.ID_MAP.keySet()) {
            if (key.startsWith(raw)) {
                suggestion.add(key);
            }
        }
    }
}
