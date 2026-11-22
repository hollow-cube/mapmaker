package net.hollowcube.apiserver.chat;

import net.hollowcube.ipc.chat.MessagePart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTokenizerTest {

    @Test
    void tokenize_findsEveryUrlWithOrWithoutItsScheme() {
        assertEquals(
            List.of(
                new MessagePart.Raw("go to "),
                new MessagePart.Url("https://hollowcube.net/maps"),
                new MessagePart.Raw(" and "),
                new MessagePart.Url("bb.com")
            ),
            MessageTokenizer.tokenize("go to https://hollowcube.net/maps and bb.com", null)
        );
    }

    @Test
    void tokenize_takesTheNameOutOfAnEmojiAndLeavesLoneColonsAlone() {
        assertEquals(
            List.of(new MessagePart.Raw("nice "), new MessagePart.Emoji("thumbs_up-2")),
            MessageTokenizer.tokenize("nice :thumbs_up-2:", null)
        );
        assertEquals(
            List.of(new MessagePart.Raw("a : b : c")),
            MessageTokenizer.tokenize("a : b : c", null)
        );
    }

    @Test
    void tokenize_resolvesTheMapTagOnlyWithAMapToResolveItTo() {
        assertEquals(
            List.of(
                new MessagePart.Raw("try "),
                new MessagePart.Map("m1"),
                new MessagePart.Raw("!")
            ),
            MessageTokenizer.tokenize("try [map]!", "m1")
        );
        assertEquals(
            List.of(new MessagePart.Raw("try [map]")),
            MessageTokenizer.tokenize("try [map]", null)
        );
    }

    @Test
    void tokenize_neverLooksInsideAPartItHasAlreadyMade() {
        // Urls run first and only raw text is rescanned, so the colons in a link are part of it
        // rather than an emoji cut out of the middle of one.
        assertEquals(
            List.of(new MessagePart.Url("https://hollowcube.net/:x:")),
            MessageTokenizer.tokenize("https://hollowcube.net/:x:", null)
        );
    }

    @Test
    void tokenize_splitsOneMessageIntoEveryKindOfPart() {
        assertEquals(
            List.of(
                new MessagePart.Raw("look "),
                new MessagePart.Emoji("eyes"),
                new MessagePart.Raw(" "),
                new MessagePart.Map("m1"),
                new MessagePart.Raw(" at "),
                new MessagePart.Url("hollowcube.net")
            ),
            MessageTokenizer.tokenize("look :eyes: [map] at hollowcube.net", "m1")
        );
    }
}
