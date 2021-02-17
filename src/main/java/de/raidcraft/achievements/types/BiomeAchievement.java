package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.util.EnumUtil;
import de.raidcraft.achievements.util.LocationUtil;
import lombok.extern.java.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.stream.Collectors;

import static de.raidcraft.achievements.Messages.Colors.*;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

@Log(topic = "RCAchievements:biome")
public class BiomeAchievement extends CountAchievement implements Listener {

    public static final String VISITED_BIOMES = "visited_biomes";

    public static class Factory implements TypeFactory<BiomeAchievement> {

        @Override
        public String identifier() {

            return "biome";
        }

        @Override
        public Class<BiomeAchievement> typeClass() {

            return BiomeAchievement.class;
        }

        @Override
        public BiomeAchievement create(AchievementContext context) {

            return new BiomeAchievement(context);
        }
    }

    private final Set<Biome> biomeTypes = new HashSet<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Set<Biome>> playerVisitedBiomesMap = new HashMap<>();

    protected BiomeAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        biomeTypes.clear();

        for (String biomeName : config.getStringList("biomes")) {
            Biome biome = EnumUtil.searchEnum(Biome.class, biomeName);
            if (biome != null) {
                biomeTypes.add(biome);
            } else {
                log.warning("invalid biome name " + biomeName + " in config of: " + achievement());
            }
        }

        if (biomeTypes.isEmpty()) {
            biomeTypes.addAll(Arrays.asList(Biome.values()));
        }

        config.set("count", config.getInt("count", biomeTypes.size()));
        super.load(config);

        suffix(config.getString("suffix", "Biome besucht"));

        return true;
    }

    @Override
    public Component progressText(AchievementPlayer player) {

        Set<Biome> playerBiomes = playerVisitedBiomesMap.getOrDefault(player.id(), new HashSet<>());
        TextComponent.Builder builder = text();
        text().append(super.progressText(player));

        for (Biome biome : biomeTypes) {
            builder.append(text(" - ", TEXT)
                    .append(text(biome.getKey().getKey(), playerBiomes.contains(biome) ? SUCCESS : ERROR))
            ).append(newline());
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void enable() {

        Map<String, Collection<String>> entry = store().get(VISITED_BIOMES, Map.class, new HashMap<String, Collection<String>>());
        for (Map.Entry<String, Collection<String>> playerEntry : entry.entrySet()) {
            playerVisitedBiomesMap.put(UUID.fromString(playerEntry.getKey()), playerEntry.getValue().stream()
                    .map(s -> EnumUtil.searchEnum(Biome.class, s))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
            );
        }

        super.enable();
    }

    @Override
    public void disable() {

        store().set(VISITED_BIOMES, playerVisitedBiomesMap.entrySet().stream()
                .collect(toMap(t -> t.getKey().toString(), entry -> entry.getValue().stream()
                        .map(biome -> biome.getKey().toString())
                        .collect(toSet())
                ))).save();

        super.disable();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {

        if (!moved(event.getPlayer())) return;
        if (notApplicable(event.getPlayer())) return;
        Biome biome = event.getPlayer().getLocation().getBlock().getBiome();
        if (!biomeTypes.contains(biome)) return;

        Set<Biome> visitedBiomes = playerVisitedBiomesMap
                .computeIfAbsent(event.getPlayer().getUniqueId(), uuid -> new HashSet<>());
        if (visitedBiomes.contains(biome)) return;

        visitedBiomes.add(biome);
        playerVisitedBiomesMap.put(event.getPlayer().getUniqueId(), visitedBiomes);
        increaseAndCheck(player(event.getPlayer()));
    }

    private boolean moved(Player player) {

        Location lastLocation = lastLocations.getOrDefault(player.getUniqueId(), player.getLocation());
        lastLocations.put(player.getUniqueId(), lastLocation);
        return !LocationUtil.isBlockEquals(lastLocation, player.getLocation());
    }
}
