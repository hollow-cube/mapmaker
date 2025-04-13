package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

public class EntityRenameFix implements Function<Value, Value> {
    private final BiFunction<Value, String, String> mapper;

    public EntityRenameFix(@NotNull BiFunction<Value, String, String> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Value apply(Value value) {
        if (!(value.get("id").value() instanceof String id))
            return null;

        String newId = mapper.apply(value, id);
        if (newId != null && !newId.equals(id))
            value.put("id", newId);

        return null;
    }
}
