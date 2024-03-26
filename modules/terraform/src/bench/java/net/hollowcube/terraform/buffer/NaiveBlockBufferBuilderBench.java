package net.hollowcube.terraform.buffer;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class NaiveBlockBufferBuilderBench {

    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        public NaiveBlockBufferBuilder buffer;

        @Setup(Level.Iteration)
        public void setup() {
            buffer = new NaiveBlockBufferBuilder(null);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(value = 1, warmups = 0)
    @Warmup(iterations = 3)
    public void bufferSmallRange(ExecutionPlan plan) throws InterruptedException {

        for (int x = 0; x < 55; x++) {
            for (int y = 0; y < 55; y++) {
                for (int z = 0; z < 55; z++) {
                    plan.buffer.set(x, y, z, 1);
                }
            }
        }

    }
}
