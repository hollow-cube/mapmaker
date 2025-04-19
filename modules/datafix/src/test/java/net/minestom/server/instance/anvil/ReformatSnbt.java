package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

public class ReformatSnbt {
    public static void main(String[] args) throws Exception {
        var snbt = """
                
                {Motion:[0.0d,0.0d,0.0d],UUIDLeast:-4968301346988093425L,Invulnerable:0b,Air:300s,OnGround:1b,Dimension:0,PortalCooldown:0,Rotation:[90.0f,0.0f],FallDistance:0.0f,UUIDMost:-1251681295587983044L,CustomName:"MINECART WITH DA CHEST",Pos:[54.11500000953674d,68.0d,55.88499999046326d],Fire:-1s,Items:[{Slot:3b,id:"minecraft:speckled_melon",Count:1b,Damage:0s},{Slot:5b,id:"minecraft:bookshelf",Count:1b,Damage:0s},{Slot:10b,id:"minecraft:lead",Count:1b,Damage:0s},{Slot:11b,id:"minecraft:diamond_helmet",Count:1b,tag:{ench:[{lvl:1s,id:4s},{lvl:1s,id:6s}]},Damage:0s},{Slot:12b,id:"minecraft:spawn_egg",Count:2b,Damage:120s},{Slot:13b,id:"minecraft:cobblestone",Count:1b,tag:{display:{Lore:["Blah Blah Blah"]}},Damage:0s},{Slot:14b,id:"minecraft:cooked_fish",Count:1b,Damage:0s},{Slot:15b,id:"minecraft:potion",Count:1b,Damage:8265s},{Slot:16b,id:"minecraft:potion",Count:1b,Damage:16450s},{Slot:21b,id:"minecraft:banner",Count:1b,tag:{RepairCost:1,BlockEntityTag:{Patterns:[{Pattern:"ts",Color:1},{Pattern:"bs",Color:1}],Base:15},display:{Name:"DaBanner"}},Damage:15s},{Slot:23b,id:"minecraft:sapling",Count:1b,Damage:2s}],id:"MinecartChest",CustomNameVisible:0b}
                
                
                
                
                
                
                
                
                
                """;

        System.out.println(TagStringIOExt.writeTag(TagStringIOExt.readTag(snbt), "    "));
    }
}
