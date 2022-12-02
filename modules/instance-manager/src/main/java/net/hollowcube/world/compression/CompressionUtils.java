package net.hollowcube.world.compression;

import java.io.File;
import java.util.ArrayList;

public class CompressionUtils {
    /**
     * Get an ArrayList that contains all the files from the specified folder
     *
     * @param folder The folder you want all the files from
     * @return An ArrayList containing all the files of the specified folder
     */
    public static ArrayList<File> getFileList(final File folder) {
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
