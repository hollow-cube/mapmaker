package net.hollowcube.mapmaker.map.block.placement.vanilla;

import net.hollowcube.mapmaker.map.block.placement.WaterloggedPlacementRule;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>NOT FOR PUBLIC DISTRIBUTION UNDER ANY CIRCUMSTANCE -- CODE IS DECOMPILED FROM THE VANILLA SERVER</p>
 *
 * <p>This placement rule is a copy paste of the vanilla logic for rails. It does not work like normal placement
 * rules (it does updates with instance setters), and honestly I have no idea whats going on in it.</p>
 *
 * <p>I would like to rewrite this at some point. The client does NOT predict rail updates so it should be
 * reasonable to create a fairly nice placement rule for rails. At the time of writing however, I am more
 * interested in releasing something than spending 2 days figuring out functional rails.</p>
 *
 * <p>NOT FOR PUBLIC DISTRIBUTION UNDER ANY CIRCUMSTANCE -- CODE IS DECOMPILED FROM THE VANILLA SERVER</p>
 */
public class RailPlacementRule extends WaterloggedPlacementRule {
    private final boolean isStraight;

    public RailPlacementRule(@NotNull Block block, boolean isStraight) {
        super(block);
        this.isStraight = isStraight;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placement) {
        var instance = placement.instance();
        var blockPosition = placement.placePosition();

        return new RailState(instance, blockPosition, this.block, this.isStraight)
                .place(true, true, this.block.getProperty("shape"))
                .getState().withProperty("waterlogged", waterlogged(placement));
    }

    public static class RailState {
        private final Block.Getter level;
        private final Point pos;
        private Block state;
        private final boolean isStraight;
        private final List<Point> connections = new ArrayList<>();

        public RailState(Block.Getter instance, Point $$1, Block state, boolean isStraight) {
            this.level = instance;
            this.pos = $$1;
            this.state = state;
            String shape = state.getProperty("shape");
            this.isStraight = isStraight;
            this.updateConnections(shape);
        }

        private void updateConnections(String shape) {
            this.connections.clear();
            switch (shape) {
                case "north_south": {
                    this.connections.add(this.pos.relative(BlockFace.NORTH));
                    this.connections.add(this.pos.relative(BlockFace.SOUTH));
                    break;
                }
                case "east_west": {
                    this.connections.add(this.pos.relative(BlockFace.WEST));
                    this.connections.add(this.pos.relative(BlockFace.EAST));
                    break;
                }
                case "ascending_east": {
                    this.connections.add(this.pos.relative(BlockFace.WEST));
                    this.connections.add(this.pos.relative(BlockFace.EAST).add(0, 1, 0));
                    break;
                }
                case "ascending_west": {
                    this.connections.add(this.pos.relative(BlockFace.WEST).add(0, 1, 0));
                    this.connections.add(this.pos.relative(BlockFace.EAST));
                    break;
                }
                case "ascending_north": {
                    this.connections.add(this.pos.relative(BlockFace.NORTH).add(0, 1, 0));
                    this.connections.add(this.pos.relative(BlockFace.SOUTH));
                    break;
                }
                case "ascending_south": {
                    this.connections.add(this.pos.relative(BlockFace.NORTH));
                    this.connections.add(this.pos.relative(BlockFace.SOUTH).add(0, 1, 0));
                    break;
                }
                case "south_east": {
                    this.connections.add(this.pos.relative(BlockFace.EAST));
                    this.connections.add(this.pos.relative(BlockFace.SOUTH));
                    break;
                }
                case "south_west": {
                    this.connections.add(this.pos.relative(BlockFace.WEST));
                    this.connections.add(this.pos.relative(BlockFace.SOUTH));
                    break;
                }
                case "north_west": {
                    this.connections.add(this.pos.relative(BlockFace.WEST));
                    this.connections.add(this.pos.relative(BlockFace.NORTH));
                    break;
                }
                case "north_east": {
                    this.connections.add(this.pos.relative(BlockFace.EAST));
                    this.connections.add(this.pos.relative(BlockFace.NORTH));
                }
            }
        }

