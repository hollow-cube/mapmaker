package net.hollowcube.mapmaker.map.block.handler;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class StructureBlockHandler implements BlockHandler {
    public static @NotNull Block forOffsetSize(@NotNull Point offset, @NotNull Point size, @NotNull Mode mode) {
        var tagHandler = TagHandler.newHandler();
        tagHandler.setTag(POS_X, offset.blockX());
        tagHandler.setTag(POS_Y, offset.blockY());
        tagHandler.setTag(POS_Z, offset.blockZ());
        tagHandler.setTag(SIZE_X, size.blockX());
        tagHandler.setTag(SIZE_Y, size.blockY());
        tagHandler.setTag(SIZE_Z, size.blockZ());
        tagHandler.setTag(MODE, mode);
        tagHandler.setTag(SHOW_BOUNDING_BOX, true);
        return Block.STRUCTURE_BLOCK
                .withHandler(StructureBlockHandler.INSTANCE)
                .withNbt(tagHandler.asCompound());
    }

    public enum Mode {
        SAVE,
        LOAD,
        CORNER,
        DATA
    }

    public enum Mirror {
        NONE,
        LEFT_RIGHT,
        FRONT_BACK
    }

    public enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        COUNTERCLOCKWISE_90
    }

    private static final NamespaceID ID = NamespaceID.from("minecraft:structure_block");
    public static final StructureBlockHandler INSTANCE = new StructureBlockHandler();

    public static final Tag<String> AUTHOR = Tag.String("author").defaultValue("?");
    public static final Tag<Boolean> IGNORE_ENTITIES = Tag.Boolean("ignoreEntities").defaultValue(true);
    public static final Tag<Float> INTEGRITY = Tag.Float("integrity").defaultValue(1.0f);
    public static final Tag<String> METADATA = Tag.String("metadata");
    public static final Tag<Mirror> MIRROR = Tag.String("mirror").map(Mirror::valueOf, Mirror::name);
    public static final Tag<Mode> MODE = Tag.String("mode").map(Mode::valueOf, Mode::name);
    public static final Tag<String> NAME = Tag.String("name");
    public static final Tag<Integer> POS_X = Tag.Integer("posX");
    public static final Tag<Integer> POS_Y = Tag.Integer("posY");
    public static final Tag<Integer> POS_Z = Tag.Integer("posZ");
    public static final Tag<Boolean> POWERED = Tag.Boolean("powered").defaultValue(false);
    public static final Tag<Rotation> ROTATION = Tag.String("rotation").map(Rotation::valueOf, Rotation::name);
    public static final Tag<Long> SEED = Tag.Long("seed").defaultValue(0L);
    public static final Tag<Boolean> SHOW_BOUNDING_BOX = Tag.Boolean("showboundingbox").defaultValue(false);
    public static final Tag<Integer> SIZE_X = Tag.Integer("sizeX");
    public static final Tag<Integer> SIZE_Y = Tag.Integer("sizeY");
    public static final Tag<Integer> SIZE_Z = Tag.Integer("sizeZ");

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(
                AUTHOR, IGNORE_ENTITIES, INTEGRITY,
                METADATA, MIRROR, MODE, NAME,
                POS_X, POS_Y, POS_Z,
                POWERED, ROTATION, SEED, SHOW_BOUNDING_BOX,
                SIZE_X, SIZE_Y, SIZE_Z
        );
    }
}
