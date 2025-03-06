package net.hollowcube.terraform.storage;

import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface TerraformInstanceStorage extends Taggable {

    Tag<TerraformInstanceStorage> TERRAFORM_INSTANCE_STORAGE_TAG = Tag.Transient("terraform:instance_storage");

    @ApiStatus.Internal
    void load(TagReadable reader);

    @ApiStatus.Internal
    void save(TagWritable writer);

    static @Nullable Taggable get(Instance instance) {
        return instance.getTag(TERRAFORM_INSTANCE_STORAGE_TAG);
    }
}
