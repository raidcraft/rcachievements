package de.raidcraft.achievements.entities;

import com.google.common.base.Strings;
import de.raidcraft.achievements.entities.query.QAchievement;
import io.ebean.Finder;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.ebean.text.json.EJson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static de.raidcraft.achievements.AchievementsPlugin.TABLE_PREFIX;

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
@Table(name = TABLE_PREFIX + "achievements")
public class Achievement extends BaseEntity {

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

        return new QAchievement().where()
                .alias.ieq(alias)
                .findOneOrEmpty();
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

        Achievement achievement = find.byId(uuid);

        if (achievement == null) {
            achievement = byAlias(alias).orElse(new Achievement(uuid, alias));
        }

        return achievement.load(config);
    }

    /**
     * A unique key of the achievement used in configurations and references.
     */
    @Index(unique = true)
    private String alias;
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
    private boolean secret = true;
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

    @DbJson
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    @Column(name = "config")
    private Map<String, Object> _config = new HashMap<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    @Transient
    private transient boolean loaded = false;

    Achievement(UUID uuid, String alias) {

        this.id(uuid);
        this.alias(alias);
        this.name(alias);
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

        this.name(config.getString("name", name()));
        this.description(config.getString("description", description()));
        this.enabled(config.getBoolean("enabled", enabled()));
        this.secret(config.getBoolean("secret", secret()));
        this.hidden(config.getBoolean("hidden", hidden()));
        this.broadcast(config.getBoolean("broadcast", broadcast()));
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
}
