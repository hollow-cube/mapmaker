package net.hollowcube.mapmaker.command.arg;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;

import java.io.Reader;
import java.util.Objects;

public class JsonArgument extends Argument<JsonElement> {

    private static final Gson GSON = new Gson();

    protected JsonArgument(String id) {
        super(id);
    }

    @Override
    public ParseResult<JsonElement> parse(CommandSender sender, StringReader reader) {
        try {
            var jsonReader = new JsonReader(new StringReaderReader(reader));
            jsonReader.setStrictness(Strictness.STRICT);
            return success(GSON.<JsonElement>fromJson(jsonReader, JsonElement.class));
        } catch (Throwable e) {
            String message = e.getMessage();
            if (e.getCause() instanceof MalformedJsonException) {
                // Remove this its useless to the user
                message = e.getCause().getMessage()
                    .replace("Use JsonReader.setStrictness(Strictness.LENIENT) to accept ", "")
                    .replace("\nSee https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json", "");
            }
            return syntaxError(reader.pos(), message);
        }
    }

    @Override
    public boolean shouldSuggest() {
        return false;
    }

    private static class StringReaderReader extends Reader {

        private final StringReader reader;

        public StringReaderReader(StringReader reader) {
            this.reader = reader;
        }

        @Override
        public int read() {
            return reader.canRead() ? reader.read() : -1;
        }

        @Override
        public int read(char[] cbuf, int off, int len) {
            if (reader.canRead()) {
                Objects.checkFromIndexSize(off, len, cbuf.length);

                if (len == 0) return 0;

                int n = Math.min(reader.remaining(), len);
                for (int i = 0; i < n; i++) {
                    cbuf[off + i] = reader.read();
                }

                return n;
            }
            return -1;
        }

        @Override
        public void close() {

        }
    }
}
