package io.github.steaf23.bingoreloaded.player;

import io.github.steaf23.bingoreloaded.gameloop.BingoGame;
import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.gameloop.BingoSession;
import io.github.steaf23.bingoreloaded.data.BingoStatType;
import io.github.steaf23.bingoreloaded.data.BingoTranslation;
import io.github.steaf23.bingoreloaded.gui.EffectOptionFlags;
import io.github.steaf23.bingoreloaded.item.ItemCooldownManager;
import io.github.steaf23.bingoreloaded.settings.PlayerKit;
import io.github.steaf23.bingoreloaded.tasks.BingoTask;
import io.github.steaf23.bingoreloaded.util.Message;
import io.github.steaf23.bingoreloaded.util.PDCHelper;
import io.github.steaf23.bingoreloaded.util.TranslatedMessage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * This class describes a player in a single bingo session.
 * This class will still exist if the player leaves the game/world.
 * This instance will be removed when the session gets destroyed.
 */
public class BingoPlayer implements BingoParticipant
{
    public final String playerName;
    private final BingoSession session;
    private final BingoTeam team;
    private final UUID playerId;
    private final String displayName;
    private final ItemCooldownManager itemCooldowns;

    public BingoPlayer(Player player, BingoTeam team, BingoSession session)
    {
        this.playerId = player.getUniqueId();
        this.team = team;
        this.session = session;
        this.playerName = player.getName();
        this.displayName = player.getDisplayName();
        this.itemCooldowns = new ItemCooldownManager();
    }

    @Override
    public Optional<Player> gamePlayer()
    {
        if (!offline().isOnline())
            return Optional.ofNullable(null);

        Player player = Bukkit.getPlayer(playerId);
        if (!BingoReloaded.getWorldNameOfDimension(player.getWorld()).equals(session.worldName))
        {
            return Optional.ofNullable(null);
        }
        return Optional.ofNullable(player);
    }

