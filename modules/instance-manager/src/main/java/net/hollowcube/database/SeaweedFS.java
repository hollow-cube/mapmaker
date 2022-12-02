package net.hollowcube.database;

import net.anumbrella.seaweedfs.core.FileSource;
import net.anumbrella.seaweedfs.core.FileTemplate;
import net.anumbrella.seaweedfs.core.file.FileHandleStatus;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SeaweedFS {
    private String host = "localhost";
    private int port = 9333;
    private FileSource fileSource;

    public SeaweedFS() {}

    public SeaweedFS(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects minestom to the seaweedfs server
     */
    public void connectToSeaweedFs() {
        this.connectToSeaweedFs(this.host, this.port);
    }

    /**
     * Connects minestom to the seaweedfs server
     *
     * @param host the host ip address of the seaweed server
     * @param port the port of the seaweedfs server (default: 9333)
     */
    public void connectToSeaweedFs(String host, int port) {
        this.host = host;
        this.port = port;
        this.fileSource = new FileSource();
        this.fileSource.setHost(host);
        this.fileSource.setPort(port);
        try {
            this.fileSource.startup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Disconnects minestom from the seaweedfs server
     */
    public void disconnectFromSeaweedFs() {
        if(this.fileSource == null) {
            return;
        }
        this.fileSource.shutdown();
    }

    /**
     * Uploads a new file to the seaweedfs server
     *
     * @param fileName The name of the file
     * @param inputStream An InputStream that provides the content of the file you want to upload
     * @return An unique id used by seaweedfs (you need this id to get the file back)
     */
    public String uploadFile(String fileName, InputStream inputStream) {
        FileTemplate template = new FileTemplate(this.fileSource.getConnection());
        FileHandleStatus status;
        try {
            status = template.saveFileByStream(fileName, inputStream, ContentType.DEFAULT_BINARY);
        } catch (IOException ex) {
            ex.printStackTrace();
            return "upload error";
        }
        return status.getFileId();
    }

    /**
     * Updates a file that already exists on the seaweedfs server
     *
     * @param fileId The unique file id of the file you want to update
     * @param fileName The new file name of the file
     * @param inputStream An InputStream that provides the content of the file you want to upload
     */
    public void updateFile(String fileId, String fileName, InputStream inputStream) {
        FileTemplate template = new FileTemplate(this.fileSource.getConnection());
        try {
            template.updateFileByStream(fileId, fileName, inputStream, ContentType.DEFAULT_BINARY);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public OutputStream downloadFile(String fileId) {
        FileTemplate template = new FileTemplate(this.fileSource.getConnection());
        OutputStream outputStream = null;
        try {
            outputStream = template.getFileStream(fileId).getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return outputStream;
    }

    /**
     * Deletes a file from the seaweedfs server
     *
     * @param fileId The unique file id of the file you want to delete
     */
    public void deleteFile(String fileId) {
        FileTemplate template = new FileTemplate(this.fileSource.getConnection());
        try {
            template.deleteFile(fileId);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return this.host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }
}
