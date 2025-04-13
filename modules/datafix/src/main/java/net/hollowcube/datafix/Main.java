package net.hollowcube.datafix;

import net.hollowcube.datafix.versions.v0xxx.V101;
import net.hollowcube.datafix.versions.v0xxx.V704;
import net.hollowcube.datafix.versions.v0xxx.V99;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        new V99();
        new V101();
        new V704();

        System.out.println(((DataTypeImpl) DataType.BLOCK_ENTITY).stringify());

        var result = DataVersion.convert(DataType.BLOCK_ENTITY, new HashMap<>(Map.of(
                "id", "Chest",
                "Items", new ArrayList<>(List.of(
                        new HashMap<String, Object>(Map.of("id", "minecraft:stone"))
                ))
        )), 99, 1000);
        System.out.println(result);
    }
}
