package moe.langua.lab.minecraft.scoop.listeners;

import moe.langua.lab.minecraft.scoop.BootStrap;
import moe.langua.lab.minecraft.scoop.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Login implements Listener {
    BootStrap instance;

    public Login(BootStrap instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        instance.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
            InetAddress inetAddress = event.getAddress();
            if (inetAddress == null) {
                instance.getLogger().warning("This login will not be recorded, because getAddress() method in AsyncPlayerPreLoginEvent returns null.");
                return;
            }
            if (!Bukkit.getOfflinePlayer(event.getUniqueId()).hasPlayedBefore()) {
                HashMap<Long, UUID> result = instance.lookup(event.getAddress());
                if (result.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ChatColor.YELLOW).append("============================================\n")
                            .append("The IP address of new player ").append(event.getName()).append(" (").append(inetAddress.toString()).append(")").append(" is associated with registered player(s):\n");
                    ArrayList<Long> timeList = instance.sort(result.keySet());
                    for (long x : timeList) {
                        stringBuilder.append(ChatColor.YELLOW).append("    -").append(Bukkit.getOfflinePlayer(result.get(x)).getName()).append(ChatColor.GRAY).append(" (").append(Util.dateFormatter.format(new Date(x))).append(") ");
                        if (Bukkit.getOfflinePlayer(result.get(x)).isBanned()) {
                            stringBuilder.append(ChatColor.RED).append("(Banned)");
                        }
                        stringBuilder.append("\n");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    instance.getLogger().warning(stringBuilder.toString());
                    for (Player x : Bukkit.getOnlinePlayers()) {
                        if (x.hasPermission("melonscoop.alarm")) x.sendMessage(stringBuilder.toString());
                    }
                }
            }
            instance.addLoginRecord(event.getUniqueId(), event.getAddress());
        });
    }
}
