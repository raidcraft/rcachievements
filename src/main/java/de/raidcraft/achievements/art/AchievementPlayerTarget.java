package de.raidcraft.achievements.art;

import de.raidcraft.achievements.entities.AchievementPlayer;
import io.artframework.AbstractTarget;
import io.artframework.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AchievementPlayerTarget extends AbstractTarget<AchievementPlayer> implements MessageSender {

    public AchievementPlayerTarget(AchievementPlayer source) {
        super(source);
    }

    @Override
    public String uniqueId() {

        return source().id().toString();
    }

    @Override
    public void sendMessage(String... message) {

        Player player = Bukkit.getPlayer(source().id());
        if (player != null) {
            player.sendMessage(message);
        }
    }
}
