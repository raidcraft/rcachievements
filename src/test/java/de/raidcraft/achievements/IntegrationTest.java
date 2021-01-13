package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.types.LocationAchievement;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest extends TestBase {

    @Nested
    @DisplayName("Commands")
    class Commands {

        private Player player;
        private Achievement achievement;

        @BeforeEach
        void setUp() {
            player = server().addPlayer("foobar");
            player.setOp(true);
            achievement = loadAchievement();
        }

        @Nested
        @DisplayName("/rca:admin")
        class AdminCommands {

            @Nested
            @DisplayName("add")
            class add {

                @Test
                @DisplayName("should work")
                void shouldWork() {

                    server().dispatchCommand(player,"rca:admin add " + player.getName() + " " + achievement.alias());
                    PlayerAchievement playerAchievement = PlayerAchievement.of(achievement, AchievementPlayer.of(player));
                    assertThat(playerAchievement)
                            .extracting(PlayerAchievement::isUnlocked)
                            .isEqualTo(true);
                }
            }

            @Nested
            @DisplayName("create")
            class create {

                @Test
                @DisplayName("should set achievement type to location")
                void shouldSetLocationType() {

                    server().dispatchCommand(player, "rca:admin create loc foobar 1");

                    assertThat(Achievement.byAlias("foobar"))
                            .isPresent().get()
                            .extracting(Achievement::type)
                            .isEqualTo(LocationAchievement.TYPE);
                }
            }
        }
    }
}
