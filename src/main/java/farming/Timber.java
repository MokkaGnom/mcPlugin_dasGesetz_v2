package farming;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import utility.HelperFunctions;

import java.util.*;
import java.util.Map.Entry;

public class Timber implements Listener, ManagedPlugin
{
    public static final Map<Material, Material> TIMBER_BLOCK_MATERIAL = Map.of(
            Material.ACACIA_LOG, Material.ACACIA_LEAVES,
            Material.BIRCH_LOG, Material.BIRCH_LEAVES,
            Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES,
            Material.JUNGLE_LOG, Material.JUNGLE_LEAVES,
            Material.MANGROVE_LOG, Material.MANGROVE_LEAVES,
            Material.OAK_LOG, Material.OAK_LEAVES,
            Material.SPRUCE_LOG, Material.SPRUCE_LEAVES,
            Material.CHERRY_LOG, Material.CHERRY_LEAVES
    );
    public static final Set<Material> TIMBER_TOOL_MATERIAL = Set.of(Material.DIAMOND_AXE, Material.GOLDEN_AXE, Material.IRON_AXE, Material.NETHERITE_AXE, Material.STONE_AXE, Material.WOODEN_AXE);
    public static final int BREAK_LEAVES_RADIUS = 8;
    public static final int BREAK_LEAVES_HEIGHT = 20;
    private static final String BREAK_LEAVES_JSON_KEY = "Timber.BreakLeaves";
    private final boolean breakLeaves;

    public Timber() {
        this.breakLeaves = Manager.getInstance().getConfig().getBoolean(BREAK_LEAVES_JSON_KEY);
    }

    //TODO: Testen (vor allem die Zeit überprüfen)
    public void breakLeaves(List<Block> treeLogs, Material treeMaterial) {
        long startTime = System.currentTimeMillis();
        Location treeTrunkLocation = treeLogs.getFirst().getLocation();
        Material leaveMaterial = TIMBER_BLOCK_MATERIAL.get(treeMaterial);
        Location firstLocation = treeTrunkLocation.subtract(BREAK_LEAVES_RADIUS, 0, BREAK_LEAVES_RADIUS);
        Location secondLocation = treeTrunkLocation.add(BREAK_LEAVES_RADIUS, BREAK_LEAVES_HEIGHT, BREAK_LEAVES_RADIUS);
        for(int y = firstLocation.getBlockY(); y <= secondLocation.getBlockY(); y++) {
            for(int x = firstLocation.getBlockX(); x <= secondLocation.getBlockX(); x++) {
                for(int z = firstLocation.getBlockZ(); z <= secondLocation.getBlockZ(); z++) {
                    Block block = treeTrunkLocation.getWorld().getBlockAt(x, y, z);
                    if(block.getType().equals(leaveMaterial) && block.getBlockData() instanceof Leaves leave && !leave.isPersistent()) {
                        Bukkit.getScheduler().runTask(Manager.getInstance(), () -> block.breakNaturally());
                    }
                }
            }
        }
        Manager.getInstance().sendInfoMessage(getMessagePrefix(),
                String.format("BreakLeaves-Time: %sms", System.currentTimeMillis() - startTime));
    }

    /**
     * @param item            The item
     * @param damageToInflict Damage to inflict on item
     * @return True if item is damaged (false if item doesn't have enough durability left to apply damage)
     */
    public boolean damageItem(ItemStack item, int damageToInflict) {

        if(item.getItemMeta() instanceof Damageable damageable) {
            int calculatedDamage = damageToInflict;
            int maxDurability = item.getType().getMaxDurability();

            if(item.containsEnchantment(Enchantment.UNBREAKING)) { //TODO: Ka, ob die nachfolgenden Rechnungen richtig sind
                double enchChance = 100.d / (item.getEnchantmentLevel(Enchantment.UNBREAKING) + 1);
                for(int i = 0; i < calculatedDamage; i++) {
                    double chance = Math.floor(Math.random() * 101);
                    if(chance < enchChance)
                        calculatedDamage--;
                }
            }

            if(damageable.getDamage() + calculatedDamage < maxDurability) {
                damageable.setDamage(damageable.getDamage() + calculatedDamage);
                item.setItemMeta(damageable);
                return true;
            }
        }
        return false;
    }

    /**
     * @param firstBlock    First block
     * @param blockMaterial Material to filter blocks
     * @return A list with all blocks from 'blockMaterial' which are above 'firstBlock'
     */
    public List<Block> getBlockList(Block firstBlock, Material blockMaterial) {
        List<Block> blockList = new ArrayList<>();
        Block nextBlock = firstBlock;
        while(nextBlock.getType().equals(blockMaterial)) {
            blockList.add(nextBlock);
            nextBlock = nextBlock.getChunk().getBlock(nextBlock.getX() & 15, nextBlock.getY() + 1, nextBlock.getZ() & 15);
        }
        return blockList;
    }

    /**
     * @param block  First Block to break
     * @param player Player
     * @return If successful
     */
    public List<Block> breakWood(Block block, Player player) {
        List<Block> logBlocks = getBlockList(block, block.getType());

        // Checking/setting damage on tool
        ItemStack item = player.getInventory().getItemInMainHand();
        if(!damageItem(item, logBlocks.size())) {
            return logBlocks;
        }
        player.getInventory().setItemInMainHand(item);

        // Removing the tree logs and putting it in player inventory or dropping it, if full
        for(Block i : logBlocks) {
            HashMap<Integer, ItemStack> drop = player.getInventory().addItem(new ItemStack(i.getType(), 1));
            for(Entry<Integer, ItemStack> entry : drop.entrySet()) {
                player.getWorld().dropItemNaturally(i.getLocation(), entry.getValue());
            }
            i.setType(Material.AIR);
        }

        return logBlocks;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockMaterial = event.getBlock().getType();

        if(player.isSneaking() || !hasDefaultUsePermission(player))
            return;

        // Breaking tree
        if(TIMBER_BLOCK_MATERIAL.containsKey(block.getType())
                && TIMBER_TOOL_MATERIAL.contains(player.getInventory().getItemInMainHand().getType())) {
            List<Block> logsBroken = breakWood(block, player);
            if(breakLeaves) {
                Bukkit.getScheduler().runTaskAsynchronously(Manager.getInstance(), () -> breakLeaves(logsBroken, blockMaterial));
            }
        }
    }

    @Override
    public boolean onEnable() {
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public String getName() {
        return "Timber";
    }

    @Override
    public ChatColor getMessageColor() {
        return ManagedPlugin.DEFAULT_CHAT_COLOR;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.timberPermission");
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(BREAK_LEAVES_JSON_KEY, true);
    }
}