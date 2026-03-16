package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

public class V4292 extends DataVersion {
    public V4292() {
        super(4292);

        // TODO more text component schema here.

        addFix(DataTypes.TEXT_COMPONENT, V4292::fixTextComponentHoverAndClickEvent);
    }

    private static @Nullable Value fixTextComponentHoverAndClickEvent(Value textComponent) {
        var hoverEvent = textComponent.remove("hoverEvent");
        var clickEvent = textComponent.remove("clickEvent");
        if (!hoverEvent.isMapLike() && !clickEvent.isMapLike()) return null;
        textComponent.put("hover_event", fixHoverEvent(hoverEvent));
        textComponent.put("click_event", fixClickEvent(clickEvent));
        return null;
    }

    private static Value fixHoverEvent(Value hoverEvent) {
        var action = hoverEvent.get("action").as(String.class, "");
        switch (action) {
            case "show_text" -> copyFields(hoverEvent, hoverEvent, List.of("contents", "value"));
            case "show_item" -> {
                var contents = hoverEvent.remove("contents");
                if (contents.value() instanceof String id) {
                    hoverEvent.put("id", id);
                } else {
                    copyFields(contents, hoverEvent, List.of("id", "id", "count", "count", "components", "components"));
                }
            }
            case "show_entity" -> copyFields(hoverEvent.remove("contents"), hoverEvent,
                    List.of("id", "uuid", "type", "id", "name", "name"));
        }
        return hoverEvent;
    }

    private static @Nullable Value fixClickEvent(Value clickEvent) {
        var action = clickEvent.get("action").as(String.class, "");
        var value = clickEvent.remove("value");
        return switch (action) {
            case "open_url" -> {
                if (!validateUri(value.as(String.class, "")))
                    yield null; // remove the click event
                clickEvent.put("url", value);
                yield clickEvent;
            }
            case "open_file" -> {
                clickEvent.put("path", value);
                yield clickEvent;
            }
            case "run_command", "suggest_command" -> {
                if (!validateChat(value.as(String.class, "")))
                    yield null; // remove the click event
                clickEvent.put("command", value);
                yield clickEvent;
            }
            case "change_page" -> {
                var maybeNumber = value.as(Number.class, null);
                if (maybeNumber == null) {
                    try {
                        maybeNumber = Integer.parseInt(value.as(String.class, ""));
                    } catch (NumberFormatException ignored) {
                        yield null;
                    }
                }
                clickEvent.put("page", Math.max(maybeNumber.intValue(), 1));
                yield clickEvent;
            }
            default -> clickEvent;
        };
    }

    private static void copyFields(Value from, Value to, List<String> renames) {
        for (int i = 0; i < renames.size(); i += 2) {
            var old = from.remove(renames.get(i));
            if (old.isNull()) continue;
            to.put(renames.get(i + 1), old);
        }
    }

    private static boolean validateUri(String string) {
        try {
            URI uRI = new URI(string);
            String string2 = uRI.getScheme();
            if (string2 == null) {
                return false;
            } else {
                String string3 = string2.toLowerCase(Locale.ROOT);
                return "http".equals(string3) || "https".equals(string3);
            }
        } catch (URISyntaxException var4) {
            return false;
        }
    }

    private static boolean validateChat(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == 167 || c < ' ' || c == 127) {
                return false;
            }
        }

        return true;
    }

}