        private void removeSoftConnections() {
            for (int $$0 = 0; $$0 < this.connections.size(); ++$$0) {
                RailState $$1 = this.getRail(this.connections.get($$0));
                if ($$1 == null || !$$1.connectsTo(this)) {
                    this.connections.remove($$0--);
                    continue;
                }
                this.connections.set($$0, $$1.pos);
            }
        }

        private static boolean isRail(@NotNull Block.Getter instance, @NotNull Point blockPosition) {
            return isRail(instance.getBlock(blockPosition, Block.Getter.Condition.TYPE));
        }

        private static boolean isRail(@NotNull Block block) {
            return block.id() == Block.RAIL.id() || block.id() == Block.POWERED_RAIL.id() ||
                    block.id() == Block.DETECTOR_RAIL.id() || block.id() == Block.ACTIVATOR_RAIL.id();
        }

        @Nullable
        private RailState getRail(Point blockPosition) {
            Point $$1 = blockPosition;
            Block $$2 = this.level.getBlock($$1);
            if (isRail($$2)) {
                return new RailState(this.level, $$1, $$2, $$2.id() == Block.DETECTOR_RAIL.id() || $$2.id() == Block.ACTIVATOR_RAIL.id() || $$2.id() == Block.POWERED_RAIL.id());
            }
            $$1 = blockPosition.add(0, 1, 0);
            $$2 = this.level.getBlock($$1);
            if (isRail($$2)) {
                return new RailState(this.level, $$1, $$2, $$2.id() == Block.DETECTOR_RAIL.id() || $$2.id() == Block.ACTIVATOR_RAIL.id() || $$2.id() == Block.POWERED_RAIL.id());
            }
            $$1 = blockPosition.add(0, -1, 0);
            $$2 = this.level.getBlock($$1);
            if (isRail($$2)) {
                return new RailState(this.level, $$1, $$2, $$2.id() == Block.DETECTOR_RAIL.id() || $$2.id() == Block.ACTIVATOR_RAIL.id() || $$2.id() == Block.POWERED_RAIL.id());
            }
            return null;
        }

        private boolean connectsTo(RailState $$0) {
            return this.hasConnection($$0.pos);
        }

        private boolean hasConnection(@NotNull Point blockPosition) {
            for (Point connection : this.connections) {
                if (connection.blockX() != blockPosition.blockX() || connection.blockZ() != blockPosition.blockZ())
                    continue;
                return true;
            }
            return false;
        }

        private boolean canConnectTo(RailState $$0) {
            return this.connectsTo($$0) || this.connections.size() != 2;
        }

        private void connectTo(RailState $$0) {
            this.connections.add($$0.pos);
            Point northPosition = this.pos.relative(BlockFace.NORTH);
            Point southPosition = this.pos.relative(BlockFace.SOUTH);
            Point westPosition = this.pos.relative(BlockFace.WEST);
            Point eastPosition = this.pos.relative(BlockFace.EAST);
            boolean hasNorth = this.hasConnection(northPosition);
            boolean hasSouth = this.hasConnection(southPosition);
            boolean hasWest = this.hasConnection(westPosition);
            boolean hasEast = this.hasConnection(eastPosition);
            String shape = null;
            if (hasNorth || hasSouth) {
                shape = "north_south";
            }
            if (hasWest || hasEast) {
                shape = "east_west";
            }
            if (!this.isStraight) {
                if (hasSouth && hasEast && !hasNorth && !hasWest) {
                    shape = "south_east";
                }
                if (hasSouth && hasWest && !hasNorth && !hasEast) {
                    shape = "south_west";
                }
                if (hasNorth && hasWest && !hasSouth && !hasEast) {
                    shape = "north_west";
                }
                if (hasNorth && hasEast && !hasSouth && !hasWest) {
                    shape = "north_east";
                }
            }
            if ("north_south".equals(shape)) {
                if (isRail(this.level, northPosition.add(0, 1, 0))) {
                    shape = "ascending_north";
                }
                if (isRail(this.level, southPosition.add(0, 1, 0))) {
                    shape = "ascending_south";
                }
            }
            if ("east_west".equals(shape)) {
                if (isRail(this.level, eastPosition.add(0, 1, 0))) {
                    shape = "ascending_east";
                }
                if (isRail(this.level, westPosition.add(0, 1, 0))) {
                    shape = "ascending_west";
                }
            }
            if (shape == null) {
                shape = "north_south";
            }
            this.state = this.state.withProperty("shape", shape);
            if (this.level instanceof Instance i)
                i.setBlock(this.pos, this.state);
        }

