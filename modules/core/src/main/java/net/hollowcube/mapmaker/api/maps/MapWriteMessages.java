package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.ipc.map.BeginVerificationResult;
import net.hollowcube.ipc.map.BuilderResult;
import net.hollowcube.ipc.map.CreateMapResult;
import net.hollowcube.ipc.map.DeleteVerificationResult;
import net.hollowcube.ipc.map.PublishMapResult;
import net.hollowcube.ipc.map.PublishReadiness;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.Locale;

/// What a player is told when a map write did not do what they asked.
public final class MapWriteMessages {

    public static Component failure(CreateMapResult result) {
        return switch (result) {
            case CreateMapResult.NoSlots(int limit) ->
                Component.translatable("map.write.no_slots", Component.text(limit));
            case CreateMapResult.SizeLocked _ -> Component.translatable("map.write.size_locked");
            case CreateMapResult.NotFound _ -> Component.translatable("map.write.not_found");
            case CreateMapResult.Unknown _ -> Component.translatable("generic.unknown_error");
            case CreateMapResult.Success _ ->
                throw new IllegalArgumentException("success has no failure message");
        };
    }

    public static Component failure(PublishMapResult result) {
        return switch (result) {
            case PublishMapResult.NotFound _ -> Component.translatable("map.write.not_found");
            case PublishMapResult.Blocked(var readiness) -> blocked(readiness);
            case PublishMapResult.Unknown _ -> Component.translatable("generic.unknown_error");
            case PublishMapResult.Success _ ->
                throw new IllegalArgumentException("success has no failure message");
        };
    }

    public static Component failure(BuilderResult result) {
        return switch (result) {
            case BuilderResult.NoSlots _ -> Component.translatable("map.write.builder.no_slots");
            case BuilderResult.CapacityReached _ ->
                Component.translatable("map.write.builder.capacity_reached");
            case BuilderResult.InvitesDisabled _ ->
                Component.translatable("map.write.builder.invites_disabled");
            case BuilderResult.AlreadyBuilder _ ->
                Component.translatable("map.write.builder.already_builder");
            case BuilderResult.InviteGone _ -> Component.translatable("map.write.builder.invite_gone");
            case BuilderResult.MapNotFound _ -> Component.translatable("map.write.builder.map_not_found");
            case BuilderResult.PlayerNotFound _ ->
                Component.translatable("map.write.builder.player_not_found");
            case BuilderResult.MapPublished _ ->
                Component.translatable("map.write.builder.map_published");
            case BuilderResult.Owner _ -> Component.translatable("map.write.builder.owner");
            case BuilderResult.Unknown _ -> Component.translatable("map.write.builder.unknown");
            case BuilderResult.Success _, BuilderResult.AlreadyDone _ ->
                throw new IllegalArgumentException("success has no failure message");
        };
    }

    public static Component verification(BeginVerificationResult result) {
        return Component.translatable(
            "map.write.verification." + result.name().toLowerCase(Locale.ROOT));
    }

    public static Component verification(DeleteVerificationResult result) {
        return Component.translatable(
            "map.write.verification." + result.name().toLowerCase(Locale.ROOT));
    }

    private static Component blocked(PublishReadiness readiness) {
        var missing = readiness.missing().stream()
            .map(requirement -> Component.translatable(
                "map.write.requirement." + requirement.name().toLowerCase(Locale.ROOT)))
            .toList();
        return Component.translatable("map.write.publish_blocked",
            Component.join(JoinConfiguration.commas(true), missing));
    }

    private MapWriteMessages() {}
}
