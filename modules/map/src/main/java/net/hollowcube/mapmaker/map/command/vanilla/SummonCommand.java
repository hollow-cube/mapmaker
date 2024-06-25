package net.hollowcube.mapmaker.map.command.vanilla;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class SummonCommand extends CommandDsl {
    private final Argument<EntityType> entityArg = Argument.Resource("entity", "minecraft:entity_type", EntityType::fromNamespaceId);
    private final Argument<Point> posArg = Argument.RelativeVec3("pos");
    private final Argument<CompoundBinaryTag> nbtArg = Argument.CompoundBinaryTag("nbt");

    @Inject
    public SummonCommand() {
        super("summon");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleSummonEntity), entityArg);
        addSyntax(playerOnly(this::handleSummonEntity), entityArg, posArg);
        addSyntax(playerOnly(this::handleSummonEntity), entityArg, posArg, nbtArg);
    }

    private void handleSummonEntity(@NotNull Player player, @NotNull CommandContext context) {
        final EntityType entityType = context.get(entityArg);
        Point pos = context.get(posArg);
        CompoundBinaryTag nbt = context.get(nbtArg);

        if (pos == null) pos = Vec.fromPoint(player.getPosition());
        if (nbt == null) nbt = CompoundBinaryTag.empty();

        final MapEntity entity = MapEntityType.create(entityType, UUID.randomUUID());
        if (nbt.size() > 0) entity.readData(nbt);
        entity.setInstance(player.getInstance(), pos).thenRun(() -> {
            player.sendMessage(Component.text("Summoned ").append(LanguageProviderV2.getVanillaTranslation(entityType)));
        });
    }

}
