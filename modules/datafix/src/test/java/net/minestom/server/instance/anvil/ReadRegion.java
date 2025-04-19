package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

import java.nio.file.Path;

public class ReadRegion {

    /*

    ENT: {Brain:{memories:{"minecraft:home":{value:{pos:[I;49,-60,49],dimension:"minecraft:overworld"}},"minecraft:last_slept":{value:9839L},"minecraft:last_woken":{value:9739L},"minecraft:job_site":{value:{pos:[I;49,-60,50],dimension:"minecraft:overworld"}}}},HurtByTimestamp:0,FoodLevel:0b,Invulnerable:0b,FallFlying:0b,ForcedAge:0,Gossips:[],PortalCooldown:0,AbsorptionAmount:0.0f,LastRestock:0L,FallDistance:0.0f,SleepingX:49,DeathTime:0s,SleepingZ:49,SleepingY:-60,Xp:0,LastGossipDecay:3722L,HandDropChances:[0.085f,0.085f],PersistenceRequired:0b,id:"minecraft:villager",UUID:[I;1915514874,215696102,-1397392397,1902646503],Age:0,Motion:[0.0d,-0.04760748098837379d,0.0d],Health:20.0f,LeftHanded:0b,Air:300s,OnGround:1b,Offers:{Recipes:[{maxUses:12,buyB:{count:1,id:"minecraft:book"},sell:{components:{"minecraft:stored_enchantments":{levels:{"minecraft:binding_curse":1}}},count:1,id:"minecraft:enchanted_book"},buy:{count:22,id:"minecraft:emerald"},priceMultiplier:0.2f},{maxUses:12,sell:{count:1,id:"minecraft:bookshelf"},buy:{count:9,id:"minecraft:emerald"},priceMultiplier:0.05f}]},Rotation:[105.704254f,0.0f],HandItems:[{},{}],RestocksToday:0,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],Pos:[49.5d,-59.4375d,49.5d],Fire:-1s,ArmorItems:[{},{},{},{}],CanPickUpLoot:1b,VillagerData:{profession:"minecraft:librarian",level:1,type:"minecraft:plains"},attributes:[{id:"minecraft:movement_speed",base:0.5d},{id:"minecraft:follow_range",modifiers:[{amount:-0.03267670794824462d,id:"minecraft:random_spawn_bonus",operation:"add_multiplied_base"}],base:16.0d}],HurtTime:0s,Inventory:[]}
    ENT: {Brain:{memories:{"minecraft:has_hunting_cooldown":{value:1b,ttl:61L}}},HurtByTimestamp:0,Invulnerable:0b,FallFlying:0b,ForcedAge:0,PortalCooldown:0,AbsorptionAmount:0.0f,FallDistance:0.0f,InLove:0,DeathTime:0s,HandDropChances:[0.085f,0.085f],PersistenceRequired:0b,id:"minecraft:axolotl",UUID:[I;-1408725217,136071867,-1727282273,1468454487],Age:0,Motion:[0.0d,0.0d,0.0d],FromBucket:0b,Health:14.0f,LeftHanded:0b,Air:6000s,OnGround:0b,Rotation:[135.00002f,0.0f],HandItems:[{},{}],Variant:4,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],Pos:[61.0d,-60.0d,61.0d],Fire:-1s,ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,attributes:[{id:"minecraft:movement_speed",base:1.0d}],HurtTime:0s}
    ENT: {Brain:{memories:{}},HurtByTimestamp:0,Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,type:"medium",FallDistance:0.0f,DeathTime:0s,HandDropChances:[0.085f,0.085f],PersistenceRequired:0b,id:"minecraft:salmon",UUID:[I;333418892,1157713861,-1314221221,-1356584791],Motion:[-0.02136451114874177d,-0.005d,-7.472663730343813E-5d],FromBucket:1b,Health:3.0f,LeftHanded:0b,Air:300s,OnGround:0b,Rotation:[90.904335f,0.0f],HandItems:[{},{}],ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],Pos:[62.60285200126078d,-60.0d,59.517699685501924d],Fire:-1s,ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,attributes:[{id:"minecraft:movement_speed",base:0.7d},{id:"minecraft:follow_range",modifiers:[{amount:-0.0724048673012876d,id:"minecraft:random_spawn_bonus",operation:"add_multiplied_base"}],base:16.0d}],HurtTime:0s}
    ENT: {Brain:{memories:{}},HurtByTimestamp:0,Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,type:"medium",FallDistance:0.0f,DeathTime:0s,HandDropChances:[0.085f,0.085f],PersistenceRequired:0b,id:"minecraft:salmon",UUID:[I;-903377376,-579646158,-1402865257,-207567411],Motion:[-0.02281809684186626d,-0.005d,0.0d],FromBucket:1b,Health:3.0f,LeftHanded:1b,Air:300s,OnGround:1b,Rotation:[123.01819f,0.0f],HandItems:[{},{}],ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],Pos:[62.205571942758155d,-60.0d,57.974999994039536d],Fire:-1s,ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,attributes:[{id:"minecraft:movement_speed",base:0.7d},{id:"minecraft:follow_range",modifiers:[{amount:0.013402806808172251d,id:"minecraft:random_spawn_bonus",operation:"add_multiplied_base"}],base:16.0d}],HurtTime:0s}
    ENT: {Brain:{memories:{}},HurtByTimestamp:0,Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,type:"large",FallDistance:0.0f,DeathTime:0s,HandDropChances:[0.085f,0.085f],PersistenceRequired:0b,id:"minecraft:salmon",UUID:[I;-211641872,-348633674,-2111214768,650036848],Motion:[0.0d,-0.005d,0.0d],FromBucket:0b,Health:3.0f,LeftHanded:0b,Air:300s,OnGround:0b,Rotation:[135.0f,0.0f],HandItems:[{},{}],ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],Pos:[61.14999997615814d,-60.0d,58.14999997615814d],Fire:-1s,ArmorItems:[{},{},{},{}],CanPickUpLoot:0b,attributes:[{id:"minecraft:movement_speed",base:0.7d}],HurtTime:0s}
    ENT: {Motion:[-4.192105307722989E-137d,-0.04d,0.0d],Health:5s,Invulnerable:0b,Air:300s,OnGround:1b,PortalCooldown:0,Rotation:[16.282919f,0.0f],FallDistance:0.0f,Item:{count:1,id:"minecraft:salmon"},Pos:[61.17551254587057d,-60.0d,63.25d],PickupDelay:0s,Fire:-1s,id:"minecraft:item",UUID:[I;2051745361,610026185,-1878350734,-1526964928],Age:2340s}
    ENT: {Motion:[0.0d,0.0d,0.0d],Facing:1b,ItemRotation:0b,Invulnerable:0b,Air:300s,OnGround:0b,PortalCooldown:0,Rotation:[0.0f,-90.0f],FallDistance:0.0f,Item:{components:{"minecraft:dyed_color":{show_in_tooltip:0b,rgb:16711680}},count:1,id:"minecraft:leather_horse_armor"},ItemDropChance:1.0f,Pos:[55.5d,-59.96875d,57.5d],Fire:-1s,TileY:-60,id:"minecraft:item_frame",TileX:55,Invisible:0b,UUID:[I;1125220582,1325353446,-1687759354,-435992984],TileZ:57,Fixed:0b}
    ENT: {Motion:[0.0d,0.0d,0.0d],ReapplicationDelay:20,Radius:0.70999575f,Owner:[I;-1393872273,-636140100,-1087434348,203520012],DurationOnUse:0,Invulnerable:0b,Duration:600,Air:300s,OnGround:0b,PortalCooldown:0,Rotation:[0.0f,0.0f],RadiusPerTick:-0.005f,Particle:{color:-7502362,type:"minecraft:entity_effect"},FallDistance:0.0f,Pos:[52.31063832850161d,-60.0d,56.20487043993389d],Fire:-1s,potion_contents:{potion:"minecraft:strong_turtle_master"},RadiusOnUse:-0.5f,id:"minecraft:area_effect_cloud",WaitTime:10,UUID:[I;-179392093,-2104344454,-1392635965,-354599359],Age:267}
    ENT: {Motion:[-0.1977597803760322d,-0.3110916445620715d,0.18142963237468684d],Owner:[I;-1393872273,-636140100,-1087434348,203520012],Invulnerable:0b,LeftOwner:1b,Air:300s,OnGround:0b,PortalCooldown:0,Rotation:[-47.465977f,-22.092405f],FallDistance:0.0f,Item:{components:{"minecraft:potion_contents":{potion:"minecraft:strong_turtle_master"}},count:1,id:"minecraft:lingering_potion"},Pos:[54.53699294255313d,-58.394067255278706d,58.11802430337431d],HasBeenShot:1b,Fire:-1s,id:"minecraft:potion",UUID:[I;575666707,21708835,-1567342342,45036066]}
    ENT: {Motion:[-0.2099584939021249d,0.08313474658189234d,0.2006757471512613d],Owner:[I;-1393872273,-636140100,-1087434348,203520012],Invulnerable:0b,Air:300s,OnGround:0b,PortalCooldown:0,Rotation:[-46.29496f,35.55391f],FallDistance:0.0f,Item:{components:{"minecraft:potion_contents":{potion:"minecraft:leaping"}},count:1,id:"minecraft:splash_potion"},Pos:[56.20296715874919d,-57.208246587409256d,56.63918432004599d],HasBeenShot:1b,Fire:-1s,id:"minecraft:potion",UUID:[I;1168513632,763969789,-1502602729,305362085]}

     */

    public static void main(String[] args) throws Exception {
        var path = "/Users/matt/Library/Application Support/mc-cli/profiles/1.21.3-fabric/saves/New World (2)/entities/r.0.0.mca";
        var rf = new RegionFile(Path.of(path));
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                if (x != 3 || z != 3) continue;
                var chunk = rf.readChunkData(x, z);
                if (chunk == null) continue;
//                System.out.println(level.keySet());
                var entities = chunk.getList("Entities");
                for (var entity : entities) {
                    System.out.println("ENT: " + TagStringIOExt.writeTag(entity));
                }
//                var tileEntities = chunk.getList("block_entities");
//                for (var tileEntity : tileEntities) {
//                    System.out.println("TILE: " + TagStringIOExt.writeTag(tileEntity));
//                }

            }
        }
    }
}
