package net.hollowcube.mapmaker;

import net.hollowcube.mapmaker.lang.LangMergeTransform;

import java.nio.file.Files;
import java.nio.file.Path;

public class Packer {
    private static final System.Logger logger = System.getLogger(Packer.class.getName());

    private static final Path RESOURCE_DIR = Path.of("./resources");
    private static final Path OUT_DIR = Path.of("./build/packer");

    public static void main(String[] args) throws Exception {
        logger.log(System.Logger.Level.INFO, "Hello, world!");

        Files.createDirectories(OUT_DIR);

        new LangMergeTransform().doit(new PackContext(RESOURCE_DIR, OUT_DIR));

    }
}
