package net.hollowcube.datafix.versions.v1xxx;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V1451_3 extends DataVersion {
    private static final Object2IntMap<String> BLOCK_TO_ID = new Object2IntOpenHashMap<>();

    public V1451_3() {
        super(1451, 3);

        addReference(DataTypes.ENTITY, "minecraft:egg", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:ender_pearl", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:fireball", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:potion", field -> field
            .single("inTile", DataTypes.BLOCK_NAME)
            .single("Potion", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:small_fireball", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:snowball", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:wither_skull", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:xp_bottle", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:arrow", field -> field
            .single("inBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:enderman", field -> field
            .single("carriedBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:falling_block", field -> field
            .single("BlockState", DataTypes.BLOCK_STATE)
            .single("TileEntityData", DataTypes.BLOCK_ENTITY));
        addReference(DataTypes.ENTITY, "minecraft:spectral_arrow", field -> field
            .single("inBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:chest_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE)
            .single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.ENTITY, "minecraft:furnace_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:hopper_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE)
            .list("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:spawner_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:tnt_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));

        // TODO: there is a block name fix in inTile for a handful of entities here,
        //  but I (matt) dont understand the mechanics of it. need to revisit.
        //  FROM LATER ME: I THINK THIS IS SOLVED WITH REFERENCES ABOVE. FIND A REAL EXAMPLE TO TEST

        // i think how this entire fix works is that we upgrade block names to latest and then backreference them but idk totally

        addFix(DataTypes.ENTITY, "minecraft:falling_block", V1451_3::updateFallingBlock);
        addFix(DataTypes.ENTITY, "minecraft:enderman", V1451_3::updateEnderman);
        addFix(DataTypes.ENTITY, "minecraft:arrow", V1451_3::updateInTile);
        addFix(DataTypes.ENTITY, "minecraft:spectral_arrow", V1451_3::updateInTile);
        addFix(DataTypes.ENTITY, "minecraft:commandblock_minecart", V1451_3::updateDisplayTile);
        addFix(DataTypes.ENTITY, "minecraft:minecart", V1451_3::updateDisplayTile);
        addFix(DataTypes.ENTITY, "minecraft:chest_minecart", V1451_3::updateDisplayTile);
        addFix(DataTypes.ENTITY, "minecraft:furnace_minecart", V1451_3::updateDisplayTile);
        addFix(DataTypes.ENTITY, "minecraft:tnt_minecart", V1451_3::updateDisplayTile);
        addFix(DataTypes.ENTITY, "minecraft:hopper_minecart", V1451_3::updateDisplayTile);
        addFix(DataTypes.ENTITY, "minecraft:spawner_minecart", V1451_3::updateDisplayTile);

        addFix(DataTypes.ITEM_STACK, "minecraft:filled_map", V1451_3::fixMapItemStackMapId);
    }

    private static @Nullable Value updateFallingBlock(Value entity) {
        var id = switch (entity.remove("Block").value()) {
            case String s -> BLOCK_TO_ID.getOrDefault(s, 0);
            case Number n -> n.intValue();
            case null, default -> {
                var tileId = entity.remove("TileID").as(Number.class, null);
                if (tileId != null) yield tileId.intValue();
                yield entity.remove("Tile").as(Number.class, 0).byteValue() & 0xFF;
            }
        };
        var data = entity.remove("Data").as(Number.class, 0).intValue();
        entity.put("BlockState", V1450.getBlockState(id, data));
        return null;
    }

    private static @Nullable Value updateEnderman(Value entity) {
        return updateBlockIdAndDataToState(entity, "carried",
                "carriedData", "carriedBlockState");
    }

    private static @Nullable Value updateDisplayTile(Value entity) {
        return updateBlockIdAndDataToState(entity, "DisplayTile",
                "DisplayData", "DisplayState");
    }

    private static @Nullable Value updateInTile(Value entity) {
        return updateBlockIdAndDataToState(entity, "inTile",
                "inData", "inBlockState");
    }

    private static @Nullable Value updateBlockIdAndDataToState(Value value, String blockIdField, String blockDataField, String blockStateField) {
        var id = switch (value.remove(blockIdField).value()) {
            case String s -> BLOCK_TO_ID.getOrDefault(s, 0);
            case Number n -> n.intValue();
            case null, default -> 0;
        };
        var data = value.remove(blockDataField).as(Number.class, 0).intValue();
        value.put(blockStateField, V1450.getBlockState(id, data));
        return null;
    }

    private static Value fixMapItemStackMapId(Value itemStack) {
        itemStack.put("map", itemStack.get("Damage").as(Number.class, 0).intValue());
        return null;
    }

    static {
        BLOCK_TO_ID.defaultReturnValue(0);
        BLOCK_TO_ID.put("minecraft:air", 0);
        BLOCK_TO_ID.put("minecraft:stone", 1);
        BLOCK_TO_ID.put("minecraft:grass", 2);
        BLOCK_TO_ID.put("minecraft:dirt", 3);
        BLOCK_TO_ID.put("minecraft:cobblestone", 4);
        BLOCK_TO_ID.put("minecraft:planks", 5);
        BLOCK_TO_ID.put("minecraft:sapling", 6);
        BLOCK_TO_ID.put("minecraft:bedrock", 7);
        BLOCK_TO_ID.put("minecraft:flowing_water", 8);
        BLOCK_TO_ID.put("minecraft:water", 9);
        BLOCK_TO_ID.put("minecraft:flowing_lava", 10);
        BLOCK_TO_ID.put("minecraft:lava", 11);
        BLOCK_TO_ID.put("minecraft:sand", 12);
        BLOCK_TO_ID.put("minecraft:gravel", 13);
        BLOCK_TO_ID.put("minecraft:gold_ore", 14);
        BLOCK_TO_ID.put("minecraft:iron_ore", 15);
        BLOCK_TO_ID.put("minecraft:coal_ore", 16);
        BLOCK_TO_ID.put("minecraft:log", 17);
        BLOCK_TO_ID.put("minecraft:leaves", 18);
        BLOCK_TO_ID.put("minecraft:sponge", 19);
        BLOCK_TO_ID.put("minecraft:glass", 20);
        BLOCK_TO_ID.put("minecraft:lapis_ore", 21);
        BLOCK_TO_ID.put("minecraft:lapis_block", 22);
        BLOCK_TO_ID.put("minecraft:dispenser", 23);
        BLOCK_TO_ID.put("minecraft:sandstone", 24);
        BLOCK_TO_ID.put("minecraft:noteblock", 25);
        BLOCK_TO_ID.put("minecraft:bed", 26);
        BLOCK_TO_ID.put("minecraft:golden_rail", 27);
        BLOCK_TO_ID.put("minecraft:detector_rail", 28);
        BLOCK_TO_ID.put("minecraft:sticky_piston", 29);
        BLOCK_TO_ID.put("minecraft:web", 30);
        BLOCK_TO_ID.put("minecraft:tallgrass", 31);
        BLOCK_TO_ID.put("minecraft:deadbush", 32);
        BLOCK_TO_ID.put("minecraft:piston", 33);
        BLOCK_TO_ID.put("minecraft:piston_head", 34);
        BLOCK_TO_ID.put("minecraft:wool", 35);
        BLOCK_TO_ID.put("minecraft:piston_extension", 36);
        BLOCK_TO_ID.put("minecraft:yellow_flower", 37);
        BLOCK_TO_ID.put("minecraft:red_flower", 38);
        BLOCK_TO_ID.put("minecraft:brown_mushroom", 39);
        BLOCK_TO_ID.put("minecraft:red_mushroom", 40);
        BLOCK_TO_ID.put("minecraft:gold_block", 41);
        BLOCK_TO_ID.put("minecraft:iron_block", 42);
        BLOCK_TO_ID.put("minecraft:double_stone_slab", 43);
        BLOCK_TO_ID.put("minecraft:stone_slab", 44);
        BLOCK_TO_ID.put("minecraft:brick_block", 45);
        BLOCK_TO_ID.put("minecraft:tnt", 46);
        BLOCK_TO_ID.put("minecraft:bookshelf", 47);
        BLOCK_TO_ID.put("minecraft:mossy_cobblestone", 48);
        BLOCK_TO_ID.put("minecraft:obsidian", 49);
        BLOCK_TO_ID.put("minecraft:torch", 50);
        BLOCK_TO_ID.put("minecraft:fire", 51);
        BLOCK_TO_ID.put("minecraft:mob_spawner", 52);
        BLOCK_TO_ID.put("minecraft:oak_stairs", 53);
        BLOCK_TO_ID.put("minecraft:chest", 54);
        BLOCK_TO_ID.put("minecraft:redstone_wire", 55);
        BLOCK_TO_ID.put("minecraft:diamond_ore", 56);
        BLOCK_TO_ID.put("minecraft:diamond_block", 57);
        BLOCK_TO_ID.put("minecraft:crafting_table", 58);
        BLOCK_TO_ID.put("minecraft:wheat", 59);
        BLOCK_TO_ID.put("minecraft:farmland", 60);
        BLOCK_TO_ID.put("minecraft:furnace", 61);
        BLOCK_TO_ID.put("minecraft:lit_furnace", 62);
        BLOCK_TO_ID.put("minecraft:standing_sign", 63);
        BLOCK_TO_ID.put("minecraft:wooden_door", 64);
        BLOCK_TO_ID.put("minecraft:ladder", 65);
        BLOCK_TO_ID.put("minecraft:rail", 66);
        BLOCK_TO_ID.put("minecraft:stone_stairs", 67);
        BLOCK_TO_ID.put("minecraft:wall_sign", 68);
        BLOCK_TO_ID.put("minecraft:lever", 69);
        BLOCK_TO_ID.put("minecraft:stone_pressure_plate", 70);
        BLOCK_TO_ID.put("minecraft:iron_door", 71);
        BLOCK_TO_ID.put("minecraft:wooden_pressure_plate", 72);
        BLOCK_TO_ID.put("minecraft:redstone_ore", 73);
        BLOCK_TO_ID.put("minecraft:lit_redstone_ore", 74);
        BLOCK_TO_ID.put("minecraft:unlit_redstone_torch", 75);
        BLOCK_TO_ID.put("minecraft:redstone_torch", 76);
        BLOCK_TO_ID.put("minecraft:stone_button", 77);
        BLOCK_TO_ID.put("minecraft:snow_layer", 78);
        BLOCK_TO_ID.put("minecraft:ice", 79);
        BLOCK_TO_ID.put("minecraft:snow", 80);
        BLOCK_TO_ID.put("minecraft:cactus", 81);
        BLOCK_TO_ID.put("minecraft:clay", 82);
        BLOCK_TO_ID.put("minecraft:reeds", 83);
        BLOCK_TO_ID.put("minecraft:jukebox", 84);
        BLOCK_TO_ID.put("minecraft:fence", 85);
        BLOCK_TO_ID.put("minecraft:pumpkin", 86);
        BLOCK_TO_ID.put("minecraft:netherrack", 87);
        BLOCK_TO_ID.put("minecraft:soul_sand", 88);
        BLOCK_TO_ID.put("minecraft:glowstone", 89);
        BLOCK_TO_ID.put("minecraft:portal", 90);
        BLOCK_TO_ID.put("minecraft:lit_pumpkin", 91);
        BLOCK_TO_ID.put("minecraft:cake", 92);
        BLOCK_TO_ID.put("minecraft:unpowered_repeater", 93);
        BLOCK_TO_ID.put("minecraft:powered_repeater", 94);
        BLOCK_TO_ID.put("minecraft:stained_glass", 95);
        BLOCK_TO_ID.put("minecraft:trapdoor", 96);
        BLOCK_TO_ID.put("minecraft:monster_egg", 97);
        BLOCK_TO_ID.put("minecraft:stonebrick", 98);
        BLOCK_TO_ID.put("minecraft:brown_mushroom_block", 99);
        BLOCK_TO_ID.put("minecraft:red_mushroom_block", 100);
        BLOCK_TO_ID.put("minecraft:iron_bars", 101);
        BLOCK_TO_ID.put("minecraft:glass_pane", 102);
        BLOCK_TO_ID.put("minecraft:melon_block", 103);
        BLOCK_TO_ID.put("minecraft:pumpkin_stem", 104);
        BLOCK_TO_ID.put("minecraft:melon_stem", 105);
        BLOCK_TO_ID.put("minecraft:vine", 106);
        BLOCK_TO_ID.put("minecraft:fence_gate", 107);
        BLOCK_TO_ID.put("minecraft:brick_stairs", 108);
        BLOCK_TO_ID.put("minecraft:stone_brick_stairs", 109);
        BLOCK_TO_ID.put("minecraft:mycelium", 110);
        BLOCK_TO_ID.put("minecraft:waterlily", 111);
        BLOCK_TO_ID.put("minecraft:nether_brick", 112);
        BLOCK_TO_ID.put("minecraft:nether_brick_fence", 113);
        BLOCK_TO_ID.put("minecraft:nether_brick_stairs", 114);
        BLOCK_TO_ID.put("minecraft:nether_wart", 115);
        BLOCK_TO_ID.put("minecraft:enchanting_table", 116);
        BLOCK_TO_ID.put("minecraft:brewing_stand", 117);
        BLOCK_TO_ID.put("minecraft:cauldron", 118);
        BLOCK_TO_ID.put("minecraft:end_portal", 119);
        BLOCK_TO_ID.put("minecraft:end_portal_frame", 120);
        BLOCK_TO_ID.put("minecraft:end_stone", 121);
        BLOCK_TO_ID.put("minecraft:dragon_egg", 122);
        BLOCK_TO_ID.put("minecraft:redstone_lamp", 123);
        BLOCK_TO_ID.put("minecraft:lit_redstone_lamp", 124);
        BLOCK_TO_ID.put("minecraft:double_wooden_slab", 125);
        BLOCK_TO_ID.put("minecraft:wooden_slab", 126);
        BLOCK_TO_ID.put("minecraft:cocoa", 127);
        BLOCK_TO_ID.put("minecraft:sandstone_stairs", 128);
        BLOCK_TO_ID.put("minecraft:emerald_ore", 129);
        BLOCK_TO_ID.put("minecraft:ender_chest", 130);
        BLOCK_TO_ID.put("minecraft:tripwire_hook", 131);
        BLOCK_TO_ID.put("minecraft:tripwire", 132);
        BLOCK_TO_ID.put("minecraft:emerald_block", 133);
        BLOCK_TO_ID.put("minecraft:spruce_stairs", 134);
        BLOCK_TO_ID.put("minecraft:birch_stairs", 135);
        BLOCK_TO_ID.put("minecraft:jungle_stairs", 136);
        BLOCK_TO_ID.put("minecraft:command_block", 137);
        BLOCK_TO_ID.put("minecraft:beacon", 138);
        BLOCK_TO_ID.put("minecraft:cobblestone_wall", 139);
        BLOCK_TO_ID.put("minecraft:flower_pot", 140);
        BLOCK_TO_ID.put("minecraft:carrots", 141);
        BLOCK_TO_ID.put("minecraft:potatoes", 142);
        BLOCK_TO_ID.put("minecraft:wooden_button", 143);
        BLOCK_TO_ID.put("minecraft:skull", 144);
        BLOCK_TO_ID.put("minecraft:anvil", 145);
        BLOCK_TO_ID.put("minecraft:trapped_chest", 146);
        BLOCK_TO_ID.put("minecraft:light_weighted_pressure_plate", 147);
        BLOCK_TO_ID.put("minecraft:heavy_weighted_pressure_plate", 148);
        BLOCK_TO_ID.put("minecraft:unpowered_comparator", 149);
        BLOCK_TO_ID.put("minecraft:powered_comparator", 150);
        BLOCK_TO_ID.put("minecraft:daylight_detector", 151);
        BLOCK_TO_ID.put("minecraft:redstone_block", 152);
        BLOCK_TO_ID.put("minecraft:quartz_ore", 153);
        BLOCK_TO_ID.put("minecraft:hopper", 154);
        BLOCK_TO_ID.put("minecraft:quartz_block", 155);
        BLOCK_TO_ID.put("minecraft:quartz_stairs", 156);
        BLOCK_TO_ID.put("minecraft:activator_rail", 157);
        BLOCK_TO_ID.put("minecraft:dropper", 158);
        BLOCK_TO_ID.put("minecraft:stained_hardened_clay", 159);
        BLOCK_TO_ID.put("minecraft:stained_glass_pane", 160);
        BLOCK_TO_ID.put("minecraft:leaves2", 161);
        BLOCK_TO_ID.put("minecraft:log2", 162);
        BLOCK_TO_ID.put("minecraft:acacia_stairs", 163);
        BLOCK_TO_ID.put("minecraft:dark_oak_stairs", 164);
        BLOCK_TO_ID.put("minecraft:slime", 165);
        BLOCK_TO_ID.put("minecraft:barrier", 166);
        BLOCK_TO_ID.put("minecraft:iron_trapdoor", 167);
        BLOCK_TO_ID.put("minecraft:prismarine", 168);
        BLOCK_TO_ID.put("minecraft:sea_lantern", 169);
        BLOCK_TO_ID.put("minecraft:hay_block", 170);
        BLOCK_TO_ID.put("minecraft:carpet", 171);
        BLOCK_TO_ID.put("minecraft:hardened_clay", 172);
        BLOCK_TO_ID.put("minecraft:coal_block", 173);
        BLOCK_TO_ID.put("minecraft:packed_ice", 174);
        BLOCK_TO_ID.put("minecraft:double_plant", 175);
        BLOCK_TO_ID.put("minecraft:standing_banner", 176);
        BLOCK_TO_ID.put("minecraft:wall_banner", 177);
        BLOCK_TO_ID.put("minecraft:daylight_detector_inverted", 178);
        BLOCK_TO_ID.put("minecraft:red_sandstone", 179);
        BLOCK_TO_ID.put("minecraft:red_sandstone_stairs", 180);
        BLOCK_TO_ID.put("minecraft:double_stone_slab2", 181);
        BLOCK_TO_ID.put("minecraft:stone_slab2", 182);
        BLOCK_TO_ID.put("minecraft:spruce_fence_gate", 183);
        BLOCK_TO_ID.put("minecraft:birch_fence_gate", 184);
        BLOCK_TO_ID.put("minecraft:jungle_fence_gate", 185);
        BLOCK_TO_ID.put("minecraft:dark_oak_fence_gate", 186);
        BLOCK_TO_ID.put("minecraft:acacia_fence_gate", 187);
        BLOCK_TO_ID.put("minecraft:spruce_fence", 188);
        BLOCK_TO_ID.put("minecraft:birch_fence", 189);
        BLOCK_TO_ID.put("minecraft:jungle_fence", 190);
        BLOCK_TO_ID.put("minecraft:dark_oak_fence", 191);
        BLOCK_TO_ID.put("minecraft:acacia_fence", 192);
        BLOCK_TO_ID.put("minecraft:spruce_door", 193);
        BLOCK_TO_ID.put("minecraft:birch_door", 194);
        BLOCK_TO_ID.put("minecraft:jungle_door", 195);
        BLOCK_TO_ID.put("minecraft:acacia_door", 196);
        BLOCK_TO_ID.put("minecraft:dark_oak_door", 197);
        BLOCK_TO_ID.put("minecraft:end_rod", 198);
        BLOCK_TO_ID.put("minecraft:chorus_plant", 199);
        BLOCK_TO_ID.put("minecraft:chorus_flower", 200);
        BLOCK_TO_ID.put("minecraft:purpur_block", 201);
        BLOCK_TO_ID.put("minecraft:purpur_pillar", 202);
        BLOCK_TO_ID.put("minecraft:purpur_stairs", 203);
        BLOCK_TO_ID.put("minecraft:purpur_double_slab", 204);
        BLOCK_TO_ID.put("minecraft:purpur_slab", 205);
        BLOCK_TO_ID.put("minecraft:end_bricks", 206);
        BLOCK_TO_ID.put("minecraft:beetroots", 207);
        BLOCK_TO_ID.put("minecraft:grass_path", 208);
        BLOCK_TO_ID.put("minecraft:end_gateway", 209);
        BLOCK_TO_ID.put("minecraft:repeating_command_block", 210);
        BLOCK_TO_ID.put("minecraft:chain_command_block", 211);
        BLOCK_TO_ID.put("minecraft:frosted_ice", 212);
        BLOCK_TO_ID.put("minecraft:magma", 213);
        BLOCK_TO_ID.put("minecraft:nether_wart_block", 214);
        BLOCK_TO_ID.put("minecraft:red_nether_brick", 215);
        BLOCK_TO_ID.put("minecraft:bone_block", 216);
        BLOCK_TO_ID.put("minecraft:structure_void", 217);
        BLOCK_TO_ID.put("minecraft:observer", 218);
        BLOCK_TO_ID.put("minecraft:white_shulker_box", 219);
        BLOCK_TO_ID.put("minecraft:orange_shulker_box", 220);
        BLOCK_TO_ID.put("minecraft:magenta_shulker_box", 221);
        BLOCK_TO_ID.put("minecraft:light_blue_shulker_box", 222);
        BLOCK_TO_ID.put("minecraft:yellow_shulker_box", 223);
        BLOCK_TO_ID.put("minecraft:lime_shulker_box", 224);
        BLOCK_TO_ID.put("minecraft:pink_shulker_box", 225);
        BLOCK_TO_ID.put("minecraft:gray_shulker_box", 226);
        BLOCK_TO_ID.put("minecraft:silver_shulker_box", 227);
        BLOCK_TO_ID.put("minecraft:cyan_shulker_box", 228);
        BLOCK_TO_ID.put("minecraft:purple_shulker_box", 229);
        BLOCK_TO_ID.put("minecraft:blue_shulker_box", 230);
        BLOCK_TO_ID.put("minecraft:brown_shulker_box", 231);
        BLOCK_TO_ID.put("minecraft:green_shulker_box", 232);
        BLOCK_TO_ID.put("minecraft:red_shulker_box", 233);
        BLOCK_TO_ID.put("minecraft:black_shulker_box", 234);
        BLOCK_TO_ID.put("minecraft:white_glazed_terracotta", 235);
        BLOCK_TO_ID.put("minecraft:orange_glazed_terracotta", 236);
        BLOCK_TO_ID.put("minecraft:magenta_glazed_terracotta", 237);
        BLOCK_TO_ID.put("minecraft:light_blue_glazed_terracotta", 238);
        BLOCK_TO_ID.put("minecraft:yellow_glazed_terracotta", 239);
        BLOCK_TO_ID.put("minecraft:lime_glazed_terracotta", 240);
        BLOCK_TO_ID.put("minecraft:pink_glazed_terracotta", 241);
        BLOCK_TO_ID.put("minecraft:gray_glazed_terracotta", 242);
        BLOCK_TO_ID.put("minecraft:silver_glazed_terracotta", 243);
        BLOCK_TO_ID.put("minecraft:cyan_glazed_terracotta", 244);
        BLOCK_TO_ID.put("minecraft:purple_glazed_terracotta", 245);
        BLOCK_TO_ID.put("minecraft:blue_glazed_terracotta", 246);
        BLOCK_TO_ID.put("minecraft:brown_glazed_terracotta", 247);
        BLOCK_TO_ID.put("minecraft:green_glazed_terracotta", 248);
        BLOCK_TO_ID.put("minecraft:red_glazed_terracotta", 249);
        BLOCK_TO_ID.put("minecraft:black_glazed_terracotta", 250);
        BLOCK_TO_ID.put("minecraft:concrete", 251);
        BLOCK_TO_ID.put("minecraft:concrete_powder", 252);
        BLOCK_TO_ID.put("minecraft:structure_block", 255);
    }
}
