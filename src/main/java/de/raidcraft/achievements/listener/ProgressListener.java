package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.events.AchievementProgressChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static de.raidcraft.achievements.Messages.Colors.ACCENT;
import static de.raidcraft.achievements.Messages.Colors.HIGHLIGHT;
import static de.raidcraft.achievements.Messages.Colors.TEXT;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class ProgressListener implements Listener {

    private final RCAchievements plugin;

    public ProgressListener(RCAchievements plugin) {

        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onProgressChange(AchievementProgressChangeEvent event) {

        event.player().bukkitPlayer().ifPresent(player -> {
            float progress = event.progress();
            Component progressBar = Messages.progressBar(progress, 100, '|', NamedTextColor.GREEN, NamedTextColor.GRAY);

            text().append(text(event.achievement().name(), HIGHLIGHT, BOLD))
                    .append(text(": ", TEXT))
                    .append(text("[", ACCENT))
                    .append(progressBar)
                    .append(text("] ", ACCENT))
                    .append(text((int) progress * 100 + "%", TEXT));

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, BungeeComponentSerializer.get().serialize(progressBar));
        });
    }
}
