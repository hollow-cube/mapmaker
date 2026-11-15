package net.hollowcube.terraform.apply;

import java.util.BitSet;

/// An applied change to a section
///
/// Cells are indexed section-locally: `y << 8 | z << 4 | x` for blocks and `y << 4 | z << 2 | x` for biome cells.
record SectionChange(int sectionY, BitSet blocks, BitSet biomes) {}
