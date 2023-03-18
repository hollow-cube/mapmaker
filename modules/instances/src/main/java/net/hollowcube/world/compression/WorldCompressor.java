package net.hollowcube.world.compression;

import com.github.luben.zstd.Zstd;
import net.hollowcube.world.util.FileUtil;
import net.minestom.server.world.DimensionType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldCompressor {
    // valid values: 0-22 (0 = no compression; 22 = highest compression)
    private static final int COMPRESSION_LEVEL = 22;
    /**
     * Compresses the region files of the given world using the Zstandard algorithm
     *
     * @param worldname   The worlds name whose files you want to compress
     * @param dimensionType The worlds dimension type
     * @return A CompressedWorldData object that contains the compressed world data
     *         and original data size (needed for decompression)
     */
    public static CompressedWorldData compressWorldRegionFiles(final String worldname, final DimensionType dimensionType) {
        // get correct path to region files
        String path = worldname + "/region"; //TODO figure out correct world file path
        /**switch (dimensionType) {
         case NORMAL:
         path = Bukkit.getWorldContainer().toPath() + "//" + worldname + "//region";
         break;
         case NETHER:
         path = Bukkit.getWorldContainer().toPath() + "//" + worldname + "//DIM-1//region";
         break;
         case THE_END:
         path = Bukkit.getWorldContainer().toPath() + "//" + worldname + "//DIM1//region";
         break;
         }**/
        // get the region folder
        File folder = new File(path);
        // put all .mca region files into a list
        List<File> fileList = FileUtil.getFileList(folder);
        // create ByteArrayOutputStream and ZipOutputStream to put all region files into an uncompressed zip file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipFile = new ZipOutputStream(bos);
        // set compression level of the zipping process to 0, zstandard will do the real compression
        zipFile.setLevel(Deflater.NO_COMPRESSION);
        // byte array that will contain the zipped data
        byte[] zippedData = null;
        try {
            // loop through all region files
            for (File file : fileList) {
                // create FileInputStream that reads in the region file
                FileInputStream fis = new FileInputStream(file);
                // get exact file path of the region file
                String zipFilePath = file.getCanonicalPath().substring(folder.getCanonicalPath().length() + 1, file.getCanonicalPath().length());
                // put it into the zip archive
                ZipEntry zipEntry = new ZipEntry(zipFilePath);
                zipFile.putNextEntry(zipEntry);
                // write the file into the zip archive
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipFile.write(bytes, 0, length);
                }
                // close entry and FileInputStream (important: we dont want unreleased ressources! not closing it will cause memory leaks!)
                zipFile.closeEntry();
                fis.close();
            }
            // close zip output stream
            zipFile.close();
            // put zip data into the byte array
            zippedData = bos.toByteArray();
            // close ByteArrayOutputStream
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // placeholder value
        int zippedSize = zippedData.length;
        // return a new CompressedWorldData object that contains the zstandard
        // compressed zip data and the original zipped data size

        return new CompressedWorldData(Zstd.compress(zippedData, COMPRESSION_LEVEL), zippedSize);
    }
}
