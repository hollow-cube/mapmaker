package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

public class ReformatSnbt {
    public static void main(String[] args) throws Exception {
        var snbt = """
                
                {
                        extra: [
                            {
                                text: " ᴀᴅᴅɪᴛɪᴏɴᴀʟ ʙᴜɪʟᴅᴇʀѕ"
                            },
                            {
                                strikethrough: 1b,
                                text: "      "
                            },
                            {
                                : "✦"
                            },
                            {
                                strikethrough: 1b,
                                text: "      "
                            },
                            {
                                text: ""
                            },
                            {
                                color: "#FFFEC2",
                                text: "ᴄʜᴇᴇѕɪᴇʀᴘᴀѕᴛᴀ"
                            },
                            {
                                text: ""
                            },
                            {
                                color: "#FFBBAE",
                                text: "ѕʏɴᴛʜ_ʟᴇᴍᴍᴏɴ"
                            },
                            {
                                text: ""
                            },
                            {
                                color: "#FFA3F6",
                                text: "ʟᴏᴇꜰᴀʀѕ"
                            }
                        ],
                        text: ""
                    }
                
                """;

        System.out.println(TagStringIOExt.writeTag(TagStringIOExt.readTag(snbt), ""));
    }
}
