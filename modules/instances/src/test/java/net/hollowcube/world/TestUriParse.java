package net.hollowcube.world;

import com.google.common.base.Splitter;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUriParse {

    @Test
    public void manualParseUri() throws Exception {
        var uri = new URI("s3://accessKey:secretKey@address/bucket");

        assertEquals("s3", uri.getScheme());

        var userInfo = Splitter.on(':').splitToList(uri.getUserInfo());
        assertEquals(2, userInfo.size());
        var accessKey = userInfo.get(0);
        assertEquals("accessKey", accessKey);
        var secretKey = userInfo.get(1);
        assertEquals("secretKey", secretKey);

        var host = uri.getHost();
        assertEquals("address", host);

        var path = uri.getPath();
        if (path.startsWith("/"))
            path = path.substring(1);
        assertEquals("bucket", path);
    }
}
