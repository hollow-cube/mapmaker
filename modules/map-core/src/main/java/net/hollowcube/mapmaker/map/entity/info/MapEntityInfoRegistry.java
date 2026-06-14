package net.hollowcube.mapmaker.map.entity.info;

import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.*;
import net.hollowcube.mapmaker.map.entity.impl.animal.aquatic.*;
import net.hollowcube.mapmaker.map.entity.impl.animal.equine.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.ShulkerEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.golem.CopperGolemEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.golem.IronGolemEntity;
import net.hollowcube.mapmaker.map.entity.impl.animal.golem.SnowGolemEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.EndermiteEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.SilverfishEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.illager.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.nether.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.skeleton.*;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.CaveSpiderEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.arthropod.SpiderEntity;
import net.hollowcube.mapmaker.map.entity.impl.hostile.zombie.*;
import net.hollowcube.mapmaker.map.entity.impl.other.ArmorStandEntity;
import net.hollowcube.mapmaker.map.entity.impl.other.EndCrystalEntity;
import net.hollowcube.mapmaker.map.entity.impl.villager.VillagerEntity;
import net.hollowcube.mapmaker.map.entity.impl.villager.WanderingTraderEntity;
import net.hollowcube.mapmaker.map.entity.impl.villager.ZombieVillagerEntity;
import net.minestom.server.entity.metadata.EntityMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapEntityInfoRegistry {

    private static final Map<Class<?>, MapEntityInfo<?>> REGISTRY = new ConcurrentHashMap<>();
    static {
        // Animals
        register(AllayEntity.class, AllayEntity.INFO);
        register(ArmadilloEntity.class, ArmadilloEntity.INFO);
        register(AxolotlEntity.class, AxolotlEntity.INFO);
        register(BeeEntity.class, BeeEntity.INFO);
        register(CamelEntity.class, CamelEntity.INFO);
        register(CamelHuskEntity.class, CamelHuskEntity.INFO);
        register(ChickenEntity.class, ChickenEntity.INFO);
        register(CowEntity.class, CowEntity.INFO);
        register(MooshroomCowEntity.class, MooshroomCowEntity.INFO);
        register(DolphinEntity.class, DolphinEntity.INFO);
        register(DonkeyEntity.class, DonkeyEntity.INFO);
        register(HorseEntity.class, HorseEntity.INFO);
        register(LlamaEntity.class, LlamaEntity.INFO);
        register(MuleEntity.class, MuleEntity.INFO);
        register(SkeletonHorseEntity.class, SkeletonHorseEntity.INFO);
        register(ZombieHorseEntity.class, ZombieHorseEntity.INFO);
        register(TraderLlamaEntity.class, TraderLlamaEntity.INFO);
        register(CatEntity.class, CatEntity.INFO);
        register(CodEntity.class, CodEntity.INFO);
        register(PufferfishEntity.class, PufferfishEntity.INFO);
        register(SalmonEntity.class, SalmonEntity.INFO);
        register(TropicalFishEntity.class, TropicalFishEntity.INFO);
        register(FoxEntity.class, FoxEntity.INFO);
        register(FrogEntity.class, FrogEntity.INFO);
        register(TadpoleEntity.class, TadpoleEntity.INFO);
        register(GoatEntity.class, GoatEntity.INFO);
        register(CopperGolemEntity.class, CopperGolemEntity.INFO);
        register(IronGolemEntity.class, IronGolemEntity.INFO);
        register(SnowGolemEntity.class, SnowGolemEntity.INFO);
        register(HappyGhastEntity.class, HappyGhastEntity.INFO);
        register(NautilusEntity.class, NautilusEntity.INFO);
        register(ZombieNautilusEntity.class, ZombieNautilusEntity.INFO);
        register(PandaEntity.class, PandaEntity.INFO);
        register(ParrotEntity.class, ParrotEntity.INFO);
        register(PigEntity.class, PigEntity.INFO);
        register(PolarBearEntity.class, PolarBearEntity.INFO);
        register(RabbitEntity.class, RabbitEntity.INFO);
        register(SheepEntity.class, SheepEntity.INFO);
        register(SnifferEntity.class, SnifferEntity.INFO);
        register(SquidEntity.class, SquidEntity.INFO);
        register(GlowSquidEntity.class, GlowSquidEntity.INFO);
        register(TurtleEntity.class, TurtleEntity.INFO);
        register(BatEntity.class, BatEntity.INFO);
        register(WolfEntity.class, WolfEntity.INFO);
        register(StriderEntity.class, StriderEntity.INFO);
        register(OcelotEntity.class, OcelotEntity.INFO);

        register(ShulkerEntity.class, ShulkerEntity.INFO);
        register(ZombieEntity.class, ZombieEntity.INFO);
        register(ZombieVillagerEntity.class, ZombieVillagerEntity.INFO);
        register(ZombifiedPiglinEntity.class, ZombifiedPiglinEntity.INFO);
        register(DrownedEntity.class, DrownedEntity.INFO);
        register(HuskEntity.class, HuskEntity.INFO);
        register(BoggedEntity.class, BoggedEntity.INFO);
        register(ParchedEntity.class, ParchedEntity.INFO);
        register(StrayEntity.class, StrayEntity.INFO);
        register(SkeletonEntity.class, SkeletonEntity.INFO);
        register(WitherSkeletonEntity.class, WitherSkeletonEntity.INFO);
        register(SpiderEntity.class, SpiderEntity.INFO);
        register(CaveSpiderEntity.class, CaveSpiderEntity.INFO);
        register(BreezeEntity.class, BreezeEntity.INFO);
        register(CreakingEntity.class, CreakingEntity.INFO);
        register(CreeperEntity.class, CreeperEntity.INFO);
        register(GuardianEntity.class, GuardianEntity.INFO);
        register(ElderGuardianEntity.class, ElderGuardianEntity.INFO);
        register(PhantomEntity.class, PhantomEntity.INFO);
        register(SilverfishEntity.class, SilverfishEntity.INFO);
        register(EndermiteEntity.class, EndermiteEntity.INFO);
        register(SlimeEntity.class, SlimeEntity.INFO);
        register(MagmaCubeEntity.class, MagmaCubeEntity.INFO);
        register(PillagerEntity.class, PillagerEntity.INFO);
        register(VindicatorEntity.class, VindicatorEntity.INFO);
        register(EvokerEntity.class, EvokerEntity.INFO);
        register(IllusionerEntity.class, IllusionerEntity.INFO);
        register(RavagerEntity.class, RavagerEntity.INFO);
        register(VexEntity.class, VexEntity.INFO);
        register(WitchEntity.class, WitchEntity.INFO);
        register(BlazeEntity.class, BlazeEntity.INFO);
        register(PiglinEntity.class, PiglinEntity.INFO);
        register(PiglinBruteEntity.class, PiglinBruteEntity.INFO);
        register(EndermanEntity.class, EndermanEntity.INFO);
        register(WardenEntity.class, WardenEntity.INFO);
        register(ZoglinEntity.class, ZoglinEntity.INFO);
        register(HoglinEntity.class, HoglinEntity.INFO);
        register(GhastEntity.class, GhastEntity.INFO);
        register(VillagerEntity.class, VillagerEntity.INFO);
        register(WanderingTraderEntity.class, WanderingTraderEntity.INFO);

        register(ArmorStandEntity.class, ArmorStandEntity.INFO);
        register(EndCrystalEntity.class, EndCrystalEntity.INFO);
    }

    private static <M extends EntityMeta, E extends MapEntity<? extends M>> void register(Class<E> clazz, MapEntityInfo<E> info) {
        REGISTRY.put(clazz, info);
    }

    public static <E extends MapEntity<?>> @Nullable MapEntityInfo<E> get(E entity) {
        //noinspection unchecked
        return (MapEntityInfo<E>) REGISTRY.get(entity.getClass());
    }
}
