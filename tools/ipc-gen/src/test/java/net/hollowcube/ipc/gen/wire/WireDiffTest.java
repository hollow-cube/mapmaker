package net.hollowcube.ipc.gen.wire;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The compatibility rules, one handcrafted pair of descriptors each. `old` is always the client
/// still running; `current` is what main is about to deploy.
class WireDiffTest {

    private static final String BASE = """
        {
          "services": {
            "echo": {
              "interface": "test.EchoService",
              "methods": {
                "record": {"params": [{"name": "event", "type": "test.Event"}]},
                "echo": {"params": [{"name": "message", "type": "String"}, {"name": "count", "type": "int"}], "returns": {"type": "test.Reply"}}
              }
            }
          },
          "types": {
            "test.Reply": {"kind": "record", "used": ["response"], "fields": [{"name": "text", "type": "String"}, {"name": "note", "type": "String", "nullable": true}]},
            "test.Event": {"kind": "record", "used": ["request"], "fields": [{"name": "name", "type": "String"}, {"name": "note", "type": "String", "nullable": true}]},
            "test.Color": {"kind": "enum", "constants": ["RED", "GREEN"]},
            "test.Shape": {"kind": "sealed", "discriminator": "type", "variants": {"circle": "test.Circle"}}
          },
          "subjects": {"point.moved": "test.PointMoved"},
          "notifications": {"invite": "test.Invite"}
        }
        """;

    private static List<String> breaks(String current) {
        return WireDiff.diff(WireDescriptor.parse(BASE), WireDescriptor.parse(current)).stream()
            .map(WireDiff.Break::toString)
            .toList();
    }

    private static String edited(String from, String to) {
        assertTrue(BASE.contains(from), "fixture does not contain: " + from);
        return BASE.replace(from, to);
    }

    @Test
    void identicalDescriptorsAreCompatible() {
        assertEquals(List.of(), breaks(BASE));
    }

    @Test
    void addingAServiceMethodOrNullableParamIsSafe() {
        assertEquals(List.of(), breaks(edited(
            "\"record\": {",
            "\"ping\": {\"params\": []}, \"record\": {")));
        assertEquals(List.of(), breaks(edited(
            "{\"name\": \"count\", \"type\": \"int\"}",
            "{\"name\": \"count\", \"type\": \"int\"}, {\"name\": \"suffix\", \"type\": \"String\", \"nullable\": true}")));
        assertEquals(List.of(), breaks(edited(
            "\"services\": {",
            "\"services\": {\"other\": {\"interface\": \"test.Other\", \"methods\": {}},")));
    }

    @Test
    void removingAParamIsSafe() {
        assertEquals(List.of(), breaks(edited(
            "{\"name\": \"message\", \"type\": \"String\"}, {\"name\": \"count\", \"type\": \"int\"}",
            "{\"name\": \"message\", \"type\": \"String\"}")));
    }

    @Test
    void removingAServiceOrMethodBreaks() {
        assertEquals(List.of("service echo: removed"), breaks(edited("\"echo\": {\n      \"interface\"", "\"other\": {\n      \"interface\"")));
        assertEquals(List.of("service echo / method record: removed"), breaks(edited(
            "\"record\": {\"params\": [{\"name\": \"event\", \"type\": \"test.Event\"}]},", "")));
    }

    @Test
    void addingANonNullParamBreaks() {
        assertEquals(List.of("service echo / method echo / param suffix: non-null param added; an old client does not send it"),
            breaks(edited(
                "{\"name\": \"count\", \"type\": \"int\"}",
                "{\"name\": \"count\", \"type\": \"int\"}, {\"name\": \"suffix\", \"type\": \"String\"}")));
    }

    @Test
    void changingAParamTypeOrNullabilityBreaks() {
        assertEquals(List.of("service echo / method echo / param count: type int -> long"),
            breaks(edited("{\"name\": \"count\", \"type\": \"int\"}", "{\"name\": \"count\", \"type\": \"long\"}")));
        assertEquals(List.of("service echo / method echo / param count: non-null -> nullable; an old client does not expect null"),
            breaks(edited("{\"name\": \"count\", \"type\": \"int\"}", "{\"name\": \"count\", \"type\": \"int\", \"nullable\": true}")));
    }

    @Test
    void changingTheReturnTypeBreaks() {
        assertEquals(List.of("service echo / method echo / returns: type test.Reply -> String"),
            breaks(edited("\"returns\": {\"type\": \"test.Reply\"}", "\"returns\": {\"type\": \"String\"}")));
        assertEquals(List.of("service echo / method echo / returns: non-null -> nullable; an old client does not expect null"),
            breaks(edited("\"returns\": {\"type\": \"test.Reply\"}", "\"returns\": {\"type\": \"test.Reply\", \"nullable\": true}")));
        assertEquals(List.of("service echo / method echo / returns: test.Reply -> void"),
            breaks(edited(", \"returns\": {\"type\": \"test.Reply\"}", "")));
    }

    @Test
    void addingAFieldToAResponseRecordIsSafe() {
        assertEquals(List.of(), breaks(edited(
            "{\"name\": \"text\", \"type\": \"String\"}",
            "{\"name\": \"text\", \"type\": \"String\"}, {\"name\": \"extra\", \"type\": \"int\"}")));
    }

