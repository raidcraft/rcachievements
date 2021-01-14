package de.raidcraft.achievements;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.google.common.base.Strings;
import de.raidcraft.achievements.commands.AdminCommands;
import de.raidcraft.achievements.commands.PlayerCommands;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.DataStore;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.listener.PlayerListener;
import io.ebean.Database;
import kr.entree.spigradle.annotations.PluginMain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.Config;
import net.silthus.ebean.EbeanWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Constants.SHOW_HIDDEN;

@Accessors(fluent = true)
@PluginMain
public class RCAchievements extends JavaPlugin {

    @Getter
    @Accessors(fluent = true)
    private static RCAchievements instance;

    private Database database;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private PluginConfig pluginConfig;

    private PaperCommandManager commandManager;
    @Getter
    private AchievementManager achievementManager;
    private PlayerListener playerListener;

    @Getter
    private static boolean testing = false;

    public RCAchievements() {
        instance = this;
    }

    public RCAchievements(
            JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        instance = this;
        testing = true;
    }

    @Override
    public void onEnable() {

        loadConfig();
        setupDatabase();
        setupAchievementManager();
        setupListener();
        setupCommands();
    }

    @Override
    public void onDisable() {

        achievementManager().unload();
    }

    public void reload() {

        loadConfig();
        achievementManager().reload();
    }

    private void loadConfig() {

        getDataFolder().mkdirs();
        pluginConfig = new PluginConfig(new File(getDataFolder(), "config.yml").toPath());
        pluginConfig.loadAndSave();
    }

    private void setupAchievementManager() {

        this.achievementManager = new AchievementManager(this);
        achievementManager.registerDefaults();
        achievementManager.load();
    }

    private void setupListener() {

        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        setupBungeecord();
    }

    private void setupBungeecord() {

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", playerListener);
    }

    private void setupCommands() {

        this.commandManager = new PaperCommandManager(this);

        // context resolver
        achievementPlayerContext(commandManager);
        achievementsContext(commandManager);

        // completions
        playersCompletion(commandManager);
        achievementsCompletion(commandManager);

        // conditions
        visibleCondition(commandManager);

        commandManager.registerCommand(new AdminCommands(this));
        commandManager.registerCommand(new PlayerCommands(this));
    }

    private void achievementsCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("achievements", context ->
                Achievement.allEnabled()
                .stream().map(Achievement::alias)
                .collect(Collectors.toSet()));
    }

    private void achievementsContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerIssuerAwareContext(Achievement.class, context -> {
            String arg = context.popFirstArg();
            try {
                return Achievement.find.byId(UUID.fromString(arg));
            } catch (Exception e) {
                if (context.hasFlag("self")) {
                    return PlayerAchievement.find.query().where()
                            .eq("player_id", context.getPlayer().getUniqueId())
                            .and().ieq("achievement.alias", arg)
                            .findOneOrEmpty()
                            .map(PlayerAchievement::achievement)
                            .orElse(null);
                } else {
                    return Achievement.byAlias(arg).orElse(null);
                }
            }

        });
    }

    private void visibleCondition(PaperCommandManager commandManager) {

        commandManager.getCommandConditions().addCondition(Achievement.class, "visible", (context, execContext, value) -> {
            if (value.hidden() && !context.getIssuer().hasPermission(SHOW_HIDDEN)) {
                throw new ConditionFailedException("Du kannst versteckte Erfolge nicht anzeigen.");
            }
        });
    }

    private void playersCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("players", context -> AchievementPlayer.find.all()
                .stream()
                .map(AchievementPlayer::name)
                .collect(Collectors.toSet()));
    }

    private void achievementPlayerContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerIssuerAwareContext(AchievementPlayer.class, context -> {

            if (context.hasFlag("self")) {
                return AchievementPlayer.of(context.getPlayer());
            }

            String arg = context.popFirstArg();
            Player player;
            if (!Strings.isNullOrEmpty(arg) && arg.startsWith("@")) {
                player = selectPlayer(context.getSender(), arg);
            } else {
                if (Strings.isNullOrEmpty(arg)) {
                    return AchievementPlayer.of(context.getPlayer());
                }
                try {
                    UUID uuid = UUID.fromString(arg);
                    return AchievementPlayer.find.byId(uuid);
                } catch (Exception e) {
                    player = Bukkit.getPlayerExact(arg);
                }
            }

            if (player == null) {
                throw new InvalidCommandArgument("No player by the name \"" + arg + "\" was not found.");
            }

            return AchievementPlayer.of(player);
        });
    }

    private void setupDatabase() {

        this.database = new EbeanWrapper(Config.builder(this)
                .entities(
                        AchievementPlayer.class,
                        Achievement.class,
                        PlayerAchievement.class,
                        DataStore.class
                )
                .build()).connect();
    }

    private Player selectPlayer(CommandSender sender, String playerIdentifier) {

        List<Player> matchedPlayers;
        try {
            matchedPlayers = getServer().selectEntities(sender, playerIdentifier).parallelStream()
                    .unordered()
                    .filter(e -> e instanceof Player)
                    .map(e -> ((Player) e))
                    .collect(Collectors.toList());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new InvalidCommandArgument(String.format("Error parsing selector '%s' for %s! See console for more details",
                    playerIdentifier, sender.getName()));
        }
        if (matchedPlayers.isEmpty()) {
            throw new InvalidCommandArgument(String.format("No player found with selector '%s' for %s",
                    playerIdentifier, sender.getName()));
        }
        if (matchedPlayers.size() > 1) {
            throw new InvalidCommandArgument(String.format("Error parsing selector '%s' for %s. ambiguous result (more than one player matched) - %s",
                    playerIdentifier, sender.getName(), matchedPlayers.toString()));
        }

        return matchedPlayers.get(0);
    }
}

