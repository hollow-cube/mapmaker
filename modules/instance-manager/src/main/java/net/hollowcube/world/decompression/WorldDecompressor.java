package net.hollowcube.world.decompression;

import com.github.luben.zstd.Zstd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WorldDecompressor {
    private static final int MAX_RECOVERY_TRIES = 69;

    /**
     * Decompresses the region files using Zstandard
     *
     * @param compressedData The compressed data you want to decompress into region files
     * @param zippedSize     The original zipped data archive size, needed for decompression
     * @return A list of decompressed RegionFileData objects
     */
    public static ArrayList<RegionFileData> decompressWorldRegionFiles(byte[] compressedData, int zippedSize) {
        //decompress the data back to the zipped archive
        byte[] zippedData;
        try {
            zippedData = Zstd.decompress(compressedData, zippedSize);
        } catch (RuntimeException ex) {
            System.out.println("§eUnable to decompress a world region file! Trying to repair...");
            System.out.println("§7Trying to recover original size...");
            long recoveredOriginalSize = Zstd.decompressedSize(compressedData);
            if (recoveredOriginalSize != 0) {
                System.out.println("§7Successfully recovered original size! Continuing to load the world...");
                zippedData = Zstd.decompress(compressedData, (int) recoveredOriginalSize);
            } else {
                System.out.println("§eUnable to recover original size! Applying alternative decompression...");
                zippedData = alternativeDecompression(compressedData);
                if (zippedData == null) {
                    return null;
                }
            }
        }
        //create a new byte buffer array
        byte[] buffer = new byte[1024];
        //create the list where we put in all the decompressed region file data
        ArrayList<RegionFileData> decompressedRegionData = new ArrayList<>();
        //create input streams
        ByteArrayInputStream bis = new ByteArrayInputStream(zippedData);
        ZipInputStream zis = new ZipInputStream(bis);
        try {
            //placeholder value that keeps track of the current entry while looping
            ZipEntry zipEntry = zis.getNextEntry();
            //loop as long as zipEntry is not null
            while (zipEntry != null) {
                //create ByteArrayOutputStream
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                //write content of the zipEntry to the ByteArrayOutputStream
                int length;
                while ((length = zis.read(buffer)) > 0) {
                    bos.write(buffer, 0, length);
                }
                //create a new RegionFileData object with the decompressed data and the original file name, then add it to the list
                decompressedRegionData.add(new RegionFileData(bos.toByteArray(), zipEntry.getName()));
                //close the ByteArrayOutputStream
                bos.close();
                //get the next entry in the zip archive and loop again
                zipEntry = zis.getNextEntry();
            }
            //close all input streams
            zis.closeEntry();
            zis.close();
            bis.close();
            //catch IO Exception that might happen here
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //return the final list of decompressed region data
        return decompressedRegionData;
    }

    /**
     * An alternative decompression method. Do not use unless there is no other way
     * to recover a compressed file. Even if this is successful it doesnt mean the
     * file is fully recovered.
     *
     * @param compressedData The compressed data
     * @return a recovered decompressed byte[]. null if not able to recover
     */
    private static byte[] alternativeDecompression(byte[] compressedData) {
        return alternativeDecompression(compressedData, 1);
    }

    private static byte[] alternativeDecompression(byte[] compressedData, int tries) {
        if (tries >= MAX_RECOVERY_TRIES) {
            System.out.println("§cUnable to recover decompressed world file! Giving up after 20 tries.");
            return null;
        }
        System.out.println("§7Applying alternative decompression (Try: " + tries + ")");
        boolean isError = false;
        byte[] uncompressedData = new byte[tries * 1000000];
        try {
            Zstd.decompress(uncompressedData, compressedData);
        } catch (Exception ex) {
            isError = true;
        }
        if (isError) {
            uncompressedData = null;
            return alternativeDecompression(compressedData, tries + 1);
        }
        return uncompressedData;
    }
}
