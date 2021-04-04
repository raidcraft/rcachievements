package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.util.EnumUtil;
import de.raidcraft.achievements.util.LocationUtil;
import io.ebeaninternal.server.lib.Str;
import lombok.extern.java.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    final Set<Biome> biomeTypes = new HashSet<>();
    final Map<UUID, Location> lastLocations = new HashMap<>();
    final Map<UUID, Set<Biome>> playerVisitedBiomesMap = new HashMap<>();

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

    @Override
    public void enable() {

        Bukkit.getOnlinePlayers().forEach(this::loadBiomes);

        super.enable();
    }

    @Override
    public void disable() {

        playerVisitedBiomesMap.keySet()
                .stream().map(Bukkit::getOfflinePlayer)
                .forEach(this::saveBiomes);

        super.disable();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {

        loadBiomes(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {

        saveBiomes(event.getPlayer());
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

    @SuppressWarnings("unchecked")
    private void loadBiomes(Player player) {

        Set<String> visitedBiomes = Set.copyOf(store(player).get(VISITED_BIOMES, Collection.class, new HashSet<>()));

        playerVisitedBiomesMap.compute(player.getUniqueId(), (uuid, biomes) ->
                Stream.concat(visitedBiomes.stream()
                                .map(s -> EnumUtil.searchEnum(Biome.class, s)),
                        Objects.requireNonNullElse(biomes, new HashSet<Biome>()).stream())
                        .collect(Collectors.toSet())
        );
    }

    private void saveBiomes(OfflinePlayer player) {

        Set<String> biomes = playerVisitedBiomesMap.getOrDefault(player.getUniqueId(), new HashSet<>())
                .stream().map(biome -> biome.getKey().toString())
                .collect(toSet());
        store(player).set(VISITED_BIOMES, biomes).save();
    }
}
