package net.hollowcube.mapmaker.editor.terraform;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.hollowcube.schem.writer.SchematicWriter;
import net.hollowcube.terraform.schem.SchematicHeader;
import net.hollowcube.terraform.storage.TerraformStorage;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class TerraformStorageHttp extends AbstractHttpService implements TerraformStorage {
    private final String url;

    public TerraformStorageHttp() {
        var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
        if (mapServiceUrl == null) mapServiceUrl = "http://localhost:9126";

        this.url = mapServiceUrl + "/v3/internal/terraform";
    }

    @Override
    public byte @Nullable [] loadPlayerSession(String playerId) {
        var req = HttpRequest.newBuilder()
            .GET().uri(URI.create(url + "/session/" + playerId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> res.body();
            case 404 -> null;
            default ->
                throw new RuntimeException("Failed to fetch player session (" + res.statusCode() + "): " + new String(res.body()));
        };
    }

    @Override
    public void savePlayerSession(String playerId, byte[] session) {
        var body = HttpRequest.BodyPublishers.ofByteArray(session);
        var req = HttpRequest.newBuilder()
            .PUT(body).uri(URI.create(url + "/session/" + playerId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200)
            throw new RuntimeException("Failed to save player session (" + res.statusCode() + "): " + new String(res.body()));
    }

    @Override
    public byte @Nullable [] loadLocalSession(String playerId, String instanceId) {
        var req = HttpRequest.newBuilder()
            .GET().uri(URI.create(url + "/session/" + playerId + "/" + instanceId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> res.body();
            case 404 -> null;
            default ->
                throw new RuntimeException("Failed to fetch local session (" + res.statusCode() + "): " + new String(res.body()));
        };
    }

    @Override
    public void saveLocalSession(String playerId, String instanceId, byte[] session) {
        var body = HttpRequest.BodyPublishers.ofByteArray(session);
        var req = HttpRequest.newBuilder()
            .PUT(body).uri(URI.create(url + "/session/" + playerId + "/" + instanceId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200)
            throw new RuntimeException("Failed to save local session (" + res.statusCode() + "): " + new String(res.body()));
    }

    @Override
    public List<SchematicHeader> listSchematics(String playerId) {
        var req = HttpRequest.newBuilder()
            .GET().uri(URI.create(url + "/schem/" + playerId))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofString());
        return switch (res.statusCode()) {
            case 200 -> GSON.fromJson(res.body(), new TypeToken<ArrayList<SchematicHeader>>() {
            }.getType());
            default ->
                throw new RuntimeException("Failed to fetch schematic list (" + res.statusCode() + "): " + res.body());
        };
    }

    @Override
    public @Nullable Schematic loadSchematicData(String playerId, String name) {
        var req = HttpRequest.newBuilder()
            .GET().uri(URI.create(url + "/schem/" + playerId + "/" + name))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> {
                try {
                    yield SchematicReader.detecting().read(res.body());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read schem data: " + e.getMessage());
                }
            }
            case 404 -> null;
            default ->
                throw new RuntimeException("Failed to fetch schem data (" + res.statusCode() + "): " + new String(res.body()));
        };
    }

    @Override
    public SchematicCreateResult createSchematic(String playerId, String name, Schematic schematic, boolean overwrite) {
        var schemData = SchematicWriter.sponge().write(schematic);
        var endpoint = String.format("%s/schem/%s/%s?dimx=%d&dimy=%d&dimz=%d&size=%d&overwrite=%b",
            this.url, playerId, name,
            schematic.size().blockX(), schematic.size().blockY(), schematic.size().blockZ(),
            schemData.length, overwrite
        );
        var reqBody = HttpRequest.BodyPublishers.ofByteArray(schemData);
        var req = HttpRequest.newBuilder()
            .POST(reqBody).uri(URI.create(endpoint))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> SchematicCreateResult.SUCCESS;
            case 400 -> SchematicCreateResult.ENTRY_LIMIT_EXCEEDED;
            case 409 -> SchematicCreateResult.DUPLICATE_ENTRY;
            default ->
                throw new RuntimeException("Failed to write new schem (" + res.statusCode() + "): " + new String(res.body()));
        };
    }

    @Override
    public SchematicDeleteResult deleteSchematic(String playerId, String name) {
        var req = HttpRequest.newBuilder()
            .DELETE().uri(URI.create(url + "/schem/" + playerId + "/" + name))
            .build();
        var res = doRequest(req, HttpResponse.BodyHandlers.ofByteArray());
        return switch (res.statusCode()) {
            case 200 -> SchematicDeleteResult.SUCCESS;
            case 404 -> SchematicDeleteResult.NOT_FOUND;
            default ->
                throw new RuntimeException("Failed to delete schem (" + res.statusCode() + "): " + new String(res.body()));
        };
    }
}
