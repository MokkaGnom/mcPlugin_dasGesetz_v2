package deathChest;

//Bukkit-Event:

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static deathChest.DeathChestConstants.*;

public class DeathChestManager implements Listener, ManagedPlugin
{
    /**
     * Zeit bis entfernen (in Sekunden)
     */
    private final long timer;

    /**
     * Ob der Inhalt der DeathChest nach Ablauf des Timers gedroppt werden sollen
     */
    private final boolean dropItems;

    /**
     * Alle DeathChests, nach Spieler-UUID gemappt
     */
    private final Map<UUID, List<DeathChest>> deathChests;

    public DeathChestManager() {
        this.timer = Manager.getInstance().getConfig().getInt(DESPAWN_TIME_JSON_KEY);
        this.dropItems = Manager.getInstance().getConfig().getBoolean(DROP_ITEMS_JSON_KEY);
        deathChests = new HashMap<>();
    }

    public String getMessageString(String message) {
        return ChatColor.GRAY + "[" + ChatColor.AQUA + getName() + ChatColor.GRAY + "] " + ChatColor.WHITE + message;
    }

    public boolean sendMessage(CommandSender receiver, List<String> messages) {
        if(receiver != null) {
            for(String message : messages) {
                receiver.sendMessage(getMessageString(message));
            }
            return true;
        }
        return false;
    }

    public boolean sendMessage(CommandSender receiver, String message) {
        if(receiver != null) {
            receiver.sendMessage(getMessageString(message));
            return true;
        }
        return false;
    }

