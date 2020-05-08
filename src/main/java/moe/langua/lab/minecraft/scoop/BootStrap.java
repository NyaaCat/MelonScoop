package moe.langua.lab.minecraft.scoop;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import moe.langua.lab.minecraft.scoop.commands.Dig;
import moe.langua.lab.minecraft.scoop.listeners.Login;
import moe.langua.lab.minecraft.scoop.utils.Util;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BootStrap extends JavaPlugin{

    private BiMap<Integer, InetAddress> addressIndexMap;
    private BiMap<Integer, UUID> uniqueIDIndexMap;
    private Map<Integer, HashMap<Integer, Long>> addressToUniqueIDMap;
    private Map<Integer, HashMap<Integer, Long>> uniqueIDToAddressMap;
    private final Map<String, UUID> nameToUniqueIDMap = new HashMap<>();
    private static final int PLAYER_NAME_OFFSET = 33;
    private final Pattern UUID_REGEX_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Override
    public void onEnable() {
        if (!this.getDataFolder().exists()) {
            try {
                this.getLogger().info(ChatColor.DARK_AQUA+"Initializing...");
                this.getDataFolder().mkdir();
                setup();
                save();
            } catch (IOException e) {
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                this.getLogger().warning("Exception occurred when loading player data... Plugin will be disabled automatically. Please check the stack trace and delete the plugin data folder before next startup to restart player database build.");
                return;
            }
        } else {
            try {
                this.getLogger().info(ChatColor.DARK_AQUA + "Loading player data...");
                load();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        //register commands and listeners
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, this::save, 6000, 6000/*Auto saving per five minute*/);
        this.getServer().getPluginManager().registerEvents(new Login(this), this);
        this.getServer().getPluginManager().registerEvents(new UpdateChecker(this),this);
        this.getCommand("dig").setExecutor(new Dig(this));
        this.getLogger().info(ChatColor.DARK_AQUA + "Done! " + uniqueIDIndexMap.size() + " players with " + addressIndexMap.size() + " IP addresses loaded.");
        new Metrics(this, 6823); // setup bStats
    }

    private void setup() throws IOException {
        addressIndexMap = HashBiMap.create();
        uniqueIDIndexMap = HashBiMap.create();
        addressToUniqueIDMap = new HashMap<>();
        uniqueIDToAddressMap = new HashMap<>();

        File logFileFolder = new File("./logs");
        File[] logFiles = logFileFolder.listFiles();
        assert logFiles != null;
        HashMap<Long, File> fileHashMap = new HashMap<>();
        for(File x:logFiles){
            fileHashMap.put(x.lastModified(),x);
        }
        ArrayList<Long> timeList = sort(fileHashMap.keySet());
        Collections.reverse(timeList);
        int fileNumber = timeList.size();
        int read = 0;
        BufferedReader reader;
        for (long x : timeList) {
            this.getLogger().info(ChatColor.DARK_AQUA +"Processing log file generated in "+ Util.dateFormatter.format(new Date(x))+" ("+(read++) + " out of "+ fileNumber + " completed)");
            File file = fileHashMap.get(x);
            if (!file.getName().endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), UTF_8));
            }
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (line.length() < 35) continue;
                    boolean isLoginMessage = line.charAt(33) != '[' && line.contains("] logged in with entity id ");
                    boolean isUniqueIDMessage = line.substring(10, 32).equalsIgnoreCase(" [User Authenticator #");
                    if (isLoginMessage) {
                        if (line.substring(PLAYER_NAME_OFFSET).length() < 60) continue;
                        String sub = line.substring(PLAYER_NAME_OFFSET, line.indexOf("logged in"));
                        String playerName = sub.substring(0, sub.indexOf("["));
                        String playerIPAddress = sub.substring(sub.indexOf("[/") + 2, sub.lastIndexOf(":"));
                        InetAddress playerINetAddress = InetAddress.getByName(playerIPAddress);
                        UUID playerUniqueID = nameToUniqueIDMap.get(playerName);
                        if (playerUniqueID == null) continue; /*ignore damaged log records*/
                        int hour = Integer.parseInt(line.substring(1, 3));
                        int minute = Integer.parseInt(line.substring(4, 6));
                        int second = Integer.parseInt(line.substring(7, 9));
                        long dayTimeInMillSeconds = (hour * 3600 + minute * 60 + second) * 1000;
                        long playerLoginTime;
                        long dayStart;
                        if (file.getName().equalsIgnoreCase("latest.log")) {
                            dayStart = file.lastModified() - (file.lastModified() % 86400000)/*a day*/;
                            playerLoginTime = dayStart + dayTimeInMillSeconds;
                        } else {
                            int year = Integer.parseInt(file.getName().substring(0, 4));
                            int month = Integer.parseInt(file.getName().substring(5, 7));
                            int day = Integer.parseInt(file.getName().substring(8, 10));
                            Calendar c = Calendar.getInstance();
                            c.set(year, month - 1/*JANUARY is 0*/, day);
                            playerLoginTime = c.getTimeInMillis() + dayTimeInMillSeconds;
                        }
                        addLoginRecord(playerUniqueID, playerINetAddress, playerLoginTime);
                    } else if (isUniqueIDMessage) {
                        String sub = line.substring(33);
                        if (sub.length() < 64) continue;
                        String playerName = sub.substring(sub.indexOf("/INFO]: UUID of player ") + 23, sub.indexOf(" is "));
                        String playerUniqueID = sub.substring(sub.indexOf(" is ") + 4);
                        if (!UUID_REGEX_PATTERN.matcher(playerUniqueID).matches()) continue;
                        nameToUniqueIDMap.put(playerName, UUID.fromString(playerUniqueID));
                    }
                } catch (IndexOutOfBoundsException ignore) { /*ignore unknown IndexOutOfBoundsException*/}
            }
        }
    }

    @Override
    public void onDisable() {
        save();
    }

    public synchronized HashMap<Long, InetAddress> lookup(UUID uniqueID) {
        if (!uniqueIDIndexMap.containsValue(uniqueID)) return new HashMap<>();
        HashMap<Integer, Long> playerAddressDataMap = getPlayerAddressData(uniqueID);
        HashMap<Long, InetAddress> result = new HashMap<>();
        for (int x : playerAddressDataMap.keySet()) {
            result.put(playerAddressDataMap.get(x), addressIndexMap.get(x));
        }
        return result;
    }

    public synchronized HashMap<Long, UUID> lookup(InetAddress inetAddress) {
        if (!addressIndexMap.containsValue(inetAddress)) return new HashMap<>();
        HashMap<Integer, Long> addressServedPlayerDataMap = getAddressServedPlayerData(inetAddress);
        HashMap<Long, UUID> result = new HashMap<>();
        for (int x : addressServedPlayerDataMap.keySet()) {
            result.put(addressServedPlayerDataMap.get(x), uniqueIDIndexMap.get(x));
        }
        return result;
    }

    public void addLoginRecord(UUID playerUniqueID, InetAddress playerINetAddress) {
        addLoginRecord(playerUniqueID, playerINetAddress, System.currentTimeMillis());
    }

    private synchronized void addLoginRecord(UUID playerUniqueID, InetAddress playerINetAddress, long time) {
        Map<Integer, Long> playerLoginAddressMap = getPlayerAddressData(playerUniqueID);
        Map<Integer, Long> addressServedPlayersMap = getAddressServedPlayerData(playerINetAddress);
        int aid = this.getINetAddressID(playerINetAddress);
        int uid = this.getUniqueIDID(playerUniqueID);
        if (!playerLoginAddressMap.containsKey(aid) || playerLoginAddressMap.get(aid) < time)
            playerLoginAddressMap.put(aid, time);
        if (!addressServedPlayersMap.containsKey(uid) || addressServedPlayersMap.get(uid) < time)
            addressServedPlayersMap.put(uid, time);
    }

    private void load() throws IOException, ClassNotFoundException {
        addressIndexMap = (BiMap<Integer, InetAddress>) readObjectFromFile(new File(getDataFolder(), "address.index"));
        uniqueIDIndexMap = (BiMap<Integer, UUID>) readObjectFromFile(new File(getDataFolder(), "player.index"));
        addressToUniqueIDMap = (Map<Integer, HashMap<Integer, Long>>) readObjectFromFile(new File(getDataFolder(), "address.data"));
        uniqueIDToAddressMap = (Map<Integer, HashMap<Integer, Long>>) readObjectFromFile(new File(getDataFolder(), "player.data"));
    }

    private synchronized void save() {
        try {
            saveObjectToFile(addressIndexMap, new File(getDataFolder(), "address.index"));
            saveObjectToFile(uniqueIDIndexMap, new File(getDataFolder(), "player.index"));
            saveObjectToFile(addressToUniqueIDMap, new File(getDataFolder(), "address.data"));
            saveObjectToFile(uniqueIDToAddressMap, new File(getDataFolder(), "player.data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveObjectToFile(Map targetObject, File targetFile) throws IOException {
        if (targetFile.exists()) targetFile.delete();
        targetFile.createNewFile();
        FileOutputStream out = new FileOutputStream(targetFile);
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(targetObject);
        objOut.flush();
        objOut.close();
        out.flush();
        out.close();
    }

    private Object readObjectFromFile(File targetFile) throws IOException, ClassNotFoundException {
        FileInputStream in = new FileInputStream(targetFile);
        if (targetFile.length() == 0) {
            return null;
        }
        ObjectInputStream objIn = new ObjectInputStream(in);
        return objIn.readObject();
    }

    private HashMap<Integer, Long> getPlayerAddressData(UUID playerUniqueID) {
        int uid = getUniqueIDID(playerUniqueID);
        if (!uniqueIDToAddressMap.containsKey(uid)) uniqueIDToAddressMap.put(uid, new HashMap<>());
        return uniqueIDToAddressMap.get(uid);
    }

    private HashMap<Integer, Long> getAddressServedPlayerData(InetAddress inetAddress) {
        int aid = getINetAddressID(inetAddress);
        if (!addressToUniqueIDMap.containsKey(aid)) addressToUniqueIDMap.put(aid, new HashMap<>());
        return addressToUniqueIDMap.get(aid);
    }

    private int getUniqueIDID(UUID uniqueID) {
        if (!uniqueIDIndexMap.inverse().containsKey(uniqueID)) uniqueIDIndexMap.put(uniqueIDIndexMap.size(), uniqueID);
        return uniqueIDIndexMap.inverse().get(uniqueID);
    }

    private int getINetAddressID(InetAddress address) {
        if (!addressIndexMap.inverse().containsKey(address)) addressIndexMap.put(addressIndexMap.size(), address);
        return addressIndexMap.inverse().get(address);
    }

    public ArrayList<Long> sort(Set<Long> longs) {
        ArrayList<Long> timeList = new ArrayList<>();
        for (long x : longs) {
            if (timeList.isEmpty()) {
                timeList.add(x);
                continue;
            }
            addElementToTimeList:
            {
                for (int i = 0; i < timeList.size(); i++) {
                    if (timeList.get(i) > x) {
                        timeList.add(i, x);
                        break addElementToTimeList;
                    }
                }
                timeList.add(x);
            }
        }
        return timeList;
    }

}
