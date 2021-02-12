package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.events.AchievementProgressChangeEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.swing.border.AbstractBorder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static de.raidcraft.achievements.Messages.Colors.ACCENT;
import static de.raidcraft.achievements.Messages.Colors.HIGHLIGHT;
import static de.raidcraft.achievements.Messages.Colors.TEXT;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class ProgressListener implements Listener {

    private final RCAchievements plugin;
    // player achievement -> bossbar
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public ProgressListener(RCAchievements plugin) {

        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onProgressChange(AchievementProgressChangeEvent event) {

        if (!event.playerAchievement().isActive()) return;

        event.player().bukkitPlayer().ifPresent(player -> {
            float progress = event.progress();

            if (plugin.pluginConfig().isProgressActionBar()) {
                Component progressBar = Messages.progressBar(progress, 50, '|', ChatColor.GREEN, ChatColor.GRAY);

                TextComponent.Builder builder = text().append(text(event.achievement().name(), HIGHLIGHT, BOLD))
                        .append(text(": ", TEXT))
                        .append(text("[", ACCENT))
                        .append(progressBar)
                        .append(text("] ", ACCENT))
                        .append(text(String.format(Locale.GERMANY, "%.2f", progress * 100f) + "%", TEXT));

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, BungeeComponentSerializer.get().serialize(builder.build()));
            }

            if (plugin.pluginConfig().isProgressBossBar()) {

                Audience audience = BukkitAudiences.create(plugin).player(player);
                BossBar bossBar = BossBar.bossBar(event.progressText(), progress, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
                BossBar activeBossBar = activeBossBars.remove(event.playerAchievement().id());
                if (activeBossBar != null) {
                    audience.hideBossBar(activeBossBar);
                }
                audience.showBossBar(bossBar);
                activeBossBars.put(event.playerAchievement().id(), bossBar);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    BossBar activeBar = activeBossBars.remove(event.playerAchievement().id());
                    if (player.isOnline() && activeBar != null) {
                        BukkitAudiences.create(plugin).player(player).hideBossBar(activeBar);
                    }
                }, plugin.pluginConfig().getBossBarDuration());
            }
        });
    }
}
