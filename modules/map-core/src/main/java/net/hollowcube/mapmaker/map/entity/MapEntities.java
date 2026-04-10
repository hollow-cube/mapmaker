package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.*;
import net.hollowcube.mapmaker.map.entity.impl.animal.aquatic.*;
import net.hollowcube.mapmaker.map.entity.impl.animal.equine.*;
import net.hollowcube.mapmaker.map.entity.impl.animal.golem.CopperGolemEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.golem.IronGolemEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.golem.SnowGolemEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.CaveSpiderEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.EndermiteEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.SilverfishEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.SpiderEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.illager.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.nether.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.zombie.DrownedEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.zombie.HuskEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.zombie.ZombieEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.zombie.ZombifiedPiglinEntity;
import net.hollowcube.mapmaker.map.entity.impl.other.*;
import net.hollowcube.mapmaker.map.entity.impl.villager.VillagerEntity;
import net.hollowcube.mapmaker.map.entity.impl.villager.WanderingTraderEntity;
import net.hollowcube.mapmaker.map.entity.impl.villager.ZombieVillagerEntity;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEditorScreen;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public final class MapEntities {

    public static @NotNull Argument<@NotNull EntityType> Argument(@NotNull String id) {
        return Argument.Word(id).map(
            /* mapper */ (sender, raw) -> {
                if (!(sender instanceof Player player)) return new ParseResult.Failure<>(-1, "Only players can use this argument");
                if (MapWorld.forPlayer(player) == null) return new ParseResult.Failure<>(-1, "Not in a map world");

                var foundAny = false;
                for (var type : EntityType.values()) {
                    if (!MapEntityType.hasOverride(type)) continue;

                    if (type.key().asString().equals(raw)) {
                        return new ParseResult.Success<>(type);
                    } else if (type.key().asString().startsWith(raw) || type.key().value().startsWith(raw)) {
                        foundAny = true;
                    }
                }
                return foundAny ? new ParseResult.Partial<>() : new ParseResult.Failure<>(-1, "No entity type found with that id");
            },
            /* suggester */ (sender, raw, suggestion) -> {
                if (!(sender instanceof Player player)) return;
                var world = MapWorld.forPlayer(player);
                if (world == null) return;

                for (var type : EntityType.values()) {
                    if (!MapEntityType.hasOverride(type)) continue;

                    if (type.key().asString().startsWith(raw) || type.key().value().startsWith(raw)) {
                        suggestion.add(type.key().asString());
                    }
                }
            }
        );
    }

    public static void init(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addListener(PlayerEntityInteractEvent.class, MapEntities::handleEntityInteract);
        eventNode.addListener(EntityAttackEvent.class, MapEntities::handleEntityAttack);

        MapEntityType.override(EntityType.ITEM_FRAME, ItemFrameEntity::new);
        MapEntityType.override(EntityType.GLOW_ITEM_FRAME, ItemFrameEntity.Glowing::new);
        MapEntityType.override(EntityType.PAINTING, PaintingEntity::new);
        MapEntityType.override(EntityType.ARMOR_STAND, ArmorStandEntity::new);
        MapEntityType.override(EntityType.END_CRYSTAL, EndCrystalEntity::new);
        MapEntityType.override(EntityType.LEASH_KNOT, LeashKnotEntity::new);

        // Animals
        MapEntityType.override(EntityType.ALLAY, AllayEntity::new);
        MapEntityType.override(EntityType.ARMADILLO, ArmadilloEntity::new);
        MapEntityType.override(EntityType.AXOLOTL, AxolotlEntity::new);
        MapEntityType.override(EntityType.BEE, BeeEntity::new);
        MapEntityType.override(EntityType.CAMEL, CamelEntity::new);
        MapEntityType.override(EntityType.CAMEL_HUSK, CamelHuskEntity::new);
        MapEntityType.override(EntityType.CHICKEN, ChickenEntity::new);
        MapEntityType.override(EntityType.COW, CowEntity::new);
        MapEntityType.override(EntityType.MOOSHROOM, MooshroomCowEntity::new);
        MapEntityType.override(EntityType.DOLPHIN, DolphinEntity::new);
        MapEntityType.override(EntityType.DONKEY, DonkeyEntity::new);
        MapEntityType.override(EntityType.HORSE, HorseEntity::new);
        MapEntityType.override(EntityType.LLAMA, LlamaEntity::new);
        MapEntityType.override(EntityType.MULE, MuleEntity::new);
        MapEntityType.override(EntityType.SKELETON_HORSE, SkeletonHorseEntity::new);
        MapEntityType.override(EntityType.ZOMBIE_HORSE, ZombieHorseEntity::new);
        MapEntityType.override(EntityType.TRADER_LLAMA, TraderLlamaEntity::new);
        MapEntityType.override(EntityType.CAT, CatEntity::new);
        MapEntityType.override(EntityType.COD, CodEntity::new);
        MapEntityType.override(EntityType.PUFFERFISH, PufferfishEntity::new);
        MapEntityType.override(EntityType.SALMON, SalmonEntity::new);
        MapEntityType.override(EntityType.TROPICAL_FISH, TropicalFishEntity::new);
        MapEntityType.override(EntityType.FROG, FrogEntity::new);
        MapEntityType.override(EntityType.TADPOLE, TadpoleEntity::new);
        MapEntityType.override(EntityType.GOAT, GoatEntity::new);
        MapEntityType.override(EntityType.COPPER_GOLEM, CopperGolemEntity::new);
        MapEntityType.override(EntityType.IRON_GOLEM, IronGolemEntity::new);
        MapEntityType.override(EntityType.SNOW_GOLEM, SnowGolemEntity::new);
        MapEntityType.override(EntityType.HAPPY_GHAST, HappyGhastEntity::new);
        MapEntityType.override(EntityType.NAUTILUS, NautilusEntity::new);
        MapEntityType.override(EntityType.ZOMBIE_NAUTILUS, ZombieNautilusEntity::new);
        MapEntityType.override(EntityType.PANDA, PandaEntity::new);
        MapEntityType.override(EntityType.PARROT, ParrotEntity::new);
        MapEntityType.override(EntityType.PIG, PigEntity::new);
        MapEntityType.override(EntityType.POLAR_BEAR, PolarBearEntity::new);
        MapEntityType.override(EntityType.RABBIT, RabbitEntity::new);
        MapEntityType.override(EntityType.SHEEP, SheepEntity::new);
        MapEntityType.override(EntityType.SNIFFER, SnifferEntity::new);
        MapEntityType.override(EntityType.SQUID, SquidEntity::new);
        MapEntityType.override(EntityType.GLOW_SQUID, GlowSquidEntity::new);
        MapEntityType.override(EntityType.TURTLE, TurtleEntity::new);
        MapEntityType.override(EntityType.BAT, BatEntity::new);
        MapEntityType.override(EntityType.FOX, FoxEntity::new);
        MapEntityType.override(EntityType.WOLF, WolfEntity::new);
        MapEntityType.override(EntityType.STRIDER, StriderEntity::new);
        MapEntityType.override(EntityType.OCELOT, OcelotEntity::new);

        // Hostile
        MapEntityType.override(EntityType.SHULKER, ShulkerEntity::new);
        MapEntityType.override(EntityType.ZOMBIE, ZombieEntity::new);
        MapEntityType.override(EntityType.ZOMBIE_VILLAGER, ZombieVillagerEntity::new);
        MapEntityType.override(EntityType.HUSK, HuskEntity::new);
        MapEntityType.override(EntityType.ZOMBIFIED_PIGLIN, ZombifiedPiglinEntity::new);
        MapEntityType.override(EntityType.DROWNED, DrownedEntity::new);
        MapEntityType.override(EntityType.BOGGED, BoggedEntity::new);
        MapEntityType.override(EntityType.PARCHED, ParchedEntity::new);
        MapEntityType.override(EntityType.STRAY, StrayEntity::new);
        MapEntityType.override(EntityType.SKELETON, SkeletonEntity::new);
        MapEntityType.override(EntityType.WITHER_SKELETON, WitherSkeletonEntity::new);
        MapEntityType.override(EntityType.SPIDER, SpiderEntity::new);
        MapEntityType.override(EntityType.CAVE_SPIDER, CaveSpiderEntity::new);
        MapEntityType.override(EntityType.BREEZE, BreezeEntity::new);
        MapEntityType.override(EntityType.CREAKING, CreakingEntity::new);
        MapEntityType.override(EntityType.CREEPER, CreeperEntity::new);
        MapEntityType.override(EntityType.GUARDIAN, GuardianEntity::new);
        MapEntityType.override(EntityType.ELDER_GUARDIAN, ElderGuardianEntity::new);
        MapEntityType.override(EntityType.PHANTOM, PhantomEntity::new);
        MapEntityType.override(EntityType.SILVERFISH, SilverfishEntity::new);
        MapEntityType.override(EntityType.ENDERMITE, EndermiteEntity::new);
        MapEntityType.override(EntityType.SLIME, SlimeEntity::new);
        MapEntityType.override(EntityType.MAGMA_CUBE, MagmaCubeEntity::new);
        MapEntityType.override(EntityType.PILLAGER, PillagerEntity::new);
        MapEntityType.override(EntityType.VINDICATOR, VindicatorEntity::new);
        MapEntityType.override(EntityType.EVOKER, EvokerEntity::new);
        MapEntityType.override(EntityType.ILLUSIONER, IllusionerEntity::new);
        MapEntityType.override(EntityType.RAVAGER, RavagerEntity::new);
        MapEntityType.override(EntityType.VEX, VexEntity::new);
        MapEntityType.override(EntityType.WITCH, WitchEntity::new);
        MapEntityType.override(EntityType.BLAZE, BlazeEntity::new);
        MapEntityType.override(EntityType.PIGLIN, PiglinEntity::new);
        MapEntityType.override(EntityType.PIGLIN_BRUTE, PiglinBruteEntity::new);
        MapEntityType.override(EntityType.ENDERMAN, EndermanEntity::new);
        MapEntityType.override(EntityType.WARDEN, WardenEntity::new);
        MapEntityType.override(EntityType.ZOGLIN, ZoglinEntity::new);
        MapEntityType.override(EntityType.HOGLIN, HoglinEntity::new);
        MapEntityType.override(EntityType.GHAST, GhastEntity::new);
        MapEntityType.override(EntityType.VILLAGER, VillagerEntity::new);
        MapEntityType.override(EntityType.WANDERING_TRADER, WanderingTraderEntity::new);

        MapEntityType.override(EntityType.BLOCK_DISPLAY, DisplayEntity.Block::new);
        MapEntityType.override(EntityType.ITEM_DISPLAY, DisplayEntity.Item::new);
        MapEntityType.override(EntityType.TEXT_DISPLAY, DisplayEntity.Text::new);

        MapEntityType.override(EntityType.MARKER, MarkerEntity::new);
        MapEntityType.override(EntityType.INTERACTION, InteractionEntity::new);

        // Events
        eventNode.addListener(PlayerCustomClickEvent.class, InteractionEditorScreen::onCallback);
    }

    // TODO split editing logic
    private static void handleEntityInteract(@NotNull PlayerEntityInteractEvent event) {
        if (!(event.getTarget() instanceof MapEntity<?> mapEntity)) return;

        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        if (world == null || !world.players().contains(player)) return;

        mapEntity.onRightClick(world, player, event.getHand(), event.getInteractPosition());
    }

    // TODO split editing logic
    private static void handleEntityAttack(@NotNull EntityAttackEvent event) {
        if (!(event.getTarget() instanceof MapEntity<?> mapEntity)) return;

        if (!(event.getEntity() instanceof Player player)) return;
        var world = MapWorld.forPlayer(player);
        if (world == null || !world.players().contains(player)) return;
        if (ItemRegistry.isUsingItem(player)) return;

        mapEntity.onLeftClick(world, player);
    }
}
