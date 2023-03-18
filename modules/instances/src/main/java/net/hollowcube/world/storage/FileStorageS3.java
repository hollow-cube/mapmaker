package net.hollowcube.world.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public record FileStorageS3(
        @NotNull AmazonS3 s3,
        @NotNull String bucket
) implements FileStorage {

    /**
     * @param uri A URI in the format of s3://accessKey:secretKey@address/bucket.
     *            Currently, must follow this exact format.
     */
    public static @NotNull FileStorage connect(@NotNull String uri) {
        try {
            var parsed = new URI(uri);
            var query = splitQuery(parsed);
            Check.argCondition(!parsed.getScheme().equals("s3"), "URI scheme must be s3");

            var userInfo = parsed.getUserInfo().split(":");
            Check.argCondition(userInfo.length != 2, "URI user info must be in the format of accessKey:secretKey");
            String accessKey = userInfo[0], secretKey = userInfo[1];

            var address = parsed.getHost();
            Check.argCondition(address == null, "URI host must be specified");
            if (parsed.getPort() > 0)
                address += ":" + parsed.getPort();
            if (Boolean.parseBoolean(query.get("insecure")))
                address = "http://" + address;
            else
                address = "https://" + address;

            var bucket = parsed.getPath();
            if (bucket.startsWith("/"))
                bucket = bucket.substring(1);

            var credentials = new BasicAWSCredentials(accessKey, secretKey);
            var s3 = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(address, "us-east-1"))
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .enablePathStyleAccess()
                    .build();

            //todo make this use futures
            Check.argCondition(!s3.doesBucketExistV2(bucket), "Supplied bucket (" + bucket + ") does not exist");

            return new FileStorageS3(s3, bucket);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<@NotNull String> uploadFile(@NotNull String path, @NotNull InputStream data, long size, @NotNull Map<String, String> userMetadata) {
        return CompletableFuture.supplyAsync(() -> {
            //todo what errors need to be handled here?
            var metadata = new ObjectMetadata();
            metadata.setUserMetadata(userMetadata);
            metadata.setContentType("application/octet-stream");
            metadata.setContentLength(size);
            s3.putObject(bucket, path, data, metadata);
            return path;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<@NotNull StoredFile> downloadFile(@NotNull String path) {
        return CompletableFuture.supplyAsync(() -> {
            var object = s3.getObject(bucket, path);
            return new StoredFile(object.getObjectContent(),
                    object.getObjectMetadata().getContentLength(),
                    object.getObjectMetadata().getUserMetadata());
        }, ForkJoinPool.commonPool());
    }

    private static @NotNull Map<String, String> splitQuery(URI uri) {
        Map<String, String> queryPairs = new HashMap<>();
        String query = uri.getQuery();
        if (query == null) return Map.of();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return Map.copyOf(queryPairs);
    }
}
