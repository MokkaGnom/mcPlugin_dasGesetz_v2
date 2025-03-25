package home;

import manager.PluginCommands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.List;
import java.util.Set;

public class HomeCommands extends PluginCommands<HomeCommands.BaseCommand, HomeManager> {
    public enum BaseCommand implements BaseCommandInterface {
        LIST(1, 0), ADD(1, 1), REMOVE(1, 1), TP(1, 1);

        BaseCommand(int pos, int argCount) {
            this.pos = pos;
            this.argCount = argCount;
        }

        private final int pos;
        private final int argCount;

        @Override
        public int getPos() {
            return pos;
        }

        @Override
        public int getArgCount() {
            return argCount;
        }
    }

    public HomeCommands(HomeManager homeManager) {
        super(homeManager, Set.of(
                new Command<>(BaseCommand.LIST),
                new Command<>(BaseCommand.ADD),
                new Command<>(BaseCommand.REMOVE),
                new Command<>(BaseCommand.TP)
        ));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if(sender instanceof Player player) {
            Command<BaseCommand> bc = getCommand(args);
            if(bc == null) {
                manager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
                return false;
            }
            switch(bc.command()) {
                case BaseCommand.LIST:
                    List<String> homeNames = manager.getAllHomeNames(player.getUniqueId());
                    if(homeNames.isEmpty()) {
                        manager.sendMessage(player, "noHomes");
                    }
                    else {
                        for(String homeName : homeNames) {
                            manager.sendMessageDirect(player, homeName);
                        }
                    }
                    return true;
                case BaseCommand.ADD:
                    switch(manager.addHome(player.getUniqueId(), args[1], player.getLocation().getBlock().getLocation())) {
                        case 0 -> manager.sendMessage(player, "added");
                        case 1 -> manager.sendMessage(player, "homeExists");
                        case 2 -> manager.sendMessage(player, "maxHomes");
                    }
                    return true;
                case BaseCommand.REMOVE:
                    switch(manager.removeHome(player.getUniqueId(), args[1])) {
                        case 0 -> manager.sendMessage(player, "removed");
                        case 1 -> manager.sendMessage(player, "homeNotFound");
                        case 2 -> manager.sendMessage(player, ErrorMessage.UNKNOWN_ERROR);
                    }
                    return true;
                case BaseCommand.TP:
                    switch(manager.teleportToHome(player.getUniqueId(), args[1])) {
                        case 0 -> manager.sendMessageFormat(player, "tp", args[1]);
                        case 1 -> manager.sendMessage(player, "homeNotFound");
                    }
                    return true;
            }
            manager.sendMessageDirect(player, "WTF? Pls fix."); //TODO: "Missing return statement"
            return false;
        }
        else {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
            return false;
        }
    }
}