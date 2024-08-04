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
import org.bukkit.event.HandlerList;
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
import utility.ErrorMessage;

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

    private final boolean messagePlayer;

    /**
     * Alle DeathChests, nach Spieler-UUID gemappt
     */
    private final Map<UUID, List<DeathChest>> deathChests;

    private static final String DESPAWN_TIME_JSON_KEY = "DeathChest.DespawnInSeconds";
    private static final String DROP_ITEMS_JSON_KEY = "DeathChest.DespawnDropping";
    private static final String MESSAGE_TO_PLAYER_JSON_KEY = "DeathChest.MessagePlayer";

    public DeathChestManager() {
        this.timer = Manager.getInstance().getConfig().getInt(DESPAWN_TIME_JSON_KEY);
        this.dropItems = Manager.getInstance().getConfig().getBoolean(DROP_ITEMS_JSON_KEY);
        this.messagePlayer = Manager.getInstance().getConfig().getBoolean(MESSAGE_TO_PLAYER_JSON_KEY);
        deathChests = new HashMap<>();
    }


    public void sendMessage(CommandSender receiver, List<String> messages) {
        for(String message : messages) {
            sendMessage(receiver, message);
        }
    }

    public DeathChest createDeathCest(Player player, List<ItemStack> items) {
        List<DeathChest> chests = Objects.requireNonNullElse(deathChests.get(player.getUniqueId()), new ArrayList<>());
        DeathChest chest = new DeathChest(player, items);
        chest.setTaskID(Bukkit.getScheduler().runTaskLater(
                Manager.getInstance(), () -> removeDeathChest(chest, false, false), Manager.convertSecondsToTicks(timer)
        ).getTaskId());
        chests.add(chest);
        deathChests.put(player.getUniqueId(), chests);
        return chest;
    }

    public boolean removeDeathChest(DeathChest chest, boolean onlyIfEmpty) {
        return removeDeathChest(chest, onlyIfEmpty, true);
    }

    public boolean removeDeathChest(DeathChest chest, boolean onlyIfEmpty, boolean cancelRemoveTask) {
        boolean removed = (onlyIfEmpty ? chest.removeIfEmpty() : chest.remove(dropItems));
        if(removed) {
            if(cancelRemoveTask) {
                Bukkit.getScheduler().cancelTask(chest.getTaskID());
            }
            sendMessage(Bukkit.getPlayer(chest.getOwner()), String.format(COLLECTED_PLAYER_MESSAGE));
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(COLLECTED_SERVER_MESSAGE, chest.toString()));
            deathChests.get(chest.getOwner()).remove(chest);
            return true;
        }
        return false;
    }

    public List<DeathChest> getDeathChests(UUID uuid) {
        return Objects.requireNonNullElse(deathChests.get(uuid), new ArrayList<>());
    }

    public boolean isBlockDeathChest(Block block) {
        return block.hasMetadata(DeathChest.METADATA_KEY);
    }

    public DeathChest getDeathChest(Block block) {
        if(block == null) return null;
        for(UUID uuid : deathChests.keySet()) {
            List<DeathChest> deathChests = getDeathChests(uuid).stream().filter(dc -> dc.getLocation().getBlock().equals(block)).toList();
            if(!deathChests.isEmpty()) {
                return deathChests.getFirst();
            }
        }
        return null;
    }

    public DeathChest getDeathChest(Inventory inventory, UUID playerUUID) {
        if(playerUUID != null) {
            List<DeathChest> chests = getDeathChests(playerUUID).stream().filter(dc -> dc.getChestInventory().equals(inventory)).toList();
            if(!chests.isEmpty()) {
                return chests.getFirst();
            }
        }
        else {
            for(UUID uuid : deathChests.keySet()) {
                List<DeathChest> deathChests = getDeathChests(uuid).stream().filter(dc -> dc.getChestInventory().equals(inventory)).toList();
                if(!deathChests.isEmpty()) {
                    return deathChests.getFirst();
                }
            }
        }
        return null;
    }

    public String getDeathChestInfoForPlayer(DeathChest deathChest) {
        return String.format(INFO_FOR_PLAYER,
                deathChest.getLocation().getX(), deathChest.getLocation().getY(), deathChest.getLocation().getZ(),
                getTimer() - ((System.currentTimeMillis() - deathChest.getTimeSpawned()) / 1000));
    }

    /**
     * Creates a DeathChest
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if(hasDefaultUsePermission(p)) {
            DeathChest dc = createDeathCest(p, event.getDrops());
            if(dc != null) {
                event.getDrops().clear();
                sendMessage(p, String.format(CREATED_PLAYER_MESSAGE, dc.getLocation().getX(), dc.getLocation().getY(), dc.getLocation().getZ(), timer));
                Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(CREATED_SERVER_MESSAGE, dc.toString()));
            }
        }
        else {
            sendMessage(p, ERROR_NO_PERMISSION_CREATE);
        }
    }

    /**
     * Let a player with according permissions, collect the chest, either by sneak+click or normally opening the chest
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK && isBlockDeathChest(event.getClickedBlock())) {
            Player player = event.getPlayer();
            DeathChest dc = getDeathChest(event.getClickedBlock());
            if(dc != null) {
                event.setCancelled(true); // To stop the "normal" chest inventory from opening
                if((dc.checkIfOwner(player.getUniqueId()) && hasDefaultUsePermission(player)) || hasAdminPermission(player)) {
                    if(dc.collect()) {
                        removeDeathChest(dc, false);
                    }
                }
                else {
                    sendMessage(player, ErrorMessage.NO_PERMISSION.message());
                    if(this.messagePlayer) {
                        sendMessage(Bukkit.getPlayer(dc.getOwner()), String.format(COLLECT_STEAL_MESSAGE_TO_OWNER, player.getName(), dc.getLocation().getX(), dc.getLocation().getY(), dc.getLocation().getZ()));
                    }
                }
            }
        }
    }

    /**
     * Checks if DeathChest is empty and removes it if so
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        List<DeathChest> deathChestList = getDeathChests(event.getPlayer().getUniqueId()).stream()
                .filter(dc -> dc.getChestInventory().equals(event.getInventory())).toList();
        if(!deathChestList.isEmpty()) {
            removeDeathChest(deathChestList.getFirst(), true);
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
        HandlerList.unregisterAll(this);
        try {
            Manager.getInstance().getCommand(DeathChestCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(DeathChestCommands.CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "DeathChest";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.AQUA;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.deathChestPermission", "dg.deathChestByPassPermission");
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(DESPAWN_TIME_JSON_KEY, 600); // 10min
        config.setInlineComments(DESPAWN_TIME_JSON_KEY, List.of("Zeit, bis die DeathChest automatisch despawned (in Sekunden)"));
        config.addDefault(DROP_ITEMS_JSON_KEY, true);
        config.setInlineComments(DROP_ITEMS_JSON_KEY, List.of("Ob die Items gedropped werden sollen, wenn die DeathChest despawned"));
        config.addDefault(MESSAGE_TO_PLAYER_JSON_KEY, false);
        config.setInlineComments(MESSAGE_TO_PLAYER_JSON_KEY, List.of("Ob der Spieler benachrichtigt werden soll, wenn jemand anderes versucht die DeathChest abzubauen, etc."));
    }

    public long getTimer() {
        return timer;
    }

    public boolean isDropItems() {
        return dropItems;
    }
}
