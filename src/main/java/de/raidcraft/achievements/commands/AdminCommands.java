package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import de.raidcraft.achievements.AchievementsPlugin;

public class AdminCommands extends BaseCommand {

    private final AchievementsPlugin plugin;

    public AdminCommands(AchievementsPlugin plugin) {
        this.plugin = plugin;
    }
}
