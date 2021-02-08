package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import lombok.extern.java.Log;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.Set;

@Log(topic = "RCAchievements:block")
public class BlockAchievement extends CountAchievement implements Listener {

    public static class Factory implements TypeFactory<BlockAchievement> {

        @Override
        public String identifier() {

            return "block";
        }

        @Override
        public Class<BlockAchievement> typeClass() {

            return BlockAchievement.class;
        }

        @Override
        public BlockAchievement create(AchievementContext context) {

            return new BlockAchievement(context);
        }
    }

    private Action action;
    private final Set<Material> blockTypes = new HashSet<>();

    protected BlockAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        super.load(config);

        blockTypes.clear();

        for (String block : config.getStringList("blocks")) {
            Material material = Material.matchMaterial(block);
            if (material != null) {
                blockTypes.add(material);
            } else {
                log.severe("unknown block type " + block + " in config of: " + alias());
            }
        }

        if (blockTypes.isEmpty()) {
            log.severe("no block types configured in config of " + alias() + "!");
            return false;
        }

        try {
            action = Action.valueOf(config.getString("action", "place").toUpperCase());
        } catch (IllegalArgumentException e) {
            log.severe("unknown action " + config.getString("action") + " in config of: " + alias());
            return false;
        }

        suffix(config.getString("suffix", "Blöcke " + (action == Action.BREAK ? "abgebaut" : "gesetzt")));

        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (action != Action.PLACE) return;

        process(event.getPlayer(), event.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {

        if (action != Action.BREAK) return;

        process(event.getPlayer(), event.getBlock());
    }

    private void process(Player player, Block block) {

        if (notApplicable(player)) return;
        if (!blockTypes.contains(block.getType())) return;

        increaseAndCheck(player(player));
    }

    enum Action {
        PLACE,
        BREAK
    }
}
