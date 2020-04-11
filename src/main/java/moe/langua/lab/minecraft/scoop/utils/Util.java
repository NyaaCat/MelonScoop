package moe.langua.lab.minecraft.scoop.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Util {
    static {
        TextComponent online = new TextComponent("Online");
        online.setColor(ChatColor.GREEN);
        Util.online = online;
        TextComponent offline = new TextComponent("Offline");
        offline.setColor(ChatColor.GRAY);
        Util.offline = offline;
        TextComponent banned = new TextComponent("Banned");
        banned.setColor(ChatColor.RED);
        Util.banned = banned;
    }

    //from https://stackoverflow.com/questions/740299/how-do-i-sort-a-set-to-a-list-in-java
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    private static TextComponent online;
    private static TextComponent offline;
    private static TextComponent banned;

    public static TextComponent getPlayerTag(OfflinePlayer player){
        TextComponent target = new TextComponent(player.getName());
        TextComponent hover = new TextComponent();
        if (isBanned(player)) {
            target.setColor(ChatColor.RED);
            target.setStrikethrough(true);
            hover.addExtra(banned);
        } else {
            if (player.isOnline()) {
                hover.addExtra(online);
            } else {
                hover.addExtra(offline);
            }
        }
        TextComponent[] hovers = new TextComponent[1];
        hovers[0] = hover;
        target.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hovers));
        return target;
    }

    public static TextComponent getPlayerTag(UUID uniqueID) {
        OfflinePlayer player = getPlayer(uniqueID);
        return getPlayerTag(player);
    }

    public static OfflinePlayer getPlayer(UUID uniqueID){
        OfflinePlayer target;
        if((target = Bukkit.getPlayer(uniqueID)) == null){
            target = Bukkit.getOfflinePlayer(uniqueID);
        }
        return target;
    }

    public static boolean isBanned(OfflinePlayer player){
        return Bukkit.getServer().getBannedPlayers().contains(player);
    }

}
