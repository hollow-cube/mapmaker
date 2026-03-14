package net.hollowcube.mapmaker.dev.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.enbs.ENBSWriter;
import net.hollowcube.enbs.ExtendedNoteBlockSong;
import net.hollowcube.nbs.DefaultNbsPlayer;
import net.hollowcube.nbs.NBSPlayer;
import net.hollowcube.nbs.NBSReader;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.zip.ZipInputStream;

public class PlayNbsCommand extends CommandDsl {
    public static final PlayNbsCommand INSTANCE = new PlayNbsCommand();
    private static final String NOTEBLOCK_WORLD_URL = "https://api.noteblock.world/api/v1/song/{songid}";
    private static final String NOTEBLOCK_WORLD_DOWNLOAD_URL = NOTEBLOCK_WORLD_URL + "/open";
    private static final Logger logger = LoggerFactory.getLogger(PlayNbsCommand.class);

    private final Argument<String> url = Argument.GreedyString("url");
    private final Argument<String> id = Argument.Word("id");
    private final Argument<String> save = Argument.Literal("save");
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private @Nullable NBSPlayer player;

    public PlayNbsCommand() {
        super("play_nbs_song");
        addSyntax(playerOnly(this::execute), Argument.Literal("play"), url);
        addSyntax(playerOnly(this::downloadNoteblockWorldSong), Argument.Literal("noteblock_world"), id);
        addSyntax(playerOnly(this::downloadNoteblockWorldSong), Argument.Literal("noteblock_world"), id, save);
        addSyntax(playerOnly(this::pause), Argument.Literal("pause"));
        addSyntax(playerOnly(this::resume), Argument.Literal("resume"));
        addSyntax(playerOnly(this::stop), Argument.Literal("stop"));
        addSyntax(playerOnly(this::restart), Argument.Literal("restart"));
    }

    private void downloadNoteblockWorldSong(Player player, CommandContext commandContext) {
        if (this.player != null) {
            this.player.stop();
        }
        var id = commandContext.get(this.id);
        try {
            var songResponse = client.send(HttpRequest.newBuilder()
                                                   .uri(URI.create(NOTEBLOCK_WORLD_URL.replace("{songid}", id)))
                                                   .build(), HttpResponse.BodyHandlers.ofString());
            if (songResponse.statusCode() != 200) return;
            var data = new Gson().fromJson(songResponse.body(), JsonObject.class);

            var response = client.send(HttpRequest.newBuilder()
                                               .uri(URI.create(NOTEBLOCK_WORLD_DOWNLOAD_URL.replace("{songid}", id)))
                                               .header("src", "downloadButton")
                                               .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            var body = response.body();
            var file = new ZipInputStream(new ByteArrayInputStream(download(new URL(body).toURI().toASCIIString())));
            while (file.available() != 0) {
                var next = file.getNextEntry();
                if (next.isDirectory()) continue;
                if (next.getName().endsWith(".nbs")) {
                    var song = NBSReader.reader().read(file.readAllBytes());
                    this.player = new DefaultNbsPlayer(song, player);
                    this.player.start();
                    if (commandContext.has(save)) {
                        var enbs = new ExtendedNoteBlockSong(
                                song,
                                Map.of(
                                        "link", "https://noteblock.world/song/%s".formatted(id),
                                        "license", data.get("license").getAsString()
                                )
                        );
                        Files.write(
                                Path.of(id + ".enbs"),
                                ENBSWriter.writer().write(enbs),
                                StandardOpenOption.CREATE_NEW
                        );
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to load song", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void pause(Player player, CommandContext commandContext) {
        this.player.pause();
    }

    private void resume(Player player, CommandContext commandContext) {
        this.player.resume();
    }

    private void stop(Player player, CommandContext commandContext) {
        this.player.stop();
    }

    private void restart(Player player, CommandContext commandContext) {
        this.player.restart();
    }

    private void execute(Player player, CommandContext commandContext) {
        if (this.player != null) {
            this.player.stop();
        }
        var url = commandContext.get(this.url);
        play(player, url);
    }

    private void play(Player player, String url) {
        try {
            var nbs = NBSReader.reader().read(download(url));
            this.player = new DefaultNbsPlayer(nbs, player);
            this.player.start();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to load song", e);
        }
    }


    private byte[] download(String url) throws IOException, InterruptedException {
        var response = client.send(HttpRequest.newBuilder().uri(URI.create(url)).build(),
                                   HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new RuntimeException("meow");
        return response.body();
    }
}
