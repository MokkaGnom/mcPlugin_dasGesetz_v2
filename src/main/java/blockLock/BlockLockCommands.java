package blockLock;

import manager.ManagedPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

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

    private void listFriends(Player player, Set<UUID> friendsList) {
        for(UUID uuid : friendsList) {
            blManager.sendMessageFormat(player, "listFriends", Bukkit.getOfflinePlayer(uuid).getName(), uuid.toString());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        Block block = HelperFunctions.getTargetBlock(player);

        if(args.length == 1) // lock/unlock/listFriends
        {
            if(args[0].equalsIgnoreCase("test") && blManager.hasAdminPermission(sender)) {
                blManager.sendMessageFormat(player, "direct", BlockLock.getBlockLockMeta(block));
                return true;
            }

            if(args[0].equalsIgnoreCase(CommandStrings.UNLOCK)) {
                blManager.unlock(player, block, true);
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.LOCK)) {
                blManager.lock(player, block);
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.LIST_FRIENDS)) {
                Set<UUID> friendsList = blManager.getFriends(player.getUniqueId(), block);
                if(friendsList == null) {
                    friendsList = blManager.getFriends(player.getUniqueId());
                }

                if(friendsList.isEmpty()) {
                    blManager.sendMessage(player, "noFriends");
                }
                else {
                    listFriends(player, friendsList);
                }
            }
            else {
                blManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
                return false;
            }
            return true;
        }
        else if(args.length == 2) {
            // showmenu
            if(args[0].equalsIgnoreCase(CommandStrings.SHOW_MENU)) {
                boolean bool = HelperFunctions.isArgumentTrue(args[1]);
                blManager.setShowSneakMenu(player, bool);
                if(bool)
                    blManager.sendMessage(player, "menuActive");
                else
                    blManager.sendMessage(player, "menuInactive");
                return true;
            }

            // Friends:
            OfflinePlayer friendPlayer = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(offlinePlayer ->
                    {
                        String playerName = offlinePlayer.getName();
                        if(playerName != null) {
                            return playerName.equalsIgnoreCase(args[1]);
                        }
                        return false;
                    }).findFirst().orElse(null);
            if(friendPlayer != null) {
                UUID friend = friendPlayer.getUniqueId();
                String friendName = friendPlayer.getName();

                if(args[0].equalsIgnoreCase(CommandStrings.ADD_FRIEND)) {
                    if(blManager.addLocalFriend(player.getUniqueId(), block, friend))
                        blManager.sendMessageFormat(player, "addLocalFriend", friendName);
                    else
                        blManager.sendMessageFormat(player, "addLocalFriendFailed", friendName);
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE_FRIEND)) {
                    if(blManager.removeLocalFriend(player.getUniqueId(), block, friend))
                        blManager.sendMessageFormat(player, "removeLocalFriend", friendName);
                    else
                        blManager.sendMessageFormat(player, "removeLocalFriendFailed", friendName);
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.ADD_GLOBAL_FRIEND)) {
                    if(blManager.addGlobalFriend(player.getUniqueId(), friend))
                        blManager.sendMessageFormat(player, "addGlobalFriend", friendName);
                    else
                        blManager.sendMessageFormat(player, "addGlobalFriendFailed", friendName);
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE_GLOBAL_FRIEND)) {
                    if(blManager.removeGlobalFriend(player.getUniqueId(), friend))
                        blManager.sendMessageFormat(player, "removeGlobalFriend", friendName);
                    else
                        blManager.sendMessageFormat(player, "removeGlobalFriendFailed", friendName);
                }
                else {
                    blManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
                    return false;
                }
            }
            else {
                blManager.sendMessageFormat(player, "noPlayerFound", args[1]);
            }
            return true;
        }
        else {
            blManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
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
            return Stream.concat(ManagedPlugin.ENABLE_STRINGS.stream(), ManagedPlugin.DISABLE_STRINGS.stream()).toList();
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }

}
