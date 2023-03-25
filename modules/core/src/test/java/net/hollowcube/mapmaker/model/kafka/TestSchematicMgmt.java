package net.hollowcube.mapmaker.model.kafka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestSchematicMgmt {

    @Test
    void testParse1() {
        var json = """
                {
                    "origin": "og",
                    "timestamp": 123456789,
                    "action": 0,
                    "id": "idd",
                    "name": "namme",
                    "owner": "owwner"
                }
                """;
        var msg = SchematicMgmt.fromJson(json);
        assertEquals("og", msg.origin());
        assertEquals(123456789, msg.timestamp());
        assertEquals(SchematicMgmt.ACTION_UPLOAD, msg.action());
        assertEquals("idd", msg.id());
        assertEquals("namme", msg.name());
        assertEquals("owwner", msg.owner());
        assertNull(msg.data());
        assertNull(msg.dataArray());
    }

}
