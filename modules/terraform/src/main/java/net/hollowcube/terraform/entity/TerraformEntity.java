package net.hollowcube.terraform.entity;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Optional interface for entities to implement some terraform-specific behavior such as reading from NBT data.</p>
 *
 * <p>Not all of this functionality is necessarily Terraform specific, but needs to be accessed from Terraform.</p>
 */
public interface TerraformEntity {

    void readData(@NotNull CompoundBinaryTag tag);

    void writeData(@NotNull CompoundBinaryTag.Builder tag);

    default @NotNull CompoundBinaryTag writeToTag() {
        var builder = CompoundBinaryTag.builder();
        writeData(builder);
        return builder.build();
    }
}
