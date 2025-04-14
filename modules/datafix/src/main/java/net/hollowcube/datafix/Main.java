package net.hollowcube.datafix;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.datafix.util.Value;

import java.util.Comparator;

public class Main {
    public static void main(String[] args) {

        DataFixes.build();

        var type = (DataTypeImpl) DataTypes.BLOCK_NAME;
        var value = Value.wrap("minecraft:grass");

        var fixes = type.fixes().stream().sorted(Comparator.comparingInt(Pair::first)).toList();
        for (var fix : fixes) {
            System.out.println("at " + fix.key());
            var result = fix.value().apply(value);
            System.out.println("result: " + result);
            if (result != null) value = result;
        }

        System.out.println("final result: " + value);
    }
}
