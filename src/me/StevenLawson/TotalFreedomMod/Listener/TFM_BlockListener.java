package me.StevenLawson.TotalFreedomMod.Listener;

import me.StevenLawson.TotalFreedomMod.TFM_Heartbeat;
import me.StevenLawson.TotalFreedomMod.TFM_Log;
import me.StevenLawson.TotalFreedomMod.TFM_PlayerData;
import me.StevenLawson.TotalFreedomMod.TFM_ProtectedArea;
import me.StevenLawson.TotalFreedomMod.TFM_RollbackManager;
import me.StevenLawson.TotalFreedomMod.TFM_SuperadminList;
import me.StevenLawson.TotalFreedomMod.TFM_Util;
import me.StevenLawson.TotalFreedomMod.TotalFreedomMod;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;

public class TFM_BlockListener implements Listener
{
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event)
    {
        if (!TotalFreedomMod.allowFireSpread)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event)
    {
        if (!TotalFreedomMod.allowFirePlace)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        Location block_pos = event.getBlock().getLocation();

        if (TotalFreedomMod.nukeMonitor)
        {
            TFM_PlayerData playerdata = TFM_PlayerData.getPlayerData(player);

            Location player_pos = player.getLocation();

            boolean out_of_range = false;
            if (!player_pos.getWorld().equals(block_pos.getWorld()))
            {
                out_of_range = true;
            }
            else if (player_pos.distanceSquared(block_pos) > (TotalFreedomMod.nukeMonitorRange * TotalFreedomMod.nukeMonitorRange))
            {
                out_of_range = true;
            }

            if (out_of_range)
            {
                if (playerdata.incrementAndGetFreecamDestroyCount() > TotalFreedomMod.freecamTriggerCount)
                {
                    TFM_Util.bcastMsg(player.getName() + " has been flagged for possible freecam nuking.", ChatColor.RED);
                    TFM_Util.autoEject(player, "Freecam (extended range) block breaking is not permitted on this server.");

                    playerdata.resetFreecamDestroyCount();

                    event.setCancelled(true);
                    return;
                }
            }

            Long lastRan = TFM_Heartbeat.getLastRan();
            if (lastRan == null || lastRan + TotalFreedomMod.HEARTBEAT_RATE * 1000L < System.currentTimeMillis())
            {
                //TFM_Log.warning("Heartbeat service timeout - can't check block place/break rates.");
            }
            else
            {
                if (playerdata.incrementAndGetBlockDestroyCount() > TotalFreedomMod.nukeMonitorCountBreak)
                {
                    TFM_Util.bcastMsg(player.getName() + " is breaking blocks too fast!", ChatColor.RED);
                    TFM_Util.autoEject(player, "You are breaking blocks too fast. Nukers are not permitted on this server.");

                    playerdata.resetBlockDestroyCount();

                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (TotalFreedomMod.protectedAreasEnabled)
        {
            if (!TFM_SuperadminList.isUserSuperadmin(player))
            {
                if (TFM_ProtectedArea.isInProtectedArea(block_pos))
                {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRollbackBlockBreak(BlockBreakEvent event)
    {
        TFM_RollbackManager.blockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        Player player = event.getPlayer();
        Location block_pos = event.getBlock().getLocation();

        if (TotalFreedomMod.nukeMonitor)
        {
            TFM_PlayerData playerdata = TFM_PlayerData.getPlayerData(player);

            Location player_pos = player.getLocation();

            boolean out_of_range = false;
            if (!player_pos.getWorld().equals(block_pos.getWorld()))
            {
                out_of_range = true;
            }
            else if (player_pos.distanceSquared(block_pos) > (TotalFreedomMod.nukeMonitorRange * TotalFreedomMod.nukeMonitorRange))
            {
                out_of_range = true;
            }

            if (out_of_range)
            {
                if (playerdata.incrementAndGetFreecamPlaceCount() > TotalFreedomMod.freecamTriggerCount)
                {
                    TFM_Util.bcastMsg(player.getName() + " has been flagged for possible freecam building.", ChatColor.RED);
                    TFM_Util.autoEject(player, "Freecam (extended range) block building is not permitted on this server.");

                    playerdata.resetFreecamPlaceCount();

                    event.setCancelled(true);
                    return;
                }
            }

            Long lastRan = TFM_Heartbeat.getLastRan();
            if (lastRan == null || lastRan + TotalFreedomMod.HEARTBEAT_RATE * 1000L < System.currentTimeMillis())
            {
                //TFM_Log.warning("Heartbeat service timeout - can't check block place/break rates.");
            }
            else
            {
                if (playerdata.incrementAndGetBlockPlaceCount() > TotalFreedomMod.nukeMonitorCountPlace)
                {
                    TFM_Util.bcastMsg(player.getName() + " is placing blocks too fast!", ChatColor.RED);
                    TFM_Util.autoEject(player, "You are placing blocks too fast.");

                    playerdata.resetBlockPlaceCount();

                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (TotalFreedomMod.protectedAreasEnabled)
        {
            if (!TFM_SuperadminList.isUserSuperadmin(player))
            {
                if (TFM_ProtectedArea.isInProtectedArea(block_pos))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        switch (event.getBlockPlaced().getType())
        {
            case LAVA:
            case STATIONARY_LAVA:
            {
                if (TotalFreedomMod.allowLavaPlace)
                {
                    TFM_Log.info(String.format("%s placed lava @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));
                    player.sendMessage(ChatColor.GRAY + "Lava placement is currently disabled.");

                    event.setCancelled(true);
                }
                break;
            }
            case WATER:
            case STATIONARY_WATER:
            {
                if (TotalFreedomMod.allowWaterPlace)
                {
                    TFM_Log.info(String.format("%s placed water @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));
                    player.sendMessage(ChatColor.GRAY + "Water placement is currently disabled.");

                    event.setCancelled(true);
                }
                break;
            }
            case FIRE:
            {
                if (TotalFreedomMod.allowFirePlace)
                {
                    TFM_Log.info(String.format("%s placed fire @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));
                    player.sendMessage(ChatColor.GRAY + "Fire placement is currently disabled.");

                    event.setCancelled(true);
                }
                break;
            }
            case TNT:
            {
                if (TotalFreedomMod.allowExplosions)
                {
                    TFM_Log.info(String.format("%s placed TNT @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));

                    player.sendMessage(ChatColor.GRAY + "TNT is currently disabled.");
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRollbackBlockPlace(BlockPlaceEvent event)
    {
        TFM_RollbackManager.blockPlace(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event)
    {
        if (!TotalFreedomMod.allowFliudSpread)
        {
            event.setCancelled(true);
        }
    }
}
