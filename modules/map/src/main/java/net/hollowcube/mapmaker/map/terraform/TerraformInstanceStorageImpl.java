package net.hollowcube.mapmaker.map.terraform;

import net.hollowcube.terraform.storage.TerraformInstanceStorage;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

public class TerraformInstanceStorageImpl implements TerraformInstanceStorage {

    public static final Tag<BinaryTag> TAG = Tag.NBT("terraform:instance_storage");

    private final TagHandler handler = TagHandler.newHandler();

    @Override
    public void load(TagReadable reader) {
        BinaryTag tag = reader.getTag(TAG);
        if (!(tag instanceof CompoundBinaryTag compound)) return;
        handler.updateContent(compound);
    }

    @Override
    public void save(TagWritable writer) {
        writer.setTag(TAG, this.tagHandler().asCompound());
    }

    @Override
    public @NotNull TagHandler tagHandler() {
        return handler;
    }
}
