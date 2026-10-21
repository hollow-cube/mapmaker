package net.hollowcube.terraform.legacy.buffer;

import org.jetbrains.annotations.NotNull;

class NaiveBlockBufferBuilderTest extends BufferBuilderTestBase {

    @Override
    @NotNull BlockBuffer.Builder createBuilder() {
        return new NaiveBlockBufferBuilder(null);
    }

}
