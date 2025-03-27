package net.hollowcube.mapmaker;

import java.nio.file.Files;
import java.nio.file.Path;

public class Packer {
    private static final System.Logger logger = System.getLogger(Packer.class.getName());

    private static final Path RESOURCE_DIR;
    private static Path OUT_DIR;

    static {
        try {
            Path resourceDir = Path.of("./resources");
            Files.createDirectories(resourceDir);
            RESOURCE_DIR = resourceDir.toRealPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        try {
            var outDir = Path.of(args.length > 0 && args[0].equals("out_here_hack") ? "." : "./build/packer");
            Files.createDirectories(outDir);
            OUT_DIR = outDir.toRealPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        logger.log(System.Logger.Level.INFO, "Hello, world!");
        logger.log(System.Logger.Level.INFO, "RESOURCE_DIR: {0}", RESOURCE_DIR);

        Files.createDirectories(OUT_DIR);

        PackContext ctx = new PackContext(RESOURCE_DIR, OUT_DIR);
        SpriteTransform spriteTransform = new SpriteTransform();
        spriteTransform.process(ctx);

        LangMergeTransform langTransform = new LangMergeTransform();
        langTransform.init(ctx, spriteTransform);
        langTransform.process(ctx);

        FontTransform fontTransform = new FontTransform();
        fontTransform.init(ctx, spriteTransform);
        fontTransform.process(ctx);

        ItemModelTransform itemModelTransform = new ItemModelTransform();
        itemModelTransform.process(ctx);

        //TODO(1.21.4) What did this do and why is it disabled
//        ModelTransform modelTransform = new ModelTransform();
//        modelTransform.init(ctx, fontTransform);
//        modelTransform.process(ctx);

        CosmeticV2Transform cosmeticV2Transform = new CosmeticV2Transform();
        cosmeticV2Transform.init(ctx);
        cosmeticV2Transform.process(ctx);

        DynamicDataTransform hubMerchantsTransform = new DynamicDataTransform("hub_merchants");
        hubMerchantsTransform.process(ctx);

        Hub5x5Transform hub5x5Transform = new Hub5x5Transform();
        hub5x5Transform.init(ctx);
//        hub5x5Transform.process(ctx);

        new LangCloner().process(ctx);

        ctx.cleanup();
    }
}
