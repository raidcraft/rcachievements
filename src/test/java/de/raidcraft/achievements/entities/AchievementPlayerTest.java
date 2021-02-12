package de.raidcraft.achievements.entities;

import be.seeseemelk.mockbukkit.WorldMock;
import de.raidcraft.achievements.Constants;
import de.raidcraft.achievements.TestBase;
import org.bukkit.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementPlayerTest extends TestBase {

    @Nested
    @DisplayName("canView(...)")
    class canView {

        @Test
        @DisplayName("should not allow viewing hidden")
        void shouldNotAllowViewingHidden() {

            Achievement achievement = loadAchievement().hidden(true);

            assertThat(player().canView(achievement)).isFalse();
        }

        @Test
        @DisplayName("should allow view if unlocked")
        void shouldViewIfUnlocked() {

            Achievement achievement = loadAchievement().hidden(true);
            player().add(achievement);

            assertThat(player().canView(achievement)).isTrue();
        }

        @Test
        @DisplayName("should allow viewing if player has permission")
        void shouldAllowViewingIfPlayerHasPermission() {

            bukkitPlayer().addAttachment(plugin(), Constants.SHOW_HIDDEN, true);

            assertThat(player().canView(loadAchievement())).isTrue();
        }

        @Test
        @DisplayName("should allow viewing if not hidden")
        void shouldAllowViewingIfNotHidden() {

            Achievement achievement = loadAchievement().hidden(false);

            assertThat(player().canView(achievement)).isTrue();
        }
    }

    @Nested
    @DisplayName("canViewDetails(...)")
    class canViewDetails {

        @Test
        @DisplayName("should allow viewing if not secret or hidden")
        void shouldAllowViewingIfNotSecretOrHidden() {

            Achievement achievement = loadAchievement().secret(false).hidden(false);

            assertThat(player().canViewDetails(achievement)).isTrue();
        }

        @Test
        @DisplayName("should allow viewing if player has bypass permission")
        void shouldAllowViewingIfPlayerHasBypassPermission() {

            bukkitPlayer().addAttachment(plugin(), Constants.SHOW_SECRET, true);
            Achievement achievement = loadAchievement().secret(true);

            assertThat(player().canViewDetails(achievement)).isTrue();
        }

        @Test
        @DisplayName("should not allow viewing if secret")
        void shouldNotAllowViewingIfSecret() {

            Achievement achievement = loadAchievement().secret(true);

            assertThat(player().canViewDetails(achievement)).isFalse();
        }

        @Test
        @DisplayName("should allow viewing if unlocked")
        void shouldAllowViewingIfUnlocked() {

            Achievement achievement = loadAchievement().secret(true);
            player().add(achievement);

            assertThat(player().canViewDetails(achievement)).isTrue();
        }

        @Test
        @DisplayName("should not allow viewing if hidden")
        void shouldNotAllowViewingIfHidden() {

            Achievement achievement = loadAchievement().secret(false).hidden(true);

            assertThat(player().canViewDetails(achievement)).isFalse();
        }
    }
    
    @Nested
    @DisplayName("canUnlock(...)")
    class CanUnlock {

        @Test
        @DisplayName("should allow unlocking default achievement")
        void shouldAllowUnlockingDefaultAchievement() {

            assertThat(player().canUnlock(loadAchievement())).isTrue();
        }

        @Test
        @DisplayName("should not allow unlocking unlocked achievements")
        void shouldNotUnlockUnlockedAchievements() {

            Achievement achievement = loadAchievement();
            player().add(achievement);

            assertThat(player().canUnlock(achievement)).isFalse();
        }

        @Test
        @DisplayName("should not allow unlocking restricted achievements")
        void shouldNotAllowUnlockingRestrictedAchievements() {

            Achievement achievement = loadAchievement().restricted(true);

            assertThat(player().canUnlock(achievement)).isFalse();
        }

        @Test
        @DisplayName("should allow unlocking restricted achievement with permission")
        void shouldAllowUnlockingRestrictedAchievementWithPermission() {

            bukkitPlayer().addAttachment(plugin(), Constants.ACHIEVEMENT_PERMISSION_PREFIX + "foobar", true);
            Achievement achievement = loadAchievement("foobar").get().restricted(true);

            assertThat(player().canUnlock(achievement)).isTrue();
        }

        @Test
        @DisplayName("should not allow unlocking achievement in different world")
        void shouldNotAllowUnlockingAchievementsInDifferentWorld() {

            Achievement achievement = loadAchievement().worlds(Collections.singletonList("foobar"));

            assertThat(player().canUnlock(achievement)).isFalse();
        }

        @Test
        @DisplayName("should allow unlocking achievement if player is in world")
        void shouldAllowUnlockingAchievementIfPlayerIsInWorld() {

            Achievement achievement = loadAchievement().worlds(Collections.singletonList("foobar"));
            WorldMock foobar = server().addSimpleWorld("foobar");
            bukkitPlayer().setLocation(new Location(foobar, 0, 64, 0));

            assertThat(player().canUnlock(achievement)).isTrue();
        }
    }
}