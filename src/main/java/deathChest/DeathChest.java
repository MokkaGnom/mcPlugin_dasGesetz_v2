package deathChest;

import manager.Manager;
import org.bukkit.Bukkit;
//Bukkit:
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.block.Block;
import org.bukkit.metadata.MetadataValue;

import java.util.*;
//Java:
import java.util.Map.Entry;

public class DeathChest
{
    public static final Material DEATH_CHEST_MATERIAL = Material.CHEST;
    public static final String METADATA_KEY = "DeathChest";
    public static final String CHEST_NAME = "%s's Deathchest";
    public static final int METADATA_ID_INDEX = 0;

    private final long timeSpawned;
    private final Material oldMaterial;
    private final Location location;
    private final Inventory inventory;
    private final UUID owner;
    private int taskID;

    public DeathChest(Player p, List<ItemStack> items) {
        this.owner = p.getUniqueId();
        this.timeSpawned = System.currentTimeMillis();

        // Creating Chest:
        Block block = p.getLocation().getBlock();
        // Moving up till air or water is found
        while(!(block.getType().equals(Material.AIR) || block.getType().equals(Material.WATER))) {
            block = block.getChunk().getBlock(block.getX() & 15, block.getY() + 1, block.getZ() & 15);
        }
        this.oldMaterial = block.getType();
        this.location = block.getLocation();
        block.setType(DEATH_CHEST_MATERIAL);
        block.setMetadata(METADATA_KEY, new FixedMetadataValue(Manager.getInstance(), hashCode()));

        // Creating ArmorStand for visualisation:
        ArmorStand armorStand = (ArmorStand) block.getWorld().spawnEntity(block.getLocation().add(0.5, 0, 0.5), EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setVisualFire(false);
        armorStand.setCollidable(false);
        armorStand.setSilent(true);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setSmall(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(String.format(CHEST_NAME, p.getName()));
        armorStand.setMetadata(METADATA_KEY, new FixedMetadataValue(Manager.getInstance(), hashCode()));

        // Creating Inventory:
        //TODO: Evtl. kann man den owner auf die DeathChest (den Block) setzen und damit das eigentliche Inventar der Kiste überschreiben
        inventory = Bukkit.createInventory(null, (int) Math.ceil(items.size() / 9.0d) * 9, String.format(CHEST_NAME, p.getName()));
        for(ItemStack i : items) {
            inventory.addItem(i);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof DeathChest that)) return false;
        return oldMaterial == that.oldMaterial && Objects.equals(location, that.location) && Objects.equals(inventory, that.inventory) && Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldMaterial, location.getX(), location.getY(), location.getZ(), owner, timeSpawned);
    }

    @Override
    public String toString() {
        return "DeathChest{" +
                "timeSpawned=" + timeSpawned +
                ", oldMaterial=" + oldMaterial +
                ", location=" + location +
                ", inventory=" + inventory +
                ", owner=" + owner +
                ", taskID=" + taskID +
                ", id=" + hashCode() +
                '}';
    }

    /**
     *
     * @return Ob Inventar leer
     */
    public boolean collect() {
        Player player = Bukkit.getServer().getPlayer(owner);
        if(player == null) {
            return false;
        }

        if(player.isSneaking()) {
            List<ItemStack> newItems = new ArrayList<>();
            for(ItemStack i : inventory.getContents()) {
                if(i == null)
                    continue;

                HashMap<Integer, ItemStack> drop = player.getInventory().addItem(i);

                for(Entry<Integer, ItemStack> entry : drop.entrySet()) {
                    if(entry.getValue().getAmount() > 0) {
                        newItems.add(entry.getValue());
                    }
                }
            }

            if(newItems.isEmpty()) {
                inventory.clear();
            }
            else {
                ItemStack[] array = new ItemStack[newItems.size()];
                newItems.toArray(array);
                inventory.setContents(array);
            }
        }
        else {
            player.openInventory(inventory);
        }
        return inventory.isEmpty();
    }

    public boolean removeIfEmpty() {
        return ((!location.getBlock().getType().equals(DEATH_CHEST_MATERIAL) || inventory.isEmpty()) && remove(true));
    }

    public boolean remove(boolean dropItems) {
        Block block = location.getBlock();
        List<MetadataValue> metadataChestList = block.getMetadata(METADATA_KEY);
        if(metadataChestList.size() > METADATA_ID_INDEX && metadataChestList.get(METADATA_ID_INDEX).asInt() == hashCode()) {
            // Entfernen des Armorstands
            for(Entity i : block.getChunk().getEntities()) {
                if(i instanceof ArmorStand) {
                    List<MetadataValue> metadataArmorstandList = i.getMetadata(METADATA_KEY);
                    if(metadataArmorstandList.size() > METADATA_ID_INDEX && metadataArmorstandList.get(METADATA_ID_INDEX).asInt() == hashCode()) {
                        i.remove();
                        break;
                    }
                }
            }

            if(dropItems) {
                for(ItemStack i : inventory.getContents()) {
                    if(i != null) {
                        location.getWorld().dropItem(location, i);
                    }
                }
            }
            inventory.clear();
            block.setType(oldMaterial);
            block.removeMetadata(METADATA_KEY, Manager.getInstance());
            return true;
        }
        return false;
    }

    public boolean checkIfOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwner() {
        return owner;
    }

    public Inventory getChestInventory() {
        return inventory;
    }

    public long getTimeSpawned() {
        return timeSpawned;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }
}