package de.raidcraft.achievements.entities;

import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.Index;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import net.silthus.ebean.BaseEntity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Optional;
import java.util.UUID;

import static de.raidcraft.achievements.Constants.TABLE_PREFIX;

@Getter
@Setter
@Accessors(fluent = true)
@Entity
@Table(name = TABLE_PREFIX + "placed_blocks")
@Log(topic = "RCSkills")
@EqualsAndHashCode(of = {"world", "x", "y", "z"}, callSuper = false)
public class PlacedBlock extends BaseEntity {

    public static final Finder<UUID, PlacedBlock> find = new Finder<>(PlacedBlock.class);

    /**
     * Creates an database entry for the given block.
     * <p>This will check if the block already exists in the database
     * and does nothing if so.
     * <p>Use the constructors of this class directly if you want to bulk save and cache the blocks.
     *
     * @param player the player that placed the block
     * @param block the block that was placed
     * @return the new or existing database entry
     */
    public static PlacedBlock create(Player player, Block block) {

        return at(block.getLocation()).orElseGet(() -> {
            PlacedBlock placedBlock = new PlacedBlock(block, player.getUniqueId());
            placedBlock.save();
            return placedBlock;
        });
    }

    public static Optional<PlacedBlock> at(Location location) {

        World world = location.getWorld();
        if (world == null) return Optional.empty();

        return find.query().where()
                .eq("x", location.getBlockX())
                .and().eq("y", location.getBlockY())
                .and().eq("z", location.getBlockZ())
                .and().eq("world", world.getUID())
                .findOneOrEmpty();
    }

    public static Optional<PlacedBlock> from(PlacedBlock block) {

        return find.query().where()
                .eq("x", block.x())
                .and().eq("y", block.y())
                .and().eq("z", block.z())
                .and().eq("world", block.world())
                .findList().stream().findAny();
    }

    @Index
    private UUID world;
    @Index
    private int x;
    @Index
    private int y;
    @Index
    private int z;
    private String type;
    private UUID placedBy;

    public PlacedBlock(Block block, UUID placedBy) {

        this(block);
        placedBy(placedBy);
    }

    public PlacedBlock(Block block) {

        world(block.getWorld().getUID());
        x(block.getX());
        y(block.getY());
        z(block.getZ());
        type(block.getType().getKey().toString());
    }
}
