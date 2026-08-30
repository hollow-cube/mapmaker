package net.hollowcube.apiserver.chat;

import net.hollowcube.ipc.chat.MessagePart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTokenizerTest {

    @Test
    void tokenize_leavesPlainTextAsOnePart() {
        assertEquals(List.of(new MessagePart.Raw("hello world")),
            MessageTokenizer.tokenize("hello world", null));
    }

    @Test
    void tokenize_findsAUrlWithOrWithoutItsScheme() {
        assertEquals(List.of(new MessagePart.Raw("go to "), new MessagePart.Url("https://hollowcube.net/maps")),
            MessageTokenizer.tokenize("go to https://hollowcube.net/maps", null));
        assertEquals(List.of(new MessagePart.Url("hollowcube.net")),
            MessageTokenizer.tokenize("hollowcube.net", null));
    }

    @Test
    void tokenize_findsEveryUrlInOneMessage() {
        assertEquals(List.of(
                new MessagePart.Url("aa.com"), new MessagePart.Raw(" and "), new MessagePart.Url("bb.com")),
            MessageTokenizer.tokenize("aa.com and bb.com", null));
    }

    @Test
    void tokenize_needsTwoCharactersBeforeTheDotToCallSomethingAUrl() {
        // Go's pattern, kept as it is: `a.com` is one character short of it, so it stays text.
        assertEquals(List.of(new MessagePart.Raw("a.com")), MessageTokenizer.tokenize("a.com", null));
    }

    @Test
    void tokenize_takesTheNameOutOfAnEmoji() {
        assertEquals(List.of(new MessagePart.Raw("nice "), new MessagePart.Emoji("sus")),
            MessageTokenizer.tokenize("nice :sus:", null));
        assertEquals(List.of(new MessagePart.Emoji("thumbs_up-2")),
            MessageTokenizer.tokenize(":thumbs_up-2:", null));
    }

    @Test
    void tokenize_leavesSomethingThatIsNotAnEmojiAlone() {
        assertEquals(List.of(new MessagePart.Raw("a : b : c")),
            MessageTokenizer.tokenize("a : b : c", null));
    }

    @Test
    void tokenize_resolvesTheMapTagToTheSendersMap() {
        assertEquals(List.of(new MessagePart.Raw("try "), new MessagePart.Map("m1"), new MessagePart.Raw("!")),
            MessageTokenizer.tokenize("try [map]!", "m1"));
    }

    @Test
    void tokenize_leavesTheMapTagAsTextWithNoMapToResolveItTo() {
        assertEquals(List.of(new MessagePart.Raw("try [map]")),
            MessageTokenizer.tokenize("try [map]", null));
    }

    @Test
    void tokenize_neverLooksInsideAPartItHasAlreadyMade() {
        // Urls run first and only raw text is rescanned, so the colons in a link are part of it
        // rather than an emoji cut out of the middle of one.
        assertEquals(List.of(new MessagePart.Url("https://hollowcube.net/:x:")),
            MessageTokenizer.tokenize("https://hollowcube.net/:x:", null));
    }

    @Test
    void tokenize_splitsOneMessageIntoEveryKindOfPart() {
        assertEquals(List.of(
                new MessagePart.Raw("look "), new MessagePart.Emoji("eyes"), new MessagePart.Raw(" "),
                new MessagePart.Map("m1"), new MessagePart.Raw(" at "), new MessagePart.Url("hollowcube.net")),
            MessageTokenizer.tokenize("look :eyes: [map] at hollowcube.net", "m1"));
    }
}
