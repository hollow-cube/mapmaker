package net.hollowcube.terraform.compat.metabrush;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class VoxelSniperExtensions {


    public static void b(Instance instance, Block defaultBlock, Map<Point, UnknownMaybeListOfBlocks> hashMap, int metaRadius, double intensity, double gridMultiplier, int radius, boolean smooth, boolean adapt, boolean distance) {
        int n3 = radius * 2;
        int n4 = (int)(gridMultiplier * (double)radius);
        int n5 = radius / 2;


        Map<Point, Block> changes = new HashMap<>();
        hashMap.values().forEach(o_02 -> {
            Point blockVector3 = o_02.origin;
            Point blockVector32 = new Vec(blockVector3.blockX(), blockVector3.blockY(), blockVector3.blockZ()); // todo seems like these are the same thing 2x
            if (metaRadius > 0) {
                expandBlockSet2(instance, 0, metaRadius, o_02.a, new HashMap<>(), new HashMap<>());
            }

            List<Point> arrayList = new ArrayList<>();
            double d2 = n3 * n3;
            for (int x = -n3; x <= n3; ++x) {
                for (int y = -n3; y <= n3; ++y) {
                    for (int z = -n3; z <= n3; ++z) {
                        Point object = blockVector32.add(x, y, z);
                        if (!((double)(x * x + y * y + z * z) <= d2) || x % n4 != 0 || y % n4 != 0 || z % n4 != 0 || getBlockInSession(object, instance).isAir() || !(object.distance(blockVector32) <= (double)n3))
                            continue;
                        arrayList.add(object);
                    }
                }
            }

            Map<Point, Double> hashMap2 = new HashMap<>();
            int n6 = 2 * n5 - 1;
            for (Point object : arrayList) {
                double d3 = 0.0;
                for (int x = -n5; x <= n5; ++x) {
                    for (int y = -n5; y <= n5; ++y) {
                        for (int z = -n5; z <= n5; ++z) {
                            Point blockVector34 = object.add(x, y, z);
                            if (getBlockInSession(blockVector34, instance).isAir()) continue;
                            d3 += 1.0;
                        }
                    }
                }
                hashMap2.put(object, d3 / (double)(n6 * n6 * n6));
            }

            k_0 k_02 = new k_0();
            o_02.a.keySet().forEach(blockVector33 -> {
                AtomicReference<Double> atomicReference = new AtomicReference<Double>(0.0);
                for (int i = -2; i <= 2; ++i) {
                    for (int j = -2; j <= 2; ++j) {
                        for (int k = -2; k <= 2; ++k) {
                            if (!(Math.pow(i, 2.0) + Math.pow(j, 2.0) + Math.pow(k, 2.0) <= 4.0)) continue;
                            Point blockVector34 = blockVector33.add(i, j, k);
                            if (o_02.a.containsKey(new Vec(blockVector34.blockX(), blockVector34.blockY(), blockVector34.blockZ()))) {
                                if (!(o_02.a.get(new Vec(blockVector34.blockX(), blockVector34.blockY(), blockVector34.blockZ()))).isSolid()) continue;
                                atomicReference.getAndSet(atomicReference.get() + 1.0);
                                continue;
                            }
                            if (!getBlockInSession(new Vec((int)(blockVector33.blockX() + i), (int)(blockVector33.blockY() + j), (int)(blockVector33.blockZ() + k)), instance).isSolid()) continue;
                            atomicReference.getAndSet(atomicReference.get() + 1.0);
                        }
                    }
                }
                atomicReference.getAndSet(atomicReference.get() / Math.pow(5.0, 3.0));
                o_02.d.put(blockVector33, atomicReference.get());
                if (adapt) {
                    double d;
                    if (distance) {
                        double d24 = blockVector33.distance(blockVector3) / (double)radius;
                        d = Math.min(1.0 - Math.sqrt(1.0 - Math.pow(d24 - 1.0, 2.0)), 1.0);
                    } else {
                        d = o_02.d.get(blockVector33);
                    }
                    k_02.a(k_02.a() + (d += hashMap2.keySet().stream().mapToDouble(blockVector324 -> {
                        double dddd = blockVector33.distance(new Vec((int)blockVector324.blockX(), (int)blockVector324.blockY(), (int)blockVector324.blockZ())) / (double)n5;
                        return 1.0 / Math.max(dddd * dddd, 1.0) * hashMap2.get(blockVector324);
                    }).sum()) / (double)(hashMap2.size() + 1));
                    Point blockVector35 = new Vec((int)blockVector33.blockX(), (int)blockVector33.blockY(), (int)blockVector33.blockZ());
                    k_02.a(blockVector35, d / (double)(hashMap2.size() + 1));
                }
            });
            if (smooth) {
                for (Double d4 : o_02.d.values()) {
                    o_02.c += d4.doubleValue();
                }
                o_02.c /= (double)o_02.d.values().size();
            }
            if (smooth) {
                for (Point blockVector35 : o_02.d.keySet()) {
                    Point blockVector36 = new Vec((int)blockVector35.blockX(), (int)blockVector35.blockY(), (int)blockVector35.blockZ());
                    if (!(o_02.d.get(blockVector35) * (intensity / 100.0) < o_02.c) || !o_02.a.containsKey(blockVector35.add(o_02.e))) continue;
                    changes.put(new Vec((int)blockVector36.blockX(), (int)blockVector36.blockY(), (int)blockVector36.blockZ()), getMostCommonBlock(blockVector36, instance, o_02.a));
                }
            }
            if (adapt) {
                for (Point blockVector37 : k_02.a.keySet()) {
                    if (!((Double)k_02.a.get(blockVector37) * (intensity / 100.0) > k_02.a() / (double)k_02.a.size())) continue;
                    changes.put(new Vec((int)blockVector37.blockX(), (int)blockVector37.blockY(), (int)blockVector37.blockZ()), defaultBlock);
                }
            }
            if (!adapt && !smooth) {
                changes.putAll(o_02.a);
            }
        });

        for (Point blockVector3 : changes.keySet()) {
            instance.setBlock(blockVector3, changes.get(blockVector3));
        }
    }

    public static void a(Instance var0, Block block, Map<Point, UnknownMaybeListOfBlocks> var1, int var2, double var3, double var5, int var7, boolean var8, boolean var9, boolean var10) {
        int var11 = var7 * 2;
        int var12 = (int)(var5 * (double)var7);
        int var13 = var7 / 2;
        Map<Point, Block> var15 = new HashMap<>();
        var1.values().forEach((var13x) -> {
            Point var14x = var13x.origin;
            Point var15x = new Vec(var14x.blockX(), var14x.blockY(), var14x.blockZ());
            if (var2 > 0) {
                expandBlockSet2(var0, 0, var2, var13x.a, new HashMap<>(), new HashMap<>());
            }

            List<Point> var16 = new ArrayList();
            double var17 = (double)(var11 * var11);

            int var20;
            Point var22;
            for(int var19 = -var11; var19 <= var11; ++var19) {
                for(var20 = -var11; var20 <= var11; ++var20) {
                    for(int var21 = -var11; var21 <= var11; ++var21) {
                        var22 = new Vec(var14x.blockX() + var19, var14x.blockY() + var20, var14x.blockZ() + var21);
                        if ((double)(var19 * var19 + var20 * var20 + var21 * var21) <= var17 && var19 % var12 == 0 && var20 % var12 == 0 && var21 % var12 == 0 &&
                                var0.getBlock(var22).isAir() && var22.distance(var15x) <= (double)var11) {
                            var16.add(var22);
                        }
                    }
                }
            }

            Map<Point, Double> var29 = new HashMap();
            var20 = 2 * var13 - 1;
            Iterator<Point> var30 = var16.iterator();

            while(var30.hasNext()) {
                var22 = var30.next();
                double var23 = 0.0;

                for(int var25 = -var13; var25 <= var13; ++var25) {
                    for(int var26 = -var13; var26 <= var13; ++var26) {
                        for(int var27 = -var13; var27 <= var13; ++var27) {
                            Point var28 = new Vec(var22.blockX() + var25, var22.blockY() + var26, var22.blockZ() + var27);
                            if (var0.getBlock(var28).isAir()) {
                                ++var23;
                            }
                        }
                    }
                }

                var29.put(var22, var23 / (double)(var20 * var20 * var20));
            }

            k_0 var31 = new k_0();
            var13x.a.keySet().forEach((var9x) -> {
                AtomicReference<Double> var10x = new AtomicReference<>(0.0);

                for(int var111 = -2; var111 <= 2; ++var111) {
                    for(int var121 = -2; var121 <= 2; ++var121) {
                        for(int var13xx = -2; var13xx <= 2; ++var13xx) {
                            if (Math.pow((double)var111, 2.0) + Math.pow((double)var121, 2.0) + Math.pow((double)var13xx, 2.0) <= 4.0) {
                                Point var14xx = var9x.add(var111, var121, var13xx);
                                if (var13x.a.containsKey(new Vec(var14xx.blockX(), var14xx.blockY(), var14xx.blockZ()))) {
                                    if ((var13x.a.get(new Vec(var14xx.blockX(), var14xx.blockY(), var14xx.blockZ()))).isAir()) {
                                        var10x.getAndSet((Double)var10x.get() + 1.0);
                                    }
                                } else if (var0.getBlock(new Vec(var9x.blockX() + var111, var9x.blockY() + var121, var9x.blockZ() + var13xx)).isAir()) {
                                    var10x.getAndSet((Double)var10x.get() + 1.0);
                                }
                            }
                        }
                    }
                }

                var10x.getAndSet((Double)var10x.get() / (4.1887902047863905 * Math.pow(2.0, 3.0)));
                var13x.d.put(var9x, (Double)var10x.get());
                if (var9) {
                    double var152;
                    if (var10) {
                        double var164 = var9x.distance(var14x) / (double)var7;
                        var152 = Math.min(1.0 - Math.sqrt(1.0 - Math.pow(var164 - 1.0, 2.0)), 1.0);
                    } else {
                        var152 = var13x.d.get(var9x);
                    }

                    var152 += var29.keySet().stream().mapToDouble((var3x) -> {
                        double var4 = var9x.distance(new Vec(var3x.blockX(), var3x.blockY(), var3x.blockZ())) / (double)var13;
                        return 1.0 / Math.max(var4 * var4, 1.0) * (Double)var29.get(var3x);
                    }).sum();
                    var31.a(var31.a() + var152 / (double)(var29.size() + 1));
                    Point var17x = new Vec(var9x.blockX(), var9x.blockY(), var9x.blockZ());
                    var31.a(var17x, var152 / (double)(var29.size() + 1));
                }

            });
            Iterator<Double> var32;
            if (var8) {
                Double var33;
                for(var32 = var13x.d.values().iterator(); var32.hasNext(); var13x.c += var33) {
                    var33 = (Double)var32.next();
                }

                var13x.c /= (double)var13x.d.values().size();
            }

            Point var34;
            if (var8) {
                Iterator<Point> var32x = var13x.d.keySet().iterator();

                while(var32x.hasNext()) {
                    var34 = var32x.next();
                    Point var24 = new Vec(var34.blockX(), var34.blockY(), var34.blockZ());
                    if ((Double)var13x.d.get(var34) * (var3 / 100.0) > var13x.c && var13x.a.containsKey(var34.add(var13x.e))) {
                        var15.put(new Vec(var24.blockX(), var24.blockY(), var24.blockZ()), Block.AIR);
                    }
                }
            }

            if (var9) {
                Iterator<Point> var32xx = var31.a.keySet().iterator();

                while(var32xx.hasNext()) {
                    var34 = var32xx.next();
                    if ((Double)var31.a.get(var34) * (var3 / 100.0) > var31.a() / (double)var31.a.size()) {
                        var15.put(new Vec(var34.blockX(), var34.blockY(), var34.blockZ()), Block.AIR);
                    }
                }
            }

            if (!var9 && !var8) {
                var15.putAll(var13x.a);
            }

        });

        for (Point var17 : var15.keySet()) {
            var0.setBlock(var17, var15.get(var17));
//            var14.setBlockType(var17, ((BlockState)var15.get(var17)).getBlockType());
        }

    }


    public static void b2(Instance var0, Block block, Map<Point, UnknownMaybeListOfBlocks> var1, int var2, double var3, double var5, int var7, boolean var8, boolean var9, boolean var10) {
        int var11 = var7 * 2;
        int var12 = (int)(var5 * (double)var7);
        int var13 = var7 / 2;
        Map<Point, Block> var15 = new HashMap<>();
        var1.values().forEach((var13x) -> {
            Point var14x = var13x.origin;
            Point var15x = new Vec(var14x.blockX(), var14x.blockY(), var14x.blockZ());
            if (var2 > 0) {
                expandBlockSet2(var0, 0, var2, var13x.a, new HashMap<>(), new HashMap<>());
            }

            List<Point> var16 = new ArrayList<>();
            double var17 = (double)(var11 * var11);

            int var20;
            Point var22;
            for(int var19 = -var11; var19 <= var11; ++var19) {
                for(var20 = -var11; var20 <= var11; ++var20) {
                    for(int var21 = -var11; var21 <= var11; ++var21) {
                        var22 = new Vec(var14x.blockX() + var19, var14x.blockY() + var20, var14x.blockZ() + var21);
                        if ((double)(var19 * var19 + var20 * var20 + var21 * var21) <= var17 && var19 % var12 == 0 && var20 % var12 == 0 && var21 % var12 == 0 &&
                                !getBlockInSession(var22, var0).isAir() && var22.distance(var15x) <= (double)var11) {
                            var16.add(var22);
                        }
                    }
                }
            }

            HashMap<Point, Double> var29 = new HashMap<>();
            var20 = 2 * var13 - 1;
            Iterator<Point> var30 = var16.iterator();

            while(var30.hasNext()) {
                var22 = var30.next();
                double var23 = 0.0;

                for(int var25 = -var13; var25 <= var13; ++var25) {
                    for(int var26 = -var13; var26 <= var13; ++var26) {
                        for(int var27 = -var13; var27 <= var13; ++var27) {
                            Point var28 = new Vec(var22.blockX() + var25, var22.blockY() + var26, var22.blockZ() + var27);
                            if (!getBlockInSession(var28, var0).isAir()) {
                                ++var23;
                            }
                        }
                    }
                }

                var29.put(var22, var23 / (double)(var20 * var20 * var20));
            }

            k_0 var31 = new k_0();
            var13x.a.keySet().forEach((var9x) -> {
                AtomicReference var10x = new AtomicReference(0.0);

                for(int var11xx = -2; var11xx <= 2; ++var11xx) {
                    for(int var12xx = -2; var12xx <= 2; ++var12xx) {
                        for(int var13xx = -2; var13xx <= 2; ++var13xx) {
                            if (Math.pow((double)var11xx, 2.0) + Math.pow((double)var12xx, 2.0) + Math.pow((double)var13xx, 2.0) <= 4.0) {
                                Point var14xx = var9x.add(var11xx, var12xx, var13xx);
                                if (var13x.a.containsKey(new Vec(var14xx.blockX(), var14xx.blockY(), var14xx.blockZ()))) {
                                    if ((var13x.a.get(new Vec(var14xx.blockX(), var14xx.blockY(), var14xx.blockZ()))).isSolid()) {
                                        var10x.getAndSet((Double)var10x.get() + 1.0);
                                    }
                                } else if (getBlockInSession(new Vec(var9x.blockX() + var11xx, var9x.blockY() + var12xx, var9x.blockZ() + var13xx), var0).isSolid()) {
                                    var10x.getAndSet((Double)var10x.get() + 1.0);
                                }
                            }
                        }
                    }
                }

                var10x.getAndSet((Double)var10x.get() / Math.pow(5.0, 3.0));
                var13x.d.put(var9x, (Double)var10x.get());
                if (var9) {
                    double var15xx;
                    if (var10) {
                        double var16xx = var9x.distance(var14x) / (double)var7;
                        var15xx = Math.min(1.0 - Math.sqrt(1.0 - Math.pow(var16xx - 1.0, 2.0)), 1.0);
                    } else {
                        var15xx = (Double)var13x.d.get(var9x);
                    }

                    var15xx += var29.keySet().stream().mapToDouble((var3xx) -> {
                        double var4 = var9x.distance(new Vec(var3xx.blockX(), var3xx.blockY(), var3xx.blockZ())) / (double)var13;
                        return 1.0 / Math.max(var4 * var4, 1.0) * (Double)var29.get(var3xx);
                    }).sum();
                    var31.a(var31.a() + var15xx / (double)(var29.size() + 1));
                    Point var17xx = new Vec(var9x.blockX(), var9x.blockY(), var9x.blockZ());
                    var31.a(var17xx, var15xx / (double)(var29.size() + 1));
                }

            });

            if (var8) {
                Double var33;
                for(var var32 = var13x.d.values().iterator(); var32.hasNext(); var13x.c += var33) {
                    var33 = (Double)var32.next();
                }

                var13x.c /= (double)var13x.d.values().size();
            }

            Point var34;
            if (var8) {
                Iterator<Point> var32 = var13x.d.keySet().iterator();

                while(var32.hasNext()) {
                    var34 = var32.next();
                    Point var24 = new Vec(var34.blockX(), var34.blockY(), var34.blockZ());
                    if ((Double)var13x.d.get(var34) * (var3 / 100.0) < var13x.c && var13x.a.containsKey(var34.add(var13x.e))) {
                        var15.put(new Vec(var24.blockX(), var24.blockY(), var24.blockZ()), getMostCommonBlock(var24, var0, var13x.a));
                    }
                }
            }

            if (var9) {
                Iterator<Point> var32 = var31.a.keySet().iterator();

                while(var32.hasNext()) {
                    var34 = var32.next();
                    if ((Double)var31.a.get(var34) * (var3 / 100.0) > var31.a() / (double)var31.a.size()) {
                        var15.put(new Vec(var34.blockX(), var34.blockY(), var34.blockZ()), block);
                    }
                }
            }

            if (!var9 && !var8) {
                var15.putAll(var13x.a);
            }

        });
        Iterator<Point> var16 = var15.keySet().iterator();

        while(var16.hasNext()) {
            Point var17 = var16.next();
            var0.setBlock(var17, var15.get(var17));
//            var14.setBlockType(var17, ((BlockState)var15.get(var17)).getBlockType());
        }

    }
    
    public static Block getBlockInSession(Point blockVector3, Instance editSession) {
        //todo
//        if (blockVector3.getBlockY() > editSession.getMaxY() || blockVector3.getBlockY() < editSession.getMinY())
//            return BlockTypes.STONE.getDefaultState();
        return editSession.getBlock(blockVector3);
    }

    public static void expandBlockSet(Instance instance, int iteration, int maxIterations, Map<Point, Block> hashMap, Map<Point, Block> newBlocks, Map<Point, Block> hashMap3) {
        if (iteration == 0) {
            hashMap.keySet().forEach(origin -> {
                for (int x = -1; x <= 1; ++x) {
                    for (int y = -1; y <= 1; ++y) {
                        for (int z = -1; z <= 1; ++z) {
                            if (!(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0) <= Math.sqrt(2.0))) continue;

                            Point blockPosition = origin.add(x, y, z);
                            Point relativePosition = new Vec(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
                            if (hashMap.containsKey(blockPosition) || hashMap3.containsKey(blockPosition)) continue;

                            newBlocks.put(blockPosition, instance.getBlock(relativePosition));
                        }
                    }
                }
            });
            hashMap.putAll(newBlocks);
            VoxelSniperExtensions.expandBlockSet(instance, iteration + 1, maxIterations, hashMap, new HashMap<>(), newBlocks);
        } else if (iteration < maxIterations) {
            hashMap3.keySet().forEach(origin -> {
                for (int x = -1; x <= 1; ++x) {
                    for (int y = -1; y <= 1; ++y) {
                        for (int z = -1; z <= 1; ++z) {
                            if (!(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0) <= Math.sqrt(2.0))) continue;

                            Point blockPosition = origin.add(x, y, z);
                            Point relativePosition = new Vec(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
                            if (hashMap.containsKey(blockPosition) || hashMap3.containsKey(blockPosition)) continue;

                            newBlocks.put(blockPosition, instance.getBlock(relativePosition));
                        }
                    }
                }
            });
            hashMap.putAll(newBlocks);
            VoxelSniperExtensions.expandBlockSet(instance, iteration + 1, maxIterations, hashMap, new HashMap<>(), newBlocks);
        }
    }

    public static void expandBlockSet2(Instance instance, int n, int n2, Map<Point, Block> hashMap, Map<Point, Block> hashMap2, Map<Point, Block> hashMap3) {
        if (n == 0) {
            hashMap.keySet().forEach(blockVector3 -> {
                for (int i = -1; i <= 1; ++i) {
                    for (int j = -1; j <= 1; ++j) {
                        for (int k = -1; k <= 1; ++k) {
                            if (!(Math.pow(i, 2.0) + Math.pow(j, 2.0) + Math.pow(k, 2.0) <= Math.sqrt(2.0))) continue;
                            Point blockVector32 = blockVector3.add(i, j, k);
                            Point blockVector33 = new Vec((int)blockVector32.blockX(), (int)blockVector32.blockY(), (int)blockVector32.blockZ());
                            if (hashMap.containsKey(blockVector32)) continue;
                            hashMap2.put(blockVector32, instance.getBlock(blockVector33));
                        }
                    }
                }
            });
            hashMap.putAll(hashMap2);
            expandBlockSet2(instance, n + 1, n2, hashMap, new HashMap(), hashMap2);
        } else if (n < n2) {
            hashMap3.keySet().forEach(blockVector3 -> {
                for (int i = -1; i <= 1; ++i) {
                    for (int j = -1; j <= 1; ++j) {
                        for (int k = -1; k <= 1; ++k) {
                            if (!(Math.pow(i, 2.0) + Math.pow(j, 2.0) + Math.pow(k, 2.0) <= Math.sqrt(2.0))) continue;
                            Point blockVector32 = blockVector3.add(i, j, k);
                            Point blockVector33 = new Vec((int)blockVector32.blockX(), (int)blockVector32.blockY(), (int)blockVector32.blockZ());
                            if (hashMap.containsKey(blockVector32) || hashMap3.containsKey(blockVector32)) continue;
                            hashMap2.put(blockVector32, instance.getBlock(blockVector33));
                        }
                    }
                }
            });
            hashMap.putAll(hashMap2);
            expandBlockSet2(instance, n + 1, n2, hashMap, new HashMap(), hashMap2);
        }
    }


    public static Block getMostCommonBlock(Point blockPosition, Instance editSession, Map<Point, Block> cachedBlocks) {
        //todo handle instance min/max
//        int sessionMaxY = editSession.getMaxY();
//        int sessionMinY = editSession.getMinY();

        Map<Block, Double> countByBlock = new HashMap<>();
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    Block state = cachedBlocks.getOrDefault(
                            blockPosition.add(x, y, z),
                            editSession.getBlock(blockPosition.add(x, y, z)) 
                    );
                    countByBlock.put(state, countByBlock.getOrDefault(state, 0.0) + 1.0);
                }
            }
        }

        double max = countByBlock.values().stream().mapToDouble(d -> d).max().getAsDouble();
        return countByBlock.keySet().stream()
                .filter(blockState -> max == countByBlock.get(blockState))
                .findAny()
                .filter(blockState -> blockState.isSolid())
                .orElse(Block.AIR);
    }

