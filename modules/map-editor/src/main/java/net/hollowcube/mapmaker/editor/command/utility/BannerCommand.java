package net.hollowcube.mapmaker.editor.command.utility;

import com.google.gson.JsonSyntaxException;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.JsonUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.banner.BannerPattern;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.BannerPatterns;
import net.minestom.server.registry.Holder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

@SuppressWarnings("UnstableApiUsage")
public class BannerCommand extends CommandDsl {

    private static final Map<DyeColor, Material> COLOR_TO_BANNER = OpUtils.build(new EnumMap<>(DyeColor.class), map -> {
        for (var color : DyeColor.values()) {
            var bannerId = color.name().toLowerCase(Locale.ROOT) + "_banner";
            map.put(color, Objects.requireNonNull(Material.fromKey(bannerId)));
        }
    });

    private final Argument<String> urlArg = Argument.GreedyString("url")
            .description("The Minecraft.wiki sharable banner URL");

    public BannerCommand() {
        super("banner");

        description = "Gives you a banner from a Minecraft.wiki URL";

        setCondition(builderOnly());
        addSyntax((player, _) ->
                          player.sendMessage(TranslatableBuilder.of("commands.banner.no_url").build())
        );
        addSyntax(playerOnly(this::handleGiveBanner), urlArg);
    }

    private void handleGiveBanner(Player player, CommandContext context) {
        try {
            var url = URI.create(context.get(urlArg));
            var urlFragment = url.getFragment();
            if (urlFragment == null || urlFragment.isEmpty()) {
                player.sendMessage(Component.translatable("commands.banner.malformed_url"));
                return;
            }

            var query = urlFragment.startsWith("?") ? urlFragment.substring(1) : urlFragment;
            var queries = Arrays.stream(query.split("&"))
                    .map(s -> s.split("=", 2))
                    .filter(s -> s.length == 2)
                    .collect(Collectors.toMap(s -> s[0], s -> URLDecoder.decode(s[1], StandardCharsets.UTF_8)));

            var patterns = OpUtils.map(
                    queries.get("activePatterns"),
                    it -> JsonUtil.fromJson(BannerLayer.VANILLA_LIST, it)
            );
            var baseColor = OpUtils.map(
                    queries.get("baseColor"),
                    it -> DyeColor.CODEC.decode(Transcoder.JAVA, it)
            );

            if (patterns == null || !(baseColor instanceof Result.Ok(var color))) {
                player.sendMessage(Component.translatable("commands.banner.malformed_url"));
            } else {
                PlayerUtil.giveItem(
                        player,
                        ItemStack.builder(COLOR_TO_BANNER.get(color))
                                .set(DataComponents.BANNER_PATTERNS, new BannerPatterns(patterns))
                                .build()
                );
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.translatable("commands.banner.invalid_url"));
        } catch (JsonSyntaxException e) {
            player.sendMessage(Component.translatable("commands.banner.malformed_url"));
        }
    }

    private record BannerLayer(int id, Holder<BannerPattern> pattern, DyeColor color) {

        public static final Codec<BannerLayer> CODEC = StructCodec.struct(
                "id", Codec.INT, BannerLayer::id,
                "name", BannerPattern.HOLDER_CODEC, BannerLayer::pattern,
                "color", DyeColor.CODEC, BannerLayer::color,
                BannerLayer::new
        );
        public static final Codec<List<BannerPatterns.Layer>> VANILLA_LIST = CODEC.list().transform(
                layers -> layers
                        .stream()
                        .sorted(Comparator.comparingInt(BannerLayer::id))
                        .map(BannerLayer::toVanilla)
                        .toList(),
                layers -> {
                    List<BannerLayer> result = new ArrayList<>();
                    for (int i = 0; i < layers.size(); i++) {
                        var layer = layers.get(i);
                        result.add(new BannerLayer(i, layer.pattern(), layer.color()));
                    }
                    return result;
                }
        );

        public BannerPatterns.Layer toVanilla() {
            return new BannerPatterns.Layer(pattern, color);
        }
    }
}
