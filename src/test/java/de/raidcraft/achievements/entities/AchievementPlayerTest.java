package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.Constants;
import de.raidcraft.achievements.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}