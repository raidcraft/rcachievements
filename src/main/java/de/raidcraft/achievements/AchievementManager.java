package de.raidcraft.achievements;

import com.google.common.base.Strings;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.types.*;
import de.raidcraft.achievements.util.ConfigUtil;
import de.raidcraft.achievements.util.graphs.CycleSearch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log(topic = "RCAchievements")
@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
public final class AchievementManager {

    private final RCAchievements plugin;
    private final Map<String, Set<Map.Entry<String, ConfigurationSection>>> failedLoads = new HashMap<>();
    private final Map<String, AchievementType.Registration<?>> types = new HashMap<>();
    private final Map<UUID, AchievementContext> activeAchievements = new HashMap<>();

    AchievementManager(RCAchievements plugin) {
        this.plugin = plugin;
    }

    void registerDefaults() {

        try {
            register(new NoneAchievementType.Factory());
            register(new LocationAchievement.Factory());
            register(new MobKillAchievement.Factory());
            register(new BlockAchievement.Factory());
            register(new CombinedAchievement.Factory());
            register(new CraftAchievement.Factory());
            register(new LoginAchievement.Factory());
            register(new PlayerStatisticAchievement.Factory());
            register(new BiomeAchievement.Factory());
        } catch (TypeRegistrationException e) {
            log.severe("failed to register default types: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reload() {

        failedLoads().clear();
        loadAchievements();
        reloadAchievements();
    }

    void load() {

        loadAchievements();
    }

    void unload() {

        activeAchievements().values().forEach(AchievementContext::disable);
        activeAchievements.clear();
        types.clear();
        failedLoads.clear();
    }

    /**
     * Registers a new achievement type factor for creating and initializing achievement types.
     * <p>Create new achievement types by implementing {@link AchievementType} and providing a
     * {@link TypeFactory} that provides the meta information and can create new instances of your type.
     * <p>You can directly use the {@link #register(String, Class, Function)} method to register your type
     * without a factory.
     *
     * @param factory the factory that holds the meta information for your type
     * @param <TType> the type that is registered
     * @throws TypeRegistrationException if a type with the same identifier is already registered
     * @return the created type registration
     */
    public <TType extends AchievementType> AchievementType.Registration<TType> register(@NonNull TypeFactory<TType> factory) throws TypeRegistrationException {

        return register(factory.identifier(), factory.typeClass(), factory::create);
    }

    /**
     * Registers a new achievement type factor for creating and initializing achievement types.
     * <p>Create new achievement types by implementing {@link AchievementType} and providing a
     * {@link Function} that can create new instances of your type.
     * <p>Use the {@link #register(TypeFactory)} method to directly register an instance of your type factory.
     *
     * @param identifier the unique type identifier used inside the config to reference the type
     * @param typeClass the class of the type that is created by the factory
     * @param factory the factory that creates instances of your type
     * @param <TType> the type that is registered
     * @throws TypeRegistrationException if a type with the same identifier is already registered
     * @return the created type registration
     */
    public <TType extends AchievementType> AchievementType.Registration<TType> register(@NonNull String identifier,
                                                                                        @NonNull Class<TType> typeClass,
                                                                                        @NonNull Function<AchievementContext, TType> factory) throws TypeRegistrationException {

        AchievementType.Registration<TType> registration = new AchievementType.Registration<>(identifier.toLowerCase(), typeClass, factory);
        Optional<AchievementType.Registration<?>> existingType = registration(registration.identifier());
        if (existingType.isPresent()) {
            throw new TypeRegistrationException("Type with identifier \"" + identifier + "\" is already registered by class: "
                    + existingType.get().typeClass().getCanonicalName(), registration);
        }

        types().put(registration.identifier(), registration);
        log.info("registered achievement type \"" + registration.identifier() + "\": " + registration.typeClass().getCanonicalName());

        Set<Map.Entry<String, ConfigurationSection>> failedLoads = Objects.requireNonNullElse(
                failedLoads().remove(registration.identifier()),
                new HashSet<>()

        );
        if (!failedLoads.isEmpty()) {
            log.info("lazy loading " + failedLoads.size() + " achievements of type " + registration.identifier());
            for (Map.Entry<String, ConfigurationSection> entry : failedLoads) {
                loadAchievement(entry.getKey(), entry.getValue()).ifPresent(this::initialize);
            }
        }

        return registration;
    }

    /**
     * Unregisters the given achievement type and disables all active achievements.
     * <p>Nothing happens if no achievement type with the given class is registered.
     *
     * @param typeClass the type class that should be disabled
     * @param <TType> the type of the achievement
     */
    public <TType extends AchievementType> void unregister(Class<TType> typeClass) {

        List<AchievementType.Registration<?>> registrations = types().values().stream()
                .filter(registration -> registration.typeClass().equals(typeClass))
                .collect(Collectors.toList());

        List<AchievementContext> contexts = registrations.stream()
                .flatMap(registration -> activeAchievements.values().stream()
                        .filter(achievementContext -> achievementContext.achievement().type().equals(registration.identifier()))
                ).collect(Collectors.toList());

        for (AchievementContext context : contexts) {
            context.disable();
            activeAchievements.remove(context.achievement().id());
        }
    }

    /**
     * Unloads the given achievement disabling any active context and clears the cache.
     *
     * @param achievement the achievement that is unloaded
     */
    public void unload(@NonNull Achievement achievement) {

        active(achievement).ifPresent(AchievementContext::disable);
        activeAchievements.remove(achievement.id());
    }

    /**
     * Tries to get a registered type with the given type identifier.
     *
     * @param type the type identifier of the achievement type registration
     * @return the registered type or an empty optional
     */
    public Optional<AchievementType.Registration<?>> registration(String type) {

        if (Strings.isNullOrEmpty(type)) return Optional.empty();

        return Optional.ofNullable(types().get(type.toLowerCase()));
    }

    /**
     * Checks if the given achievement type exists and is loaded.
     *
     * @param type the achievement type key to check. can be null or empty.
     * @return true if the type exists and is loaded
     */
    public boolean hasType(String type) {

        if (Strings.isNullOrEmpty(type)) return false;

        return types().containsKey(type.toLowerCase());
    }

    /**
     * Tries to find an active achievement context for the given achievement alias.
     * <p>A context is only active if it is enabled. Contexts that are only loaded
     * are not considered active.
     *
     * @param alias the alias of the achievement
     * @return the active context or an empty optional
     */
    public Optional<AchievementContext> active(String alias) {

        if (Strings.isNullOrEmpty(alias)) return Optional.empty();

        return Achievement.byAlias(alias).flatMap(this::active);
    }

    /**
     * Tries to find an active achievement context for the given achievement id.
     * <p>A context is only active if it is enabled. Contexts that are only loaded
     * are not considered active.
     *
     * @param uuid the id of the achievement
     * @return the active context or an empty optional
     */
    public Optional<AchievementContext> active(@NonNull UUID uuid) {

        return Optional.ofNullable(activeAchievements().get(uuid));
    }

    /**
     * Tries to find an active achievement context for the given achievement.
     * <p>A context is only active if it is enabled. Contexts that are only loaded
     * are not considered active.
     *
     * @param achievement the achievement to get an active context for
     * @return the active context or an empty optional
     */
    public Optional<AchievementContext> active(@NonNull Achievement achievement) {

        return active(achievement.id());
    }

    /**
     * Initializes the achievement creating a new instance of the type,
     * loading the type and then starts listening to events.
     * <p>The {@link AchievementType} is responsible for actually awarding the
     * achievement to players when a certain event occurs. Every achievement
     * needs to be initialized for it to work.
     * <p>Load your achievement with one of the load methods in this manager and
     * then call initialize to start listening for achievement events.
     *
     * @param achievement the achievemet that should be initialized. must not be null.
     * @return the created {@link AchievementContext} if the initialization was succcessful
     */
    public Optional<AchievementContext> initialize(@NonNull Achievement achievement) {

        achievement.refresh();
        achievement.children().forEach(this::initialize);

        if (achievement.disabled()) return Optional.empty();
        if (activeAchievements.containsKey(achievement.id())) {
            return Optional.of(activeAchievements.get(achievement.id()));
        }

        return registration(achievement.type()).map(registration -> {
            AchievementContext context = AchievementContext.create(plugin, achievement, registration);
            context.enable();
            activeAchievements.put(achievement.id(), context);
            return context;
        });
    }

    /**
     * Loads all achievements from the base path provided inside the config.yml.
     * <p>{@link #initialize(Achievement)} will be called for each achievement after
     * all achievements are loaded.
     *
     * @see #loadAchievements(Path)
     */
    void loadAchievements() {

        File path = new File(plugin().getDataFolder(), plugin().pluginConfig().getAchievements());
        path.mkdirs();

        List<Achievement> fileAchievements = loadAchievements(path.toPath());

        for (List<Achievement> cycle : CycleSearch.of(Achievement.find.all()).getCycles()) {
            log.severe("Found circular parent/child achievements: " + CycleSearch.dependencyGraphToString(cycle));
            log.severe("Not enabling the achievements until the issue is resolved.");
            return;
        }

        fileAchievements.forEach(this::initialize);

        List<Achievement> achievements = Achievement.unknownSource();
        achievements.forEach(this::initialize);
        log.info("loaded " + achievements.size() + " achievements without a file config from the database.");

        if (plugin().pluginConfig().isAutoSave()) {
            achievements.stream()
                    .filter(achievement -> !achievement.isChild())
                    .forEach(achievement -> {
                File file = filePathOf(achievement);
                if (file.exists()) {
                    YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(file);
                    String id = existingConfig.getString("id");
                    if (Strings.isNullOrEmpty(id)) {
                        if (!achievement.alias().equalsIgnoreCase(existingConfig.getString("alias"))) {
                            log.warning("cannot save " + achievement + " to disk. A file already exists with an empty id but different alias: " + file.getAbsolutePath());
                            return;
                        }
                    } else {
                        try {
                            if (!achievement.id().equals(UUID.fromString(id))) {
                                log.warning("cannot save " + achievement + " to disk. A file already exists with a different id: " + file.getAbsolutePath());
                                return;
                            }
                        } catch (Exception e) {
                            log.warning("cannot save " + achievement + " to disk. A file already exists without a valid id: " + file.getAbsolutePath());
                            return;
                        }
                    }
                }

                try {
                    achievement.toConfig().save(file);
                    log.info("saved database achievement " + achievement + " to disk: " + file.getAbsolutePath());
                } catch (IOException e) {
                    log.severe("failed to save achievement configuration of " + achievement + " to " + file.getAbsolutePath() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Gets all active achievements registered in this manager and calls
     * {@link AchievementContext#reload()} on the underlying context.
     */
    void reloadAchievements() {

        activeAchievements().values().forEach(AchievementContext::reload);
    }

    /**
     * Loads all achievements recursivlely inside the given path.
     * <p>The default alias for each achievement will be computed from the
     * path and file name: {@link ConfigUtil#getFileIdentifier(Path, File)}.
     * <p>The file maybe updated with an achievement id if the loading succeeded
     * and the id changed.
     * <p>The achievement will be stored in the {@link #failedLoads()} map by its
     * type if the loading fails. Use it to lazy load achievements after their type
     * was registered.
     * <p>{@link #initialize(Achievement)} needs to be called afterwards to
     * initialize the underlying type and start listening for achievement events.
     *
     * @param path the root path to start loading achievements from
     * @return a list of loaded achievements
     * @see #loadAchievement(Path, File)
     */
    public List<Achievement> loadAchievements(@NonNull Path path) {

        try {
            Files.createDirectories(path);
            List<File> files = Files.find(path, Integer.MAX_VALUE,
                    (file, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))
                    .collect(Collectors.toList());


            int fileCount = files.size();
            List<Achievement> achievements = files.stream()
                    .map(file -> loadAchievement(path, file))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            log.info("Loaded " + achievements.size() + "/" + fileCount + " achievements from " + path);
            return achievements;
        } catch (IOException e) {
            log.severe("unable to achievements from " + path + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Tries to load an achievement from the given file.
     * <p>The default alias of the achievement will be computed from the
     * base path and file name: {@link ConfigUtil#getFileIdentifier(Path, File)}.
     * <p>The file maybe updated with an achievement id if the loading succeeded
     * and the id changed.
     * <p>The achievement will be stored in the {@link #failedLoads()} map by its
     * type if the loading fails. Use it to lazy load achievements after their type
     * was registered.
     * <p>{@link #initialize(Achievement)} needs to be called afterwards to
     * initialize the underlying type and start listening for achievement events.
     *
     * @param path the path where the config is loaded from
     * @param file the configuration file
     * @return the loaded achievement or an empty optional
     * @see #loadAchievement(String, ConfigurationSection)
     */
    public Optional<Achievement> loadAchievement(@NonNull Path path, @NonNull File file) {

        if (!file.exists() || !(file.getName().endsWith(".yml") && !file.getName().endsWith(".yaml"))) {
            return Optional.empty();
        }

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            Optional<Achievement> achievement = loadAchievement(config.getString("alias", ConfigUtil.getFileIdentifier(path, file)), config);
            config.save(file);
            achievement.ifPresentOrElse(a -> {
                        a.refresh();
                        a.source(file.getAbsolutePath()).save();
                        log.info("loaded achievement \"" + a.alias() + "\" (" + a.type() + ") from: " + file);
                    },
                    () -> log.warning("failed to load achievement from config: " + file));
            return achievement;
        } catch (IOException | InvalidConfigurationException e) {
            log.severe("unable to load achievement config " + file.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Tries to load an achievement with the given alias and config.
     * <p>This method will simply synchronize the provided config with
     * the database entry of the achievement. The id property of the provided
     * config object is updated with the id from the database.
     * A save operation on the underlying configuration file should be called afterwards.
     * <p>{@link #initialize(Achievement)} needs to be called afterwards to
     * initialize the underlying type and start listening for achievement events.
     * <p>Will return an empty optional in one of the folowing cases:
     * <ul>
     *     <li>The config is null
     *     <li>The type set in the config does not exist
     *     <li>A database error occured
     * </ul>
     *
     * @param alias the alias of the achievement. must not be null or empty.
     * @param config the config of the achievement. can be null, but then an empty returns.
     * @return the loaded achievement
     */
    public Optional<Achievement> loadAchievement(@NonNull String alias, ConfigurationSection config) {

        if (config == null) return Optional.empty();
        String type = registration(config);
        if (!hasType(type)) {
            log.warning("uknown achievement type \"" + type + "\" in achievement config of: " + alias);
            loadFailed(alias, config);
            return Optional.empty();
        }

        Achievement achievement;
        String idString = config.getString("id");
        if (!Strings.isNullOrEmpty(idString)) {
            try {
                UUID id = UUID.fromString(idString);

                Optional<Achievement> existing = Achievement.byAlias(alias);
                if (Achievement.byId(id).isEmpty() && existing.isPresent()) {
                    log.severe("an achievement with the same alias \"" + alias + "\" but a different id already exists: " + existing.get().id());
                    return Optional.empty();
                }

                config.set("category", config.getString("category", plugin.pluginConfig().getUncategorizedAlias()));
                achievement = Achievement.load(id, alias, config);
            } catch (IllegalArgumentException e) {
                log.severe(idString + " is not a valid UUID inside achievement config of: " + alias);
                return Optional.empty();
            }
        } else {
            achievement = Achievement.load(alias, config);
        }

        config.set("id", achievement.id().toString());

        ConfigurationSection childs = config.getConfigurationSection("childs");
        if (childs != null) {
            for (String childKey : childs.getKeys(false)) {
                ConfigurationSection section = childs.getConfigurationSection(childKey);
                if (section != null) {
                    section.set("parent", achievement.id().toString());
                    section.set("type", section.getString("type", achievement.type()));
                    loadAchievement(section.getString("alias", achievement.alias() + ":" + childKey), section);
                }
            }
        }

        return Optional.of(achievement);
    }

    /**
     * Finds the file that represents the location of the given achievement.
     * <p>The config may or may not exist, but the path to it will always be created.
     * <p>Use the {@link Achievement#toConfig()} and {@link YamlConfiguration#save(File)} methods
     * to store the given achievement in the configuration returned from this.
     *
     * @param achievement the achievement to get the config file for
     * @return the config file of the given achievement. may not exist yet.
     */
    public File filePathOf(Achievement achievement) {

        File baseDir = new File(plugin.getDataFolder(), plugin.pluginConfig().getAchievements());
        File file = new File(baseDir, achievement.alias() + ".yml");
        file.getParentFile().mkdirs();

        return file;
    }

    private void loadFailed(String alias, ConfigurationSection config) {

        String type = registration(config);
        Set<Map.Entry<String, ConfigurationSection>> entries = failedLoads().getOrDefault(type, new HashSet<>());
        entries.add(Map.entry(alias, config));
        failedLoads().put(type, entries);
    }

    private String registration(@NonNull ConfigurationSection config) {

        return config.getString("type", plugin().pluginConfig().getDefaultType());
    }
}
