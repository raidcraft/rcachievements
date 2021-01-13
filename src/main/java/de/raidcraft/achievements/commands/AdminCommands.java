package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.annotation.*;
import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.types.LocationAchievement;
import io.ebeaninternal.server.lib.Str;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.function.Function;

import static de.raidcraft.achievements.Constants.PERMISSION_PREFIX;
import static de.raidcraft.achievements.Messages.*;

@CommandAlias("rca:admin|rcaa|rcachievements:admin")
@CommandPermission(PERMISSION_PREFIX + "admin")
public class AdminCommands extends BaseCommand {

    public static final String RELOAD = "/rca:admin reload";
    public static final String SET_NAME = "/rca:admin set name ";
    public static final String SET_DESC = "/rca:admin set desc ";
    public static final String SET_ENABLED = "/rca:admin set enabled ";
    public static final String SET_SECRET = "/rca:admin set secret ";
    public static final String SET_HIDDEN = "/rca:admin set hidden ";
    public static final String SET_RESTRICTED = "/rca:admin set restricted ";
    public static final String SET_BROADCAST = "/rca:admin set broadcast ";
    public static final String[] SET_COMMANDS = new String[]{
            SET_NAME,
            SET_DESC,
            SET_SECRET,
            SET_BROADCAST,
            SET_RESTRICTED,
            SET_HIDDEN,
            SET_ENABLED
    };

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

    @Subcommand("set")
    public class SetCommands extends BaseCommand {

        @Subcommand("name")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.name")
        @CommandCompletion("@achievements *")
        public void name(Achievement achievement, String name) {

            achievement.name(name).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "name"));
        }

        @Subcommand("desc|description")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.description")
        @CommandCompletion("@achievements *")
        public void desc(Achievement achievement, String description) {

            achievement.description(description).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "description"));
        }

        @Subcommand("enabled")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.enabled")
        @CommandCompletion("@achievements *")
        public void enabled(Achievement achievement, boolean enabled) {

            achievement.enabled(enabled).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "enabled"));
        }

        @Subcommand("hidden")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.hidden")
        @CommandCompletion("@achievements *")
        public void hidden(Achievement achievement, boolean hidden) {

            achievement.hidden(hidden).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "hidden"));
        }

        @Subcommand("secret")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.secret")
        @CommandCompletion("@achievements *")
        public void secret(Achievement achievement, boolean secret) {

            achievement.secret(secret).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "secret"));
        }

        @Subcommand("broadcast")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.broadcast")
        @CommandCompletion("@achievements *")
        public void broadcast(Achievement achievement, boolean broadcast) {

            achievement.broadcast(broadcast).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "broadcast"));
        }

        @Subcommand("restricted")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.restricted")
        @CommandCompletion("@achievements *")
        public void restricted(Achievement achievement, boolean restricted) {

            achievement.restricted(restricted).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "restricted"));
        }
    }

    @Subcommand("create")
    public class CreateCommands extends BaseCommand {

        @Subcommand("loc|location")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.create.location")
        @CommandCompletion("* *")
        @Description("Creates a new location achievement at the current position.")
        public void location(Player player, String alias, int radius) {

            if (Achievement.byAlias(alias).isPresent()) {
                throw new ConditionFailedException("Es gibt bereits einen Erfolg mit dem alias: " + alias);
            }

            Achievement achievement = Achievement.create(alias, LocationAchievement.create(player.getLocation(), radius))
                    .type(LocationAchievement.TYPE);
            achievement.save();

            send(getCurrentCommandIssuer(), createSuccess(achievement));
        }
    }
}
