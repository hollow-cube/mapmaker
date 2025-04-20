package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class EntityRenameFix implements DataFix {
    private final BiFunction<Value, String, String> mapper;

    public EntityRenameFix(@NotNull BiFunction<Value, String, String> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Value fix(Value value) {
        if (!(value.get("id").value() instanceof String id))
            return null;

        String newId = mapper.apply(value, id);
        if (newId != null && !newId.equals(id))
            value.put("id", newId);

        return null;
    }
}
