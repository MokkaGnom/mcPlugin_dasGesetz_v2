package blockLogger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class BlockLogger implements Listener
{
    public static final String DELETE_LOG_INFO_FORMAT = "Delete Logfile of \"%s\": %b";
    public static final String LOG_FORMAT = "[%s]: %s: (%s) X:%s Y:%s Z:%s\n";
    public static final String LOG_PLACED_BLOCK = "PLACED";
    public static final String LOG_REMOVED_BLOCK = "REMOVED";

    private final Material loggedMaterial;
    private final File logFile;

    public BlockLogger(Material loggedMaterial) {
        this.loggedMaterial = loggedMaterial;
        this.logFile = new File(String.format(BlockLoggerManager.FILE_PATH, loggedMaterial.name()));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.getBlock().getType() == loggedMaterial && !event.isCancelled()) {
            logBlock(event.getBlock(), event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(event.getBlock().getType() == loggedMaterial && !event.isCancelled()) {
            logBlock(event.getBlock(), event.getPlayer(), false);
        }
    }

    public void logBlock(Block block, Player player, boolean placed) {
        try {
            FileWriter fw = new FileWriter(logFile, true);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            String dateStr = dateFormat.format(new Date());

            fw.write(String.format(LOG_FORMAT, dateStr, player.getName(), (placed ? LOG_PLACED_BLOCK : LOG_REMOVED_BLOCK), block.getX(), block.getY(), block.getZ()));
            fw.flush();
            fw.close();

        } catch(Exception e) {
            BlockLoggerManager.getInstance().logWarning(loggedMaterial, e.getMessage());
        }
    }

    public void deleteLog() {
        if(logFile.exists()) {
            try {
                BlockLoggerManager.getInstance().logInfo(loggedMaterial, String.format(DELETE_LOG_INFO_FORMAT, loggedMaterial.name(), logFile.delete()));
            } catch(Exception e) {
                BlockLoggerManager.getInstance().logWarning(loggedMaterial, e.getMessage());
            }
        }
    }
}