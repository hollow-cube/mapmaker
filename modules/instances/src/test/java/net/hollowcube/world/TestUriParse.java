package net.hollowcube.world;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUriParse {

    @Test
    public void manualParseUri() throws Exception {
        var uri = new URI("s3://accessKey:secretKey@address/bucket");

        assertEquals("s3", uri.getScheme());

        var userInfo = uri.getUserInfo().split(":");
        assertEquals(2, userInfo.length);
        var accessKey = userInfo[0];
        assertEquals("accessKey", accessKey);
        var secretKey = userInfo[1];
        assertEquals("secretKey", secretKey);

        var host = uri.getHost();
        assertEquals("address", host);

        var path = uri.getPath();
        if (path.startsWith("/"))
            path = path.substring(1);
        assertEquals("bucket", path);
    }
}
