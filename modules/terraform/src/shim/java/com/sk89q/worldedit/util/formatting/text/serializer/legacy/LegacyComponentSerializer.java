package com.sk89q.worldedit.util.formatting.text.serializer.legacy;

import com.sk89q.worldedit.util.formatting.text.TextComponent;

public interface LegacyComponentSerializer {

    TextComponent deserialize(String input);

    LegacyComponentSerializer INSTANCE = new LegacyComponentSerializer() {

        @Override
        public TextComponent deserialize(String input) {
            return new TextComponent(input);
        }
    };
}