    @Override
    public UUID getId()
    {
        return playerId;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Nullable
    public Optional<Player> asOnlinePlayer()
    {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }

    public OfflinePlayer offline()
    {
        return Bukkit.getOfflinePlayer(playerId);
    }

    public void giveKit(PlayerKit kit)
    {
        if (gamePlayer().isEmpty())
            return;

        Player player = gamePlayer().get();

        Message.log("Giving kit to " + player.getDisplayName(), session.worldName);

        var items = kit.getItems(team.getColor());
        player.closeInventory();
        Inventory inv = player.getInventory();
        inv.clear();
        items.forEach(i ->
        {
            var meta = i.getItemMeta();

            // Show enchantments except on the wand
            if (!PlayerKit.WAND_ITEM.isKeyEqual(i))
            {
                meta.removeItemFlags(ItemFlag.values());
            }
            var pdc = meta.getPersistentDataContainer();
            pdc = PDCHelper.setBoolean(pdc, "kit.kit_item", true);

            i.setItemMeta(meta);
            inv.setItem(i.getSlot(), i);
        });
    }

    public void giveBingoCard()
    {
        if (gamePlayer().isEmpty())
            return;

        Player player = gamePlayer().get();

        Message.log("Giving card to " + player.getDisplayName(), session.worldName);

        BingoReloaded.scheduleTask(task -> {
            for (ItemStack itemStack : player.getInventory())
            {
                if (PlayerKit.CARD_ITEM.isKeyEqual(itemStack))
                {
                    player.getInventory().remove(itemStack);
                    break;
                }
            }

            player.getInventory().setItemInOffHand(PlayerKit.CARD_ITEM.copyToSlot(8));
        });
    }

    public void giveEffects(EnumSet<EffectOptionFlags> effects, int gracePeriod)
    {
        if (gamePlayer().isEmpty())
            return;

        takeEffects(false);
        Player player = gamePlayer().get();

        Message.log("Giving effects to " + player.getDisplayName(), session.worldName);

        BingoReloaded.scheduleTask(task -> {
            if (effects.contains(EffectOptionFlags.NIGHT_VISION))
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
            if (effects.contains(EffectOptionFlags.WATER_BREATHING))
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 1, false, false));
            if (effects.contains(EffectOptionFlags.FIRE_RESISTANCE))
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
            if (effects.contains(EffectOptionFlags.SPEED))
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 2, 100, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2, 100, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, BingoReloaded.ONE_SECOND * gracePeriod, 100, false, false));
        });
    }

    /**
     *
     * @param force ignore if the player is actually in the world playing the game at this moment.
     */
    public void takeEffects(boolean force)
    {
        if (force)
        {
            if (offline().isOnline())
            {
                Message.log("Taking effects from " + asOnlinePlayer().get().getDisplayName(), session.worldName);

                for (PotionEffectType effect : PotionEffectType.values())
                {
                    Bukkit.getPlayer(playerId).removePotionEffect(effect);
                }
            }
        }
        else
        {
            if (gamePlayer().isEmpty())
                return;

            Message.log("Taking effects from " + asOnlinePlayer().get().getDisplayName(), session.worldName);

            for (PotionEffectType effect : PotionEffectType.values())
            {
                gamePlayer().get().removePotionEffect(effect);
            }
        }
    }

    public void showDeathMatchTask(BingoTask task)
    {
        if (gamePlayer().isEmpty())
            return;

        String itemKey = task.material.isBlock() ? "block" : "item";
        itemKey += ".minecraft." + task.material.getKey().getKey();

        new TranslatedMessage(BingoTranslation.DEATHMATCH).color(ChatColor.GOLD)
                .component(new TranslatableComponent(itemKey))
                .send(gamePlayer().get());
    }

    @Override
    public boolean alwaysActive()
    {
        return false;
    }

    public boolean useGoUpWand(ItemStack wand, double wandCooldownSeconds, int downDistance, int upDistance, int platformLifetimeSeconds)
    {
        if (gamePlayer().isEmpty())
             return false;

        Player player = gamePlayer().get();
        if (!PlayerKit.WAND_ITEM.isKeyEqual(wand))
            return false;

        if (!itemCooldowns.isCooldownOver(wand))
        {
            double timeLeft = itemCooldowns.getTimeLeft(wand) / 1000.0;
            new TranslatedMessage(BingoTranslation.COOLDOWN).color(ChatColor.RED).arg(String.format("%.2f", timeLeft)).send(player);
            return false;
        }

        BingoReloaded.scheduleTask(task -> {
            itemCooldowns.addCooldown(wand, (int)(wandCooldownSeconds * 1000));

            double distance = 0.0;
            double fallDistance = 5.0;
            // Use the wand
            if (gamePlayer().get().isSneaking())
            {
                distance = -downDistance;
                fallDistance = 0.0;
            }
            else
            {
                distance = upDistance + 5;
                fallDistance = 5.0;
            }

            Location newLocation = player.getLocation();
            newLocation.setY(newLocation.getY() + distance + fallDistance);
            player.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            newLocation.setY(newLocation.getY() - fallDistance);

            BingoGame.spawnPlatform(newLocation, 1);

            BingoReloaded.scheduleTask(laterTask -> {
                BingoGame.removePlatform(newLocation, 1);
            }, Math.max(0, platformLifetimeSeconds) * BingoReloaded.ONE_SECOND);

            player.playSound(player, Sound.ENTITY_SHULKER_TELEPORT, 0.8f, 1.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, BingoReloaded.ONE_SECOND * 10, 100, false, false));

            BingoReloaded.incrementPlayerStat(player, BingoStatType.WAND_USES);
        });
        return true;
    }

    @Override
    public BingoSession getSession()
    {
        return session;
    }

    @Nullable
    @Override
    public BingoTeam getTeam()
    {
        return team;
    }
}
