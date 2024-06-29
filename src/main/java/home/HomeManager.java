package home;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeManager implements TabExecutor, ManagedPlugin
{
    public static final String COMMAND = "home";
    private List<HomePlayer> players;
    private File saveFile;

    public HomeManager ()
    {
        players = new ArrayList<HomePlayer>();
        this.saveFile = new File(Manager.getInstance().getDataFolder(), "Homes.bin");
        HomePlayer.maxHomes = Manager.getInstance().getConfig().getInt("Homes.MaxHomes");
    }

    private static String getMessageString (String message)
    {
        return ChatColor.GRAY + "[" + ChatColor.BLUE + "Homes" + ChatColor.GRAY + "] " + ChatColor.WHITE + message;
    }

    public static boolean sendMessage (CommandSender sender, String[] message)
    {
        if (sender != null)
        {
            String[] messages = new String[message.length];
            for (int i = 0; i < messages.length; i++)
            {
                messages[i] = getMessageString(message[i]);
            }
            sender.sendMessage(messages);
            return true;
        }
        return false;
    }

    public static boolean sendMessage (CommandSender sender, String message)
    {
        if (sender != null)
        {
            sender.sendMessage(getMessageString(message));
            return true;
        }
        return false;
    }

    @Override
    public boolean onCommand (CommandSender sender, Command command, String label, String[] args)
    {
        HomePlayer hp = getHomePlayer(sender);
        if (hp == null)
        {
            try
            {
                hp = new HomePlayer(((Player) sender).getUniqueId());
                players.add(hp);
            } catch (Exception e)
            {
                sendMessage(sender, "Error: You are not a Player!");
                Bukkit.getLogger().severe("Homes (HomePlayer(sender)): " + e.getLocalizedMessage());
                return false;
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list"))
        {
            String[] allHomes = hp.getAllHomes();
            if (allHomes.length == 0)
            {
                sendMessage(sender, "You have no homes");
            } else
            {
                sendMessage(sender, allHomes);
            }
            return true;
        } else if (args.length == 2)
        {
            if (args[0].equalsIgnoreCase("add"))
            {
                if (hp.addHome(args[1]))
                {
                    sendMessage(sender, "Added home " + args[1]);
                } else
                {
                    sendMessage(sender, "Couldn't add home " + args[1]);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("remove"))
            {
                if (hp.removeHome(args[1]))
                {
                    sendMessage(sender, "Removed home " + args[1]);
                } else
                {
                    sendMessage(sender, "Couldn't remove home " + args[1]);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("tp"))
            {
                if (hp.teleport(args[1]))
                {
                    sendMessage(sender, "Teleported to home " + args[1]);
                } else
                {
                    sendMessage(sender, "Couldn't teleport to home " + args[1]);
                }
                return true;
            } else
            {
                sendMessage(sender, "Invalid syntax (2)");
                return false;
            }
        } else
        {
            sendMessage(sender, "Invalid syntax (0)");
            return false;
        }
    }

    @Override
    public List<String> onTabComplete (CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 1)
        {
            return Arrays.asList("add", "remove", "tp", "list");
        } else if (args.length == 2)
        {
            HomePlayer hp = getHomePlayer(sender);
            if (hp != null)
            {
                return hp.getAllHomeNames();
            }
        }
        return Arrays.asList("");
    }

    public HomePlayer getHomePlayer (CommandSender sender)
    {
        try
        {
            for (HomePlayer i : players)
            {
                if (i.getOwner().equals(((Player) sender).getUniqueId()))
                {
                    return i;
                }
            }
            return null;
        } catch (Exception e)
        {
            Bukkit.getLogger().severe("HomeManager (getHomePlayer): " + e.getLocalizedMessage());
            return null;
        }
    }

    public boolean saveToFile ()
    {
        try
        {
            saveFile.createNewFile(); // Creates saveFile, if not exists
            FileOutputStream f = new FileOutputStream(saveFile, false);
            ObjectOutputStream o = new ObjectOutputStream(f);

            // Write count of objects to file
            o.write(players.size());

            // Write objects to file
            for (HomePlayer i : players)
            {
                o.writeObject(i);
            }

            o.close();
            f.close();
            Bukkit.getConsoleSender().sendMessage("Homes saved " + players.size() + " homes");
            return true;
        } catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage("Homes Error (Save): " + e.getLocalizedMessage());
        }
        return false;
    }

    public boolean loadFromFile ()
    {
        try
        {
            if (saveFile.createNewFile()) // Creates saveFile, if not exists
            {
                Bukkit.getConsoleSender().sendMessage("Homes Warning (Load): Skipped (Empty file)");
                return false;
            }
            FileInputStream f = new FileInputStream(saveFile);
            ObjectInputStream o = new ObjectInputStream(f);

            int size = o.read();

            for (int i = 0; i < size; i++)
            {
                players.add((HomePlayer) o.readObject());
            }

            o.close();
            f.close();
            Bukkit.getConsoleSender().sendMessage("Homes loaded " + players.size() + "/" + size + " homes");
            return true;
        } catch (Exception e)
        {
            Bukkit.getConsoleSender().sendMessage("Homes Error (Load): " + e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean onEnable ()
    {
        loadFromFile();
        try
        {
            Manager.getInstance().getCommand(COMMAND).setExecutor(this);
            Manager.getInstance().getCommand(COMMAND).setTabCompleter(this);
        }
        catch (NullPointerException e)
        {
            Manager.getInstance().sendErrorMessage(e.getMessage());
            onDisable();
            return false;
        }
        return true;
    }

    @Override
    public void onDisable ()
    {
        saveToFile();
    }

    @Override
    public String getName ()
    {
        return "Homes";
    }

    @Override
    public void createDefaultConfig (FileConfiguration config)
    {
        config.addDefault("Homes.MaxHomes", 10);

    }
}
