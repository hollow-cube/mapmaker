package net.hollowcube.mapmaker;

import java.nio.file.Files;
import java.nio.file.Path;

public class Packer {
    private static final System.Logger logger = System.getLogger(Packer.class.getName());

    private static final Path RESOURCE_DIR;
    private static final Path OUT_DIR;

    static {
        try {
            var resourceDir = Path.of("./resources");
            Files.createDirectories(resourceDir);
            RESOURCE_DIR = resourceDir.toRealPath();

            var outDir = Path.of("./build/packer");
            Files.createDirectories(outDir);
            OUT_DIR = outDir.toRealPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        logger.log(System.Logger.Level.INFO, "Hello, world!");

        Files.createDirectories(OUT_DIR);

        var ctx = new PackContext(RESOURCE_DIR, OUT_DIR);
        var spriteTransform = new SpriteTransform();
        spriteTransform.process(ctx);

        var langTransform = new LangMergeTransform();
        langTransform.init(ctx, spriteTransform);
        langTransform.process(ctx);

        var fontTransform = new FontTransform();
        fontTransform.init(ctx, spriteTransform);
        fontTransform.process(ctx);

        ctx.cleanup();

    }
}
