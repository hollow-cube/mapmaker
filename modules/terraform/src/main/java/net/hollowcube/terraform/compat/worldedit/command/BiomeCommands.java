package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.common.components.ExtraComponents;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.terraform.compat.worldedit.command.arg.WEArgument;
import net.hollowcube.terraform.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.instance.TerraformBiomeChunk;
import net.hollowcube.terraform.instance.TerraformInstanceBiomes;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BiomeCommands {

    public static class BiomeList extends WECommand {

        private final Argument<Integer> page = Argument.Int("page").min(1).defaultValue(1);

        public BiomeList() {
            super("/biomelist", "/biomels");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), page);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var biomes = TerraformInstanceBiomes.forInstance(player.getInstance());
            if (biomes == null) return;

            var keys = new ArrayList<>(biomes.keys());
            int page = context.get(this.page) - 1;
            int maxPage = (keys.size() - 1) / 10;
            int nextPage = page + 1;

            if (page < 0 || page > maxPage) {
                player.sendMessage(ExtraComponents.translatable("commands.biome_list.invalid_page")
                        .with(page + 1)
                        .with(maxPage + 1)
                        .build()
                );
            } else {
                List<Component> lines = new ArrayList<>();
                lines.add(ExtraComponents.translatable("commands.biome_list.header")
                        .with(page + 1)
                        .with(maxPage + 1)
                        .build()
                );

                for (int i = 10 * page; i < Math.min(keys.size(), 10 * nextPage); i++) {
                    var key = keys.get(i);
                    var biome = biomes.getBiome(key);

                    if (biome == null) continue;

                    lines.add(ExtraComponents.translatable("commands.biome_list.entry")
                            .with(biomes.getName(key))
                            .with(key.name())
                            .with(getBiomeText("commands.biome_info.tooltip", biomes, key, biome))
                            .build()
                    );
                }

                player.sendMessage(ExtraComponents.multiline(lines));
            }
        }
    }

    public static class BiomeInfo extends WECommand {

        public BiomeInfo() {
            super("/biomeinfo");

            addSyntax(playerOnly(this::execute));
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var chunk = player.getInstance().getChunkAt(player.getPosition());
            var biomes = TerraformInstanceBiomes.forInstance(player.getInstance());
            if (chunk == null || biomes == null) return;

            var key = chunk.getBiome(player.getPosition());
            var biome = OpUtils.map(key, biomes::getBiome);
            if (biome == null) return;

            player.sendMessage(getBiomeText("commands.biome_info.success", biomes, key, biome));
        }
    }

    public static class SetBiome extends WECommand {

        private final Argument<DynamicRegistry.Key<Biome>> biome = WEArgument.Biome("biome");

        public SetBiome() {
            super("/setbiome");

            addSyntax(playerOnly(this::execute), biome);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var chunk = player.getInstance().getChunkAt(player.getPosition());
            var biomes = TerraformInstanceBiomes.forInstance(player.getInstance());
            if (chunk == null || biomes == null) return;

            var biome = context.get(this.biome);

            TerraformBiomeChunk.fillBiome(chunk, biome);
            TerraformBiomeChunk.sendBiomeUpdates(List.of(chunk));

            player.sendMessage(ExtraComponents.translatable("commands.set_biome.success")
                    .with(biomes.getName(biome))
                    .with(biome.name())
                    .build()
            );
        }

    }

    private static @NotNull Component getBiomeText(
            @NotNull String translation,
            @NotNull TerraformInstanceBiomes biomes,
            @NotNull DynamicRegistry.Key<Biome> key,
            @NotNull Biome biome
    ) {
        return ExtraComponents.translatable(translation)
                .with(biomes.getName(key))
                .with(key.name())
                .with(biome.precipitation().name())
                .with(String.format("#%06x", biome.effects().skyColor()))
                .with(String.format("#%06x", biome.effects().fogColor()))
                .with(String.format("#%06x", biome.effects().waterColor()))
                .with(String.format("#%06x", biome.effects().waterFogColor()))
                .with(String.format("#%06x", biome.effects().grassColor()))
                .with(String.format("#%06x", biome.effects().foliageColor()))
                .build();
    }

    private BiomeCommands() {
    }
}
