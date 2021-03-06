package de.raidcraft.achievements;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.google.common.base.Strings;
import de.raidcraft.achievements.commands.AdminCommands;
import de.raidcraft.achievements.commands.PlayerCommands;
import de.raidcraft.achievements.entities.*;
import de.raidcraft.achievements.listener.PlayerListener;
import de.raidcraft.achievements.listener.ProgressListener;
import de.raidcraft.achievements.listener.RewardListener;
import de.raidcraft.achievements.plan.PlanHook;
import de.raidcraft.achievements.types.ArtAchievement;
import io.artframework.Scope;
import io.artframework.annotations.ArtModule;
import io.artframework.annotations.OnDisable;
import io.artframework.annotations.OnEnable;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Constants.SHOW_HIDDEN;

@Accessors(fluent = true)
@PluginMain
@ArtModule("rcachievements")
public class RCAchievements extends JavaPlugin {

    @Getter
    @Accessors(fluent = true)
    private static RCAchievements instance;

    @Getter(AccessLevel.PUBLIC)
    private Database database;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private PluginConfig pluginConfig;

    private PaperCommandManager commandManager;
    @Getter
    private AchievementManager achievementManager;
    private PlayerListener playerListener;
    private RewardListener rewardListener;
    private ProgressListener progressListener;
    @Getter
    private BlockTracker blockTracker;
    @Getter
    private Scope art;
    private PlanHook planHook;

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
        if (!testing()) {
            setupPlayerAnalytics();
        }
    }

    @Override
    public void onDisable() {

        achievementManager().unload();
        blockTracker.disable();
    }

    @OnEnable
    public void onArtEnable(Scope scope) {

        this.art = scope;
        this.rewardListener = new RewardListener(this, scope);
        getServer().getPluginManager().registerEvents(rewardListener, this);
        try {
            achievementManager().register(new ArtAchievement.Factory());
        } catch (TypeRegistrationException e) {
            getLogger().severe("failed to register art-achievement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnDisable
    public void onArtDisable() {

        achievementManager().unregister(ArtAchievement.class);
        this.art = null;
    }

    public void artPresent(Consumer<Scope> art) {

        if (artEnabled()) {
            art.accept(this.art);
        }
    }

    public boolean artEnabled() {

        return art != null;
    }

    public void reload(boolean force) {

        loadConfig();
        achievementManager().reload(force);
        if (rewardListener != null) {
            rewardListener.reload();
        }
    }

    private void loadConfig() {

        getDataFolder().mkdirs();
        pluginConfig = new PluginConfig(new File(getDataFolder(), "config.yml").toPath());
        pluginConfig.loadAndSave();
    }

    private void setupPlayerAnalytics() {

        planHook = new PlanHook();
        planHook.hookIntoPlan();
    }

    private void setupAchievementManager() {

        this.achievementManager = new AchievementManager(this);
        achievementManager.registerDefaults();

        // needs to be delayed by one tick to allow all worlds to load
        Bukkit.getScheduler().runTaskLater(this, achievementManager::load, 1L);
    }

    private void setupListener() {

        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        progressListener = new ProgressListener(this);
        getServer().getPluginManager().registerEvents(progressListener, this);
        blockTracker = new BlockTracker(this);
        blockTracker.enable();
        getServer().getPluginManager().registerEvents(blockTracker, this);

        setupBungeecord();
    }

    private void setupBungeecord() {

        if (testing()) return;

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", playerListener);
    }

    private void setupCommands() {

        this.commandManager = new PaperCommandManager(this);

        // context resolver
        achievementPlayerContext(commandManager);
        achievementsContext(commandManager);
        categoryContext(commandManager);

        // completions
        playersCompletion(commandManager);
        achievementsCompletion(commandManager);
        achievementsUnlockedCompletion(commandManager);
        categoriesCompletion(commandManager);

        // conditions
        visibleCondition(commandManager);
        selfCondition(commandManager);

        commandManager.registerCommand(new AdminCommands(this));
        commandManager.registerCommand(new PlayerCommands(this));
    }

    private void selfCondition(PaperCommandManager commandManager) {

        commandManager.getCommandConditions().addCondition(AchievementPlayer.class, "self", (context, execContext, value) -> {
            if (context.getIssuer().hasPermission(Constants.SHOW_OTHERS_PERMISSION)) {
                return;
            }
            if (AchievementPlayer.of(execContext.getPlayer()).equals(value)) {
                return;
            }
            throw new ConditionFailedException("Du kannst dir nicht die Erfolge von anderen Spielern anzeigen lassen.");
        });
    }

    private void achievementsCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("achievements", context ->
                Achievement.allEnabled(true)
                .stream().map(Achievement::alias)
                .collect(Collectors.toSet()));
    }

    private void achievementsUnlockedCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("unlocked-achievements", context -> {
            AchievementPlayer player = AchievementPlayer.of(context.getPlayer());
            if (player == null) {
                return Achievement.allEnabled().stream().map(Achievement::alias).collect(Collectors.toSet());
            }

            return player.unlockedAchievements().stream()
                    .map(PlayerAchievement::achievement)
                    .map(Achievement::alias)
                    .collect(Collectors.toSet());
        });
    }

    private void categoriesCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("categories", context -> Category.
                find.all().stream()
                .map(Category::alias)
                .collect(Collectors.toSet()));
    }

    private void categoryContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerContext(Category.class, context -> {
            String arg = context.popFirstArg();
            if (Strings.isNullOrEmpty(arg) || arg.equalsIgnoreCase("all")) return null;
            return Category.byAlias(arg).orElseThrow(
                    () -> new InvalidCommandArgument("Es wurde keine Kategorie mit dem alias " + arg + " gefunden!")
            );
        });
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
                    Optional<AchievementPlayer> achievementPlayer = AchievementPlayer.byName(arg);
                    if (achievementPlayer.isPresent()) return achievementPlayer.get();
                    player = Bukkit.getPlayerExact(arg);
                }
            }

            if (player == null) {
                throw new InvalidCommandArgument("Der Spieler " + arg + " wurde nicht gefunden.");
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
                        DataStore.class,
                        Category.class,
                        PlacedBlock.class
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

