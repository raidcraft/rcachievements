package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
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
import de.raidcraft.achievements.entities.Category;
import de.raidcraft.achievements.types.CountAchievement;
import de.raidcraft.achievements.types.LocationAchievement;
import de.raidcraft.achievements.types.ManualCountAchievement;
import de.raidcraft.achievements.util.LocationUtil;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Constants.PERMISSION_PREFIX;
import static de.raidcraft.achievements.Messages.*;

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
    public static final String SET_PARENT = "/rca:admin set parent ";
    public static final String SET_CATEGORY = "/rca:admin set category ";
    public static final String SET_DELAYED_BROADCAST = "/rca:admin set delayed-broadcast ";
    public static final SetCommand[] SET_COMMANDS = new SetCommand[]{
            new SetCommand(SET_ALIAS, "Setzt den Alias (eindeutigen Namen) des Achievements.\nDer Alias sollte nur aus Kleinbuchstaben, ohne Sonderzeichen und ohne Leerzeichen bestehen."),
            new SetCommand(SET_NAME, "Setzt den für Spieler sichtbaren Namen des Achievements."),
            new SetCommand(SET_DESC, "Setzt die Beschreibung des Achievements. Je nach der Einstellung von \"secret\" und \"hidden\" wird die Beschreibung den Spielern angezeigt."),
            new SetCommand(SET_SECRET, "true/false. Wenn true sehen Spieler erst die Beschreibung wenn sie das Achievement freigeschaltet haben."),
            new SetCommand(SET_HIDDEN, "true/false. Wenn true sehen Spieler nur ???? anstatt des Namens und der Beschreibung solange sie das Achievement nicht freigeschaltet haben."),
            new SetCommand(SET_BROADCAST, "true/false. Wenn false wird nicht allen anderen Spielern mitgeteilt dass das Achievement errungen wurde.\nGenerell werden \"secret\" Achievements erst 10min nach der Freischaltung bekannt gegeben."),
            new SetCommand(SET_RESTRICTED, "true/false. Wenn true können nur Spieler/Admins mit der rcachievements.achievement.<alias> Permission das Achievement freischalten."),
            new SetCommand(SET_ENABLED, "true/false. Wenn false kann niemand mehr das Achievement freischalten."),
            new SetCommand(SET_PARENT, "Setzt das Parent Achievement des Achievements.\nAchievements werden in einer flachen Ansicht angezeigt."),
            new SetCommand(SET_CATEGORY, "Setzt die Kategorie des Achievements.\nDie Kategorie gruppiert die Achievements in der Liste."),
            new SetCommand(SET_DELAYED_BROADCAST, "Zeigt den Broadcast für andere Spieler erst nach einiger Zeit an.")
    };

    private final RCAchievements plugin;

    public AdminCommands(RCAchievements plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission(PERMISSION_PREFIX + "admin.reload")
    public void reload() {

        final CommandIssuer commandIssuer = getCurrentCommandIssuer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.reload();
            commandIssuer.sendMessage(ChatColor.GREEN + "RCAchievements wurde erfolgreich neu geladen.");
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

    @Subcommand("addcount|increase")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.addcount")
    @CommandCompletion("@players @achievements *")
    @Description("Manually increases the count of an achievement and player.")
    public void addCount(AchievementPlayer player, Achievement achievement, @Default("1") int amount) {

        plugin.achievementManager().active(achievement)
                .filter(context -> context.type() instanceof CountAchievement)
                .ifPresentOrElse(context -> {
                    ((CountAchievement) context.type()).increaseAndCheck(player, amount);
                    Messages.send(getCurrentCommandIssuer(), increaseCountSuccess(achievement, player, amount));
                }, () -> send(getCurrentCommandIssuer(), Messages.increaseCountError(achievement, player)));
    }

    @Subcommand("delete|del")
    @CommandPermission(PERMISSION_PREFIX + "admin.achievement.delete")
    @CommandCompletion("@achievements")
    @Description("Deletes an achievement from the database.")
    public void delete(Achievement achievement) {

        plugin.achievementManager().unload(achievement);
        send(getCurrentCommandIssuer(), deleteSuccess(achievement));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!Strings.isNullOrEmpty(achievement.source())) {
                File file = new File(achievement.source());
                if (file.exists()) {
                    file.delete();
                }
            }
            achievement.delete();
        });
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

        final CommandIssuer commandIssuer = getCurrentCommandIssuer();
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
                commandIssuer.sendMessage(ChatColor.GREEN + "Der Erfolg wurde erfolgreich als Datei gespeichert: " + file.getAbsolutePath());
            } catch (IOException e) {
                commandIssuer.sendMessage("Fehler beim Speichern der Datei: " + e.getMessage());
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
        @CommandCompletion("@achievements true|false")
        public void enabled(Achievement achievement, boolean enabled) {

            achievement.enabled(enabled).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "enabled"));
        }

        @Subcommand("hidden")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.hidden")
        @CommandCompletion("@achievements true|false")
        public void hidden(Achievement achievement, boolean hidden) {

            achievement.hidden(hidden).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "hidden"));
        }

        @Subcommand("secret")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.secret")
        @CommandCompletion("@achievements true|false")
        public void secret(Achievement achievement, boolean secret) {

            achievement.secret(secret).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "secret"));
        }

        @Subcommand("broadcast")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.broadcast")
        @CommandCompletion("@achievements true|false")
        public void broadcast(Achievement achievement, boolean broadcast) {

            achievement.broadcast(broadcast).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "broadcast"));
        }

        @Subcommand("broadcast-delayed")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.broadcast")
        @CommandCompletion("@achievements true|false")
        public void delayedBroadcast(Achievement achievement, boolean delayed) {

            achievement.delayedBroadcast(delayed).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "broadcast-delayed"));
        }

        @Subcommand("restricted")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.restricted")
        @CommandCompletion("@achievements true|false")
        public void restricted(Achievement achievement, boolean restricted) {

            achievement.restricted(restricted).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "restricted"));
        }

        @Subcommand("parent")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.parent")
        @CommandCompletion("@achievements @achievements")
        public void parent(Achievement achievement, Achievement parent) {

            if (achievement.equals(parent)) {
                throw new InvalidCommandArgument("Das Parent Achievement muss ein anderes sein!");
            }

            achievement.parent(parent).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "parent"));
        }

        @Subcommand("category")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.set.category")
        @CommandCompletion("@achievements @categories")
        public void category(Achievement achievement, Category category) {

            achievement.category(category).save();
            send(getCurrentCommandIssuer(), setSuccess(achievement, "category"));
        }
    }

    @Subcommand("setcategory")
    public class SetCategory extends BaseCommand {

        @Subcommand("name")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.setcategory.name")
        @CommandCompletion("@categories *")
        public void name(Category category, String name) {

            category.name(name).save();
            send(getCurrentCommandIssuer(), setCategorySuccess(category, "name"));
        }

        @Subcommand("alias")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.setcategory.alias")
        @CommandCompletion("@categories *")
        public void alias(Category category, String alias) {

            category.alias(alias).save();
            send(getCurrentCommandIssuer(), setCategorySuccess(category, "alias"));
        }

        @Subcommand("description")
        @CommandPermission(PERMISSION_PREFIX + "admin.achievement.setcategory.description")
        @CommandCompletion("@categories *")
        public void description(Category category, String description) {

            category.description(Arrays.asList(description.split("\\|"))).save();
            send(getCurrentCommandIssuer(), setCategorySuccess(category, "description"));
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

        @Subcommand("category")
        @CommandPermission(PERMISSION_PREFIX + "admin.category.create")
        @CommandCompletion("* * *")
        @Description("Creates a new achievement category.")
        public void category(String alias, String name) {

            if (Category.byAlias(alias).isPresent()) {
                throw new ConditionFailedException("Es gibt bereits eine Kategorie mit dem alias: " + alias);
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> Category.create(alias)
                    .name(name)
                    .save());

            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Die Kategorie: " + name + " (" + alias + ") wurde erstellt.");
        }
    }

    @Subcommand("purge")
    @CommandPermission(PERMISSION_PREFIX + "admin.purge")
    @CommandCompletion("@players confirm")
    public void purge(AchievementPlayer player, @Optional String confirm) {

        if (Strings.isNullOrEmpty(confirm) || !confirm.equalsIgnoreCase("confirm")) {
            throw new ConditionFailedException("Dieser Befehl löscht alle Achievements von " + player.name() + ". Führe den Befehl mit confirm aus zur Bestätigung.");
        }

        player.bukkitPlayer().ifPresent(p -> p.kickPlayer("Deine Erfolge werden zurückgesetzt. Bitte warte kurz."));
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Alle Erfolge von " + player.name() + " wurden zurückgesetzt.");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            player.delete();
            plugin.reload();
        });
    }

    @Value
    @Accessors(fluent = true)
    public static class SetCommand {

        String command;
        String description;
    }
}
