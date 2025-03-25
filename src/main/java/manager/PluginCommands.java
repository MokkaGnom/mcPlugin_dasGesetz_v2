package manager;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class PluginCommands<T extends PluginCommands.BaseCommandInterface, M extends ManagedPlugin> implements TabExecutor {

    public interface BaseCommandInterface {
        int getPos();

        int getArgCount();
    }

    public record Command<T>(T command, T previous) {

        public Command(T command) {
            this(command, null);
        }

        public String getCommand() {
            return command.toString();
        }

        public String getPrevious() {
            return previous.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(obj instanceof Command<?> c) {
                return this.command == c.command &&
                        this.previous == c.previous;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Command: \"" + command.toString().toLowerCase() +
                    "\"; Previous: \"" + previous + "\"";
        }
    }

    private final Set<Command<T>> commands;
    protected final M manager;

    /**
     * @param manager  Manager-Instance
     * @param commands Example: List.of(List.of("command_at_first_place_1", "command_at_first_place_2"), List.of("command_at_second_place_1", "command_at_second_place_2"))
     */
    public PluginCommands(M manager, Set<Command<T>> commands) {
        this.manager = manager;
        this.commands = commands;
    }

    public M getManager() {
        return manager;
    }

    public Set<Command<T>> getCommands() {
        return commands;
    }

    public Command<T> getCommand(String[] c) {
        for(int i = c.length - 1; i >= 0; i--) {
            Command<T> command = getCommand(c[i]);
            //TODO: Test, not sure if null-based
            if(command != null && command.command().getPos() == i && command.command().getArgCount() == (c.length - i)) {
                return command;
            }
        }
        return null;
    }

    public Command<T> getCommand(String command) {
        if(command == null || command.isBlank()) return null;
        for(Command<T> c : commands) {
            if(c.getCommand().equalsIgnoreCase(command)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        if(args.length >= commands.size()) return result;
        for(Command<T> c : commands) {
            if(c.getPrevious().equalsIgnoreCase(args[args.length - 2]) && c.getCommand().contains(args[args.length - 1])) {
                result.add(c.getCommand().toLowerCase());
            }
        }
        return result;
    }
}
