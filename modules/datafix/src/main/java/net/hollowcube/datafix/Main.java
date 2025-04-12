package net.hollowcube.datafix;

import net.hollowcube.datafix.versions.V101;
import net.hollowcube.datafix.versions.V99;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        new V99();
        new V101();

        System.out.println(((DataTypeImpl) DataType.ENTITY).stringify());

        var result = DataVersion.convert(DataType.ENTITY, new HashMap<>(Map.of("id", "Villager")), 99, 101);
        System.out.println(result);
    }
}
