package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.TypeFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LoginAchievement extends CountAchievement implements Listener {

    static final String LAST_LOGIN = "last_login";

    public static class Factory implements TypeFactory<LoginAchievement> {

        @Override
        public String identifier() {

            return "login";
        }

        @Override
        public Class<LoginAchievement> typeClass() {

            return LoginAchievement.class;
        }

        @Override
        public LoginAchievement create(AchievementContext context) {

            return new LoginAchievement(context);
        }
    }

    boolean reset = true;

    protected LoginAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        super.load(config);

        reset = config.getBoolean("reset", true);
        suffix(config.getString("suffix", "Tage"));

        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {

        if (notApplicable(event.getPlayer())) return;

        Bukkit.getScheduler().runTaskAsynchronously(RCAchievements.instance(), () -> {

            if (checkedToday(event.getPlayer())) return;
            if (!streak(event.getPlayer())) {
                setCountAndCheck(player(event.getPlayer()), 1);
                return;
            }

            increaseAndCheck(player(event.getPlayer()));
            store(event.getPlayer()).set(LAST_LOGIN, Instant.now().toEpochMilli()).save();
        });
    }

    boolean checkedToday(Player player) {

        return store(player).get(LAST_LOGIN, Long.class)
                .map(Instant::ofEpochMilli)
                .map(instant -> instant.truncatedTo(ChronoUnit.DAYS))
                .map(instant -> instant.equals(Instant.now().truncatedTo(ChronoUnit.DAYS)))
                .orElse(false);

    }

    boolean streak(Player player) {

        return store(player).get(LAST_LOGIN, Long.class)
                .map(Instant::ofEpochMilli)
                .map(instant -> instant.plus(1, ChronoUnit.DAYS))
                .map(instant -> instant.truncatedTo(ChronoUnit.DAYS))
                .map(instant -> instant.equals(Instant.now().truncatedTo(ChronoUnit.DAYS)))
                .orElse(true);
    }
}
