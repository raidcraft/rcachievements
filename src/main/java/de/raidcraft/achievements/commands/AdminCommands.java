package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.base.Strings;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.Constants;
import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.types.LocationAchievement;
import de.raidcraft.achievements.util.LocationUtil;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Constants.PERMISSION_PREFIX;
import static de.raidcraft.achievements.Messages.addError;
import static de.raidcraft.achievements.Messages.addSuccess;
import static de.raidcraft.achievements.Messages.createSuccess;
import static de.raidcraft.achievements.Messages.deleteSuccess;
import static de.raidcraft.achievements.Messages.send;
import static de.raidcraft.achievements.Messages.setSuccess;
import static de.raidcraft.achievements.Messages.unassignSuccess;

@CommandAlias("rca:admin|rcaa|rcachievements:admin")
@CommandPermission(PERMISSION_PREFIX + "admin")
public class AdminCommands extends BaseCommand {

    public static final BiFunction<Integer, Integer, String> NEARBY = (page, radius) -> "/rca:admin nearby " + page + " " + radius;
    public static final String RELOAD = "/rca:admin reload";
    public static final String SET_ALIAS = "/rca:admin set alias ";
    public static final String SET_NAME = "/rca:admin set name ";
    public static final String SET_DESC = "/rca:admin set desc ";
    public static final String SET_ENABLED = "/rca:admin set enabled ";
    public static final String SET_SECRET = "/rca:admin set secret ";
    public static final String SET_HIDDEN = "/rca:admin set hidden ";
    public static final String SET_RESTRICTED = "/rca:admin set restricted ";
    public static final String SET_BROADCAST = "/rca:admin set broadcast ";
    public static final SetCommand[] SET_COMMANDS = new SetCommand[]{
            new SetCommand(SET_ALIAS, "Setzt den Alias (eindeutigen Namen) des Achievements.\nDer Alias sollte nur aus Kleinbuchstaben, ohne Sonderzeichen und ohne Leerzeichen bestehen."),
            new SetCommand(SET_NAME, "Setzt den für Spieler sichtbaren Namen des Achievements."),
            new SetCommand(SET_DESC, "Setzt die Beschreibung des Achievements. Je nach der Einstellung von \"secret\" und \"hidden\" wird die Beschreibung den Spielern angezeigt."),
            new SetCommand(SET_SECRET, "true/false. Wenn true sehen Spieler erst die Beschreibung wenn sie das Achievement freigeschaltet haben."),
            new SetCommand(SET_HIDDEN, "true/false. Wenn true sehen Spieler nur ???? anstatt des Namens und der Beschreibung solange sie das Achievement nicht freigeschaltet haben."),
            new SetCommand(SET_BROADCAST, "true/false. Wenn false wird nicht allen anderen Spielern mitgeteilt dass das Achievement errungen wurde.\nGenerell werden \"secret\" Achievements erst 10min nach der Freischaltung bekannt gegeben."),
            new SetCommand(SET_RESTRICTED, "true/false. Wenn true können nur Spieler/Admins mit der rcachievements.achievement.<alias> Permission das Achievement freischalten."),
            new SetCommand(SET_ENABLED, "true/false. Wenn false kann niemand mehr das Achievement freischalten.")
    };

    private final RCAchievements plugin;

