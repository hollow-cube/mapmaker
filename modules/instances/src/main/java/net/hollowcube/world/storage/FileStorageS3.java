package net.hollowcube.world.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public record FileStorageS3(
        @NotNull AmazonS3 s3
) implements FileStorage {

    public static @NotNull FileStorage connect(@NotNull String address, @NotNull String accessKey, @NotNull String secretKey) {



        // s3://accessKey:secretKey@address/bucket
        var credentials = new BasicAWSCredentials(accessKey, secretKey);
        var s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(address, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .enablePathStyleAccess()
                .build();
        System.out.println(s3.listBuckets());
        return new FileStorageS3(s3);
    }


    public static void main(String[] args) {
//        var storage = connect("http://localhost:9000/", "DTprdE3DBZ7vG8wQ", "qByxgkPV7rO7zo12KmRUkikSBMwYJCRj");
//        var data = "Hello World!".getBytes();
//        storage.uploadFile("test.txt", new ByteArrayInputStream(data), data.length).join();
    }

    @Override
    public @NotNull CompletableFuture<@NotNull String> uploadFile(@NotNull String path, @NotNull InputStream data, int size) {
        var metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        s3.putObject("mapmaker", path, data, metadata);
        return CompletableFuture.completedFuture(path); //todo do this properly (in the future, handle errors)
    }

    @Override
    public @NotNull CompletableFuture<InputStream> downloadFile(@NotNull String path) {
        var object = s3.getObject("mapmaker", path);
        return CompletableFuture.completedFuture(object.getObjectContent()); //todo make async
    }
}
