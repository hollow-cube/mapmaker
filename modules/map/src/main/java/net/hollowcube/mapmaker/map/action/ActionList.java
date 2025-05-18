package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.map.action.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// TODO ANVILS NEED TITLEs
public class ActionList {
    public static final List<AbstractAction<?>> ACTIONS = List.of(
            EditLivesAction.INSTANCE, EditTimerAction.INSTANCE,
            AddPotionAction.INSTANCE, RemovePotionAction.INSTANCE,
            TeleportAction.INSTANCE,
            EnableSettingAction.INSTANCE, DisableSettingAction.INSTANCE
    );

    private final List<ActionData<?>> actions = new ArrayList<>();

    public int size() {
        return actions.size();
    }

    public @Nullable ActionData<?> get(int index) {
        if (index < 0 || index >= actions.size()) return null;
        return actions.get(index);
    }

    public <T> @NotNull ActionData<T> addAction(AbstractAction<T> action) {
        var actionData = new ActionData<>(action);
        actions.add(actionData);
        return actionData;
    }

    public void remove(int index) {
        if (index < 0 || index >= actions.size()) return;
        actions.remove(index);
    }

    public static class ActionData<T> {
        private final AbstractAction<T> action;
        private T data;

        public ActionData(@NotNull AbstractAction<T> action) {
            this.action = action;
            this.data = action.defaultData();
        }

        public @NotNull AbstractAction<T> action() {
            return action;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
