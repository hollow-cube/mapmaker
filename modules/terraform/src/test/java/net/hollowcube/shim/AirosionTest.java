package net.hollowcube.shim;

import com.jayemceekay.metabrushes.common.brushes.MetaBallsBrush;
import com.sk89q.worldedit.EditSession;
import com.thevoxelbox.voxelsniper.sniper.Sniper;
import com.thevoxelbox.voxelsniper.sniper.snipe.Snipe;
import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

@EnvTest
public class AirosionTest {

    static {
        System.setProperty("minestom.inside-test", "true");
    }

    @Test
    void abc(Env env) throws Exception {
        var instance = env.createFlatInstance();
        var player = env.createPlayer(instance, new Pos(0, 41, 0, 62, 39));

        var airosionBrush = new MetaBallsBrush();
        airosionBrush.loadProperties();
        System.out.println(airosionBrush);
        try {
            airosionBrush.wrappedHandleArrowAction(new Snipe(
                    new EditSession(instance),
                    new Sniper(player)
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("DONE");
    }
}