    /// An old client never sends a field it does not know, so what it sends may only grow by
    /// fields that may be missing.
    @Test
    void addingANonNullFieldToARequestRecordBreaks() {
        assertEquals(List.of("record test.Event / field extra: non-null field added to a record an old client writes; it has to be nullable"),
            breaks(edited(
                "{\"name\": \"name\", \"type\": \"String\"}",
                "{\"name\": \"name\", \"type\": \"String\"}, {\"name\": \"extra\", \"type\": \"int\"}")));
        assertEquals(List.of(), breaks(edited(
            "{\"name\": \"name\", \"type\": \"String\"}",
            "{\"name\": \"name\", \"type\": \"String\"}, {\"name\": \"extra\", \"type\": \"Integer\", \"nullable\": true}")));
    }

    @Test
    void removingOrRetypingAFieldBreaks() {
        assertEquals(List.of("record test.Reply / field text: removed"),
            breaks(edited("{\"name\": \"text\", \"type\": \"String\"}, ", "")));
        assertEquals(List.of("record test.Reply / field text: type String -> int"),
            breaks(edited("{\"name\": \"text\", \"type\": \"String\"}", "{\"name\": \"text\", \"type\": \"int\"}")));
    }

    @Test
    void fieldNullabilityFollowsWhoWritesTheRecord() {
        // A response field going nullable is a null the old client does not expect.
        assertEquals(List.of("record test.Reply / field text: non-null -> nullable; an old client does not expect null"),
            breaks(edited("{\"name\": \"text\", \"type\": \"String\"}", "{\"name\": \"text\", \"type\": \"String\", \"nullable\": true}")));
        // A response field going non-null is the server promising more.
        assertEquals(List.of(), breaks(edited(
            "\"used\": [\"response\"], \"fields\": [{\"name\": \"text\", \"type\": \"String\"}, {\"name\": \"note\", \"type\": \"String\", \"nullable\": true}]",
            "\"used\": [\"response\"], \"fields\": [{\"name\": \"text\", \"type\": \"String\"}, {\"name\": \"note\", \"type\": \"String\"}]")));
        // A request field going non-null is a null the old client may still send.
        assertEquals(List.of("record test.Event / field note: nullable -> non-null; an old client may still send null"),
            breaks(edited(
                "\"used\": [\"request\"], \"fields\": [{\"name\": \"name\", \"type\": \"String\"}, {\"name\": \"note\", \"type\": \"String\", \"nullable\": true}]",
                "\"used\": [\"request\"], \"fields\": [{\"name\": \"name\", \"type\": \"String\"}, {\"name\": \"note\", \"type\": \"String\"}]")));
    }

    @Test
    void enumConstantsMayBeAddedButNotRemoved() {
        assertEquals(List.of(), breaks(edited("[\"RED\", \"GREEN\"]", "[\"RED\", \"GREEN\", \"BLUE\"]")));
        assertEquals(List.of("enum test.Color / constant GREEN: removed"), breaks(edited("[\"RED\", \"GREEN\"]", "[\"RED\"]")));
    }

    @Test
    void sealedVariantsMayBeAddedButNotRemovedOrRenamed() {
        assertEquals(List.of(), breaks(edited("{\"circle\": \"test.Circle\"}", "{\"circle\": \"test.Circle\", \"square\": \"test.Square\"}")));
        assertEquals(List.of("sealed test.Shape / variant circle: removed"), breaks(edited("{\"circle\": \"test.Circle\"}", "{\"round\": \"test.Circle\"}")));
        assertEquals(List.of("sealed test.Shape: discriminator type -> status"), breaks(edited("\"discriminator\": \"type\"", "\"discriminator\": \"status\"")));
    }

    @Test
    void changingATypesKindBreaks() {
        assertEquals(List.of("enum test.Color: became a record"),
            breaks(edited("\"test.Color\": {\"kind\": \"enum\", \"constants\": [\"RED\", \"GREEN\"]}",
                "\"test.Color\": {\"kind\": \"record\", \"fields\": []}")));
    }

    @Test
    void removedTypesNothingReachesAreNotBreaks() {
        assertEquals(List.of(), breaks(edited("\"test.Color\": {\"kind\": \"enum\", \"constants\": [\"RED\", \"GREEN\"]},", "")));
    }

    @Test
    void subjectsAndNotificationKeysMayBeAddedButNotRemovedOrRetyped() {
        assertEquals(List.of(), breaks(edited("\"subjects\": {", "\"subjects\": {\"point.deleted\": \"test.PointDeleted\", ")));
        assertEquals(List.of("subject point.moved: removed"), breaks(edited("\"point.moved\"", "\"point.changed\"")));
        assertEquals(List.of("subject point.moved: type test.PointMoved -> test.Moved"), breaks(edited("\"test.PointMoved\"", "\"test.Moved\"")));
        assertEquals(List.of("notification invite: removed"), breaks(edited("\"invite\": \"test.Invite\"", "")));
    }

    @Test
    void descriptorsRoundTripThroughJson() {
        var parsed = WireDescriptor.parse(BASE);
        assertEquals(parsed, WireDescriptor.parse(parsed.toJson()));
        assertEquals(parsed.toJson(), WireDescriptor.parse(parsed.toJson()).toJson());
    }
}
