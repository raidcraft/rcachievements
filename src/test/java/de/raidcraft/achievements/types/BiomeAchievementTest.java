package de.raidcraft.achievements.types;

import de.raidcraft.achievements.TestBase;
import org.bukkit.block.Biome;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

@SuppressWarnings("ALL")
class BiomeAchievementTest extends TestBase {

    private BiomeAchievement achievement;

    @Override
    @BeforeEach
    protected void setUp() {

        super.setUp();

        achievement = new BiomeAchievement(context(loadAchievement()));
    }

    @Test
    @DisplayName("should load player biomes on login")
    void shouldLoadPlayerBiomesOnLogin() {

        achievement.store(player()).set(BiomeAchievement.VISITED_BIOMES, Set.of(
                Biome.MOUNTAIN_EDGE.getKey().toString()
        ));

        achievement.onPlayerJoin(new PlayerJoinEvent(bukkitPlayer(), ""));

        assertThat(achievement.playerVisitedBiomesMap)
                .containsKey(bukkitPlayer().getUniqueId())
                .containsValue(Set.of(Biome.MOUNTAIN_EDGE));
    }

    @Test
    @DisplayName("should store player biomes on logout")
    void shouldStorePlayerBiomesOnLogout() {

        achievement.playerVisitedBiomesMap.put(player().id(), Set.of(
                Biome.JUNGLE,
                Biome.STONE_SHORE
        ));

        achievement.onPlayerQuit(new PlayerQuitEvent(bukkitPlayer(), ""));

        Optional<Set> set = achievement.store(player()).get(BiomeAchievement.VISITED_BIOMES, Set.class);
        assertThat(set).isPresent();

        assertThat((Set<String>) set.get())
                .contains(
                        Biome.JUNGLE.getKey().toString(),
                        Biome.STONE_SHORE.getKey().toString()
                );
    }
}