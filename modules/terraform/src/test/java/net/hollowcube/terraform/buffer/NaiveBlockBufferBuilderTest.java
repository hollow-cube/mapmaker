package net.hollowcube.terraform.buffer;

import org.jetbrains.annotations.NotNull;

class NaiveBlockBufferBuilderTest extends BufferBuilderTestBase {

    @Override
    @NotNull BlockBuffer.Builder createBuilder() {
        return new NaiveBlockBufferBuilder(null);
    }

}