        private boolean hasNeighborRail(Point $$0) {
            RailState $$1 = this.getRail($$0);
            if ($$1 == null) {
                return false;
            }
            $$1.removeSoftConnections();
            return $$1.canConnectTo(this);
        }

        public RailState place(boolean hasNeighborSignal, boolean $$1, String shape) {
            Point northPosition = this.pos.relative(BlockFace.NORTH);
            Point southPosition = this.pos.relative(BlockFace.SOUTH);
            Point westPosition = this.pos.relative(BlockFace.WEST);
            Point eastPosition = this.pos.relative(BlockFace.EAST);
            boolean hasNorth = this.hasNeighborRail(northPosition);
            boolean hasSouth = this.hasNeighborRail(southPosition);
            boolean hasWest = this.hasNeighborRail(westPosition);
            boolean hasEast = this.hasNeighborRail(eastPosition);
            String shape2 = null;
            boolean hasNS = hasNorth || hasSouth;
            boolean hasEW = hasWest || hasEast;
            if (hasNS && !hasEW) {
                shape2 = "north_south";
            }
            if (hasEW && !hasNS) {
                shape2 = "east_west";
            }
            boolean isSouthEast = hasSouth && hasEast;
            boolean isSouthWest = hasSouth && hasWest;
            boolean isNorthEast = hasNorth && hasEast;
            boolean isNorthWest = hasNorth && hasWest;
            if (!this.isStraight) {
                if (isSouthEast && !hasNorth && !hasWest) {
                    shape2 = "south_east";
                }
                if (isSouthWest && !hasNorth && !hasEast) {
                    shape2 = "south_west";
                }
                if (isNorthWest && !hasSouth && !hasEast) {
                    shape2 = "north_west";
                }
                if (isNorthEast && !hasSouth && !hasWest) {
                    shape2 = "north_east";
                }
            }
            if (shape2 == null) {
                if (hasNS && hasEW) {
                    shape2 = shape;
                } else if (hasNS) {
                    shape2 = "north_south";
                } else if (hasEW) {
                    shape2 = "east_west";
                }
                if (!this.isStraight) {
                    if (hasNeighborSignal) {
                        if (isSouthEast) {
                            shape2 = "south_east";
                        }
                        if (isSouthWest) {
                            shape2 = "south_west";
                        }
                        if (isNorthEast) {
                            shape2 = "north_east";
                        }
                        if (isNorthWest) {
                            shape2 = "north_west";
                        }
                    } else {
                        if (isNorthWest) {
                            shape2 = "north_west";
                        }
                        if (isNorthEast) {
                            shape2 = "north_east";
                        }
                        if (isSouthWest) {
                            shape2 = "south_west";
                        }
                        if (isSouthEast) {
                            shape2 = "south_east";
                        }
                    }
                }
            }
            if ("north_south".equals(shape2)) {
                if (isRail(this.level, northPosition.add(0, 1, 0))) {
                    shape2 = "ascending_north";
                }
                if (isRail(this.level, southPosition.add(0, 1, 0))) {
                    shape2 = "ascending_south";
                }
            }
            if ("east_west".equals(shape2)) {
                if (isRail(this.level, eastPosition.add(0, 1, 0))) {
                    shape2 = "ascending_east";
                }
                if (isRail(this.level, westPosition.add(0, 1, 0))) {
                    shape2 = "ascending_west";
                }
            }
            if (shape2 == null) {
                shape2 = shape;
            }
            this.updateConnections(shape2);
            this.state = this.state.withProperty("shape", shape2);
            if ($$1 || this.level.getBlock(this.pos).stateId() != this.state.stateId()) {
                if (this.level instanceof Instance i)
                    i.setBlock(this.pos, this.state, false);
                for (int $$18 = 0; $$18 < this.connections.size(); ++$$18) {
                    RailState $$19 = this.getRail(this.connections.get($$18));
                    if ($$19 == null) continue;
                    $$19.removeSoftConnections();
                    if (!$$19.canConnectTo(this)) continue;
                    $$19.connectTo(this);
                }
            }
            return this;
        }

        public Block getState() {
            return this.state;
        }
    }
}
