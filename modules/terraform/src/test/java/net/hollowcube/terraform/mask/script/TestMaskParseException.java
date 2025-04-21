package net.hollowcube.terraform.mask.script;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class TestMaskParseException {

    @Test
    public void testHappyCase() {
        assumeFalse(true, "TODO: fix this");
        var e = new MaskParseException(4, 6, "This is a message");
        var actual = e.toFriendlyMessage("dirt25stone");
        assertMessage(String.format("""
                dirt25stone
                %s^^ This is a message
                """, FontUtil.computeOffset(FontUtil.measureText("dirt"))), actual);
    }

//    @Test
//    public void testLongErrorMessag() {
//        var e = new MaskParseException(36, 38, "This is a message");
//        var actual = e.toFriendlyMessage("blahblahblahblahblahblahblahblahblah25stone");
//        assertMessage(String.format("""
//                ..hblah25stone
//                %s^^ This is a message
//                """, FontUtil.computeOffset(FontUtil.measureText("..hblah"))), actual);
//    }

    static void assertMessage(String expected, List<Component> actual) {
        var actualStr = actual.stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .collect(Collectors.joining("\n"));
        assertEquals("\n" + expected, "\n" + actualStr + "\n");
    }
}
