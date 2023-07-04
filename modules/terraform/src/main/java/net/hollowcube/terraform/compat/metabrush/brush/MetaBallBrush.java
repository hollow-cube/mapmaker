package net.hollowcube.terraform.compat.metabrush.brush;

import net.hollowcube.terraform.compat.metabrush.UnknownMaybeListOfBlocks;
import net.hollowcube.terraform.compat.metabrush.VoxelSniperExtensions;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MetaBallBrush {
    public double intensity;
    public int metaRadius;
    public double gridMultiplier;
    public boolean adapt;
    public boolean smooth;
    public boolean distance;

//    @Override
    public void loadProperties() {
//        super.loadProperties();
        this.intensity = 100.0;
        this.metaRadius = 2;
        this.gridMultiplier = 0.75;
        this.adapt = true;
        this.smooth = false;
        this.distance = true;
    }

//    public void handleCommand(String[] stringArray, Snipe snipe) {
//        Messenger snipeMessenger = new Messenger(snipe.createMessenger());
//        for (String string : stringArray) {
//            if (!string.equalsIgnoreCase("info")) continue;
//            snipeMessenger.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "MetaBall brush Parameters:");
//            snipeMessenger.sendMessage(ChatColor.LIGHT_PURPLE + "Ex: /b mb [MetaBrush Parameters]");
//        }
//        super.a(stringArray, snipe);
//    }

//    public void sendInfo(Snipe snipe) {
//        this.b(snipe);
//        snipe.createMessageSender().brushNameMessage().brushSizeMessage().send();
//    }

    public void handleArrowAction(Instance instance, @NotNull Point targetBlock, @NotNull Block block, int brushSize) {
        var blockVector3 = targetBlock;
        int n = brushSize;
        UnknownMaybeListOfBlocks o_02 = new UnknownMaybeListOfBlocks(blockVector3);
        for (int x = -n; x <= n; ++x) {
            for (int y = -n; y <= n; ++y) {
                for (int z = -n; z <= n; ++z) {
                    if (!(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0) <= (double) (n * n))) continue;

                    o_02.a.put(blockVector3.add(x, y, z), block);
                }
            }
        }

        Map<Point, UnknownMaybeListOfBlocks> hashMap = new HashMap<>();
        hashMap.put(blockVector3, o_02);
        VoxelSniperExtensions.b2(instance, block, hashMap, this.metaRadius, this.intensity, this.gridMultiplier, brushSize, this.smooth, this.adapt, this.distance);
    }

//    public void handleGunpowderAction(Snipe snipe) {
//        BlockVector3 blockVector3 = this.getTargetBlock();
//        int n = snipe.getToolkitProperties().getBrushSize();
//        HashMap<BlockVector3, UnknownMaybeListOfBlocks> hashMap = new HashMap<>();
//        UnknownMaybeListOfBlocks o_02 = new UnknownMaybeListOfBlocks(BlockVector3.at(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ()));
//        for (int i = -n; i <= n; ++i) {
//            for (int j = -n; j <= n; ++j) {
//                for (int k = -n; k <= n; ++k) {
//                    if (i * i + j * j + k * k > n * n) continue;
//                    o_02.a.put(blockVector3.add(i, j, k), snipe.getToolkitProperties().getBlockData());
//                }
//            }
//        }
//        hashMap.put(BlockVector3.at(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ()), o_02);
//        VoxelSniperExtensions.performGunpowderChanges(snipe, hashMap, this.metaRadius, this.intensity, this.gridMultiplier, snipe.getToolkitProperties().getBrushSize(), this.smooth, this.adapt, this.distance);
//    }
}
