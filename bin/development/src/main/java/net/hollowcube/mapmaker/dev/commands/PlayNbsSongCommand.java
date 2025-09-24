package net.hollowcube.mapmaker.dev.commands;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.nbs.DefaultNbsPlayer;
import net.hollowcube.nbs.NBSPlayer;
import net.hollowcube.nbs.NBSReader;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class PlayNbsSongCommand extends CommandDsl {
    public static final PlayNbsSongCommand INSTANCE = new PlayNbsSongCommand();
    private static final String NOTEBLOCK_WORLD_URL = "https://api.noteblock.world/api/v1/song/{songid}/open";
    private static final Logger logger = LoggerFactory.getLogger(PlayNbsSongCommand.class);

    private final Argument<String> url = Argument.GreedyString("url");
    private final Argument<String> id = Argument.Word("id");
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private NBSPlayer player;

    public PlayNbsSongCommand() {
        super("play_nbs_song");
        addSyntax(playerOnly(this::execute), Argument.Literal("play"), url);
        addSyntax(playerOnly(this::downloadNoteblockWorldSong), Argument.Literal("noteblock_world"), id);
        addSyntax(playerOnly(this::pause), Argument.Literal("pause"));
        addSyntax(playerOnly(this::resume), Argument.Literal("resume"));
        addSyntax(playerOnly(this::stop), Argument.Literal("stop"));
        addSyntax(playerOnly(this::restart), Argument.Literal("restart"));
    }

    private void downloadNoteblockWorldSong(@NotNull Player player, @NotNull CommandContext commandContext) {
        if (this.player != null) {
            this.player.stop();
        }
        var id = commandContext.get(this.id);
        try {
            var response = client.send(HttpRequest.newBuilder()
                                               .uri(URI.create(NOTEBLOCK_WORLD_URL.replace("{songid}", id)))
                                               .header("src", "downloadButton")
                                               .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            var body = response.body();
            var file = new ZipInputStream(new ByteArrayInputStream(download(new URL(body).toURI().toASCIIString())));
            while (file.available() != 0) {
                var next = file.getNextEntry();
                if (next.isDirectory()) continue;
                if (next.getName().endsWith(".nbs")) {
                    var nbs = NBSReader.nbsReader().read(file.readAllBytes());
                    this.player = new DefaultNbsPlayer(nbs, player);
                    this.player.start();
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to load song", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void pause(@NotNull Player player, @NotNull CommandContext commandContext) {
        this.player.pause();
    }

    private void resume(@NotNull Player player, @NotNull CommandContext commandContext) {
        this.player.resume();
    }

    private void stop(@NotNull Player player, @NotNull CommandContext commandContext) {
        this.player.stop();
    }

    private void restart(@NotNull Player player, @NotNull CommandContext commandContext) {
        this.player.restart();
    }

    private void execute(@NotNull Player player, @NotNull CommandContext commandContext) {
        if (this.player != null) {
            this.player.stop();
        }
        var url = commandContext.get(this.url);
        play(player, url);
    }

    private void play(@NotNull Player player, @NotNull String url) {
        try {
            var nbs = NBSReader.nbsReader().read(download(url));
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