//    public static void performArrowChanges(Map<Point, UnknownMaybeListOfBlocks> hashMap,
//                                           int metaRadius, double intensity, double gridMultiplier, int radius,
//                                           boolean smooth, boolean adapt, boolean distance) {
//        int diameter = radius * 2;
//        int n4 = (int)(gridMultiplier * (double)radius);
//        int halfRadius = radius / 2;
//        AbstractBrush abstractBrush = (AbstractBrush)snipe.getBrush();
//
//        Map<Point, Block> changes = new HashMap<>();
//        hashMap.values().forEach(o_02 -> {
//            BlockVector3 origin = BlockVector3.at(o_02.origin.blockX(), o_02.origin.blockY(), o_02.origin.blockZ());
//            if (metaRadius > 0) {
//                VoxelSniperExtensions.expandBlockSet(snipe, 0, metaRadius, o_02.a, new HashMap<>(), new HashMap<>());
//            }
//
//            List<BlockVector3> nonAirBlocksInWorld = new ArrayList<>();
//            double d2 = diameter * diameter;
//            for (int x = -diameter; x <= diameter; ++x) {
//                for (int y = -diameter; y <= diameter; ++y) {
//                    for (int z = -diameter; z <= diameter; ++z) {
//                        var blockPosition = BlockVector3.at(origin.blockX() + x, origin.blockY() + y, origin.blockZ() + z);
//                        if (!((double)(x * x + y * y + z * z) <= d2) || x % n4 != 0 || y % n4 != 0 || z % n4 != 0) continue;
//                        if (getBlockInSession(blockPosition, abstractBrush.getEditSession()).isAir() || !(blockPosition.distance(origin) <= (double)diameter)) continue;
//
//                        nonAirBlocksInWorld.add(blockPosition);
//                    }
//                }
//            }
//
//            Map<BlockVector3, Double> airRatios = new HashMap<>();
//            double cubeArea = Math.pow(2 * halfRadius - 1, 3);
//            for (BlockVector3 object : nonAirBlocksInWorld) {
//                double nonAirBlocks = 0.0;
//                for (int x = -halfRadius; x <= halfRadius; ++x) {
//                    for (int y = -halfRadius; y <= halfRadius; ++y) {
//                        for (int z = -halfRadius; z <= halfRadius; ++z) {
//                            BlockVector3 blockPosition = BlockVector3.at(object.blockX() + x, object.blockY() + y, object.blockZ() + z);
//                            if (VoxelSniperExtensions.getBlockInSession(blockPosition, abstractBrush.getEditSession()).isAir()) continue;
//
//                            nonAirBlocks += 1.0;
//                        }
//                    }
//                }
//                airRatios.put(object, nonAirBlocks / cubeArea);
//            }
//
//            k_0 k_02 = new k_0();
//            o_02.a.keySet().forEach(blockVector33 -> {
//                AtomicReference<Double> atomicReference = new AtomicReference<>(0.0);
//                for (int x = -2; x <= 2; ++x) {
//                    for (int y = -2; y <= 2; ++y) {
//                        for (int z = -2; z <= 2; ++z) {
//                            if (!(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0) <= 4.0)) continue;
//
//                            BlockVector3 blockVector34 = blockVector33.add(x, y, z);
//                            if (o_02.a.containsKey(BlockVector3.at(blockVector34.blockX(), blockVector34.blockY(), blockVector34.blockZ()))) {
//                                if (!((BlockState)o_02.a.get(BlockVector3.at(blockVector34.blockX(), blockVector34.blockY(), blockVector34.blockZ()))).getMaterial().isSolid()) continue;
//                                atomicReference.getAndSet(atomicReference.get() + 1.0);
//                                continue;
//                            }
//
//                            if (!VoxelSniperExtensions.getBlockInSession(BlockVector3.at(blockVector33.blockX() + x, blockVector33.blockY() + y, blockVector33.blockZ() + z), abstractBrush.getEditSession()).getMaterial().isSolid()) continue;
//                            atomicReference.getAndSet(atomicReference.get() + 1.0);
//                        }
//                    }
//                }
//                atomicReference.getAndSet(atomicReference.get() / Math.pow(5.0, 3.0));
//                o_02.d.put(blockVector33, atomicReference.get());
//
//                if (adapt) {
//                    double d;
//                    if (distance) {
//                        double d2 = blockVector33.distance(blockVector3) / (double)radius;
//                        d = Math.min(1.0 - Math.sqrt(1.0 - Math.pow(d2 - 1.0, 2.0)), 1.0);
//                    } else {
//                        d = o_02.d.get(blockVector33);
//                    }
//                    k_02.a(k_02.a() + (d += airRatios.keySet().stream().mapToDouble(blockVector32 -> {
//                        double d = blockVector33.distance(BlockVector3.at(blockVector32.blockX(), blockVector32.blockY(), blockVector32.blockZ())) / (double)halfRadius;
//                        return 1.0 / Math.max(d * d, 1.0) * airRatios.get(blockVector32);
//                    }).sum()) / (double)(airRatios.size() + 1));
//                    BlockVector3 blockVector35 = BlockVector3.at((int)blockVector33.blockX(), (int)blockVector33.blockY(), (int)blockVector33.blockZ());
//                    k_02.a(blockVector35, d / (double)(airRatios.size() + 1));
//                }
//            });
//
//            if (smooth) {
//                for (Double d4 : o_02.d.values()) {
//                    o_02.c += d4.doubleValue();
//                }
//                o_02.c /= (double)o_02.d.values().size();
//            }
//
//            if (smooth) {
//                for (BlockVector3 blockVector35 : o_02.d.keySet()) {
//                    BlockVector3 blockVector36 = BlockVector3.at((int)blockVector35.blockX(), (int)blockVector35.blockY(), (int)blockVector35.blockZ());
//                    if (!((Double)o_02.d.get(blockVector35) * (intensity / 100.0) < o_02.c) || !o_02.a.containsKey(blockVector35.toVector3().add(o_02.e).toBlockPoint())) continue;
//                    airRatios.put(BlockVector3.at((int)blockVector36.blockX(), (int)blockVector36.blockY(), (int)blockVector36.blockZ()), VoxelSniperExtensions.getMostCommonBlock(blockVector36, abstractBrush.getEditSession(), o_02.a));
//                }
//            }
//
//            if (adapt) {
//                for (BlockVector3 blockVector37 : k_02.a.keySet()) {
//                    if (!(k_02.a.get(blockVector37) * (intensity / 100.0) > k_02.a() / (double)k_02.a.size())) continue;
//                    airRatios.put(BlockVector3.at(blockVector37.blockX(), blockVector37.blockY(), blockVector37.blockZ()), snipe.getToolkitProperties().getBlockData());
//                }
//            }
//
//            if (!adapt && !smooth) {
//                airRatios.putAll(o_02.a);
//            }
//        });
//
//        for (BlockVector3 blockVector3 : changes.keySet()) {
//            abstractBrush.setBlock(blockVector3, changes.get(blockVector3).getBlockType());
//        }
//    }
}
