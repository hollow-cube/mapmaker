package net.hollowcube.command.demo;

import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.command.util.MockExecutor;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

public class DemoServer {

    static class SummonCommand extends CommandDsl {
        private final Argument<EntityType> entityArg = Argument.Resource("entity", "minecraft:entity_type", EntityType::fromNamespaceId);
        private final Argument<Point> posArg = Argument.RelativeVec3("pos");
        private final Argument<CompoundBinaryTag> nbtArg = Argument.CompoundBinaryTag("nbt");

        public SummonCommand() {
            super("summon", "nonce");

            addSyntax(new MockExecutor(), entityArg);
            addSyntax(new MockExecutor(), entityArg, posArg);
            addSyntax(new MockExecutor(), entityArg, posArg, nbtArg);
        }

    }

    public static void main(String[] args) {
        var server = MinecraftServer.init();

        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 39, Block.STONE));

        var commandManager = new CommandManagerImpl();
        commandManager.register(new SummonCommand());

        MinecraftServer.getConnectionManager().setPlayerProvider(CommandHandlingPlayer.createDefaultProvider(commandManager));

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });

        server.start("0.0.0.0", 25565);
    }
}
