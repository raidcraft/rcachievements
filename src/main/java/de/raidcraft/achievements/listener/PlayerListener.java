package de.raidcraft.achievements.listener;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class PlayerListener implements Listener, PluginMessageListener {

    private final RCAchievements plugin;
    private final Set<String> onlinePlayers = new HashSet<>();

    public PlayerListener(RCAchievements plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerList");
            out.writeUTF("ALL");

            Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (player == null) return;
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }, plugin.pluginConfig().getPlayerListUpdateInterval(), plugin.pluginConfig().getPlayerListUpdateInterval());
    }

    @EventHandler(ignoreCancelled = true)
    public void onAchievementUnlocked(PlayerUnlockedAchievementEvent event) {

        Messages.send(event.player(), Messages.achievementUnlockedSelf(event.playerAchievement()));
        Messages.send(event.player().id(), Messages.achievementUnlockedTitle(event.playerAchievement()));

        if (plugin.pluginConfig().isBroadcast() && event.achievement().broadcast()) {
            if (event.achievement().delayedBroadcast()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> broadcast(event.playerAchievement()), plugin.pluginConfig().getSecretBroadcastDelay());
            } else {
                broadcast(event.playerAchievement());
            }
        }
    }

    private void broadcast(PlayerAchievement achievement) {

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.getUniqueId().equals(achievement.player().id()))
                .map(AchievementPlayer::of)
                .forEach(player -> Messages.send(player, Messages.achievementUnlockedOther(achievement, player)));

        AchievementPlayer.find.query().where()
                .in("name", onlinePlayers)
                .ne("id", achievement.player().id())
                .findList()
                .forEach(player -> {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("MessageRaw");
                    out.writeUTF(player.name());

                    out.writeUTF(GsonComponentSerializer.gson().serialize(Messages.achievementUnlockedOther(achievement, player)));

                    Player sender = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                    if (sender == null) return;
                    sender.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {

        if (!channel.equals("BungeeCord")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("PlayerList")) {
            onlinePlayers.clear();
            String[] playerList = in.readUTF().split(", ");
            onlinePlayers.addAll(Arrays.asList(playerList));
        }
    }
}
