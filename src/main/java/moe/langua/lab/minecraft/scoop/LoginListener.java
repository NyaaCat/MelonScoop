package moe.langua.lab.minecraft.scoop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class LoginListener implements Listener {
    BootStrap instance;

    public LoginListener(BootStrap instance) {
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
                    stringBuilder.append(ChatColor.YELLOW).append("IP address of ").append(event.getName()).append(" (").append(inetAddress.toString()).append(")").append(" is associated with these players:\n");
                    ArrayList<Long> timeList = instance.sort(result.keySet());
                    for (long x : timeList) {
                        stringBuilder.append(ChatColor.YELLOW).append("    -").append(Bukkit.getOfflinePlayer(result.get(x)).getName()).append(ChatColor.GRAY).append(" (").append(instance.dateFormatter.format(new Date(x))).append(") ");
                        if (Bukkit.getOfflinePlayer(result.get(x)).isBanned()) {
                            stringBuilder.append(ChatColor.RED).append("(Banned)");
                        }
                        stringBuilder.append("\n");
                    }
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    instance.getLogger().warning(stringBuilder.toString());
                    for (Player x : Bukkit.getOnlinePlayers()) {
                        if (x.hasPermission("melonscoop.dig")) x.sendMessage(stringBuilder.toString());
                    }
                }
            }
            instance.addLoginRecord(event.getUniqueId(), event.getAddress());
        });
    }
}
