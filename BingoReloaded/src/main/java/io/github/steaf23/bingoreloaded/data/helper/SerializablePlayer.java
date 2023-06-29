package io.github.steaf23.bingoreloaded.data.helper;

import com.google.common.util.concurrent.ClosingFuture;
import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.util.Message;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BinaryOperator;

@SerializableAs("Player")
public class SerializablePlayer implements ConfigurationSerializable
{
    // Generated this data in plugin version x
    public String pluginVersion;
    public UUID playerId;
    public Location location;
    public double health;
    public int hunger;
    public GameMode gamemode;
    public Location spawnPoint;
    public int xpLevel;
    public float xpPoints;
    public ItemStack[] inventory;
    public ItemStack[] enderInventory;

    public static SerializablePlayer fromPlayer(JavaPlugin plugin, Player player)
    {
        SerializablePlayer data = new SerializablePlayer();
        data.pluginVersion = plugin.getDescription().getVersion();
        data.playerId = player.getUniqueId();
        data.location = player.getLocation();
        data.health = player.getHealth();
        data.hunger = player.getFoodLevel();
        data.gamemode = player.getGameMode();
        data.spawnPoint = player.getBedSpawnLocation();
        data.xpLevel = player.getLevel();
        data.xpPoints = player.getExp();
        data.inventory = player.getInventory().getContents();
        data.enderInventory = player.getEnderChest().getContents();
        return data;
    }

    /**
     * Reset all player data and set location
     * @param player
     * @param location
     * @return
     */
    public SerializablePlayer reset(Player player, Location location)
    {
        this.location = location;
        playerId = player.getUniqueId();
        health = 20.0;
        hunger = 20;
        gamemode = player.getGameMode();
        spawnPoint = null;
        xpLevel = 0;
        xpPoints = 0.0f;
        inventory = new ItemStack[player.getInventory().getSize()];
        enderInventory = new ItemStack[player.getInventory().getSize()];
        return this;
    }

    public void toPlayer(Player player)
    {
        if (!playerId.equals(player.getUniqueId()))
            return;

        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);

        player.setHealth(health);
        player.setFoodLevel(hunger);
        player.setGameMode(gamemode);
        player.setBedSpawnLocation(spawnPoint);
        player.setLevel(xpLevel);
        player.setExp(xpPoints);

        player.getInventory().clear();
        if (inventory != null)
        {
            player.getInventory().setContents(inventory);
        }
        player.getEnderChest().clear();
        if (enderInventory == null)
        {
            player.getEnderChest().setContents(enderInventory);
        }
        player.updateInventory();
    }

    @NotNull
    @Override
    public Map<String, Object> serialize()
    {
        Map<String, Object> data = new HashMap<>();

        data.put("version", pluginVersion);
        data.put("uuid", playerId.toString());
        data.put("location", location);
        data.put("health", health);
        data.put("hunger", hunger);
        data.put("gamemode", gamemode.toString());
        data.put("spawn_point", spawnPoint);
        data.put("xp_level", xpLevel);
        data.put("xp_points", xpPoints);
        data.put("inventory", inventory);
        data.put("ender_inventory", enderInventory);

        return data;
    }

    public static SerializablePlayer deserialize(Map<String, Object> data)
    {
        SerializablePlayer player = new SerializablePlayer();
        player.pluginVersion = (String)data.getOrDefault("version", "-");
        player.playerId = UUID.fromString((String)data.getOrDefault("uuid", ""));
        player.location = (Location)data.getOrDefault("location", new Location(null, 0.0, 0.0, 0.0));
        player.health = (Double)data.getOrDefault("health", 0.0);
        player.hunger = (Integer)data.getOrDefault("hunger", 0);
        player.gamemode = GameMode.valueOf((String)data.getOrDefault("gamemode", "SURVIVAL"));
        player.spawnPoint = (Location)data.getOrDefault("location", new Location(null, 0.0, 0.0, 0.0));
        player.xpLevel = (Integer)data.getOrDefault("xp_level", 0);
        player.xpPoints = ((Double)(data.getOrDefault("xp_points", 0.0f))).floatValue();
        player.inventory = ((ArrayList<ItemStack>)data.getOrDefault("inventory", null)).toArray(new ItemStack[]{});
        player.enderInventory = ((ArrayList<ItemStack>)data.getOrDefault("ender_inventory", null)).toArray(new ItemStack[]{});

        return player;
    }
}
