package net.hollowcube.mapmaker.util;

import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CoreSkulls {

    public static final ResolvableProfile UNKNOWN_PLAYER = create(
            "ewogICJ0aW1lc3RhbXAiIDogMTYxNjI1NTMwMDk3NSwKICAicHJvZmlsZUlkIiA6ICIwNzdlYzVhN2ZlYTc0ZDc2OGI3ZTE1OGRhYmE4NGFlNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTaGFkb3dBc3BlY3QiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGE5OWIwNWI5YTFkYjRkMjliNWU2NzNkNzdhZTU0YTc3ZWFiNjY4MTg1ODYwMzVjOGEyMDA1YWViODEwNjAyYSIKICAgIH0KICB9Cn0=",
            "U/PRalQOlAAbPgfNMREb2OhBdmXI80tCcjf1vb2vwtokI8FPAUM/p8ZdCJUO2srlJbhEOQasrDj3zlHUWzxm19o8Rn/OOqYy15m6fnJnOxCzke02EA0QhDn3XO0PmDBZBgNCRHv695vyzRPkJhFDHr1tSn+Lu1ongH3vYOb90KDFFhELredBXxKl/T0gtYOmVAM2JSLKlhnSmfu5inKen+rNHADO+tyzftHgOfEpSXhIuJnEsPyBT4J60VlEXGdj4X4HKWKvyKNpUQZ94356bMv11DniiTCK1h0ZeMuO8HEZH8MD2ixzQ3s3JR7YnCQxFnYTezWtZa7TzrHMa1uu4fN+7konGpW7kyfuN5w3AAyUJhcZGhMAwDuDEmeGVlhxcrinve6M/XtbzWsj1B0xf4A3eJbZ6jQeK9NRRjwexMoRiIB5zatSn2p4rGzKLgN9CgWr3Of5co/H+ryPUUgshAvvGvXZILvlNu56JJTa+WZy8Z25XhA/nkPtthJDw30H4ddZtHy20V1V+u306OzE8qjdye0bEctkEiqxFYxswFVW0YhZOuNxZnsLVOYz9PcFqbXpXVkwgGzo0QBEPN0Ua3qZvO6fsCRsBJGZO+MApLidPCDyth55GKQF09TCJ9Rkm1ClDQlN7eUwNIwbrs6TL05jsmXotl0yWa3d4/C3uxs="
    );

    public static final ResolvableProfile ROBLOX_NOOB = create(
            "ewogICJ0aW1lc3RhbXAiIDogMTY0NjU3MDc2NzMxMCwKICAicHJvZmlsZUlkIiA6ICI0NDAzZGM1NDc1YmM0YjE1YTU0OGNmZGE2YjBlYjdkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJDaGFvc0NvbXB1dHJDbHViIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2RhMjg0ZDk3ODA1YmU1MzFjYWRmMDYyN2M1YzI5ZTkwMzVlMTcxMjE1ODFkY2FiY2Y5NTEwZmZkOWQ0NjA3ZmYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
            "e0xDse9uoT4+lolWZh56K6oHGqU6hBBKRdVRKI31htNKTS1mUHIUPYsRdLy9LogLC0oc+NPnrZeXPvjioIZ06026TqSG6eP2yxwFJA18XFRWC9Syk5bDUVbdfqtxg27/0KXp+a76vK+hzTa7ezygVHMv2pu6Ujt//bO5u4+vX97R5uu92iXNbUJBn29ioVvdWncS9XPkGMpjElXZDdXgcYHqxggpMRFCJj497eqNOs6KHcTm+peUhXS3Z4mNxIQFl6f9kKRR+5nY5FLFzSj3UCOAohhyKH6kDq2fjoMykF6g0+VNpetFH8KgbYALyKzV9Fl+fyqyP6rUQ/Vm3kdeoSiy51h5Suqvc3hn5ZOyf5BdPwcyir7pBEMhkyyi9EJxZhggoFo0DDj9aTovtlBmXd7L9KuQ1auheuWChp1NDRGNrqeWbPgoXH11ThL6iGtK55dijtFqgxG+YiTU3y6mcD2G47CL6nk/t2MI+VRnKJgbjoUp9oo4DlE5CXKC3FEyDttsab9ohYZTM4c1SJ/54WneBSkJivbcUKLrS6S27SSaK3DHxVb424JNcbegGaRtm6Av66lBCG72EVyWslrmNmi0WJ+K97dNNIFVnf92MqqPLqcckXWRlbeXlqLJqxv1pvSJEA/g9KYSiGwHvQhPgZ4KTPYOgIEIUbk/8ilk4p0="
    );

    public static ResolvableProfile create(@NotNull PlayerSkin skin) {
        return create(skin.textures(), skin.signature());
    }

    private static ResolvableProfile create(@NotNull String texture, @NotNull String signature) {
        return new ResolvableProfile(new ResolvableProfile.Partial(
                null,
                null,
                List.of(
                        new GameProfile.Property(
                                "textures",
                                texture,
                                signature
                        )
                )
        ));
    }
}
