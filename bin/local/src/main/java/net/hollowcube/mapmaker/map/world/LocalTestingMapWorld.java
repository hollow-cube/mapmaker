package net.hollowcube.mapmaker.map.world;

import net.hollowcube.mapmaker.local.proj.Project;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LocalTestingMapWorld extends TestingMapWorld implements LocalProjectWorld {

    private final LocalEditingMapWorld parent;

    public LocalTestingMapWorld(@NotNull LocalEditingMapWorld parent) {
        super(parent, ((MapInstance) parent.instance()).copy());
        this.parent = parent;
    }

    @Override
    public void load() {
        super.load();

        for (var entity : parent.instance().getEntities()) {
            if (!(entity instanceof MapEntity mapEntity)) continue;
            var copy = mapEntity.copy();
            copy.setInstance(instance(), mapEntity.getPosition());
        }
    }

    @Override
    public @NotNull Project project() {
        return parent.project();
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);
    }
}
