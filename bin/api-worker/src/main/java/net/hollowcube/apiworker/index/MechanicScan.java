package net.hollowcube.apiworker.index;

import net.hollowcube.apiworker.index.MapFeatures.Mechanic;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.*;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.*;
import net.kyori.adventure.key.Key;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/// Reads the decoded actions on a map's triggers into the mechanic sets.
///
/// Anything with a sub-key of its own goes into a set keyed by registry key rather than becoming a
/// single flag, since editing gravity and editing scale are not the same map, and neither are
/// no_sprint and no_jump. Every one of these fields is optional in its action, and an action left
/// half configured in the editor decodes with it null, so each is checked before use.
final class MechanicScan {
    /// Checkpoint items are a registry of their own, so an item is matched on its registered key
    /// rather than its class. Not every item is a mechanic worth a flag.
    private static final Map<Key, Mechanic> ITEM_MECHANICS = Map.of(
        BlockCheckpointItem.ID, Mechanic.BLOCKS,
        EnderPearlCheckpointItem.ID, Mechanic.ENDER_PEARL,
        WindChargeCheckpointItem.ID, Mechanic.WIND_CHARGE,
        TridentCheckpointItem.ID, Mechanic.TRIDENT,
        MaceCheckpointItem.ID, Mechanic.MACE,
        FireworkRocketCheckpointItem.ID, Mechanic.FIREWORK_ROCKET);

    /// The result of walking every action on every trigger. `actionCount` counts actions on merged
    /// triggers, so a pad of identical plates contributes once rather than once per block.
    record Result(
        Set<Mechanic> mechanics,
        Set<String> attributes,
        Set<String> potionEffects,
        Set<String> settings,
        int actionCount
    ) {
    }

    static Result scan(List<TriggerScan.Trigger> triggers) {
        var mechanics = EnumSet.noneOf(Mechanic.class);
        var attributes = new TreeSet<String>();
        var potionEffects = new TreeSet<String>();
        var settings = new TreeSet<String>();
        int actionCount = 0;

        for (var trigger : triggers) {
            for (var action : trigger.data().actions().actions()) {
                actionCount++;

                switch (action) {
                    case GiveItemAction give -> {
                        if (give.item() != null) {
                            var item = ITEM_MECHANICS.get(CheckpointItems.getKey(give.item()));
                            if (item != null) mechanics.add(item);
                        }
                    }
                    case TakeItemAction _, TakeElytraAction _ -> mechanics.add(Mechanic.ITEM_REVOKE);
                    case GiveElytraAction _ -> mechanics.add(Mechanic.ELYTRA);

                    case EditAttributeAction edit -> {
                        if (edit.attribute() != null) attributes.add(edit.attribute().key().value());
                    }
                    case AddPotionAction potion -> {
                        if (potion.effect() != null) potionEffects.add(potion.effect().id());
                    }
                    // Disables are ignored: a setting counts as used where something turns it on,
                    // which for a map wide rule is the spawn checkpoint.
                    case EnableSettingAction enable -> {
                        if (enable.setting() != null) settings.add(enable.setting().key());
                    }

                    case EditVelocityAction _ -> mechanics.add(Mechanic.VELOCITY);
                    case TeleportAction _ -> mechanics.add(Mechanic.TELEPORT);
                    case ResetHeightAction _ -> mechanics.add(Mechanic.RESET_HEIGHT);
                    case EditLivesAction _ -> mechanics.add(Mechanic.LIVES);
                    case EditTimerAction _ -> mechanics.add(Mechanic.TIMER);
                    case ClearBlocksAction _ -> mechanics.add(Mechanic.CLEAR_BLOCKS);
                    case EditVariableAction _ -> mechanics.add(Mechanic.VARIABLE);
                    default -> {
                    }
                }
            }
        }

        return new Result(mechanics, attributes, potionEffects, settings, actionCount);
    }

    private MechanicScan() {
    }
}
