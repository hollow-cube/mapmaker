package net.hollowcube.map.world.polar;

import net.hollowcube.map.world.MapWorld;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ReadWriteWorldAccess extends ReadWorldAccess {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteWorldAccess.class);

    public ReadWriteWorldAccess(@NotNull MapWorld mapWorld) {
        super(mapWorld);
    }

    @Override
    public void saveWorldData(@NotNull Instance instance, @NotNull NetworkBuffer buffer) {
        logger.debug("writing polar world data");
        buffer.write(NetworkBuffer.BYTE, (byte) VERSION);

        mapWorld.biomes().write(buffer);
    }
}
