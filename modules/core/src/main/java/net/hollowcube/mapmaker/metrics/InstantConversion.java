package net.hollowcube.mapmaker.metrics;

import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import java.time.Instant;

public final class InstantConversion extends Conversion<Instant> {
    private static final LogicalType TIMESTAMP_MILLIS = LogicalTypes.timestampMillis();
    private static final Schema TIMESTAMP = TIMESTAMP_MILLIS.addToSchema(Schema.create(Schema.Type.LONG));

    public static final InstantConversion INSTANCE = new InstantConversion();

    @Override
    public Class<Instant> getConvertedType() {
        return Instant.class;
    }

    @Override
    public Schema getRecommendedSchema() {
        return TIMESTAMP;
    }

    @Override
    public String getLogicalTypeName() {
        return TIMESTAMP_MILLIS.getName();
    }

    @Override
    public Long toLong(Instant value, Schema schema, LogicalType type) {
        return value.toEpochMilli();
    }

    @Override
    public Instant fromLong(Long value, Schema schema, LogicalType type) {
        return Instant.ofEpochMilli(value);
    }
}
