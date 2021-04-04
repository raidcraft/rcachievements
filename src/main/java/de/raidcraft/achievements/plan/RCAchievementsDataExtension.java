package de.raidcraft.achievements.plan;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.TableProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@PluginInfo(
        name = "RCAchievements",
        iconName = "trophy",
        iconFamily = Family.SOLID,
        color = Color.DEEP_ORANGE
)
public class RCAchievementsDataExtension implements DataExtension {

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN,
                CallEvents.PLAYER_LEAVE,
                CallEvents.SERVER_EXTENSION_REGISTER,
                CallEvents.SERVER_PERIODICAL
        };
    }

    @TableProvider()
    public Table achievementStats() {

        Table.Factory factory = Table.builder()
                .columnOne("Achievement", Icon.called("book").of(Color.AMBER).of(Family.SOLID).build())
                .columnTwo("Description", Icon.called("sticky-note").of(Color.DEEP_ORANGE).of(Family.SOLID).build())
                .columnThree("Players Unlocked", Icon.called("users").of(Color.LIGHT_GREEN).of(Family.SOLID).build())
                .columnFour("Secret", Icon.called("mask").of(Color.GREY).of(Family.SOLID).build())
                .columnFive("Hidden", Icon.called("eye-slash").of(Color.GREY).of(Family.SOLID).build());

        long allPlayers = AchievementPlayer.find.all().stream().count();
        NumberFormat defaultFormat = NumberFormat.getPercentInstance();
        defaultFormat.setMinimumFractionDigits(1);

        for (Achievement achievement : Achievement.allEnabled().stream()
                .sorted(Comparator.comparing(Achievement::name))
                .collect(Collectors.toList())) {
            long count = achievement.playerAchievements().stream().filter(PlayerAchievement::isUnlocked).count();
            factory.addRow(
                    achievement.name() + " (" + achievement.alias() + ")",
                    achievement.description(),
                    count + " (" + defaultFormat.format(allPlayers / (count * 1.0)),
                    achievement.secret() ? "Yes" : "No",
                    achievement.hidden() ? "Yes" : "No"
            );
        }

        return factory.build();
    }

    @NumberProvider(
            text = "Achievements",
            description = "The number of achievements the player unlocked.",
            priority = 3,
            iconName = "trophy",
            iconFamily = Family.SOLID,
            iconColor = Color.DEEP_ORANGE,
            showInPlayerTable = true
    )
    public long level(UUID playerUUID) {

        return Optional.ofNullable(AchievementPlayer.find.byId(playerUUID))
                .map(player -> player.unlockedAchievements().size())
                .orElse(0);
    }
}
