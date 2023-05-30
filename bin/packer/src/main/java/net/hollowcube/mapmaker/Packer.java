package net.hollowcube.mapmaker;

import java.nio.file.Files;
import java.nio.file.Path;

public class Packer {
    private static final System.Logger logger = System.getLogger(Packer.class.getName());

    private static final Path RESOURCE_DIR = Path.of("./resources");
    private static final Path OUT_DIR = Path.of("./build/packer");

    public static void main(String[] args) throws Exception {
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
