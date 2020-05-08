package moe.langua.lab.minecraft.scoop.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.*;

public class Util {
    static {
        TextComponent click_notice = new TextComponent("Click to Copy UUID");
        click_notice.setColor(ChatColor.GRAY);
        click_notice.setItalic(true);
        click_notice.setStrikethrough(false);
        Util.click_notice = click_notice;
    }

    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");

    //from https://stackoverflow.com/questions/740299/how-do-i-sort-a-set-to-a-list-in-java
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        java.util.Collections.reverse(list);
        return list;
    }

    public static TextComponent getDateTag(long timeStampInMillSecond){
        TextComponent date = new TextComponent("(" + dateFormatter.format(new Date(timeStampInMillSecond)) + ")");
        date.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        date.setItalic(true);
        return date;
    }

    private static TextComponent click_notice;

    public static TextComponent getPlayerTag(OfflinePlayer player){
        TextComponent target = new TextComponent(player.getName());
        TextComponent hover = new TextComponent(player.getUniqueId().toString());
        if (isBanned(player)) {
            target.setColor(ChatColor.RED);
            target.setStrikethrough(true);
            hover.setColor(ChatColor.RED);
            hover.setStrikethrough(true);
        } else {
            if (player.isOnline()) {
                hover.setColor(ChatColor.GREEN);
            } else {
                hover.setColor(ChatColor.WHITE);
            }
        }
        hover.addExtra("\n");
        hover.addExtra(click_notice);
        TextComponent[] hovers = new TextComponent[1];
        hovers[0] = hover;
        target.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hovers));
        target.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,player.getUniqueId().toString()));
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
