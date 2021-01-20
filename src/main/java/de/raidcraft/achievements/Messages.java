package de.raidcraft.achievements;

import co.aikar.commands.CommandIssuer;
import de.raidcraft.achievements.commands.AdminCommands;
import de.raidcraft.achievements.commands.PlayerCommands;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.util.TimeUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Constants.PAGE_WIDTH;
import static de.raidcraft.achievements.Constants.RESULTS_PER_PAGE;
import static de.raidcraft.achievements.Messages.Colors.*;
import static de.raidcraft.achievements.commands.PlayerCommands.INFO;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;
import static net.kyori.adventure.text.format.TextDecoration.OBFUSCATED;

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

    public static Component deleteSuccess(Achievement achievement) {

        return text().append(text("Der Erfolg ", SUCCESS))
                .append(achievement(achievement))
                .append(text(" wurde erfolgreich gelöscht."))
                .build();
    }

    public static Component unassignSuccess(Achievement achievement, AchievementPlayer player) {

        return text().append(text("Der Erfolg ", SUCCESS))
                .append(achievement(achievement))
                .append(text(" wurde erfolgreich von ", SUCCESS))
                .append(player(player))
                .append(text(" entfernt.", SUCCESS))
                .build();
    }

    public static Component createSuccess(Achievement achievement) {

        TextComponent text = text("Klicke um den Befehl auszuführen.", NOTE, ITALIC);

        TextComponent.Builder builder = text().append(text("Der Erfolg ", SUCCESS))
                .append(achievement(achievement))
                .append(text(" wurde erfolgreich erstellt.", SUCCESS)).append(newline())
                .append(text("Nutze die \"/rca:admin set\" Befehle um z.B. den Namen und die Beschreibung zu setzen. Und führe danach ", NOTE))
                .append(text(AdminCommands.RELOAD, ACCENT).hoverEvent(text).clickEvent(runCommand(AdminCommands.RELOAD)))
                .append(text(" aus.", NOTE))
                .append(newline()).append(text("Tipp: ", TEXT))
                .append(text("Alle Befehle lassen sich per Klick ausführen und haben eine Beschreibung beim hovern.", NOTE))
                .append(newline());

        for (AdminCommands.SetCommand command : AdminCommands.SET_COMMANDS) {
            builder.append(text("  - ", TEXT))
                    .append(text(command.command(), HIGHLIGHT)
                            .hoverEvent(text()
                                    .append(text(command.description(), NOTE))
                                    .append(newline())
                                    .append(text)
                                    .build()
                                    .asHoverEvent()
                            ).clickEvent(suggestCommand(command + achievement.alias()))
                    ).append(newline());
        }

        return builder.build();
    }

    public static Component setSuccess(Achievement achievement, String property) {

        return text().append(text("Die Eigenschaft ", SUCCESS))
                .append(text(property, HIGHLIGHT))
                .append(text(" des Erfolgs ", SUCCESS))
                .append(achievement(achievement))
                .append(text(" wurde erfolgreich geändert.", SUCCESS)).append(newline())
                .append(text("Gebe ", NOTE))
                .append(text(AdminCommands.RELOAD, ACCENT)
                        .hoverEvent(text("Klicken zum Ausführen von ", NOTE).append(text(AdminCommands.RELOAD, ACCENT)))
                        .clickEvent(runCommand(AdminCommands.RELOAD))
                ).append(text(" um deine Änderungen zu aktivieren.", NOTE))
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

    public static Component achievementUnlockedOther(PlayerAchievement achievement, AchievementPlayer viewer) {

        return text().append(player(achievement.player()))
                .append(text(" hat den Erfolg ", SUCCESS))
                .append(achievement(achievement.achievement(), viewer))
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

    public static List<Component> topList(int page) {

        @SuppressWarnings("RedundantStreamOptionalCall")
        List<AchievementPlayer> players = AchievementPlayer.find.all()
                .stream().sorted()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        return Pagination.builder()
                .resultsPerPage(RESULTS_PER_PAGE)
                .width(PAGE_WIDTH)
                .build(
                        text("Erfolge Topliste", DARK_ACCENT),
                        (Pagination.Renderer.RowRenderer<AchievementPlayer>) (value, index) -> {

                            if (value == null) return Collections.singleton(empty());

                            return Collections.singleton(text()
                                    .append(text((index + 1) + ". ", SUCCESS))
                                    .append(player(value))
                                    .build()
                            );
                        }, PlayerCommands.TOP::apply
                ).render(players, page);
    }

    public static List<Component> listNearby(@NonNull AchievementPlayer player, @NonNull List<Map.Entry<Achievement, Integer>> locations, int page, int radius) {

        return Pagination.builder()
                .resultsPerPage(RESULTS_PER_PAGE)
                .width(PAGE_WIDTH)
                .build(
                        text("Erfolge von ", DARK_ACCENT).append(player(player)),
                        (Pagination.Renderer.RowRenderer<Map.Entry<Achievement, Integer>>) (value, index) -> {

                            if (value == null) return Collections.singleton(empty());

                            return Collections.singleton(text()
                                    .append(text("|  ", DARK_ACCENT))
                                    .append(achievement(value.getKey(), player))
                                    .append(text(" (", NOTE))
                                    .append(text(value.getValue() + "m", NOTE))
                                    .append(text(")", NOTE))
                                    .build());
                        }, p -> AdminCommands.NEARBY.apply(p, radius)
                ).render(locations, page);
    }

    public static List<Component> list(@NonNull AchievementPlayer player, List<Achievement> achievements, int page) {

        achievements = achievements.stream()
                .sorted()
                .sorted((o1, o2) -> Boolean.compare(player.unlocked(o1), player.unlocked(o2)))
                .sorted((o1, o2) -> Boolean.compare(player.canView(o1), player.canView(o2)))
                .collect(Collectors.toList());

        return Pagination.builder()
                .resultsPerPage(RESULTS_PER_PAGE)
                .width(PAGE_WIDTH)
                .build(
                        text("Erfolge von ", DARK_ACCENT).append(player(player)),
                        (Pagination.Renderer.RowRenderer<Achievement>) (value, index) -> {

                            if (value == null) return Collections.singleton(empty());

                            return Collections.singleton(text()
                                    .append(text("|  ", DARK_ACCENT))
                                    .append(achievement(value, player)).build());
                        }, PlayerCommands.LIST::apply
                ).render(achievements, page);
    }

    public static Component achievement(Achievement achievement) {

        return achievement(achievement, null);
    }

    public static Component achievement(Achievement achievement, AchievementPlayer player) {

        if (player != null && !player.canView(achievement)) {
            return text().append(text("[", ACCENT))
                    .append(text(achievement.name().replaceAll(".", "?"), achievementColor(achievement, player)))
                    .append(text("]", ACCENT))
                    .build();
        }

        return text().append(text("[", ACCENT))
                .append(text(achievement.name(), achievementColor(achievement, player))
                        .hoverEvent(achievementInfo(achievement, player))
                        .clickEvent(runCommand(INFO.apply(achievement)))
                )
                .append(text("]", ACCENT))
                .build();
    }

    public static Component achievementInfo(Achievement achievement) {

        return achievementInfo(achievement, null);
    }

    public static Component achievementInfo(Achievement achievement, AchievementPlayer player) {

        if (player != null && !player.canView(achievement)) {
            return text("Versteckter Erfolg", ERROR);
        }

        TextComponent.Builder builder = text().append(text(achievement.name(), achievementColor(achievement, player)));

        if (player != null
                && player.offlinePlayer().getPlayer() != null
                && player.offlinePlayer().getPlayer().hasPermission(Constants.SHOW_ALIAS)) {
            builder.append(text(" (" + achievement.alias() + ")", NOTE));
        }

        builder.append(newline());

        if (player != null) {
            PlayerAchievement playerAchievement = PlayerAchievement.of(achievement, player);
            builder.append(text(achievement.description(), NOTE, player.canViewDetails(achievement) ? ITALIC : OBFUSCATED))
                    .append(newline())
                    .append(text("Freigeschaltet: ", TEXT))
                    .append(text(playerAchievement.isUnlocked() ? TimeUtil.formatDateTime(playerAchievement.unlocked()) : "N/A", playerAchievement.isUnlocked() ? UNLOCKED : NOT_UNLOCKED));

            RCAchievements.instance().achievementManager()
                    .active(achievement)
                    .filter(context -> context.type() instanceof Progressable)
                    .map(context -> (Progressable) context.type())
                    .ifPresent(context -> builder.append(newline()).append(context.progress(player)));
        } else {
            builder.append(text(achievement.description(), NOTE, achievement.secret() ? OBFUSCATED : ITALIC));
        }

        return builder.build();
    }

    public static TextColor achievementColor(Achievement achievement, AchievementPlayer player) {

        TextColor color = ACCENT;
        if (player != null) {
            if (player.unlocked(achievement)) {
                color = UNLOCKED;
            } else if (!player.canView(achievement)) {
                color = DISABLED;
            } else {
                color = NOT_UNLOCKED;
            }
        }

        return color;
    }

    private Messages() {}
}
