package blockLock;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlockLockCommands implements TabExecutor
{
    public interface CommandStrings
    {
        String ROOT = "blockLock";
        String LOCK = "lock";
        String UNLOCK = "unlock";
        String LIST_FRIENDS = "list";
        String ADD_FRIEND = "addFriend";
        String REMOVE_FRIEND = "removeFriend";
        String ADD_GLOBAL_FRIEND = "addGlobalFriend";
        String REMOVE_GLOBAL_FRIEND = "removeGlobalFriend";
        String SHOW_MENU = "showMenu";

        List<String> FIRST_ARGUMENT = List.of(LIST_FRIENDS, LOCK, UNLOCK, ADD_FRIEND, REMOVE_FRIEND, ADD_GLOBAL_FRIEND, REMOVE_GLOBAL_FRIEND, SHOW_MENU);
    }

    private final BlockLockManager blManager;

    public BlockLockCommands(BlockLockManager blManager) {
        this.blManager = blManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 255);

        if(args.length == 1) // lock/unlock/listFriends
        {
            if(args[0].equalsIgnoreCase("test")) {

            }

            if(args[0].equalsIgnoreCase(CommandStrings.UNLOCK)) {
                blManager.unlock(player, block);
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.LOCK)) {
                blManager.lock(player, block);
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.LIST_FRIENDS)) {
                List<String> friendsList = blManager.getFriends(player.getUniqueId(), block).stream().map(UUID::toString).toList();
                if(friendsList.isEmpty()) {
                    blManager.sendMessage(sender, "No friends");
                }
                else {
                    for(String s : friendsList) {
                        blManager.sendMessage(sender, s);
                    }
                }
            }
            else {
                blManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX.message());
                return false;
            }
            return true;
        }
        else if(args.length == 2) {
            // showmenu
            if(args[0].equalsIgnoreCase(CommandStrings.SHOW_MENU)) {
                boolean bool = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("1");
                blManager.setShowSneakMenu(player, bool);
                if(bool)
                    blManager.sendMessage(player, "Menu active");
                else
                    blManager.sendMessage(player, "Menu inactive");
                return true;
            }

            // Friends:
            Player friendPlayer = Bukkit.getPlayer(args[1]);
            if(friendPlayer == null) {
                for(OfflinePlayer i : Bukkit.getOfflinePlayers()) {
                    String name = i.getName();
                    if(name != null && name.equalsIgnoreCase(args[1])) {
                        friendPlayer = i.getPlayer();
                        break;
                    }
                }
            }

            if(friendPlayer != null) {
                UUID friend = friendPlayer.getUniqueId();
                String friendName = friendPlayer.getName();

                if(args[0].equalsIgnoreCase(CommandStrings.ADD_FRIEND)) {
                    if(blManager.addFriend(player.getUniqueId(), block, friend))
                        blManager.sendMessage(player, friendName + " added (local)");
                    else
                        blManager.sendMessage(player, "Couldn't add (local) " + friendName);
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE_FRIEND)) {
                    if(blManager.removeFriend(player.getUniqueId(), block, friend))
                        blManager.sendMessage(player, friendName + " removed (local)");
                    else
                        blManager.sendMessage(player, "Couldn't remove (local) " + friendName);
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.ADD_GLOBAL_FRIEND)) {
                    if(blManager.addGlobalFriend(player.getUniqueId(), friend))
                        blManager.sendMessage(player, friendName + " added (global)");
                    else
                        blManager.sendMessage(player, "Couldn't add (global) " + friendName);
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE_GLOBAL_FRIEND)) {
                    if(blManager.removeGlobalFriend(player.getUniqueId(), friend))
                        blManager.sendMessage(player, friendName + " removed (global)");
                    else
                        blManager.sendMessage(player, "Couldn't remove (global) " + friendName);
                }
                else {
                    blManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX.message());
                    return false;
                }
                return true;
            }
            else {
                blManager.sendMessage(player, "Couldn't find player \"" + args[1] + "\"");
                return true;
            }
        }
        else {
            blManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX.message());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENT;
        }
        else if(args.length == 2 && (args[0].equalsIgnoreCase(CommandStrings.ADD_FRIEND) || args[0].equalsIgnoreCase(CommandStrings.REMOVE_FRIEND) || args[0].equalsIgnoreCase(CommandStrings.ADD_GLOBAL_FRIEND)
                || args[0].equalsIgnoreCase(CommandStrings.REMOVE_GLOBAL_FRIEND))) {
            return null;
        }
        else if(args.length == 2 && args[0].equalsIgnoreCase(CommandStrings.SHOW_MENU)) {
            return Arrays.asList("0", "1", "true", "false");
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }

}
