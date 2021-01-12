package de.raidcraft.achievements.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;

import java.util.function.Function;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Constants.PERMISSION_PREFIX;
import static de.raidcraft.achievements.Constants.SHOW_HIDDEN;
import static de.raidcraft.achievements.Messages.achievementInfo;
import static de.raidcraft.achievements.Messages.send;

@CommandAlias("rca|achievements|rcachievements")
public class PlayerCommands extends BaseCommand {

    public static final Function<Achievement, String> INFO = (achievement) -> "/rcachievements info " + achievement.id().toString();
    public static final Function<Integer, String> LIST = (page) -> "/rcachievements list " + page;

    private final RCAchievements plugin;

    public PlayerCommands(RCAchievements plugin) {
        this.plugin = plugin;
    }

    @Subcommand("info")
    @CommandCompletion("@achievements")
    @CommandPermission(PERMISSION_PREFIX + "achievement.info")
    @Description("Shows the information about an achievement.")
    public void info(@Conditions("visible") Achievement achievement) {

        send(getCurrentCommandIssuer(), achievementInfo(achievement));
    }

    @Default
    @Subcommand("list")
    @CommandAlias("erfolge")
    @CommandCompletion("* MY|ALL @players")
    @CommandPermission(PERMISSION_PREFIX + "achievement.list")
    public void list(@Default("1") int page, @Default("MY") @Values("MY|ALL") String mode, @Conditions("self") AchievementPlayer player) {

        switch (mode.toUpperCase()) {
            case "MY":
                Messages.list(player, player.unlockedAchievements().stream().map(PlayerAchievement::achievement).collect(Collectors.toList()), page)
                        .forEach(component -> send(getCurrentCommandIssuer(), component));
                break;
            case "ALL":
                Messages.list(player, Achievement.allEnabled(getCurrentCommandIssuer().hasPermission(SHOW_HIDDEN)), page)
                        .forEach(component -> send(getCurrentCommandIssuer(), component));
                break;
        }
    }
}
