package blockLock;

import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockLockManagerMenu implements Listener
{
    private static final int invSize = 9;
    private static final Material[] material = {Material.RED_WOOL, Material.AIR, Material.AIR, Material.HOPPER, Material.REDSTONE, Material.GRASS_BLOCK, Material.AIR, Material.AIR,
            Material.PLAYER_HEAD};
    private static final String[] name = {"Unlock", "", "", "Lock Hopper", "Lock Redstone", "Lock Block Below", "", "", "Friends"};

    private final BlockLockManager blManager;
    private final BlockLock blockLock;
    private final Inventory inv;
    private final Inventory friendsInv;
    private final ItemStack[] items;
    private List<ItemStack> friendsItems;

    public BlockLockManagerMenu(BlockLockManager blManager, BlockLock bl) {
        this.blManager = blManager;
        this.blockLock = bl;
        inv = Bukkit.createInventory(null, invSize, "BlockLock Manager");

        items = new ItemStack[invSize];
        ItemMeta meta = null;

        for(int i = 0; i < invSize; i++) {
            items[i] = new ItemStack(material[i]);
            if(items[i].getType().equals(Material.AIR))
                continue;
            meta = items[i].getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + name[i]);
            items[i].setItemMeta(meta);
        }

        for(int i = 0; i < invSize; i++) {
            inv.setItem(i, new ItemStack(items[i]));
        }

        SkullMeta skull = (SkullMeta) items[8].getItemMeta();
        skull.setOwningPlayer(Bukkit.getServer().getPlayer(bl.getOwner()));
        skull.setLore(Arrays.asList("Manage friends"));
        items[8].setItemMeta(skull);

        this.friendsInv = Bukkit.createInventory(null, 54, "Friends");
        this.friendsItems = null;
    }

    private boolean checkClick(ItemStack is, Player p) {
        int index = -1;
        for(int i = 0; i < invSize; i++) {
            if(items[i].equals(is)) {
                index = i;
                break;
            }
        }
        if(index != -1) {
            switch(index) {
                case 0:
                    blManager.unlock(p, p.getTargetBlock(null, 255));
                    p.closeInventory();
                    break;
                case 3:
                    blockLock.setHopperLock(!blockLock.isHopperLock());
                    break;
                case 4:
                    blockLock.setRedstoneLock(!blockLock.isRedstoneLock());
                    break;
                case 5:
                    blockLock.setBlockBelowLock(!blockLock.isBlockBelowLock());
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
            SkullMeta skull = (SkullMeta) friendsItems.get(index).getItemMeta();
            blockLock.removeFriend(skull.getOwningPlayer().getUniqueId());
            friendsItems.remove(index);
            updateFriendsInvItems();
            return true;
        }
        return false;
    }

    private boolean updateFriendsInvItems() {
        try {
            friendsItems = new ArrayList<>();

            OfflinePlayer p = null;
            SkullMeta skull = null;
            ItemStack is = null;
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ChatColor.RED + "Click to remove friend");
            List<String> allFriendsList = blManager.getFriends(blockLock.getOwner(), blockLock.getBlock());
            for(int i = 0; i < BlockLockManager.MAX_FRIENDS && i < allFriendsList.size(); i++) {
                p = Bukkit.getOfflinePlayer(allFriendsList.get(i));
                is = new ItemStack(Material.PLAYER_HEAD, 1);
                skull = (SkullMeta) is.getItemMeta();
                skull.setDisplayName(p.getName());
                skull.setOwningPlayer(p);
                skull.setLore(lore);
                is.setItemMeta(skull);
                friendsItems.add(is);
            }

            for(int i = 0; i < friendsItems.size(); i++) {
                friendsInv.setItem(i, friendsItems.get(i));
            }

            if(friendsItems.isEmpty()) {
                for(int i = 0; i < BlockLockManager.MAX_FRIENDS; i++) {
                    friendsInv.setItem(i, new ItemStack(Material.AIR));
                }
            }

            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(blManager.getMessagePrefix(), "ManangerMenu: Friend-Inventory: Item Update Exception: " + e.getLocalizedMessage());
            return false;
        }
    }

    private boolean updateInvItems() {
        try {
            ItemMeta meta = null;
            ArrayList<String> lore = null;

            // Hopper:
            meta = items[3].getItemMeta();
            lore = new ArrayList<>();
            if(blockLock.isHopperLock())
                lore.add(ChatColor.GREEN + "on");
            else
                lore.add(ChatColor.RED + "off");
            meta.setLore(lore);
            items[3].setItemMeta(meta);

            // Redstone:
            meta = items[4].getItemMeta();
            lore = new ArrayList<>();
            if(blockLock.isRedstoneLock())
                lore.add(ChatColor.GREEN + "on");
            else
                lore.add(ChatColor.RED + "off");
            meta.setLore(lore);
            items[4].setItemMeta(meta);

            // Block Below:
            meta = items[5].getItemMeta();
            lore = new ArrayList<>();
            if(blockLock.isBlockBelowLock())
                lore.add(ChatColor.GREEN + "on");
            else
                lore.add(ChatColor.RED + "off");
            meta.setLore(lore);
            items[5].setItemMeta(meta);

            inv.setContents(items);

            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(blManager.getMessagePrefix(), "ManangerMenu: Inventory: Item Update Exception: " + e.getLocalizedMessage());
            return false;
        }
    }

    public void openFriends(Player p) {
        if(updateFriendsInvItems()) {
            p.openInventory(friendsInv);
        }
        else {
            blManager.sendMessage(p, "ManangerMenu: Friend-Inventory: Item Update Exception (see log for details)");
        }
    }

    public boolean open(Player p) {
        if(updateInvItems()) {
            p.openInventory(inv);
            return true;
        }
        else {
            blManager.sendMessage(p, "ManangerMenu: Inventory: Item Update Exception (see log for details)");
            return false;
        }
    }

    // Protecting inv:

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
                checkClick(event.getCurrentItem(), (Player) event.getWhoClicked());
            }
            else if(friendsInv.equals(event.getClickedInventory())) {
                checkFriendsClick(event.getCurrentItem());
            }
            event.setCancelled(true);
        }
    }

    /**
     * Preventing anyone(hoppers, etc) from grabbing items from inv
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if(inv.equals(event.getSource()) || inv.equals(event.getDestination()) || friendsInv.equals(event.getSource()) || friendsInv.equals(event.getDestination()))
            event.setCancelled(true);
    }

}
