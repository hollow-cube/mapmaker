package org.bukkit;

public record ChatColor(String text) {

    public static final ChatColor RED = new ChatColor("<red>");
    public static final ChatColor GREEN = new ChatColor("<green>");


    @Override
    public String toString() {
        return text;
    }
}
