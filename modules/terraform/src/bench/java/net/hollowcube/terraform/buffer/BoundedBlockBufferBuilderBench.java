package net.hollowcube.terraform.buffer;

import net.minestom.server.coordinate.Vec;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class BoundedBlockBufferBuilderBench {

    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        public BoundedBlockBufferBuilder buffer;

        @Setup(Level.Iteration)
        public void setup() {
            buffer = new BoundedBlockBufferBuilder(null, Vec.ZERO, new Vec(55, 55, 55));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(value = 1, warmups = 0)
    @Warmup(iterations = 3)
    public void bufferSmallRange(ExecutionPlan plan) {
        for (int x = 0; x < 55; x++) {
            for (int y = 0; y < 55; y++) {
                for (int z = 0; z < 55; z++) {
                    plan.buffer.set(x, y, z, 1);
                }
            }
        }
    }
}
