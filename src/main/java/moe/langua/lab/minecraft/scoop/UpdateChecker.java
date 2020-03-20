package moe.langua.lab.minecraft.scoop;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

public class UpdateChecker implements Listener {
    BootStrap instance;
    private boolean hasUpdate = false;
    private String latestVersion = "";
    private URL repoAPIURL = null;

    public UpdateChecker(BootStrap instance) {
        this.instance = instance;
        try {
            repoAPIURL = new URL("https://api.github.com/repos/NyaaCat/MelonScoop/releases/latest");
        } catch (MalformedURLException ignore) {
        }

        instance.getServer().getScheduler().runTaskTimerAsynchronously(instance, () -> {
            try {
                JSONObject result = new JSONObject(jsonAPIGet(repoAPIURL));
                latestVersion = result.getString("tag_name");
                if (!instance.getDescription().getVersion().equalsIgnoreCase(latestVersion)) hasUpdate = true;
            } catch (IOException ignore) {
            }
        }, 0, 20 * 3600 * 12/*Check update every 12 hour*/);
    }

    @EventHandler
    public void onOperatorLogin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission("melonscoop.update")) return;
        if (!hasUpdate) return;
        instance.getServer().getScheduler().runTaskLater(instance,()->event.getPlayer().sendMessage(ChatColor.YELLOW + "[MelonScoop] " + ChatColor.WHITE + "A new version of MelonScoop(" + latestVersion + ") is available. Click the link to download now: https://github.com/NyaaCat/MelonScoop/releases/" + latestVersion),20);
    }

    private static String jsonAPIGet(URL reqURL) throws JSONException, IOException {
        HttpsURLConnection connection = (HttpsURLConnection) reqURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-type", "application/json");
        connection.setInstanceFollowRedirects(false);
        InputStream inputStream = connection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String tmpString;
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            tmpString = bufferedReader.readLine();
            if (tmpString == null) break;
            stringBuilder.append(tmpString);
        }
        return stringBuilder.toString();
    }
}
