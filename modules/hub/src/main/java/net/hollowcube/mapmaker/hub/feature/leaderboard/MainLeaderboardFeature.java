package net.hollowcube.mapmaker.hub.feature.leaderboard;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(HubFeature.class)
public class MainLeaderboardFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MainLeaderboardFeature.class);

    private final NpcItemModel screenModel1 = new NpcItemModel();

    @Override
    public void init(@NotNull HubServer hub) {
        screenModel1.setModel(Material.STICK, 4);
        screenModel1.setInstance(hub.instance(), new Pos(-1, 46, -24)).join();
        screenModel1.getEntityMeta().setScale(new Vec(4));
        screenModel1.getEntityMeta().setLeftRotation(new Quaternion(new Vec(0, 0, 1).normalize(), Math.toRadians(10)).into());


        Entity lbTextEntity = new Entity(EntityType.TEXT_DISPLAY) {{
            hasPhysics = false;
            setNoGravity(true);
        }};

        var lbTextMeta = (TextDisplayMeta) lbTextEntity.getEntityMeta();
        lbTextMeta.setText(Component.text()
                .append(Component.text("Leaderboard", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("#1 notmattw 100000")).appendNewline()
                .append(Component.text("#2 notmattw 100000")).appendNewline()
                .append(Component.text("#3 notmattw 100000")).appendNewline()
                .append(Component.text("#4 notmattw 100000")).appendNewline()
                .append(Component.text("#5 notmattw 100000")).appendNewline()
                .append(Component.text("#6 notmattw 100000")).appendNewline()
                .append(Component.text("#7 notmattw 100000")).appendNewline()
                .append(Component.text("#8 notmattw 100000")).appendNewline()
                .append(Component.text("#9 notmattw 100000")).appendNewline()
                .append(Component.text("#0 notmattw 100000"))
                .build());
        lbTextMeta.setBackgroundColor(0);
        lbTextEntity.setInstance(hub.instance(), new Pos(5.97, 41, -25.4, 90, 0)).join();
        lbTextMeta.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(), Math.toRadians(10)).into());
        lbTextMeta.setScale(new Vec(1.75));

        Entity lbTextEntity2 = new Entity(EntityType.TEXT_DISPLAY) {{
            hasPhysics = false;
            setNoGravity(true);
        }};


        var lbTextMeta2 = (TextDisplayMeta) lbTextEntity2.getEntityMeta();
        lbTextMeta2.setText(Component.text()
                .append(Component.text("Leaderboard", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("#1 notmattw 100000")).appendNewline()
                .append(Component.text("#2 notmattw 100000")).appendNewline()
                .append(Component.text("#3 notmattw 100000")).appendNewline()
                .append(Component.text("#4 notmattw 100000")).appendNewline()
                .append(Component.text("#5 notmattw 100000")).appendNewline()
                .append(Component.text("#6 notmattw 100000")).appendNewline()
                .append(Component.text("#7 notmattw 100000")).appendNewline()
                .append(Component.text("#8 notmattw 100000")).appendNewline()
                .append(Component.text("#9 notmattw 100000")).appendNewline()
                .append(Component.text("#0 notmattw 100000"))
                .build());
        lbTextMeta2.setBackgroundColor(0);
        lbTextEntity2.setInstance(hub.instance(), new Pos(5.97, 41, -19.8, 90, 0)).join();
        lbTextMeta2.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(), Math.toRadians(10)).into());
        lbTextMeta2.setScale(new Vec(1.75));

    }
}
