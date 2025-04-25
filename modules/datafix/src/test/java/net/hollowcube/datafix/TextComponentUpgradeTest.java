package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIOExt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextComponentUpgradeTest extends AbstractDataFixTest {

    @Test
    void v4291StyleFlagsToString() throws Exception {
        var raw = TagStringIOExt.readTag("{\"text\": \"Hover me\", \"bold\": true}");
        var actual = upgrade(DataTypes.TEXT_COMPONENT, valueFromTag(raw), 4189, 4325);
        assertEquals((byte) 1, actual.get("bold").value());
    }

    @Test
    void v4291HoverContentsRename() throws Exception {
        var raw = TagStringIOExt.readTag("{\"text\": \"Hover me\", \"hoverEvent\": {\"action\": \"show_text\", \"contents\": {\"text\": \"I am hover\"}}}");
        var result = (CompoundBinaryTag) tagFromValue(upgrade(DataTypes.TEXT_COMPONENT, valueFromTag(raw), 4189, 4325));

        assertNull(result.get("hoverEvent"));
        var hoverEvent = result.getCompound("hover_event");
        assertNotNull(hoverEvent);

        assertEquals("show_text", hoverEvent.getString("action"));
        assertEquals("I am hover", hoverEvent.getCompound("value").getString("text"));
    }

    @Test
    void v3900StringToComponentWithHeterogeneousList() {
        var raw = "{\"extra\":[\" ᴀᴅᴅɪᴛɪᴏɴᴀʟ ʙᴜɪʟᴅᴇʀѕ \\n\",{\"strikethrough\":true,\"text\":\"      \"},\" ✦ \",{\"strikethrough\":true,\"text\":\"      \"},\"\\n\",{\"color\":\"#FFFEC2\",\"text\":\"ᴄʜᴇᴇѕɪᴇʀᴘᴀѕᴛᴀ\"},\"\\n\",{\"color\":\"#FFBBAE\",\"text\":\"ѕʏɴᴛʜ_ʟᴇᴍᴍᴏɴ\"},\"\\n\",{\"color\":\"#FFA3F6\",\"text\":\"ʟᴏᴇꜰᴀʀѕ\"}],\"text\":\"\"}";
        var result = upgrade(DataTypes.TEXT_COMPONENT, Value.wrap(raw), 3900, 4325);
        var compound = assertInstanceOf(CompoundBinaryTag.class, tagFromValue(result));
        // This entry used to be a raw string in json, but should be wrapped in a compound now.
        assertEquals("✦", compound.getList("extra").getCompound(2).getString("text"));
    }
}
