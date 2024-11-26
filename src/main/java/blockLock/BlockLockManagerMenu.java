package blockLock;

import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BlockLockManagerMenu implements Listener
{
    private static final Material[] material = {Material.RED_WOOL, Material.AIR, Material.AIR, Material.HOPPER, Material.AIR, Material.REDSTONE, Material.AIR, Material.AIR,
            Material.PLAYER_HEAD};
    private static final String[] name = {"Unlock", "", "", "Lock Hopper", "", "Lock Redstone", "", "", "Local Friends"};
    public static final int INV_SIZE = material.length;
    public static final String INV_NAME = "BlockLock Manager";
    public static final String FRIEND_INV_NAME = "Manage local friends";

    private final BlockLockManager blManager;
    private final Block block;
    private final Inventory inv;
    private final Inventory friendsInv;
    private final ItemStack[] items;
    private List<ItemStack> friendsItems;

    public BlockLockManagerMenu(BlockLockManager blManager, Block block) {
        this.blManager = blManager;
        this.block = block;
        inv = Bukkit.createInventory(null, INV_SIZE, INV_NAME);

        items = new ItemStack[INV_SIZE];
        ItemMeta meta = null;

        for(int i = 0; i < INV_SIZE; i++) {
            items[i] = new ItemStack(material[i]);
            if(items[i].getType().equals(Material.AIR))
                continue;
            meta = items[i].getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + name[i]);
            items[i].setItemMeta(meta);
        }

        for(int i = 0; i < INV_SIZE; i++) {
            inv.setItem(i, new ItemStack(items[i]));
        }

        SkullMeta skull = (SkullMeta) items[8].getItemMeta();
        skull.setOwningPlayer(Bukkit.getServer().getPlayer(BlockLock.getOwner(block)));
        skull.setLore(Arrays.asList("Click to remove local friend"));
        items[8].setItemMeta(skull);

        this.friendsInv = Bukkit.createInventory(null, BlockLockManager.MAX_LOCAL_FRIENDS, FRIEND_INV_NAME);
        this.friendsItems = null;

        Bukkit.getScheduler().runTaskLater(Manager.getInstance(),
                () -> Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance()),
                1);
    }

    private boolean checkClick(int slotIndex, Player p) {
        if(slotIndex != -1) {
            switch(slotIndex) {
                case 0:
                    blManager.unlock(p, block, true);
                    p.closeInventory();
                    return true;
                case 3:
                    BlockLock.switchHopperLock(block);
                    break;
                case 5:
                    BlockLock.switchRedstoneLock(block);
                    break;
                case 8:
                    openFriends(p);
                    break;
                default:
                    break;
            }
            updateInvItems();
            return true;
        }
        return false;
    }

    private boolean checkFriendsClick(ItemStack is) {
        int index = friendsItems.indexOf(is);
        if(index != -1) {
            SkullMeta skull = (SkullMeta) is.getItemMeta();
            BlockLock.removeLocalFriend(block, skull.getOwningPlayer().getUniqueId());
            friendsItems.remove(index);
            updateFriendsInvItems();
            return true;
        }
        return false;
    }

    private boolean updateInvItems() {
        try {
            ItemMeta meta = null;
            ArrayList<String> lore = null;

            // Hopper:
            meta = items[3].getItemMeta();
            lore = new ArrayList<>();
            lore.add(BlockLock.getHopperLock(block) ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
            meta.setLore(lore);
            items[3].setItemMeta(meta);

            // Redstone:
            meta = items[5].getItemMeta();
            lore = new ArrayList<>();
            lore.add(BlockLock.getRedstoneLock(block) ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
            meta.setLore(lore);
            items[5].setItemMeta(meta);

            inv.setContents(items);

            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(blManager.getMessagePrefix(), "ManangerMenu: Inventory: Item Update Exception: " + e.getLocalizedMessage());
            return false;
        }
    }

    private boolean updateFriendsInvItems() {
        try {
            friendsItems = new ArrayList<>();
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ChatColor.RED + "Click to remove local friend");
            List<UUID> allFriendsList = new ArrayList<>(BlockLock.getLocalFriends(block));
            for(int i = 0; i < BlockLockManager.MAX_LOCAL_FRIENDS && i < allFriendsList.size(); i++) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(allFriendsList.get(i));
                ItemStack is = new ItemStack(Material.PLAYER_HEAD, 1);
                if(!is.hasItemMeta()) {
                    is.setItemMeta(Bukkit.getServer().getItemFactory().getItemMeta(Material.PLAYER_HEAD));
                }
                if(is.getItemMeta() instanceof SkullMeta skull) {
                    skull.setDisplayName(p.getName());
                    skull.setOwningPlayer(p);
                    skull.setLore(lore);
                    is.setItemMeta(skull);
                    friendsItems.add(is);
                }
            }

            for(int i = 0; i < friendsItems.size(); i++) {
                friendsInv.setItem(i, friendsItems.get(i));
            }

            if(friendsItems.isEmpty()) {
                for(int i = 0; i < BlockLockManager.MAX_LOCAL_FRIENDS; i++) {
                    friendsInv.setItem(i, new ItemStack(Material.AIR));
                }
            }

            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(blManager.getMessagePrefix(), "ManangerMenu: Friend-Inventory: Item Update Exception: " + e.getLocalizedMessage());
            return false;
        }
    }

    public boolean open(Player p) {
        return updateInvItems() && p.openInventory(inv) != null;
    }

    public boolean openFriends(Player p) {
        return updateFriendsInvItems() && p.openInventory(friendsInv) != null;
    }

    //------------------------------------------------------------------------------------------------------------------
    // Protecting inventory:

    /**
     * Preventing the player from moving items from or to inv
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Note:
        // event.getInventory() = inv
        // event.getClickedInventory() = inv or Player (depending on which inv was clicked)

        if(inv.equals(event.getInventory()) || friendsInv.equals(event.getInventory())) {
            if(inv.equals(event.getClickedInventory())) {
                checkClick(event.getSlot(), (Player) event.getWhoClicked());
            }
            else if(friendsInv.equals(event.getClickedInventory())) {
                checkFriendsClick(event.getCurrentItem());
            }
            event.setCancelled(true);
        }
    }

    /**
     * Preventing anyone(hoppers, etc.) from grabbing items from inv
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if(inv.equals(event.getSource()) || inv.equals(event.getDestination()) || friendsInv.equals(event.getSource()) || friendsInv.equals(event.getDestination()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(event.getInventory().equals(inv) || event.getInventory().equals(friendsInv)) {
            HandlerList.unregisterAll(this);
        }
    }

}
