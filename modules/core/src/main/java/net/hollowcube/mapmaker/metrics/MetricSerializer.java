package net.hollowcube.mapmaker.metrics;

import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Serializer;
import java.nio.charset.StandardCharsets;

public class MetricSerializer implements Serializer<Metric> {

    private final Gson gson = new Gson();
    @Override
    public byte[] serialize(String topic, Metric data) {
        if (data == null) {
            return null;
        }

        String jsonString = gson.toJson(data);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }
}
