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
    public static final Function<Integer, String> TOP = (page) -> "/rcachievements top " + page;

    private final RCAchievements plugin;

    public PlayerCommands(RCAchievements plugin) {
        this.plugin = plugin;
    }

    @Subcommand("info")
    @CommandCompletion("@unlocked-achievements")
    @CommandPermission(PERMISSION_PREFIX + "achievements.info")
    @Description("Shows the information about an achievement.")
    public void info(@Conditions("visible") Achievement achievement) {

        send(getCurrentCommandIssuer(), achievementInfo(achievement));
    }

    @Default
    @Subcommand("list")
    @CommandAlias("erfolge")
    @CommandCompletion("* @players")
    @CommandPermission(PERMISSION_PREFIX + "achievements.list.all")
    public void list(@Default("1") int page, @Conditions("self") AchievementPlayer player) {

        Messages.list(player, Achievement.allEnabled(getCurrentCommandIssuer().hasPermission(SHOW_HIDDEN)), page)
                .forEach(component -> send(getCurrentCommandIssuer(), component));

    }

    @Default
    @Subcommand("my|mylist")
    @CommandAlias("meineerfolge")
    @CommandCompletion("* @players")
    @CommandPermission(PERMISSION_PREFIX + "achievements.list")
    public void mylist(@Default("1") int page, @Conditions("self") AchievementPlayer player) {

        Messages.list(player, player.unlockedAchievements().stream().map(PlayerAchievement::achievement).collect(Collectors.toList()), page)
                .forEach(component -> send(getCurrentCommandIssuer(), component));

    }


    @Subcommand("top|toplist")
    @CommandCompletion("*")
    @CommandPermission(PERMISSION_PREFIX + "achievements.top")
    public void top(@Default("1") int page) {

        Messages.topList(page).forEach(component -> Messages.send(getCurrentCommandIssuer(), component));
    }
}