    public boolean sendErrorMessage(CommandSender receiver, String message) {
        if(receiver != null) {
            receiver.sendMessage(getMessageString(message));
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), message);
            return true;
        }
        return false;
    }

    public boolean createDeathCest(Player player, List<ItemStack> items) {
        List<DeathChest> chests = Objects.requireNonNullElse(deathChests.get(player.getUniqueId()), new ArrayList<>());
        DeathChest chest = new DeathChest(player, items);
        Bukkit.getScheduler().runTaskLater(Manager.getInstance(), () -> chest.remove(dropItems), timer);
        deathChests.put(player.getUniqueId(), chests);
        return chests.add(chest);
    }

    public List<DeathChest> getDeathChests(UUID uuid) {
        return Objects.requireNonNullElse(deathChests.get(uuid), new ArrayList<>());
    }

    public boolean isBlockDeathChest(Block block) {
        return block.hasMetadata(DeathChest.METADATA_KEY);
    }

    public DeathChest getDeathChest(Block block, UUID playerUUID) {
        if(playerUUID != null) {
            return getDeathChests(playerUUID).stream().filter(dc -> dc.getLocation().getBlock().equals(block)).toList().getFirst();
        }
        else {
            for(UUID uuid : deathChests.keySet()) {
                DeathChest deathChest = getDeathChests(uuid).stream().filter(dc -> dc.getLocation().getBlock().equals(block)).toList().getFirst();
                if(deathChest != null) {
                    return deathChest;
                }
            }
        }
        return null;
    }

    public DeathChest getDeathChest(Inventory inventory, UUID playerUUID) {
        if(playerUUID != null) {
            return getDeathChests(playerUUID).stream().filter(dc -> dc.getChestInventory().equals(inventory)).toList().getFirst();
        }
        else {
            for(UUID uuid : deathChests.keySet()) {
                DeathChest deathChest = getDeathChests(uuid).stream().filter(dc -> dc.getChestInventory().equals(inventory)).toList().getFirst();
                if(deathChest != null) {
                    return deathChest;
                }
            }
        }
        return null;
    }

    public String getDeathChestInfoForPlayer(DeathChest deathChest) {
        return String.format("Position(X: %s, Y: %s, Z: %s) - TTL(%s)",
                deathChest.getLocation().getX(), deathChest.getLocation().getY(), deathChest.getLocation().getZ(),
                getTimer() - ((System.currentTimeMillis() - deathChest.getTimeSpawned()) / 1000));
    }

    /**
     * Creates a DeathChest
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if(p.hasPermission("dg.deathChestPermission")) {
            if(createDeathCest(p, event.getDrops())) {
                event.getDrops().clear();
                //TODO: Ausgabe:
                /*DeathChestManager.sendMessage(owner, "Created at (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ") T: " + timer / 20 + "s");
                Bukkit.getConsoleSender().sendMessage("Created Death Chest for " + owner.toString() + " at (" + block.getX() + ", " + block.getY() + ", " + block.getZ() + ")");*/
            }
        }
        else {
            sendMessage(p, "No permission to create a DeathChest!");
        }
    }

    /**
     * Let a player with according permissions, collect the chest, either by sneak+click or normally opening the chest
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK && isBlockDeathChest(event.getClickedBlock())) {
            Player player = event.getPlayer();
            DeathChest dc = getDeathChest(event.getClickedBlock(), event.getPlayer().getUniqueId());
            if(dc != null) {
                event.setCancelled(true); // To stop the "normal" chest inventory from opening
                if((dc.checkIfOwner(player.getUniqueId()) && player.hasPermission("dg.deathChestPermission")) || player.hasPermission("dg.deathChestByPassPermission")) {
                    if(dc.collect()) {
                        deathChests.get(player.getUniqueId()).remove(dc);
                        //TODO: Ausgabe:
                        sendMessage(player, "Collected!");
                        /*DeathChestManager.sendMessage(owner, "Removed at (" + chestBlock.getX() + ", " + chestBlock.getY() + ", " + chestBlock.getZ() + ")");
                        Bukkit.getConsoleSender().sendMessage("Removed Death Chest from " + owner.toString() + " at X:" + chestBlock.getLocation().getX() + " Y:" + chestBlock.getLocation().getY()
                            + " Z:" + chestBlock.getLocation().getZ());*/
                    }
                }
                else {
                    sendMessage(player, "No permission!");
                    // sendMessage(dc.getOwner().getUniqueId(), p.getName() + " tried to open your Death Chest at X:" + dc.getBlock().getLocation().getX() + " Y:"
                    // + dc.getBlock().getLocation().getY() + " Z:" + dc.getBlock().getLocation().getZ());
                }
            }
        }
    }

    /**
     * Checks if DeathChest is empty and removes it if so
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        DeathChest deathChest = getDeathChests(event.getPlayer().getUniqueId()).stream()
                .filter(dc -> dc.getChestInventory().equals(event.getInventory())).toList().getFirst();
        if(deathChest != null) {
            deathChest.removeIfEmpty();
        }
    }

    // Protecting DeathChest:

    /**
     * Preventing player from putting items in the DeathChest
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Note:
        // event.getInventory() = DeathChest / Chest
        // event.getClickedInventory() = DeathChest / Chest or Player (depending on which inv was clicked)

        DeathChest dc = getDeathChest(event.getInventory(), event.getWhoClicked().getUniqueId());
        if(dc != null) {
            if((event.getClickedInventory().getType().equals(InventoryType.PLAYER) && (event.isShiftClick() && event.isLeftClick()))
                    || (dc.getChestInventory().equals(event.getClickedInventory()) && !event.getCursor().getType().equals(Material.AIR))) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Preventing anyone from breaking the DeathChest
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(isBlockDeathChest(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Preventing the DeathChest from being damaged
     */
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if(isBlockDeathChest(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Preventing the DeathChest from being blown up by a Creeper, Wither or TNT
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(event.getEntity() instanceof Creeper || event.getEntity() instanceof TNTPrimed || event.getEntity() instanceof Wither) {
            event.blockList().removeAll(event.blockList().stream()
                    .filter(this::isBlockDeathChest).toList());
        }
    }

    /**
     * Preventing anyone(hoppers, etc.) than the player from grabbing items from the DeathChest
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if((getDeathChest(event.getSource(), null) != null && !event.getDestination().getType().equals(InventoryType.PLAYER))
                || getDeathChest(event.getDestination(), null) != null) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onEnable() {
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try {
            DeathChestCommands dcc = new DeathChestCommands(this);
            Manager.getInstance().getCommand(DeathChestCommands.CommandStrings.ROOT).setExecutor(dcc);
            Manager.getInstance().getCommand(DeathChestCommands.CommandStrings.ROOT).setTabCompleter(dcc);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        for(Map.Entry<UUID, List<DeathChest>> entry : deathChests.entrySet()) {
            for(DeathChest dc : entry.getValue()) {
                dc.remove(true);
            }
        }
    }

    @Override
    public String getName() {
        return "DeathChest";
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(DESPAWN_TIME_JSON_KEY, 600); // 10min
        config.addDefault(DROP_ITEMS_JSON_KEY, true);
    }

    public long getTimer() {
        return timer;
    }

    public boolean isDropItems() {
        return dropItems;
    }
}
