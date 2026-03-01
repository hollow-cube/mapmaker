package net.hollowcube.mapmaker.api.interaction;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;

import java.util.List;

public interface InteractionClient {

    List<Command> getCommands();

    InteractionResponse execute(Interaction interaction);

    record Http(HttpClientWrapper http) implements InteractionClient {
        private static final String V4_PREFIX = "/v4/internal/interactions";

        @Override
        public List<Command> getCommands() {
            return http.get(
                "getRemoteInteractions",
                V4_PREFIX + "/commands",
                new TypeToken<>() {});
        }

        @Override
        public InteractionResponse execute(Interaction interaction) {
            return http.post(
                "execute",
                V4_PREFIX,
                interaction,
                new TypeToken<>() {});
        }
    }
}
