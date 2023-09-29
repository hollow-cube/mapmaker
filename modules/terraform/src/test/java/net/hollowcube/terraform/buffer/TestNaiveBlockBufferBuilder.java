package net.hollowcube.terraform.buffer;

import org.jetbrains.annotations.NotNull;

class TestNaiveBlockBufferBuilder extends BaseBufferBuilderTest {

    @Override
    @NotNull BlockBuffer.Builder createBuilder() {
        return new NaiveBlockBufferBuilder();
    }

}
