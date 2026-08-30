package net.hollowcube.ipc.chat;

import com.google.gson.JsonParser;
import net.hollowcube.ipc.Wire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The chat wire as JSON, pinned.
///
/// Every server that renders a message reads these names, and the servers doing it are older than
/// the api-server writing them, so a rename here is a break rather than a refactor.
class ChatWireTest {

    /// Always the two-argument `toJson`: a sealed family is only written through its interface's
    /// adapter, and handing gson a variant as an `Object` picks the record's own adapter and writes
    /// it without the `type` that says which one it is. The generated client and server pass the
    /// declared type, which is what makes this right in production.
    private static String json(Object value, Class<?> type) {
        return Wire.gson().toJson(value, type);
    }

    @Test
    void messagePart_namesEachVariantByItsType() {
        assertEquals("""
            {"type":"raw","text":"hello"}""", json(new MessagePart.Raw("hello"), MessagePart.class));
        assertEquals("""
            {"type":"url","text":"hollowcube.net"}""", json(new MessagePart.Url("hollowcube.net"), MessagePart.class));
        assertEquals("""
            {"type":"emoji","name":"sus"}""", json(new MessagePart.Emoji("sus"), MessagePart.class));
        assertEquals("""
            {"type":"map","mapId":"m1"}""", json(new MessagePart.Map("m1"), MessagePart.class));
    }

    @Test
    void messagePart_readsAVariantThisBuildDoesNotKnowAsUnknown() {
        var part = Wire.gson().fromJson("""
            {"type":"sticker","id":"7"}""", MessagePart.class);

        assertEquals(new MessagePart.Unknown("sticker"), part);
        // Passing one back on would say something this build cannot describe, so it cannot.
        assertThrows(IllegalArgumentException.class, () -> Wire.gson().toJson(part, MessagePart.class));
    }

    @Test
    void chatMessage_roundTripsEveryPartAndField() {
        var message = new ChatMessage("s1", ChatChannel.LOCAL, null, "m1",
            List.of(new MessagePart.Raw("look at "), new MessagePart.Map("m1"), new MessagePart.Emoji("sus")),
            42L, true);

        var json = Wire.gson().toJson(message);

        assertEquals(JsonParser.parseString("""
            {"senderId":"s1","channel":"LOCAL","mapId":"m1","parts":[
                {"type":"raw","text":"look at "},{"type":"map","mapId":"m1"},{"type":"emoji","name":"sus"}],
             "seed":42,"senderHasHypercube":true}"""), JsonParser.parseString(json));
        assertEquals(message, Wire.gson().fromJson(json, ChatMessage.class));
    }

    @Test
    void chatResult_namesEveryVariantOnTheWire() {
        assertEquals("""
            {"type":"sent"}""", json(new ChatResult.Sent(), ChatResult.class));
        // ISO-8601, so that neither end has to be told the unit.
        assertEquals("""
            {"type":"muted","expiresAt":"2026-08-30T12:00:00Z"}""",
            json(new ChatResult.Muted(Instant.parse("2026-08-30T12:00:00Z")), ChatResult.class));
        // A permanent mute is the same variant with nothing to say about when it ends.
        assertEquals("""
            {"type":"muted"}""", json(new ChatResult.Muted(null), ChatResult.class));
        assertEquals("""
            {"type":"censored"}""", json(new ChatResult.Censored(), ChatResult.class));
        // The two shapes of one variant: a target to name, and nobody to name.
        assertEquals("""
            {"type":"target-offline"}""", json(new ChatResult.TargetOffline(null), ChatResult.class));
        assertEquals("""
            {"type":"target-offline","targetId":"t1"}""",
            json(new ChatResult.TargetOffline("t1"), ChatResult.class));
        assertEquals("""
            {"type":"dm-disabled"}""", json(new ChatResult.DmDisabled(null), ChatResult.class));
        assertEquals("""
            {"type":"dm-disabled","targetId":"t1"}""", json(new ChatResult.DmDisabled("t1"), ChatResult.class));
        assertEquals("""
            {"type":"map-not-published"}""", json(new ChatResult.MapNotPublished(), ChatResult.class));
    }

    static Stream<ChatResult> everyChatResult() {
        return Stream.of(new ChatResult.Sent(),
            new ChatResult.Muted(Instant.parse("2026-08-30T12:00:00Z")), new ChatResult.Muted(null), new ChatResult.Censored(),
            new ChatResult.TargetOffline("t1"), new ChatResult.TargetOffline(null),
            new ChatResult.DmDisabled("t1"), new ChatResult.DmDisabled(null),
            new ChatResult.MapNotPublished());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyChatResult")
    void chatResult_roundTripsThroughTheWireGson(ChatResult result) {
        assertEquals(result, Wire.gson().fromJson(json(result, ChatResult.class), ChatResult.class));
    }

    @Test
    void chatResult_readsAVariantThisBuildDoesNotKnowAsUnknown() {
        var result = Wire.gson().fromJson("""
            {"type":"shadowbanned"}""", ChatResult.class);

        assertEquals(new ChatResult.Unknown("shadowbanned"), result);
    }

    @Test
    void commandOutcome_carriesItsStatusInAnObjectSoItCanGrow() {
        assertEquals("""
            {"status":"SUCCESS"}""", Wire.gson().toJson(CommandOutcome.SUCCESS));
        assertEquals(CommandOutcome.DENIED,
            Wire.gson().fromJson(Wire.gson().toJson(CommandOutcome.DENIED), CommandOutcome.class));

        var unknown = Wire.gson().fromJson("""
            {"status":"RATE_LIMITED"}""", CommandOutcome.class);
        assertEquals(CommandOutcome.Status.UNKNOWN, unknown.status());
    }

    @Test
    void chatChannel_readsAChannelThisBuildDoesNotKnowAsUnknown() {
        assertEquals(ChatChannel.UNKNOWN, Wire.gson().fromJson("\"PARTY\"", ChatChannel.class));
        assertEquals(ChatChannel.DIRECT, Wire.gson().fromJson("\"DIRECT\"", ChatChannel.class));
    }
}
