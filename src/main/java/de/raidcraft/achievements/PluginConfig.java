package de.raidcraft.achievements;

import de.exlll.configlib.annotation.Comment;
import de.exlll.configlib.annotation.ConfigurationElement;
import de.exlll.configlib.configs.yaml.BukkitYamlConfiguration;
import de.exlll.configlib.format.FieldNameFormatters;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

import static de.raidcraft.achievements.Constants.DEFAULT_TYPE;

@Getter
@Setter
public class PluginConfig extends BukkitYamlConfiguration {

    @Comment("The base path of the achievement configs.")
    private String achievements = "achievements/";
    @Comment("The achievement type that should be used if none is set in the config.")
    private String defaultType = DEFAULT_TYPE;
    @Comment("Set to false to disable the global achievement broadcast.")
    private boolean broadcast = true;
    private long playerListUpdateInterval = 200L;
    @Comment("The time in ticks how long secret achievement broadcast should be delayed.")
    private long secretBroadcastDelay = 12000L;
    private DatabaseConfig database = new DatabaseConfig();

    public PluginConfig(Path path) {

        super(path, BukkitYamlProperties.builder().setFormatter(FieldNameFormatters.LOWER_UNDERSCORE).build());
    }

    @ConfigurationElement
    @Getter
    @Setter
    public static class DatabaseConfig {

        private String username = "sa";
        private String password = "sa";
        private String driver = "h2";
        private String url = "jdbc:h2:~/achievements.db";
    }
}
