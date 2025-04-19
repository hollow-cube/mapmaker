package net.hollowcube.datafix;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIOExt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class AbstractTypedUpgradeTest extends AbstractDataFixTest {
    private final DataType dataType;
    private final BinaryTag tag;

    public AbstractTypedUpgradeTest(@NotNull DataType dataType, String type) {
        this.dataType = dataType;
        var path = "/" + type + "/" + getClass().getSimpleName() + ".snbt";
        try (var is = getClass().getResourceAsStream(path)) {
            var snbt = new String(Objects.requireNonNull(is, path).readAllBytes(), StandardCharsets.UTF_8);
            this.tag = TagStringIOExt.readTag(snbt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BinaryTag upgrade(int fromVersion, int toVersion) {
        var value = super.upgrade(dataType, valueFromTag(tag), fromVersion, toVersion);
        return tagFromValue(value);
    }

    protected CompoundBinaryTag upgradeC(int fromVersion, int toVersion) {
        return assertInstanceOf(CompoundBinaryTag.class, upgrade(fromVersion, toVersion));
    }
}
