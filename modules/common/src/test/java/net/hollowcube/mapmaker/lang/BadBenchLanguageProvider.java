package net.hollowcube.mapmaker.lang;

import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import org.junit.Test;

import java.util.List;

public class BadBenchLanguageProvider {

    public static void main(String[] args) {
        new BadBenchLanguageProvider().testGet1();
    }

    @Test
    public void testGet1() {
        var translatable = Component.translatable("test.get12.one", List.of(
                Component.text("param1"),
                Component.text("param2")
        ));

        var translatable3 = Component.translatable("test.get3.one", List.of(
                Component.text("param1"),
                Component.text("param2")
        ));

        int warmup = 10000, test = 100000;

        for (int i = 0; i < warmup; i++) {
            LanguageProvider.get(translatable);
        }

        long start = System.nanoTime();
        for (int i = 0; i < test; i++) {
            LanguageProvider.get(translatable);
        }
        long end = System.nanoTime();

        System.out.println("get1: " + ((end - start) / test) / 1000.0 + "us");

        for (int i = 0; i < warmup; i++) {
            LanguageProvider.get2(translatable);
        }

        start = System.nanoTime();
        for (int i = 0; i < test; i++) {
            LanguageProvider.get2(translatable);
        }
        end = System.nanoTime();

        System.out.println("get2: " + ((end - start) / test) / 1000.0 + "us");

        for (int i = 0; i < warmup; i++) {
            LanguageProvider.get3(translatable3);
        }

        start = System.nanoTime();
        for (int i = 0; i < test; i++) {
            LanguageProvider.get3(translatable3);
        }
        end = System.nanoTime();

        System.out.println("get3: " + ((end - start) / test) / 1000.0 + "us");
    }
}
