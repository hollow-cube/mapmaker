package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoRegistry;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;

import java.util.UUID;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class SummonCommand extends CommandDsl {

    private final Argument<EntityType> typeArg = MapEntities.Argument("type").description("The type of entity to summon");
    private final Argument<CompoundBinaryTag> nbtArg = Argument.CompoundTag("nbt").description("Optional NBT data to apply to the entity");

    public SummonCommand() {
        super("summon");

        setCondition(and(builderOnly()));

        addSyntax(playerOnly(this::addMarkerEntity), typeArg);
        addSyntax(playerOnly(this::addMarkerEntity), typeArg, nbtArg);
    }

    private void addMarkerEntity(Player player, CommandContext context) {
        var type = context.get(typeArg);

        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        if (MapEntityType.create(type, UUID.randomUUID()) instanceof MapEntity<?> entity) {
            var info = MapEntityInfoRegistry.get(entity);
            if (info != null) {
                if (context.has(nbtArg)) {
                    var builder = CompoundBinaryTag.builder();
                    entity.writeData(builder);
                    context.get(nbtArg).forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
                    entity.readData(builder.build());
                }

                entity.setInstance(world.instance(), player.getPosition().withView(0, 0));
            } else {
                player.sendMessage(
                    Component.text("You cannot summon entity of type " + type).color(NamedTextColor.RED)
                );
            }
        } else {
            player.sendMessage(Component.text("Failed summon entity of type " + type).color(NamedTextColor.RED));
        }
    }
}
