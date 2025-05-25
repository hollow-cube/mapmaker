package net.hollowcube.mapmaker.map.action;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ActionList {
    private final List<Ref> actions = new ArrayList<>();

    public ActionList() {
    }

    public ActionList(@NotNull List<Action> actions) {
        for (var action : actions) {
            this.actions.add(new Ref(ActionRegistry.getKey(action), action));
        }
    }

    public ActionList(@NotNull ActionList actions) {
        for (var action : actions.actions) {
            this.actions.add(new Ref(action.key(), action.action()));
        }
    }

    public boolean has(@NotNull Key actionKey) {
        return actions.stream().anyMatch(ref -> ref.key().equals(actionKey));
    }

    public <T extends Action> @Nullable T findLast(@NotNull Class<T> actionType) {
        for (int i = actions.size() - 1; i >= 0; i--) {
            var action = actions.get(i);
            if (action.action().getClass() == actionType) {
                //noinspection unchecked
                return (T) action.action();
            }
        }
        return null;
    }

    public @NotNull List<Action> actions() {
        return actions.stream().map(Ref::action).toList();
    }

    public int size() {
        return actions.size();
    }

    public @Nullable Ref get(int index) {
        if (index < 0 || index >= actions.size()) return null;
        return actions.get(index);
    }

    public @NotNull Ref addAction(@NotNull Key action) {
        var ref = new Ref(action, ActionRegistry.createDefault(action));
        actions.add(ref);
        return ref;
    }

    public void remove(int index) {
        if (index < 0 || index >= actions.size()) return;
        actions.remove(index);
    }

    /// Ref implements a mutable reference to an action/data.
    public class Ref implements Keyed {
        private final Key key;
        private Action data;

        Ref(@NotNull Key key, @NotNull Action data) {
            this.key = key;
            this.data = data;
        }

        @Override
        public @NotNull Key key() {
            return this.key;
        }

        public @NotNull Action action() {
            return data;
        }

        public <T extends Action> @NotNull T cast() {
            //noinspection unchecked
            return (T) data;
        }

        public <T extends Action> @NotNull T update(@NotNull Function<T, T> updater) {
            var newValue = updater.apply(cast());
            this.data = newValue;
            return newValue;
        }

        public void remove() {
            actions.remove(this);
        }

        public @NotNull ActionList parent() {
            return ActionList.this;
        }
    }
}
