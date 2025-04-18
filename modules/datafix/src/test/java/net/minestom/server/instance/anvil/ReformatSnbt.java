package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

public class ReformatSnbt {
    public static void main(String[] args) throws Exception {
        var snbt = """
                
                {AbsorptionAmount:0.0f,Age:0,Air:300s,Attributes:[{Base:4.0d,Name:"generic.maxHealth"},{Base:0.0d,Name:"generic.knockbackResistance"},{Base:0.25d,Name:"generic.movementSpeed"},{Base:16.0d,Modifiers:[{Amount:-0.016318803203646825d,Name:"Random spawn bonus",Operation:1,UUIDLeast:-7880791242789364064L,UUIDMost:3809084956027863735L}],Name:"generic.followRange"}],CanPickUpLoot:0b,DeathTime:0s,Dimension:0,DropChances:[0.085f,0.085f,0.085f,0.085f,0.085f],EggLayTime:3666,Equipment:[{},{},{},{},{}],FallDistance:0.0f,Fire:-1s,ForcedAge:0,HealF:4.0f,Health:4.0f,HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,IsChickenJockey:0b,Leashed:0b,Motion:[0.0d,-0.0784000015258789d,0.0d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[1.6466738390586868d,69.0d,106.59090124169435d],Rotation:[120.82585f,0.0f],UUID:[I;558221653,1914389031,-1966457728,-1680752328],UUIDLeast:-8445871628112248520L,UUIDMost:2397543745468449319L,attributes:[{base:4.0d,id:"minecraft:max_health"},{base:0.0d,id:"minecraft:knockback_resistance"},{base:0.25d,id:"minecraft:movement_speed"},{base:16.0d,id:"minecraft:follow_range",modifiers:[{amount:-0.016318803203646825d,id:"minecraft:random_spawn_bonus",operation:"add_multiplied_base"}]}],fall_distance:0.0d,id:"minecraft:chicken"}
                """;

        System.out.println(TagStringIOExt.writeTag(TagStringIOExt.readTag(snbt), "    "));
    }
}
