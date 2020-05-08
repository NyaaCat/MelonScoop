package moe.langua.lab.minecraft.scoop.commands;

import moe.langua.lab.minecraft.scoop.BootStrap;
import moe.langua.lab.minecraft.scoop.utils.Result;
import moe.langua.lab.minecraft.scoop.utils.Util;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Dig implements CommandExecutor {
    private HashMap<UUID, Result> resultHashMap = new HashMap<>();

    /*
    private static final String IPV4_ADDRESS_REGEX = "^(([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.){3}([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])$";
    private static final Pattern IPV4_ADDRESS_REGEX_PATTERN = Pattern.compile(IPV4_ADDRESS_REGEX);
    private static final String IPV6_ADDRESS_REGEX = "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))";
    private static final Pattern IPV6_ADDRESS_REGEX_PATTERN = Pattern.compile(IPV6_ADDRESS_REGEX);
    */

    private BootStrap instance;

    public Dig(BootStrap instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        instance.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
            if (args.length != 2) {
                commandSender.sendMessage(ChatColor.DARK_AQUA + "/dig <player|ip> <PlayerName|IPAddress>");
                return;
            }
            if (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase("p")) {
                UUID playerUniqueID;
                if (Bukkit.getPlayerExact(args[1]) == null) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                    playerUniqueID = player.getUniqueId();
                } else {
                    playerUniqueID = Bukkit.getPlayerExact(args[1]).getUniqueId();
                }
                Map<Long, InetAddress> playerLookupResult = instance.lookup(playerUniqueID);

                if (playerLookupResult.size() == 0) {
                    commandSender.sendMessage(ChatColor.RED + "Player " + args[1] + " not found.");
                    return;
                }

                HashMap<Long, TextComponent> resultComponentMap = new HashMap<>();

                AtomicInteger complete = new AtomicInteger(0);

                Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                    int total = playerLookupResult.size();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    while (complete.get() < total) {
                        commandSender.sendMessage(ChatColor.DARK_AQUA + "[MelonScoop] " + ChatColor.GRAY + "Searching... Please wait...(" + complete.get() + "/" + total + ")");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

                for (long x : playerLookupResult.keySet()) {
                    TextComponent line = new TextComponent(" - ");
                    line.addExtra(playerLookupResult.get(x).toString());
                    TextComponent date = new TextComponent("(" + instance.dateFormatter.format(new Date(x)) + ")");
                    date.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                    date.setItalic(true);
                    line.addExtra(" ");
                    line.addExtra(date);
                    HashMap<Long, UUID> timeToPlayerUUIDMap = instance.lookup(playerLookupResult.get(x));
                    if (timeToPlayerUUIDMap.keySet().size() > 1) {
                        line.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        TextComponent players = new TextComponent("(");
                        List<Long> playerList = Util.asSortedList(timeToPlayerUUIDMap.keySet());
                        for (long l : playerList) {
                            players.addExtra(Util.getPlayerTag(timeToPlayerUUIDMap.get(l)));
                            if (playerList.indexOf(l) + 1 != playerList.size()) players.addExtra(", ");
                        }
                        players.addExtra(")");
                        line.addExtra(" ");
                        line.addExtra(players);
                    } else {
                        line.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
                    }
                    resultComponentMap.put(x, line);
                    complete.addAndGet(1);
                }
                TextComponent header = new TextComponent("Addresses associated with ");
                header.addExtra(Util.getPlayerTag(playerUniqueID));
                header.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
                Result result = new Result(header, resultComponentMap, 9);
                resultHashMap.put(getSenderUUID(commandSender), result);
                commandSender.spigot().sendMessage(result.buildPage(1));
            } else if (args[0].equalsIgnoreCase("ip") || args[0].equalsIgnoreCase("i") || args[0].equalsIgnoreCase("a")) {
                InetAddress inetAddress;
                try {
                    inetAddress = InetAddress.getByName(args[1]);
                } catch (UnknownHostException e) {
                    commandSender.sendMessage(args[1] + " is not a valid IP address.");
                    return;
                }
                HashMap<Long, UUID> result = instance.lookup(inetAddress);
                if (result.size() == 0) {
                    commandSender.sendMessage(ChatColor.RED + "There is no player uses IP address " + inetAddress.toString() + " to login the server.");
                } else if (result.size() == 1) {
                    commandSender.sendMessage(ChatColor.DARK_AQUA + "IP address " + inetAddress.toString() + " is belongs to " + ChatColor.UNDERLINE + Bukkit.getOfflinePlayer((UUID) result.values().toArray()[0]).getName() + ChatColor.DARK_AQUA + ".");
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ChatColor.YELLOW).append("IP address ").append(inetAddress.toString()).append(" is associated with more than one players: \n");
                    ArrayList<Long> timeList = instance.sort(result.keySet());
                    for (long x : timeList) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(result.get(x));
                        stringBuilder.append(ChatColor.YELLOW).append("    -").append(player.getName());
                        if (player.isBanned()) stringBuilder.append(ChatColor.RED).append(" (Banned)");
                        stringBuilder.append(ChatColor.GRAY).append(ChatColor.ITALIC).append(" (").append(instance.dateFormatter.format(new Date(x))).append(")").append("\n");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    commandSender.sendMessage(stringBuilder.toString());
                }
            } else if (args[0].equalsIgnoreCase("page") || args[0].equalsIgnoreCase("l")) {
                if (!resultHashMap.containsKey(getSenderUUID(commandSender)))
                    commandSender.sendMessage(ChatColor.WHITE + "Please lookup a player or an ip address before using this command.");
                int page;
                try {
                    page = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    commandSender.sendMessage(ChatColor.RED + args[1] + "is not a number.");
                    return;
                }
                commandSender.spigot().sendMessage(resultHashMap.get(getSenderUUID(commandSender)).buildPage(page));
            } else {
                commandSender.sendMessage(ChatColor.DARK_AQUA + "/dig <player|ip> <PlayerName|ip>");
            }
        });
        return true;
    }

    private UUID getSenderUUID(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return UUID.fromString("00000000-0000-0000-0000-000000000000");
        else return ((Player) sender).getUniqueId();
    }
}
