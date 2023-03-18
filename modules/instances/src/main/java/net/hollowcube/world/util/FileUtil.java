package net.hollowcube.world.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public final class FileUtil {
    private FileUtil() {
    }

    public static void deleteDirectory(@NotNull Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Get an ArrayList that contains all the files from the specified folder
     *
     * @param folder The folder you want all the files from
     * @return An ArrayList containing all the files of the specified folder
     */
    public static @NotNull List<File> getFileList(final File folder) {
        // create a new empty arraylist
        ArrayList<File> fileList = new ArrayList<>();
        // create an array containing all the files from the folder
        File[] files = folder.listFiles();
        // loop through all the files
        for (File file : files) {
            // check if the file is not another folder (only files get added)
            if (!file.isDirectory())
                fileList.add(file); // add the file to the arraylist
        }
        return fileList;
    }
}
