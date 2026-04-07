package net.hollowcube.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.minestom.server.Viewable;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A utility class for creating sidebars that are commonly updated.
 */
public class SidebarDisplay implements Viewable {

    private static final Object[] EMPTY = new Object[0]; // An empty array to avoid creating a new one every time

    private final Sidebar sidebar;
    private final Map<String, Object[]> properties = new HashMap<>();
    private int line = 15;

    private SidebarDisplay(Component title) {
        this.sidebar = new Sidebar(title);
    }

    public static SidebarDisplay create(Component title) {
        return new SidebarDisplay(title);
    }

    /**
     * Adds an empty line to the sidebar.
     * @return this instance
     */
    public SidebarDisplay withSpacer() {
        return withLine(Component.empty());
    }

    /**
     * Adds a line to the sidebar that does not have an id and cannot be updated.
     * @param text the text of the line
     * @return this instance
     */
    public SidebarDisplay withLine(Component text) {
        return withLine(String.valueOf(line), text);
    }

    /**
     * Adds a line to the sidebar with an id that can be later used to update the line.
     * Unlike {@link #withLine(String, Component)}, this method uses a translatable key to create the line.
     *
     * @param id the id of the line
     * @param key the translatable key
     * @param defaultArgs the default arguments for the translation
     * @return this instance
     */
    public SidebarDisplay withLine(String id, String key, Object... defaultArgs) {
        this.properties.put(id, defaultArgs);
        return withLine(id, Component.translatable(key, transformArgs(defaultArgs)));
    }

    /**
     * Adds a line to the sidebar with an id that can be later used to update the line.
     * @param id the id of the line
     * @param text the text of the line
     * @return this instance
     */
    public SidebarDisplay withLine(String id, Component text) {
        this.sidebar.createLine(new Sidebar.ScoreboardLine(id, text, this.line, Sidebar.NumberFormat.blank()));
        this.line--;
        return this;
    }

    /**
     * Updates the text of a line in the sidebar if the properties are different and the line
     * was previously set as a translatable component.
     *
     * @param id the id of the line
     * @param properties the properties to update
     */
    public void update(String id, Object... properties) {
        if (!canUpdate(id, properties)) return;

        this.properties.put(id, properties);
        var oldLine = this.sidebar.getLine(id);
        if (oldLine == null) return;
        if (oldLine.getContent() instanceof TranslatableComponent component) {
            this.sidebar.updateLineContent(id, component.arguments(transformArgs(properties)));
        }
    }

    /**
     * Updates the text of a line in the sidebar if the properties are different.
     * @apiNote Unlike {@link #update(String, Object...)}, this method does not care about the previous one and only uses
     * the properties for checking if the line should be updated and not for line content.
     *
     * @param id the id of the line
     * @param text the new text of the line
     * @param properties the properties to update
     */
    public void update(String id, Component text, Object... properties) {
        if (!canUpdate(id, properties)) return;
        this.properties.put(id, properties);
        this.sidebar.updateLineContent(id, text);
    }

    public void destory() {
        new HashSet<>(this.getViewers()).forEach(this::removeViewer);
    }

    @Override
    public boolean addViewer(@NotNull Player viewer) {
        return this.sidebar.addViewer(viewer);
    }

    @Override
    public boolean removeViewer(@NotNull Player viewer) {
        return this.sidebar.removeViewer(viewer);
    }

    @Override
    public @NotNull Set<? extends @NotNull Player> getViewers() {
        return this.sidebar.getViewers();
    }

    private boolean canUpdate(String id, Object... properties) {
        var previous = this.properties.getOrDefault(id, SidebarDisplay.EMPTY);
        return (previous.length == properties.length && previous.length == 0) || !Arrays.equals(previous, properties);
    }

    private static ComponentLike[] transformArgs(Object[] args) {
        ComponentLike[] components = new ComponentLike[args.length];
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case ComponentLike component -> components[i] = component;
                case Boolean bool -> components[i] = TranslationArgument.bool(bool);
                case Number number -> components[i] = TranslationArgument.numeric(number);
                case null -> components[i] = Component.empty();
                default -> components[i] = Component.text(args[i].toString());
            }
        }
        return components;
    }

}
