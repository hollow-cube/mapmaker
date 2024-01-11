package net.hollowcube.terraform.buffer;

import org.jetbrains.annotations.NotNull;

class EmptyBlockBuffer implements BlockBuffer {
    public static final BlockBuffer INSTANCE = new EmptyBlockBuffer();

    private EmptyBlockBuffer() {

    }

    @Override
    public void forEachSection(@NotNull SectionConsumer consumer) {

    }

    @Override
    public long sizeBytes() {
        return 0;
    }
}
