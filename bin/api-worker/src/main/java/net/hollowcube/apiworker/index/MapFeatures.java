package net.hollowcube.apiworker.index;

import java.util.Locale;
import java.util.Set;

/// Everything derivable about a map from its world data alone; the `map_features` row before it
/// is written.
///
/// Deliberately holds nothing that already lives in the maps table: no rating, no play count, no
/// author. Those are joinable at query time, and copying them here would mean reindexing every map
/// whenever someone rates it. What is left is exactly the set of things you can only learn by
/// opening the artifact, which keeps indexing a pure function of the world bytes: an unchanged map
/// always produces an identical record. Ratios of what is here — fill, aspect, verticality — are
/// left to the query too.
///
/// Sizes are in blocks. Anything derived from the map's extent uses the trimmed extent, since a
/// single stray block a thousand blocks out is common and would otherwise dominate.
public record MapFeatures(
    // The mapmaker data version the world was saved with, or -1 when it predates one. Markers in
    // a world that old are not scanned, so the structure and mechanics below are empty for it.
    int dataVersion,

    long blockCount, // Non-air blocks
    // Size, trimmed of the outermost 1% of occupied cells at each end.
    int extentX, int extentY, int extentZ,
    // 8^3 cells containing at least one block: how much space the map actually occupies.
    int occupiedCells,

    // Distinct block types, ignoring block state.
    int distinctBlocks,
    // Share of blocks that are the single most common type. Near 1 with a low distinctBlocks is
    // the obstaslop signal.
    double dominantBlockFrac,

    // Everything in the world that is not a marker: displays, item frames, armour stands,
    // passengers included. Good maps tend to be decorated, so this is a quality signal, and text
    // displays in particular are how a map explains itself.
    int entityCount,
    int textDisplayCount,

    // Structure, from checkpoint, finish and status triggers.
    //
    // Each kind is read from both chunk entity data (regions) and block entity data (plates).
    // Touching plates always merge, since a 5x5 pad is one trigger however it was built. Regions
    // only merge where overlapping them is a way of drawing a shape: finishes. Two overlapping
    // checkpoint or status regions are deliberately two triggers. The spawn is not a checkpoint.

    int checkpointCount,
    // Median distance from a checkpoint to its nearest neighbour; 0 under two checkpoints.
    double checkpointSpacing,
    int finishCount,
    int statusCount,

    // Mechanics, from the action lists on every trigger.
    //
    // Presence only. A count of triggers granting something measures map length more than it
    // measures the mechanic, and items persist once granted, so a map that grants pearls once and
    // a map that tops you up at every checkpoint differ by 40x in count and not at all in what it
    // is like to play. How much of a run actually has a mechanic needs the checkpoint order, which
    // only a replay knows.
    Set<Mechanic> mechanics,
    // Attributes the map edits, by registry key: gravity, scale, step_height,
    // block_interaction_range. Any of these means the map's physics are not vanilla, so nothing
    // inferred about its difficulty from geometry can be trusted.
    Set<String> attributes,
    // Potion effects the map applies, by registry key, most often speed or jump_boost.
    Set<String> potionEffects,
    // Settings the map uses, by id. Read from the spawn checkpoint, which holds the initial flags,
    // plus every checkpoint and status trigger; a setting counts as used when any of them gives it
    // a positive value. There is no global flag to fall back on, so this is the whole picture, and
    // it says nothing about whether a setting applies to the whole map or one section.
    Set<String> settings,

    // Actions across every trigger, as an overall complexity signal. Unlike the sets above this is
    // a real quantity, though it wants normalizing by checkpointCount.
    int actionCount,

    // Triggers that did not decode and are missing from everything above, plus one for each
    // chunk whose user data did not read at all, which takes its entities with it. Anything but
    // zero means this record is incomplete.
    int decodeFailures
) {
    /// Something a map does to the player, drawn from the action list on one of its triggers.
    ///
    /// Only covers what has no natural sub-key of its own; attributes, potions and settings each
    /// get their own set so that gravity is not confused with scale, or no_sprint with no_jump.
    ///
    /// Stored lower-cased in a `text[]`, so asking for pearl maps that are not elytra maps is one
    /// GIN indexed containment test, and adding a mechanic later needs no migration.
    public enum Mechanic {
        // Items granted.
        BLOCKS, ENDER_PEARL, WIND_CHARGE, TRIDENT,
        MACE, ELYTRA, FIREWORK_ROCKET,
        // Takes an item or elytra away again, ie the map has distinct mechanical phases.
        ITEM_REVOKE,

        // Sets velocity directly, ie launch pads.
        VELOCITY,
        // Moves the player, ie the route is not contiguous.
        TELEPORT,
        // Changes the reset height, ie the fall that ends a run varies across the map.
        RESET_HEIGHT,

        // Edits lives: the map is life limited, or resets the count somewhere.
        LIVES,
        // Edits the timer: the map is time limited, or resets the clock somewhere.
        TIMER,
        // Clears placed blocks, which block-placing maps use to reset a section.
        CLEAR_BLOCKS,
        // Touches a script variable, as a rough proxy for how scripted the map is.
        VARIABLE;

        /// The value as it is stored.
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
