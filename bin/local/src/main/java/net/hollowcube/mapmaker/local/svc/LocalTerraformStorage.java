package net.hollowcube.mapmaker.local.svc;

import net.hollowcube.mapmaker.local.LocalServerRunner;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.DetectingSchematicReader;
import net.hollowcube.schem.writer.SpongeSchematicWriter;
import net.hollowcube.terraform.schem.SchematicHeader;
import net.hollowcube.terraform.storage.TerraformStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocalTerraformStorage implements TerraformStorage {
    private static final Logger logger = LoggerFactory.getLogger(LocalTerraformStorage.class);
    private static final List<String> VALID_EXTENSIONS = List.of(".schem", ".schematic", ".bp", ".litematica", ".litematic");

    private final Path tfstate;
    private final Path schematics;

    public LocalTerraformStorage() {
        this(LocalServerRunner.workspace);
    }

    public LocalTerraformStorage(@NotNull Path workspace) {
        this.tfstate = workspace.resolve("tfstate");
        this.schematics = workspace.resolve("schematics");
    }

    @Override
    public byte @Nullable [] loadPlayerSession(@NotNull String playerId) {
        var path = tfstate.resolve(playerId + "_global");
        if (!Files.exists(path)) return null;

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void savePlayerSession(@NotNull String playerId, byte @NotNull [] session) {
        try {
            var path = tfstate.resolve(playerId + "_global");
            Files.write(path, session, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte @Nullable [] loadLocalSession(@NotNull String playerId, @NotNull String instanceId) {
        var path = tfstate.resolve(playerId + "_local");
        if (!Files.exists(path)) return null;

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveLocalSession(@NotNull String playerId, @NotNull String instanceId, byte @NotNull [] session) {
        try {
            var path = tfstate.resolve(playerId + "_local");
            Files.write(path, session, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull List<@NotNull SchematicHeader> listSchematics(@NotNull String playerId) {
        try {
            if (!Files.exists(schematics))
                return List.of();
            var reader = new DetectingSchematicReader();
            return Files.list(schematics)
                    .filter(p -> VALID_EXTENSIONS.stream().anyMatch(ext -> p.getFileName().toString().endsWith(ext)))
                    .map(p -> {
                        try {
                            return Map.entry(p.getFileName().toString(), reader.read(Files.readAllBytes(p)));
                        } catch (Exception e) {
                            logger.warn("not a valid schematic: {}", p.getFileName());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(s -> new SchematicHeader(s.getKey(), 0, null, null))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable Schematic loadSchematicData(@NotNull String playerId, @NotNull String name) {
        try {
            var path = schematics.resolve(name);
            if (!Files.exists(path)) return null;
            return new DetectingSchematicReader().read(Files.readAllBytes(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull SchematicCreateResult createSchematic(@NotNull String playerId, @NotNull String name, @NotNull Schematic schematic, boolean overwrite) {
        try {
            var path = schematics.resolve(name);
            if (Files.exists(path)) {
                if (overwrite) {
                    Files.write(path, new SpongeSchematicWriter().write(schematic), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    return SchematicCreateResult.SUCCESS;
                } else {
                    return SchematicCreateResult.DUPLICATE_ENTRY;
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.write(path, new SpongeSchematicWriter().write(schematic), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return SchematicCreateResult.SUCCESS;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull SchematicDeleteResult deleteSchematic(@NotNull String playerId, @NotNull String name) {
        throw new UnsupportedOperationException("not implemented");
    }
}
