package net.hollowcube.mapmaker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

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

        var minecraftPath = Path.of(args[1]).toRealPath();
        var mcVersions = Arrays.stream(args[2].split(","))
                .map(it -> it.split(":"))
                .collect(Collectors.toMap(it -> it[0], it -> it[1]));

        try {
            var outDir = Path.of(args[0]);
//            var outDir = Path.of(args.length > 0 && args[0].equals("out_here_hack") ? "." : "./build/packer");
            Files.createDirectories(outDir);
            OUT_DIR = outDir.toRealPath();
        } catch (Exception e) {
            OUT_DIR = Files.createTempDirectory("packer");
//            throw new RuntimeException(e);
        }

        logger.log(System.Logger.Level.INFO, "Hello, world!");
        logger.log(System.Logger.Level.INFO, "RESOURCE_DIR: {0}", RESOURCE_DIR);
        logger.log(System.Logger.Level.INFO, "MINECRAFT: {0}", minecraftPath);

        Files.createDirectories(OUT_DIR);

        PackContext ctx = new PackContext(RESOURCE_DIR, OUT_DIR, minecraftPath, mcVersions);
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

        CosmeticV2Transform cosmeticV2Transform = new CosmeticV2Transform();
        cosmeticV2Transform.init(ctx);
        cosmeticV2Transform.process(ctx);

        DynamicDataTransform hubMerchantsTransform = new DynamicDataTransform("hub_merchants");
        hubMerchantsTransform.process(ctx);

        Hub5x5Transform hub5x5Transform = new Hub5x5Transform();
        hub5x5Transform.init(ctx);
        hub5x5Transform.process(ctx);

        new LangCloner().process(ctx);

        ctx.cleanup();
    }
}
