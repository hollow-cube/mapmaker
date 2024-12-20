package net.hollowcube.mapmaker.map.hdb.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.block.handler.PlayerHeadBlockHandler;
import net.hollowcube.mapmaker.map.hdb.HdbMessages;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class HdbBase64Command extends CommandDsl {
    private final Argument<String> targetArg = Argument.Word("target")
            .with("hand", "block").defaultValue("hand")
            .description("Whether to check hand or target block");

    public HdbBase64Command() {
        super("base64");

        description = "Fetch the base64 texture value of a head";

        addSyntax(playerOnly(this::handleGetBase64));
        addSyntax(playerOnly(this::handleGetBase64), targetArg);
    }

    private void handleGetBase64(@NotNull Player player, @NotNull CommandContext context) {
        boolean isHand = "hand".equals(context.get(targetArg).toLowerCase(Locale.ROOT));

        String base64;
        if (isHand) {
            var itemStack = player.getItemInMainHand();
            if (itemStack.material().id() == Material.PLAYER_HEAD.id()) {
                player.sendMessage(HdbMessages.COMMAND_BASE64_NOT_A_PLAYER_HEAD);
                return;
            }

            var skin = itemStack.get(ItemComponent.PROFILE, HeadProfile.EMPTY).skin();
            if (skin == null || skin.textures() == null) {
                player.sendMessage(HdbMessages.COMMAND_BASE64_NO_TEXTURE);
                return;
            }

            base64 = skin.textures();
        } else {
            var targetBlockPosition = PlayerUtil.getTargetBlock(player, PlayerUtil.DEFAULT_PLACE_REACH);
            if (targetBlockPosition == null) {
                player.sendMessage(HdbMessages.COMMAND_BASE64_NO_BLOCK);
                return;
            }

            var block = player.getInstance().getBlock(targetBlockPosition);
            if (block.id() != Block.PLAYER_HEAD.id() && block.id() != Block.PLAYER_WALL_HEAD.id()) {
                player.sendMessage(HdbMessages.COMMAND_BASE64_NOT_A_PLAYER_HEAD);
                return;
            }

            base64 = extractBlockBase64(block);
            if (base64.isEmpty()) {
                player.sendMessage(HdbMessages.COMMAND_BASE64_NO_TEXTURE);
                return;
            }
        }

        player.sendMessage(HdbMessages.COMMAND_BASE64_RESULT.with(base64));
    }

    private static @NotNull String extractBlockBase64(@NotNull Block block) {
        var profile = PlayerHeadBlockHandler.extractProfile(block);
        if (profile == null) return "";
        var skin = profile.skin();
        if (skin == null) return "";
        return skin.textures();
    }
}
