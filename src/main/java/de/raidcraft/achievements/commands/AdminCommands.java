package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.annotation.*;
import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import org.bukkit.ChatColor;

import static de.raidcraft.achievements.Constants.PERMISSION_PREFIX;
import static de.raidcraft.achievements.Messages.*;

@CommandAlias("rca:admin|rcaa|rcachievements:admin")
@CommandPermission(PERMISSION_PREFIX + "admin")
public class AdminCommands extends BaseCommand {

    private final RCAchievements plugin;

    public AdminCommands(RCAchievements plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission(PERMISSION_PREFIX + "admin.reload")
    public void reload() {

        plugin.reload();
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "RCAchievements wurde erfolgreich neu geladen.");
    }

    @Subcommand("add|give")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.add")
    @CommandCompletion("@players @achievements")
    @Description("Manually adds an achievement to a player.")
    public void add(AchievementPlayer player, Achievement achievement) {

        if (achievement.addTo(player)) {
            send(getCurrentCommandIssuer(), addSuccess(achievement, player));
        } else {
            send(getCurrentCommandIssuer(), addError(achievement, player));
        }
    }

    @Subcommand("remove|delete|del")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.remove")
    @CommandCompletion("@players @achievements")
    @Description("Removes an achievement from a player.")
    public void remove(AchievementPlayer player, Achievement achievement) {

        if (!player.unlocked(achievement)) {
            throw new ConditionFailedException("Der Spieler " + player.name() + " hat den Erfolg " + achievement.name() + " (" + achievement.alias() + ") nicht.");
        }

        achievement.removeFrom(player);
        send(getCurrentCommandIssuer(), removeSuccess(achievement, player));
    }
}
