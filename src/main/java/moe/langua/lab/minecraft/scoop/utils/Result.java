package moe.langua.lab.minecraft.scoop.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.List;

public class Result {
    private final int size;
    private final int pageSize;
    private final TextComponent header;
    private final HashMap<Long, TextComponent> resultMap;
    private final List<Long> sortedList;

    public Result(TextComponent headerContent, HashMap<Long, TextComponent> resultMap, int pageSize) {
        this.resultMap = resultMap;
        this.sortedList = Util.asSortedList(resultMap.keySet());
        this.pageSize = pageSize;
        this.size = sortedList.size() % pageSize == 0
                ? sortedList.size() / pageSize : ((sortedList.size() / pageSize) + 1);
        TextComponent header = new TextComponent();
        header.setColor(ChatColor.WHITE);
        header.addExtra("----- ");
        header.addExtra(headerContent);
        header.addExtra(" -----");
        this.header = header;
    }

    public TextComponent buildPage(int page) {
        TextComponent target = new TextComponent();
        if (!hasPage(page)) {
            target.setColor(ChatColor.RED);
            target.addExtra("Page " + page + "not found.");
        } else {
            target.addExtra(header);
            int offset = (page-1) * pageSize;
            for (int i = 0; i < pageSize; i++) {
                if (sortedList.size() - 1 < offset + i) continue;
                nextLine(target).addExtra(resultMap.get(sortedList.get(offset + i)));
            }
            nextLine(target).addExtra(getFooter(page));
        }

        return target;
    }

    private TextComponent getFooter(int pageNow) {
        TextComponent footer = new TextComponent();
        footer.setColor(ChatColor.WHITE);
        footer.addExtra("--- ");
        footer.addExtra("Page " + pageNow + "/" + size);
        TextComponent prev = new TextComponent("<< Prev");
        TextComponent next = new TextComponent("Next >>");
        if (hasPage(pageNow - 1)) {
            prev.setColor(ChatColor.DARK_AQUA);
            TextComponent[] textComponentsHOVER = new TextComponent[1];
            textComponentsHOVER[0] = new TextComponent("/dig page " + (pageNow - 1));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textComponentsHOVER));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dig page " + (pageNow - 1)));
        } else {
            prev.setColor(ChatColor.GRAY);
        }
        if (hasPage(pageNow + 1)) {
            next.setColor(ChatColor.DARK_AQUA);
            TextComponent[] textComponentsHOVER = new TextComponent[1];
            textComponentsHOVER[0] = new TextComponent("/dig page " + (pageNow + 1));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textComponentsHOVER));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dig page " + (pageNow + 1)));
        } else {
            next.setColor(ChatColor.GRAY);
        }

        footer.addExtra(" --- ");
        footer.addExtra(prev);
        footer.addExtra(" | ");
        footer.addExtra(next);
        footer.addExtra(" --- ");
        return footer;
    }

    private TextComponent getHeader() {
        return header;
    }

    private boolean hasPage(int page) {
        return page > 0 && page <= size;
    }


    private TextComponent nextLine(TextComponent component) {
        component.addExtra("\n");
        return component;
    }

}
