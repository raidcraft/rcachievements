package de.raidcraft.achievements;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import io.ebean.Model;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;
import java.util.function.Consumer;

import static de.raidcraft.achievements.AchievementMockFactory.TYPE;

@Getter
@Accessors(fluent = true)
public class TestBase {

    protected ServerMock server;
    protected RCAchievements plugin;
    protected AchievementManager manager;
    protected PlayerMock bukkitPlayer;
    protected AchievementPlayer player;
    protected AchievementMockFactory factory;

    @SneakyThrows
    @BeforeEach
    protected void setUp() {

        server = MockBukkit.mock();
        plugin = MockBukkit.load(RCAchievements.class);
        manager = plugin.achievementManager();

        bukkitPlayer = server.addPlayer();
        player = AchievementPlayer.of(bukkitPlayer);
        factory = new AchievementMockFactory();
        manager.register(factory);
    }

    @AfterEach
    protected void tearDown() {

        PlayerAchievement.find.all().forEach(Model::delete);
        Achievement.find.query().where().isNull("parent").findList().forEach(Model::delete);
        MockBukkit.unmock();
    }

    protected Optional<Achievement> loadAchievement(String alias, Consumer<ConfigurationSection> cfg) {

        MemoryConfiguration config = new MemoryConfiguration();
        config.set("type", TYPE);
        cfg.accept(config);
        return manager.loadAchievement(alias, config);
    }

    protected Optional<Achievement> loadAchievement(String alias) {

        return loadAchievement(alias, configurationSection -> {});
    }

    protected Achievement loadAchievement() {

        return loadAchievement(RandomStringUtils.randomAlphabetic(20)).get();
    }

    protected DefaultAchievementContext context(Achievement achievement) {

        return (DefaultAchievementContext) AchievementContext.create(plugin,
                achievement,
                manager.registration(achievement.type()).get()
        );
    }
}
