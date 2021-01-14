package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import lombok.extern.java.Log;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashSet;
import java.util.Set;

@Log(topic = "RCAchievements:craft")
public class CraftAchievement extends CountAchievement implements Listener {

    public static class Factory implements TypeFactory<CraftAchievement> {

        @Override
        public String identifier() {

            return "craft";
        }

        @Override
        public Class<CraftAchievement> typeClass() {

            return CraftAchievement.class;
        }

        @Override
        public CraftAchievement create(AchievementContext context) {

            return new CraftAchievement(context);
        }
    }

    private final Set<Material> itemTypes = new HashSet<>();

    protected CraftAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        super.load(config);
        suffix(config.getString("suffix", "Items hergestellt"));

        itemTypes.clear();

        for (String item : config.getStringList("items")) {
            Material material = Material.matchMaterial(item);
            if (material != null) {
                itemTypes.add(material);
            } else {
                log.severe("unknown item type " + item + " in config of: " + alias());
            }
        }

        if (itemTypes.isEmpty()) {
            log.severe("no item types configured in config of " + alias() + "!");
            return false;
        }

        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraft(InventoryClickEvent event) {

        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (notApplicable(player((OfflinePlayer) event.getWhoClicked()))) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (!itemTypes.contains(event.getCurrentItem().getType())) return;

        increaseAndCheck(player((OfflinePlayer) event.getWhoClicked()), event.getCurrentItem().getAmount());
    }
}
