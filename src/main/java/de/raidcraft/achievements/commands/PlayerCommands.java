package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import de.raidcraft.achievements.AchievementsPlugin;

public class PlayerCommands extends BaseCommand {

    private final AchievementsPlugin plugin;

    public PlayerCommands(AchievementsPlugin plugin) {
        this.plugin = plugin;
    }
}
