package de.raidcraft.achievements;

import co.aikar.commands.CommandIssuer;
import de.raidcraft.achievements.commands.PlayerCommands;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.util.TimeUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static de.raidcraft.achievements.Constants.PAGE_WIDTH;
import static de.raidcraft.achievements.Constants.RESULTS_PER_PAGE;
import static de.raidcraft.achievements.Messages.Colors.*;
import static de.raidcraft.achievements.commands.PlayerCommands.INFO;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class Messages {

    public static final class Colors {

        public static final TextColor BASE = YELLOW;
        public static final TextColor TEXT = BASE;
        public static final TextColor HIDDEN = GRAY;
        public static final TextColor DISABLED = DARK_GRAY;
        public static final TextColor ACCENT = GOLD;
        public static final TextColor DARK_ACCENT = DARK_AQUA;
        public static final TextColor HIGHLIGHT = AQUA;
        public static final TextColor DARK_HIGHLIGHT = DARK_AQUA;
        public static final TextColor NOT_UNLOCKED = GRAY;
        public static final TextColor UNLOCKED = GREEN;
        public static final TextColor ERROR = RED;
        public static final TextColor ERROR_ACCENT = DARK_RED;
        public static final TextColor SUCCESS = GREEN;
        public static final TextColor SUCCESS_ACCENT = DARK_GREEN;
        public static final TextColor WARNING = GOLD;
        public static final TextColor NOTE = GRAY;
    }

    public static void send(UUID playerId, Component message) {

        if (RCAchievements.testing()) return;
        BukkitAudiences.create(RCAchievements.instance())
                .player(playerId)
                .sendMessage(message);
    }

    public static void send(UUID playerId, Consumer<TextComponent.Builder> message) {

        TextComponent.Builder builder = text();
        message.accept(builder);
        send(playerId, builder.build());
    }

    public static void send(Object commandIssuer, Component message) {

        if (commandIssuer instanceof AchievementPlayer) {
            send(((AchievementPlayer) commandIssuer).id(), message);
        } else if (commandIssuer instanceof Player) {
            sendPlayer((Player) commandIssuer, message);
        } else if (commandIssuer instanceof ConsoleCommandSender) {
            sendConsole((ConsoleCommandSender) commandIssuer, message);
        } else if (commandIssuer instanceof RemoteConsoleCommandSender) {
            sendRemote((RemoteConsoleCommandSender) commandIssuer, message);
        } else if (commandIssuer instanceof CommandIssuer) {
            send((Object) ((CommandIssuer) commandIssuer).getIssuer(), message);
        }
    }

    public static void send(UUID target, Title title) {

        if (RCAchievements.testing()) return;
        BukkitAudiences.create(RCAchievements.instance())
                .player(target)
                .showTitle(title);
    }

    public static void sendPlayer(Player player, Component message) {
        send(player.getUniqueId(), message);
    }

    public static void sendConsole(ConsoleCommandSender sender, Component message) {

        sender.sendMessage(PlainComponentSerializer.plain().serialize(message));
    }

    public static void sendRemote(RemoteConsoleCommandSender sender, Component message) {

        sender.sendMessage(PlainComponentSerializer.plain().serialize(message));
    }

    public static Component addSuccess(Achievement achievement, AchievementPlayer player) {

        return text().append(text("Der Erfolg ", SUCCESS))
                .append(achievement(achievement))
                .append(text(" wurde ", SUCCESS))
                .append(player(player))
                .append(text(" erfolgreich hinzugefügt.", SUCCESS))
                .build();
    }

    public static Component addError(Achievement achievement, AchievementPlayer player) {

        return text().append(text("Der Erfolg ", ERROR))
                .append(achievement(achievement))
                .append(text(" konnte ", ERROR))
                .append(player(player))
                .append(text(" nicht zugewiesen werden.", ERROR))
                .build();
    }

    public static Component removeSuccess(Achievement achievement, AchievementPlayer player) {

        return text().append(text("Der Erfolg ", SUCCESS))
                .append(achievement(achievement))
                .append(text(" wurde erfolgreich von ", SUCCESS))
                .append(player(player))
                .append(text(" entfernt.", SUCCESS))
                .build();
    }

    public static Component achievementUnlockedSelf(PlayerAchievement achievement) {

        return text().append(text("Du", ACCENT, BOLD)
                    .hoverEvent(playerInfo(achievement.player()).asHoverEvent()))
                .append(text(" hast den Erfolg ", SUCCESS))
                .append(achievement(achievement.achievement()))
                .append(text(" freigeschaltet!", SUCCESS))
                .build();
    }

    public static Title achievementUnlockedTitle(PlayerAchievement achievement) {

        return Title.title(
                text(achievement.achievement().name(), SUCCESS),
                text(achievement.achievement().description(), ACCENT)
        );
    }

    public static Component player(AchievementPlayer player) {

        return text(player.name(), ACCENT, BOLD)
                .hoverEvent(playerInfo(player).asHoverEvent());
    }

    public static Component playerInfo(AchievementPlayer player) {

        return text().append(text(player.name(), ACCENT, BOLD))
                .append(newline())
                .append(text("Erfolge: ", TEXT))
                .append(text(player.unlockedAchievements().size(), HIGHLIGHT))
                .append(text("/", DARK_HIGHLIGHT))
                .append(text(Achievement.allEnabled().size(), ACCENT))
                .build();
    }

    public static List<Component> list(AchievementPlayer player, List<Achievement> achievements, int page) {

        return Pagination.builder()
                .resultsPerPage(RESULTS_PER_PAGE)
                .width(PAGE_WIDTH)
                .build(
                        text("Erfolge von ", DARK_ACCENT).append(player(player)),
                        new Pagination.Renderer.RowRenderer<Achievement>() {
                            @Override
                            public @NonNull Collection<Component> renderRow(Achievement value, int index) {

                                if (value == null) return Collections.singleton(empty());

                                return Collections.singleton(text()
                                        .append(text("|- ", DARK_ACCENT))
                                        .append(achievement(value, player)).build());
                            }
                        }, PlayerCommands.LIST::apply
                ).render(achievements, page);
    }

    public static Component achievement(Achievement achievement) {

        return achievement(achievement, null);
    }

    public static Component achievement(Achievement achievement, AchievementPlayer player) {

        return text().append(text("[", ACCENT))
                .append(text(achievement.name(), achievementColor(achievement, player))
                        .hoverEvent(achievementInfo(achievement).append(newline())
                                .append(text("Klicken um mehr Details anzuzeigen.", NOTE, ITALIC))
                                .asHoverEvent()
                        )
                        .clickEvent(runCommand(INFO.apply(achievement)))
                )
                .append(text("]", ACCENT))
                .build();
    }

    public static Component achievementInfo(Achievement achievement) {

        return achievementInfo(achievement, null);
    }

    public static Component achievementInfo(Achievement achievement, AchievementPlayer player) {

        TextComponent.Builder builder = text().append(text(achievement.name(), achievementColor(achievement, player)))
                .append(text(" (" + achievement.alias() + ")", NOTE))
                .append(newline())
                .append(text(achievement.description(), NOTE, achievement.secret() ? OBFUSCATED : ITALIC));

        if (player != null) {
            PlayerAchievement playerAchievement = PlayerAchievement.of(achievement, player);
            builder.append(text("Freigeschaltet: ", TEXT))
                    .append(text(playerAchievement.isUnlocked() ? TimeUtil.formatDateTime(playerAchievement.unlocked()) : "N/A", playerAchievement.isUnlocked() ? UNLOCKED : NOT_UNLOCKED));
        }

        return builder.build();
    }

    private static TextColor achievementColor(Achievement achievement, AchievementPlayer player) {

        TextColor color = ACCENT;
        if (player != null) {
            color = player.unlocked(achievement) ? UNLOCKED : NOT_UNLOCKED;
        }

        return color;
    }

    private Messages() {}
}
