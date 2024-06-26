package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.FakePlayer;
import net.hollowcube.command.util.MockExecutor;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import org.junit.jupiter.api.Test;

public class TestMojangTreeResolution {

    static {
        MinecraftServer.init();
    }


    @Test
    void testResolveSummonCommand() {
        class SummonCommand extends CommandDsl {
            private final Argument<EntityType> entityArg = Argument.Resource("entity", "minecraft:entity_type", EntityType::fromNamespaceId);
            private final Argument<Point> posArg = Argument.RelativeVec3("pos");
            private final Argument<CompoundBinaryTag> nbtArg = Argument.CompoundBinaryTag("nbt");

            public SummonCommand() {
                super("summon", "nonce");

                addSyntax(new MockExecutor(), entityArg);
//            addSyntax(new MockExecutor(), entityArg, posArg);
//            addSyntax(new MockExecutor(), entityArg, posArg, nbtArg);
            }

        }

        CommandManager manager = new CommandManagerImpl();
        manager.register(new SummonCommand());

        var packet = manager.createCommandPacketV2(new FakePlayer());
        System.out.println("DONE");
    }

}
