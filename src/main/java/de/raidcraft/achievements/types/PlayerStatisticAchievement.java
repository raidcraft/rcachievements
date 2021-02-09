package de.raidcraft.achievements.types;

import com.google.common.base.Strings;
import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.PeriodicAsync;
import de.raidcraft.achievements.Progressable;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import lombok.extern.java.Log;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import java.util.Objects;

import static de.raidcraft.achievements.Messages.Colors.ACCENT;
import static de.raidcraft.achievements.Messages.Colors.DARK_HIGHLIGHT;
import static de.raidcraft.achievements.Messages.Colors.HIGHLIGHT;
import static de.raidcraft.achievements.Messages.Colors.TEXT;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

@Log(topic = "RCAchievements:statistic")
public class PlayerStatisticAchievement extends AbstractAchievementType implements Progressable, PeriodicAsync {

    public static class Factory implements TypeFactory<PlayerStatisticAchievement> {

        @Override
        public String identifier() {

            return "statistic";
        }

        @Override
        public Class<PlayerStatisticAchievement> typeClass() {

            return PlayerStatisticAchievement.class;
        }

        @Override
        public PlayerStatisticAchievement create(AchievementContext context) {

            return new PlayerStatisticAchievement(context);
        }

    }
    private Statistic statistic;

    private EntityType entityType;
    private Material material;
    private String prefix;
    private String suffix;
    private int count;
    protected PlayerStatisticAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public Component progressText(AchievementPlayer player) {

        int value;
        switch (statistic.getType()) {
            case BLOCK:
            case ITEM:
                value = player.offlinePlayer().getStatistic(statistic, material);
                break;
            case ENTITY:
                value = player.offlinePlayer().getStatistic(statistic, entityType);
                break;
            default:
                value = player.offlinePlayer().getStatistic(statistic);
                break;
        }

        return text(prefix + " ", TEXT)
                .append(text(value, HIGHLIGHT))
                .append(text("/", DARK_HIGHLIGHT))
                .append(text(count, ACCENT))
                .append(Strings.isNullOrEmpty(suffix) ? empty() : text(" " + suffix, TEXT));
    }

    @Override
    public boolean load(ConfigurationSection config) {

        count = config.getInt("count", 1);
        prefix = config.getString("prefix", "Fortschritt:");
        suffix = config.getString("suffix");

        String stat = config.getString("statistic");
        try {
            statistic = Statistic.valueOf(Objects.requireNonNull(stat).toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.severe("invalid statistic type " + stat + " in config of " + alias() + " (" + id() + ")");
            return false;
        }

        if (statistic.getType() == Statistic.Type.BLOCK || statistic.getType() == Statistic.Type.ITEM) {
            String material = config.getString("material");
            if (!Strings.isNullOrEmpty(material)) {
                this.material = Material.matchMaterial(material);
                if (this.material == null) {
                    log.severe("invalid material \"" + material + "\" for statistic " + stat + " in config of " + alias() + " (" + id() + ")");
                    return false;
                }
            } else {
                log.severe("missing material for statistic " + stat + " in config of " + alias() + " (" + id() + ")");
                return false;
            }
        } else if (statistic.getType() == Statistic.Type.ENTITY) {
            String entity = config.getString("entity");
            if (!Strings.isNullOrEmpty(entity)) {
                try {
                    this.entityType = EntityType.valueOf(entity);
                } catch (IllegalArgumentException e) {
                    log.severe("invalid entity type \"" + entity + "\" for statistic " + stat + " in config of " + alias() + " (" + id() + ")");
                    return false;
                }
            } else {
                log.severe("missing entity type for statistic " + stat + " in config of " + alias() + " (" + id() + ")");
                return false;
            }
        }

        return true;
    }

    @Override
    public void tickAsync(Player player) {

        if (notApplicable(player)) return;

        int count;
        switch (statistic.getType()) {
            case BLOCK:
            case ITEM:
                count = player.getStatistic(statistic, material);
                break;
            case ENTITY:
                count = player.getStatistic(statistic, entityType);
                break;
            default:
                count = player.getStatistic(statistic);
                break;
        }

        if (count >= this.count) {
            addTo(player(player));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStatisticChange(PlayerStatisticIncrementEvent event) {

        if (event.getStatistic() != statistic) return;
        if (notApplicable(event.getPlayer())) return;

        switch (statistic.getType()) {
            case BLOCK:
            case ITEM:
                if (event.getMaterial() != material) {
                    return;
                }
                break;
            case ENTITY:
                if (event.getEntityType() != entityType) {
                    return;
                }
                break;
        }

        if (event.getNewValue() >= count) {
            addTo(player(event.getPlayer()));
        }
    }
}
