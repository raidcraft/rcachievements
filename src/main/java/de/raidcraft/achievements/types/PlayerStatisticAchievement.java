package de.raidcraft.achievements.types;

import com.google.common.base.Strings;
import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.PeriodicAsync;
import de.raidcraft.achievements.Progressable;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.events.AchievementCountChangedEvent;
import de.raidcraft.achievements.events.AchievementProgressChangeEvent;
import de.raidcraft.achievements.util.EnumUtil;
import lombok.extern.java.Log;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import java.util.*;

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
    private float divideProgress = 1.0f;

    private final Map<UUID, Integer> playerValues = Collections.synchronizedMap(new HashMap<>());

    protected PlayerStatisticAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public float progress(AchievementPlayer player) {

        float progress = statisticValue(player.offlinePlayer()) * 1.0f;
        return progress / count;
    }

    @Override
    public Component progressText(AchievementPlayer player) {

        int value = statisticValue(player.offlinePlayer());

        return text(prefix + " ", TEXT)
                .append(text(value, HIGHLIGHT))
                .append(text("/", DARK_HIGHLIGHT))
                .append(text(count, ACCENT))
                .append(Strings.isNullOrEmpty(suffix) ? empty() : text(" " + suffix, TEXT));
    }

    @Override
    public long progressCount(AchievementPlayer player) {

        return statisticValue(player.offlinePlayer());
    }

    @Override
    public long progressMaxCount(AchievementPlayer player) {

        return count;
    }

    @Override
    public boolean load(ConfigurationSection config) {

        count = config.getInt("count", 1);
        prefix = config.getString("prefix", "Fortschritt:");
        suffix = config.getString("suffix");
        divideProgress = (float) config.getDouble("divide_progress", 1.0);
        if (divideProgress == 0) {
            log.severe("divide_progress must not be 0 in " + achievement());
            return false;
        }

        String stat = config.getString("statistic");
        statistic = EnumUtil.searchEnum(Statistic.class, stat);
        if (statistic == null) {
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
                this.entityType = EnumUtil.searchEnum(EntityType.class, entity);
                if (entityType == null) {
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

        synchronized (playerValues) {
            int value = playerValues.compute(player.getUniqueId(), (uuid, oldValue) -> {
                int newValue = statisticValue(player);
                if (oldValue != null && newValue != oldValue) {
                    Bukkit.getScheduler().runTask(RCAchievements.instance(), () -> Bukkit.getPluginManager()
                            .callEvent(new AchievementProgressChangeEvent(PlayerAchievement.of(achievement(), player(player)), this)));
                }
                return newValue;
            });

            if (value >= this.count) {
                addTo(player(player));
            }
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

        int newValue = (int) (event.getNewValue() / divideProgress);
        synchronized (playerValues) {
            playerValues.put(event.getPlayer().getUniqueId(), newValue);
        }

        Bukkit.getPluginManager()
                .callEvent(new AchievementProgressChangeEvent(PlayerAchievement.of(achievement(), player(event.getPlayer())), this));

        if (newValue >= count) {
            addTo(player(event.getPlayer()));
        }
    }

    private int statisticValue(OfflinePlayer player) {

        if (statistic == null) return 0;
        switch (statistic.getType()) {
            case BLOCK:
            case ITEM:
                return (int) (player.getStatistic(statistic, material) / divideProgress);
            case ENTITY:
                return (int) (player.getStatistic(statistic, entityType) / divideProgress);
            default:
                return (int) (player.getStatistic(statistic) / divideProgress);
        }
    }
}
