package net.hollowcube.apiserver.chatfilter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ChatFilter implements AutoCloseable {

    /// Probabilities, in the order of [#outputs()].
    public record Scores(String version, List<Float> values) {}

    private final String version;
    private final List<String> outputs;
    private final ChatTokenizer tokenizer;
    private final OnnxModel model;
    private final @Nullable NameMask names;

    private ChatFilter(
        String version,
        List<String> outputs,
        ChatTokenizer tokenizer,
        OnnxModel model,
        @Nullable NameMask names
    ) {
        this.version = version;
        this.outputs = outputs;
        this.tokenizer = tokenizer;
        this.model = model;
        this.names = names;
    }

    public static ChatFilter load(Path onnxRuntime, Path dir) throws IOException {
        var manifest = JsonParser.parseString(Files.readString(dir.resolve("manifest.json")))
            .getAsJsonObject();
        var outputs = manifest.getAsJsonArray("outputs")
            .asList()
            .stream()
            .map(JsonElement::getAsString)
            .toList();
        var tokenizer = ChatTokenizer.load(dir.resolve("tokenizer.bin"));
        var model = OnnxModel.load(
            onnxRuntime,
            dir.resolve("model.onnx"),
            manifest.get("output").getAsString(),
            outputs.size()
        );

        // Load names only if the model manifest asks for them
        NameMask names = null;
        if (manifest.has("names")) {
            var spec = manifest.getAsJsonObject("names");
            names = NameMask.load(
                spec.get("placeholder").getAsString(),
                dir.resolve(spec.get("words").getAsString())
            );
        }

        return new ChatFilter(
            manifest.get("version").getAsString(),
            outputs,
            tokenizer,
            model,
            names
        );
    }

    public String version() {
        return version;
    }

    public List<String> outputs() {
        return outputs;
    }

    public @Nullable NameMask names() {
        return names;
    }

    public Scores score(String text) {
        var ids = tokenizer.encode(names == null ? text : names.mask(text));
        var mask = new long[ids.length];
        Arrays.fill(mask, 1);
        var logits = model.run(ids, mask, 1, ids.length);
        var values = new Float[logits.length];
        for (int i = 0; i < logits.length; i++)
            values[i] = (float) (1 / (1 + Math.exp(-logits[i])));
        return new Scores(version, List.of(values));
    }

    @Override
    public void close() {
        model.close();
    }
}
