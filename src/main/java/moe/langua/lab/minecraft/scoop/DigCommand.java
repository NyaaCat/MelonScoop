package moe.langua.lab.minecraft.scoop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class DigCommand implements CommandExecutor {

    private static final String IPV4_ADDRESS_REGEX = "^(([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.){3}([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])$";
    private static final Pattern IPV4_ADDRESS_REGEX_PATTERN = Pattern.compile(IPV4_ADDRESS_REGEX);
    private static final String IPV6_ADDRESS_REGEX = "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))";
    private static final Pattern IPV6_ADDRESS_REGEX_PATTERN = Pattern.compile(IPV6_ADDRESS_REGEX);


    private BootStrap instance;

    public DigCommand(BootStrap instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender instanceof Player) {
            if (commandSender.hasPermission("melonscoop.dig")) {
                return false;
            }
        }
        instance.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
            if (args.length != 2) {
                commandSender.sendMessage(ChatColor.DARK_AQUA + "/dig <player|ipaddress> <PlayerName|IPAddress>");
            }
            if (args[0].equalsIgnoreCase("player")) {
                UUID playerUniqueID;
                if (Bukkit.getPlayerExact(args[1]) == null) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                    if (!player.hasPlayedBefore()) {
                        commandSender.sendMessage(ChatColor.RED + "Player " + args[1] + " not found.");
                        return;
                    }
                    playerUniqueID = player.getUniqueId();
                } else {
                    playerUniqueID = Bukkit.getPlayerExact(args[1]).getUniqueId();
                }
                Map<Long, InetAddress> result = instance.lookup(playerUniqueID);
                if (result.size() == 0) {
                    commandSender.sendMessage(ChatColor.RED + "There is no IP address associated with " + args[1] + ".");
                    return;
                } else if (result.size() == 1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ChatColor.DARK_AQUA).append("There is only one ip address associated with ").append(args[1]).append(": ");
                    InetAddress address = (InetAddress) result.values().toArray()[0];
                    HashMap<Long, UUID> playerMap = instance.lookup(address);
                    if (playerMap.size() == 1) {
                        stringBuilder.append(ChatColor.DARK_AQUA).append(address.toString()).append(" ").append(ChatColor.GRAY).append(ChatColor.ITALIC).append("(").append(instance.dateFormatter.format(new Date((Long) result.keySet().toArray()[0]))).append(")");
                    } else {
                        stringBuilder.append(ChatColor.YELLOW).append(address.toString()).append(" (");
                        for (long x : playerMap.keySet()) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(playerMap.get(x));
                            if (player.isBanned()) {
                                stringBuilder.append(player.getName()).append(ChatColor.RED).append(" (Banned)").append(ChatColor.YELLOW).append(", ");
                            } else {
                                stringBuilder.append(player.getName()).append(", ");
                            }

                        }
                        stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length()).append(")");
                    }
                    commandSender.sendMessage(stringBuilder.toString());
                } else {
                    ArrayList<Long> timeList = instance.sort(result.keySet());

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ChatColor.DARK_AQUA).append("Here are IP Addresses associated with ").append(args[1]).append(":").append("\n");

                    for (long x : timeList) {
                        HashMap<Long, UUID> playerMap = instance.lookup(result.get(x));
                        if (playerMap.values().size() > 1) {
                            stringBuilder.append(ChatColor.YELLOW).append("    -").append(result.get(x).toString()).append(" (");
                            for (UUID uniqueID : playerMap.values()) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueID);
                                if (player.isBanned()) {
                                    stringBuilder.append(ChatColor.RED).append(player.getName()).append(" (Banned)").append(ChatColor.YELLOW).append(", ");
                                } else {
                                    stringBuilder.append(player.getName()).append(", ");
                                }

                            }
                            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length()).append(") ");
                        } else {
                            stringBuilder.append(ChatColor.DARK_AQUA).append("    -").append(result.get(x).toString()).append(" ");
                        }
                        stringBuilder.append(ChatColor.GRAY).append(ChatColor.ITALIC).append("(").append(instance.dateFormatter.format(new Date(x))).append(")\n");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    commandSender.sendMessage(stringBuilder.toString());
                }
            } else if (args[0].equalsIgnoreCase("ip")) {
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
                        stringBuilder.append(ChatColor.YELLOW).append("    -").append(Bukkit.getOfflinePlayer(result.get(x)).getName()).append(ChatColor.GRAY).append(ChatColor.ITALIC).append(" (").append(instance.dateFormatter.format(new Date(x))).append(")").append("\n");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    commandSender.sendMessage(stringBuilder.toString());
                }
            } else {
                commandSender.sendMessage(ChatColor.DARK_AQUA + "/dig <player|ip> <PlayerName|IPAddress>");
            }
        });
        return true;
    }
}
