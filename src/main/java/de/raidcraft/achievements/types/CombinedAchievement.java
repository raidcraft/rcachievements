package de.raidcraft.achievements.types;

import com.google.common.base.Strings;
import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.Progressable;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.events.AchievementProgressChangeEvent;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import lombok.extern.java.Log;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Messages.Colors.ACCENT;
import static de.raidcraft.achievements.Messages.Colors.DARK_HIGHLIGHT;
import static de.raidcraft.achievements.Messages.Colors.HIGHLIGHT;
import static de.raidcraft.achievements.Messages.Colors.TEXT;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

@Log(topic = "RCAchievements:combined")
public class CombinedAchievement extends AbstractAchievementType implements Listener, Progressable {

    public static class Factory implements TypeFactory<CombinedAchievement> {

        @Override
        public String identifier() {

            return "combined";
        }

        @Override
        public Class<CombinedAchievement> typeClass() {

            return CombinedAchievement.class;
        }

        @Override
        public CombinedAchievement create(AchievementContext context) {

            return new CombinedAchievement(context);
        }

    }

    private Set<Achievement> achievements = new HashSet<>();
    private String prefix;
    private String suffix;

    protected CombinedAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        prefix = config.getString("prefix", "Fortschritt:");
        suffix = config.getString("suffix", "Erfolge");

        for (String alias : config.getStringList("achievements")) {
            Achievement.byAlias(alias)
                    .filter(Achievement::enabled)
                    .ifPresentOrElse(achievements::add, () -> {
                        log.warning("no enabled achievement by alias \"" + alias + "\" found in config of: " + alias() + " (" + id() + ")");
                    });
        }

        if (achievements.isEmpty()) {
            log.severe("no achievements configured in combined achievement of "+ alias() + " (" + id() + ")");
            return false;
        }

        return super.load(config);
    }

    @Override
    public float progress(AchievementPlayer player) {

        return unlockedAchievementCount(player) * 1.0f / achievements.size();
    }

    @Override
    public Component progressText(AchievementPlayer player) {

        long count = unlockedAchievementCount(player);

        return text().append(text(prefix + " ", TEXT))
                .append(text(count, HIGHLIGHT))
                .append(text("/", DARK_HIGHLIGHT))
                .append(text(achievements.size(), ACCENT))
                .append(text(" "))
                .append(Strings.isNullOrEmpty(suffix) ? empty() : text(suffix, TEXT))
                .append(newline())
                .append(join(text(" "), achievements.stream()
                        .map(achievement -> Messages.achievement(achievement, player))
                        .collect(Collectors.toList())))
                .build();
    }

    @EventHandler(ignoreCancelled = true)
    public void onAchievementUnlocked(PlayerUnlockedAchievementEvent event) {

        if (notApplicable(event.player())) return;
        if (!achievements.contains(event.achievement())) return;

        Bukkit.getPluginManager().callEvent(new AchievementProgressChangeEvent(playerAchievement(event.player()), this));

        if (event.player().unlockedAchievements()
                .stream().map(PlayerAchievement::achievement)
                .allMatch(achievement -> achievements.contains(achievement))) {
            addTo(event.player());
        }
    }

    private long unlockedAchievementCount(AchievementPlayer player) {

        return player.unlockedAchievements().stream()
                .map(PlayerAchievement::achievement)
                .filter(achievement -> achievements.contains(achievement))
                .count();
    }
}
