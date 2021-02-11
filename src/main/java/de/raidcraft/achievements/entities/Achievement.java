package de.raidcraft.achievements.entities;

import com.google.common.base.Strings;
import de.raidcraft.achievements.Constants;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.annotation.DbDefault;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.ebean.text.json.EJson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import net.silthus.ebean.BaseEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static de.raidcraft.achievements.Constants.TABLE_PREFIX;

/**
 * The achievement entity holds all configuration information about an achievement.
 * <p>It is loaded by the plugin from config files or configured directly in the database.
 * <p>{@link AchievementPlayer}s can be awarded an achievement which are then linked as {@link PlayerAchievement}s.
 * <p>Use the {@link #byAlias(String)} or {@link #byId(UUID)} methods to get existing achievements.
 * The {@link #load(String, ConfigurationSection)} and {@link #load(UUID, String, ConfigurationSection)} methods are
 * for loading achievements from configuration files by its alias or id.
 */
@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Log(topic = "RCAchievements")
@Table(name = TABLE_PREFIX + "achievements")
public class Achievement extends BaseEntity implements Comparable<Achievement> {

    public static final Finder<UUID, Achievement> find = new Finder<>(Achievement.class);

    static {
        try {
            EJson.write(new Object());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Tries to find the given achievement by its id.
     *
     * @param uuid the id of the achievement
     * @return the achievement or an empty optional
     */
    public static Optional<Achievement> byId(@NonNull UUID uuid) {

        return Optional.ofNullable(find.byId(uuid));
    }

    /**
     * Tries to find an achievement with the given alias.
     * <p>The alias can be null or empty, but then an empty optional will be returned.
     *
     * @param alias the alias of the achievement. can be null or empty.
     * @return the achievement if it exists
     */
    public static Optional<Achievement> byAlias(String alias) {

        if (Strings.isNullOrEmpty(alias)) return Optional.empty();

        return find.query().where()
                .ieq("alias", alias)
                .findOneOrEmpty();
    }

    /**
     * @return all achievements that are enabled and have a null source
     */
    public static List<Achievement> unknownSource() {

        return find.query().where()
                .isNull("source")
                .eq("enabled", true)
                .findList();
    }

    public static List<Achievement> uncategorized() {

        return find.query()
                .where().isNull("category")
                .findList();
    }

    /**
     * @return a list of all enabled and non hidden achievements
     */
    public static List<Achievement> allEnabled() {

        return allEnabled(false);
    }

    /**
     * Gets a list of all enabled achievements.
     *
     * @param hidden set to true to include hidden achievements
     * @return a list of all enabled achievements
     */
    public static List<Achievement> allEnabled(boolean hidden) {

        ExpressionList<Achievement> query = find.query().where()
                .eq("enabled", true);
        if (!hidden) {
            return query.and()
                    .eq("hidden", false)
                    .findList();
        }

        return query.findList();
    }

    /**
     * Loads an achievement with the given alias and config.
     * <p>Will create a new achievement if none is found by the given alias.
     * <p>If one is found the config of it will be updated.
     *
     * @param alias the alias of the achievement
     * @param config the config of the achievement
     * @return the created or updated achievement
     */
    public static Achievement load(@NonNull String alias, @NonNull ConfigurationSection config) {

        return load(UUID.randomUUID(), alias, config);
    }

    /**
     * Loads an achievement with the given id from the provided config.
     * <p>Will retrieve the existing achievement and update the config
     * if one is found with the given id or alias.
     * <p>The lookup will try to find an achievement with the id first
     * and then try the alias.
     *
     * @param uuid the id of the achievement
     * @param alias the alias of the achievement
     * @param config the config of the achievement
     * @return the created or updated achievement with a new id if it was loaded by its alias and they differ.
     */
    public static Achievement load(@NonNull UUID uuid, @NonNull String alias, @NonNull ConfigurationSection config) {

        return byId(uuid)
                .or(() -> byAlias(alias))
                .orElse(new Achievement(uuid, alias))
                .load(config);
    }

    /**
     * Creates a new achievement with the given alias and provided config.
     * <p>The config builder is only for the achievement type config in the {@code with} section.
     * Use the fluent builder on the returned achievement to set the other properties.
     * <p>{@link #save()} needs to be called when the achievement has all properties set
     * or else it will not persist in the database.
     *
     * @param alias the alias of the achievement
     * @param config the config that provides the detailed configuration about the implementing achievement type
     * @return the created achievement. {@link #save()} needs to be called to persist it.
     * @throws IllegalArgumentException if an achievement with the same alias exists.
     *         <p>Check the existence of an achievement with {@link #byAlias(String)}
     *         before calling this method.
     */
    public static Achievement create(String alias, ConfigurationSection config) {

        if (byAlias(alias).isPresent()) {
            throw new IllegalArgumentException("An achievement with the same alias \"" + alias + "\" already exists!");
        }

        MemoryConfiguration root = new MemoryConfiguration();
        root.createSection("with", config.getValues(true));

        Achievement achievement = new Achievement(UUID.randomUUID(), alias);
        achievement.updateConfig(root);

        return achievement;
    }

    /**
     * Creates a new achievement with the given alias and provided config.
     * <p>The config builder is only for the achievement type config in the {@code with} section.
     * Use the fluent builder on the returned achievement to set the other properties.
     * <p>{@link #save()} needs to be called when the achievement has all properties set
     * or else it will not persist in the database.
     *
     * @param alias the alias of the achievement
     * @param builder the config builder that provides the detailed configuration about the implementing achievement type
     * @return the created achievement. {@link #save()} needs to be called to persist it.
     * @throws IllegalArgumentException if an achievement with the same alias exists.
     *         <p>Check the existence of an achievement with {@link #byAlias(String)}
     *         before calling this method.
     */
    public static Achievement create(String alias, Consumer<ConfigurationSection> builder) {

        MemoryConfiguration config = new MemoryConfiguration();
        builder.accept(config);

        return create(alias, config);
    }

    /**
     * A unique key of the achievement used in configurations and references.
     */
    @Index(unique = true)
    private String alias;

    /**
     * The type key of the achievement that determins the underlying implementation.
     * <p>The type must be registered on load or else the achievement is not enabled.
     */
    private String type = Constants.DEFAULT_TYPE;

    /**
     * The friendly name of the achievement that is displayed to the player.
     */
    private String name;

    /**
     * A detailed description about the achievement.
     * <p>Can be hidden with the {@link #secret()} or {@link #hidden()} property.
     */
    private String description = "";
    /**
     * If false the achievement is disabled in the config
     * and should be hidden from all players.
     */
    private boolean enabled = true;
    /**
     * Secret achievements hide their description to players that have not unlocked it.
     */
    private boolean secret = false;
    /**
     * Hidden achievements are only visible to players that unlocked them.
     */
    private boolean hidden = false;
    /**
     * If true the achievement will be broadcast to all online players.
     * <p>If it is hidden only the players that already have it will receive the broadcast.
     * <p>If it is secret the description will be obfuscated to all players that do not have it.
     */
    private boolean broadcast = true;
    /**
     * Restricted is true if a permission is required to obtain this achievement.
     * <p>This is useful for testing new achievements without giving every player access to it.
     */
    private boolean restricted = false;
    /**
     * If true the player will receive global rewards and the rewards of this achievement.
     */
    @DbDefault("true")
    private boolean globalRewards = true;
    /**
     * The path to the config file that loaded the achievement
     * or null if the achievement was created from code.
     */
    private String source;
    /**
     * The parent achievement of this achievement.
     * May be null.
     */
    @ManyToOne
    private Achievement parent;
    /**
     * Every achievement can have an optional category to cluster the achievements.
     */
    @ManyToOne
    private Category category;
    /**
     * A list of children achievements.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Achievement> children = new ArrayList<>();

    /**
     * The serialized configuration used to load the properties of this achievement.
     */
    @DbJson
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    @Column(name = "config")
    private Map<String, Object> _config = new HashMap<>();
    @DbJson
    @DbDefault("[]")
    private List<String> rewards = new ArrayList<>();
    /**
     * The persistent meta data store of this achievement.
     */
    @Setter(AccessLevel.PRIVATE)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private DataStore data = new DataStore();
    @Setter(AccessLevel.PRIVATE)
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PlayerAchievement> playerAchievements = new ArrayList<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    @Transient
    private transient boolean loaded = false;

    Achievement(UUID uuid, String alias) {

        this.id(uuid);
        this.alias(alias);
        this.name(alias);
    }

    Achievement(String alias) {
        this.alias(alias);
        this.name(alias);
    }

    /**
     * @return true if the achievement is disabled and should not be loaded
     */
    public boolean disabled() {

        return !enabled();
    }

    /**
     * @return true if this achievement is a child of another achievement.
     *         <p>True implies that {@link #parent()} is not null.
     */
    public boolean isChild() {

        return parent() != null;
    }

    /**
     * @return true if this achievement is the parent of at least one other achievement
     */
    public boolean isParent() {

        return !children().isEmpty();
    }

    /**
     * Constructs a configuration from the serialized configuration in the database.
     * <p>It will also contain the achievement config under the "with" key.
     * <p>Use the {@link #achievementConfig()} method if you want to retrieve it directly.
     *
     * @return a configuration mapped to the database config
     */
    public ConfigurationSection config() {

        MemoryConfiguration config = new MemoryConfiguration();
        for (Map.Entry<String, Object> entry : _config().entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        return config;
    }

    /**
     * Loads the configuration section that holds to config information
     * for loading and configuring the indidivual achievement.
     * <p>A new section will be created if none exists. Null will never be returned.
     *
     * @return the config of the achievement. never null.
     */
    public ConfigurationSection achievementConfig() {

        ConfigurationSection config = config();
        return Objects.requireNonNullElse(config.getConfigurationSection("with"), config.createSection("with"));
    }

    /**
     * Adds this achievement to the given player, unlocking it if not unlocked.
     * <p>Will do nothing and return the existing achievement if it is already unlocked.
     *
     * @param player the player to add the achievement to
     * @return true if the achievement was unlocked or is already unlocked.
     *         <p>false if the unlock failed, e.g. a cancelled event
     * @see PlayerAchievement#unlock()
     */
    public boolean addTo(AchievementPlayer player) {

        return PlayerAchievement.of(this, player).unlock();
    }

    /**
     * Removes this achievement from the player if the achievement is unlocked.
     *
     * @param player the player to remove the achievement from
     */
    public void removeFrom(AchievementPlayer player) {

        PlayerAchievement.of(this, player).delete();
    }

    /**
     * Serializes this achievement as a configuration that can be saved to disk.
     *
     * @return the new serialized configuration file. use {@link YamlConfiguration#save(File)} to save the config.
     */
    public YamlConfiguration toConfig() {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", id().toString());
        config.set("type", type());
        config.set("name", name());
        config.set("description", description());
        config.set("enabled", enabled());
        config.set("secret", secret());
        config.set("hidden", hidden());
        config.set("broadcast", broadcast());
        if(parent() != null) config.set("parent", parent().id().toString());
        if (category() != null) config.set("category", category().alias());
        for (Map.Entry<String, Object> entry : config().getValues(true).entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection childs = config.createSection("childs");
        for (Achievement child : children()) {
            childs.set(child.alias(), child.toConfig());
        }

        return config;
    }

    @PostLoad
    void onPostLoad() {

        load(config(), true);
    }

    Achievement load(ConfigurationSection config) {

        load(updateConfig(config), true);

        save();

        return this;
    }

    private void load(ConfigurationSection config, boolean force) {

        if (!force && loaded()) return;

        String parent = config.getString("parent");
        if (!Strings.isNullOrEmpty(parent)) {
            try {
                Achievement parentAchievement = Achievement.byAlias(parent)
                        .or(() -> Achievement.byId(UUID.fromString(parent)))
                        .orElse(null);
                if (!this.equals(parentAchievement)) {
                    parent(parentAchievement);

                    ConfigurationSection parentSkillConfig = parent().achievementConfig();
                    for (String key : parentSkillConfig.getKeys(true)) {
                        if (!config.isSet("with." + key)) {
                            config.set("with." + key, parentSkillConfig.get(key));
                        }
                    }

                    updateConfig(config);
                } else {
                    parent(null);
                }
            } catch (IllegalArgumentException e) {
                log.severe("the parent of " + alias() + "(" + id() + ") was not found: " + parent);
                e.printStackTrace();
            }
        }

        this.name(config.getString("name", isChild() ? parent().name() : name()));
        this.type(config.getString("type", isChild() ? parent().type() : type()));
        this.description(config.getString("description", isChild() ? parent().description() : description()));
        this.enabled(config.getBoolean("enabled", isChild() ? parent().enabled() : enabled()));
        this.secret(config.getBoolean("secret", isChild() ? parent().secret() : secret()));
        this.hidden(config.getBoolean("hidden", isChild() ? parent().hidden() : hidden()));
        this.broadcast(config.getBoolean("broadcast", isChild() ? parent().broadcast() : broadcast()));
        this.restricted(config.getBoolean("restricted", isChild() ? parent().restricted() : restricted()));
        this.globalRewards(config.getBoolean("global_rewards", isChild() ? parent().globalRewards() : globalRewards()));
        Category.byAliasOrId(config.getString("alias")).ifPresent(this::category);
        if (config.isSet("rewards")) {
            rewards(config.getStringList("rewards"));
        } else if (isChild()) {
            rewards(parent().rewards());
        }
    }

    private ConfigurationSection updateConfig(ConfigurationSection config) {

        HashMap<String, Object> cfg = new HashMap<>();
        config.getKeys(true)
                .stream()
                .filter(key -> !config.isConfigurationSection(key))
                .forEach(key -> cfg.put(key, config.get(key)));

        this._config(cfg);

        return config;
    }

    @Override
    public int compareTo(@NonNull Achievement o) {

        if (o.equals(this)) return 0;

        return name().compareTo(o.name());
    }

    @Override
    public String toString() {

        return alias() + " (" + id() + ")";
    }
}
