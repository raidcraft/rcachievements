package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import net.kyori.adventure.text.Component;
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

import static de.raidcraft.achievements.Messages.Colors.TEXT;

public class LoginAchievement extends CountAchievement implements Listener {

    private static final String LAST_LOGIN = "last_login";

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

    private final Set<UUID> checkedToday = new HashSet<>();
    private Instant lastCheck = Instant.now();
    private boolean reset = true;

    protected LoginAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        reset = config.getBoolean("reset", true);

        return true;
    }

    @Override
    public Component progress(AchievementPlayer player) {

        return super.progress(player).append(Component.text(" Tage", TEXT));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {

        if (!Instant.now().truncatedTo(ChronoUnit.DAYS).equals(lastCheck.truncatedTo(ChronoUnit.DAYS))) {
            checkedToday.clear();
        }
        if (notApplicable(event.getPlayer())) return;
        if (today(event.getPlayer())) return;
        if (!streak(event.getPlayer())) {
            count(player(event.getPlayer()), 0);
            return;
        }

        lastCheck = Instant.now();

        increaseAndCheck(player(event.getPlayer()));
        checkedToday.add(event.getPlayer().getUniqueId());
        store(event.getPlayer()).set(LAST_LOGIN, Instant.now().toEpochMilli());
    }

    private boolean today(Player player) {

        return checkedToday.contains(player.getUniqueId())
                || Instant.ofEpochMilli(store(player).get(LAST_LOGIN, long.class, Instant.EPOCH.toEpochMilli()))
                .truncatedTo(ChronoUnit.DAYS).equals(Instant.now().truncatedTo(ChronoUnit.DAYS));

    }

    private boolean streak(Player player) {

        Instant lastLoginDay = Instant.ofEpochMilli(store(player).get(LAST_LOGIN, long.class, Instant.EPOCH.toEpochMilli()))
                .truncatedTo(ChronoUnit.DAYS);

        return lastLoginDay.plus(1, ChronoUnit.DAYS).equals(Instant.now().truncatedTo(ChronoUnit.DAYS));
    }
}
