package net.hollowcube.mapmaker.runtime.parkour.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.AnvilSearchView;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.instance.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ControlledBlockListInput extends Panel {
    private final Consumer<List<Block>> onChange;

    public ControlledBlockListInput(int width, Consumer<List<Block>> onChange) {
        super(width, 1);

        background("action/editor/slot_row_" + width, 0, 0);
        this.onChange = onChange;
    }

    public void update(List<Block> blocks) {
        clear();
        add(0, 0, new Text(null, 7, 0, "placeable on")
                .font("small").align(1, -11));

        int i = 0;
        for (; i < Math.min(7, blocks.size()); i++) {
            final int blockIndex = i;
            var block = blocks.get(i);
            var button = makeBlockButton(block)
                    .lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKREMOVE)
                    .onLeftClick(() -> {
                        host.pushView(new AnvilSearchView<>("action/anvil/teleport_icon", "Search Blocks",
                                Autocompletors::searchBlocks, ControlledBlockListInput::makeBlockButton, block2 -> {
                            onChange.accept(new ArrayList<>(blocks) {{
                                set(blockIndex, block2);
                            }});
                        }));
                    })
                    .onRightClick(() -> {
                        var newBlocks = new ArrayList<>(blocks);
                        newBlocks.remove(blockIndex);
                        this.onChange.accept(newBlocks);
                    });
            add(i, 0, button);
        }
        if (i < slotWidth) {
            add(i, 0, new Button("gui.action.add", 1, 1)
                    .sprite("generic2/icon/add", 3, 3)
                    .onLeftClick(() -> {
                        host.pushView(new AnvilSearchView<>("action/anvil/teleport_icon", "Search Blocks",
                                Autocompletors::searchBlocks, ControlledBlockListInput::makeBlockButton, block -> {
                            onChange.accept(new ArrayList<>(blocks) {{
                                add(block);
                            }});
                        }));
                    }));
        }
    }

    public static Button makeBlockButton(Block block) {
        return new Button(null, 1, 1)
                .text(LanguageProviderV2.getVanillaTranslation(block)
                        .decoration(TextDecoration.ITALIC, false), List.of())
                .model(block.key().asString(), null);
    }
}