    public AdminCommands(RCAchievements plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission(PERMISSION_PREFIX + "admin.reload")
    public void reload() {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.reload();
            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "RCAchievements wurde erfolgreich neu geladen.");
        });
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

    @Subcommand("delete|del")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.delete")
    @CommandCompletion("@achievements")
    @Description("Deletes an achievement from the database.")
    public void delete(Achievement achievement) {

        plugin.achievementManager().unload(achievement);
        achievement.delete();
        send(getCurrentCommandIssuer(), deleteSuccess(achievement));
    }

    @Subcommand("unassign")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.unassign")
    @CommandCompletion("@players @unlocked-achievements")
    @Description("Unassigns an achievement from a player.")
    public void unassign(AchievementPlayer player, Achievement achievement) {

        if (!player.unlocked(achievement)) {
            throw new ConditionFailedException("Der Spieler " + player.name() + " hat den Erfolg " + achievement.name() + " (" + achievement.alias() + ") nicht.");
        }

        achievement.removeFrom(player);
        plugin.achievementManager().active(achievement).ifPresent(AchievementContext::clearCache);
        send(getCurrentCommandIssuer(), unassignSuccess(achievement, player));
    }

    @Subcommand("nearby|near")
    @CommandCompletion("*")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.nearby")
    @Description("Shows all location achievements near the current position.")
    public void nearby(Player player, @Default("1") int page, @Default("100") int radius) {

        List<Map.Entry<Achievement, Integer>> locations = Achievement.find.query()
                .where().eq("type", LocationAchievement.TYPE)
                .and().eq("enabled", true)
                .findList().stream()
                .flatMap(achievement -> plugin.achievementManager().active(achievement).stream())
                .filter(AchievementContext::initialized)
                .filter(context -> context.type() instanceof LocationAchievement)
                .map(context -> (LocationAchievement) context.type())
                .filter(achievement -> achievement.getLocation() != null)
                .filter(achievement -> LocationUtil.isWithinRadius(player.getLocation(), achievement.getLocation().getLocation(), radius))
                .sorted((o1, o2) -> LocationUtil.getBlockDistance(o2.getLocation().getLocation(), o1.getLocation().getLocation()))
                .map(achievement -> Map.entry(achievement.achievement(), LocationUtil.getBlockDistance(player.getLocation(), achievement.getLocation().getLocation())))
                .collect(Collectors.toList());

        if (locations.isEmpty()) {
            getCurrentCommandIssuer().sendMessage(ChatColor.RED + "In der Nähe wurden keine Erfolge gefunden.");
        } else {
            Messages.listNearby(AchievementPlayer.of(player), locations, page, radius)
                    .forEach(component -> send(getCurrentCommandIssuer(), component));
        }
    }

    @Subcommand("save")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.save")
    @CommandCompletion("@achievements *")
    @Description("Saves an achievement to disk.")
    public void save(Achievement achievement, @Optional String path) {

        if (!Strings.isNullOrEmpty(achievement.source())) {
            throw new ConditionFailedException("Der Erfolg " + achievement.alias() + " existiert bereits als Datei unter: " + achievement.source());
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File baseDir = new File(plugin.getDataFolder(), plugin.pluginConfig().getAchievements());
            File file;
            if (Strings.isNullOrEmpty(path)) {
                file = new File(baseDir, achievement.alias() + ".yml");
            } else {
                String fileName = path.replace(".", "/");
                if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
                    fileName += ".yml";
                }
                file = new File(baseDir, fileName);
            }
            file.getParentFile().mkdirs();

            if (file.exists()) {
                throw new ConditionFailedException("Die Datei " + file.getAbsolutePath() + " existiert bereits. Bitte verwende einen anderen Speicherort.");
            }

            try {
                achievement.toConfig().save(file);
                getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Der Erfolg wurde erfolgreich als Datei gespeichert: " + file.getAbsolutePath());
            } catch (IOException e) {
                getCurrentCommandIssuer().sendMessage("Fehler beim Speichern der Datei: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Subcommand("set")
    public class SetCommands extends BaseCommand {

        @Subcommand("alias")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.alias")
        @CommandCompletion("@achievements *")
        public void alias(Achievement achievement, String alias) {

            achievement.alias(alias).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "alias"));
        }

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

        @Subcommand("default")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.create.default")
        @CommandCompletion("*")
        @Description("Creates a new default achievement.")
        public void none(String alias) {

            if (Achievement.byAlias(alias).isPresent()) {
                throw new ConditionFailedException("Es gibt bereits einen Erfolg mit dem alias: " + alias);
            }

            Achievement achievement = Achievement.create(alias, new MemoryConfiguration())
                    .type(Constants.DEFAULT_TYPE);
            achievement.save();

            send(getCurrentCommandIssuer(), createSuccess(achievement));
        }

    }

    @Value
    @Accessors(fluent = true)
    public static class SetCommand {

        String command;
        String description;
    }
}
