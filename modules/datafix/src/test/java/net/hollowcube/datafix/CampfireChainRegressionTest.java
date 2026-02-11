package net.hollowcube.datafix;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minestom.server.codec.Transcoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CampfireChainRegressionTest {

    @Test
    void testChainToIronChainInsideCampfire() {
        var input = new Gson().fromJson("""
            {"Items":[{"count":64,"Slot":0,"id":"minecraft:chain"},{"count":1,"Slot":1,"id":"minecraft:air"},{"count":64,"Slot":2,"id":"minecraft:chain"},{"count":64,"Slot":3,"id":"minecraft:chain"}],"id":"minecraft:campfire"}
            """, JsonObject.class);
        DataFixer.buildModel();
        var result = DataFixer.upgrade(DataTypes.BLOCK_ENTITY, Transcoder.JSON, input, 4442, 4671);

        var id = result.getAsJsonObject().get("Items").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
        assertEquals("minecraft:iron_chain", id);
    }
}
