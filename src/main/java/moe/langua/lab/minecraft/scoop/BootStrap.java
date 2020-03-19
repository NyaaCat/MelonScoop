package moe.langua.lab.minecraft.scoop;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class BootStrap extends JavaPlugin {
    public SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");

    private BiMap<Integer, InetAddress> addressIndexMap;
    private BiMap<Integer, UUID> uniqueIDIndexMap;
    private Map<Integer, HashMap<Integer, Long>> addressToUniqueIDMap;
    private Map<Integer, HashMap<Integer, Long>> uniqueIDToAddressMap;
    private Map<String, UUID> nameToUniqueIDMap = new HashMap<>();
    private static final int PLAYER_NAME_OFFSET = 33;

    @Override
    public void onEnable() {
        if (!this.getDataFolder().exists()) {
            try {
                this.getDataFolder().mkdir();
                this.getLogger().info(ChatColor.DARK_AQUA + "Starting to read player data from log files...");
                setup();
                save();
            } catch (IOException e) {
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                this.getLogger().warning("Exception occurred when indexing player data... Plugin will be disabled automatically. Please check the stack trace and delete the plugin data folder before next startup to restart player data index.");
                return;
            }
        } else {
            try {
                this.getLogger().info(ChatColor.DARK_AQUA + "Reading player data...");
                load();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        //register commands and listeners
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, this::save, 6000, 6000/*Auto saving per five minute*/);
        this.getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        this.getCommand("dig").setExecutor(new DigCommand(this));
    }

    private void setup() throws IOException {
        addressIndexMap = HashBiMap.create();
        uniqueIDIndexMap = HashBiMap.create();
        addressToUniqueIDMap = new HashMap<>();
        uniqueIDToAddressMap = new HashMap<>();

        File logFileFolder = new File("./logs");
        File[] logFiles = logFileFolder.listFiles();
        assert logFiles != null;
        int fileNumber = logFiles.length;
        int readed = 0;
        BufferedReader reader;
        for (File x : logFiles) {
            if (!x.getName().endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(x), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(x)), StandardCharsets.UTF_8));
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 35) {
                    continue;
                }
                boolean isLoginMessage = line.charAt(33) != '[' && line.contains("] logged in with entity id ");
                boolean isUniqueIDMessage = line.substring(10, 32).equalsIgnoreCase(" [User Authenticator #");
                if (isLoginMessage) {
                    String sub = line.substring(PLAYER_NAME_OFFSET, line.indexOf("logged in"));
                    String playerName = sub.substring(0, sub.indexOf("["));
                    String playerIPAddress = sub.substring(sub.indexOf("[/") + 2, sub.lastIndexOf(":"));
                    InetAddress playerINetAddress = InetAddress.getByName(playerIPAddress);
                    UUID playerUniqueID = nameToUniqueIDMap.get(playerName);
                    if (playerUniqueID == null) {
                        this.getLogger().warning("Player " + playerName + " has no unique ID information recorded, Ignore.");
                        continue;
                    }
                    int hour = Integer.parseInt(line.substring(1, 3));
                    int minute = Integer.parseInt(line.substring(4, 6));
                    int second = Integer.parseInt(line.substring(7, 9));
                    long dayTimeInMillSeconds = (hour * 3600 + minute * 60 + second) * 1000;
                    long playerLoginTime;
                    long dayStart;
                    if (x.getName().equalsIgnoreCase("latest.log")) {
                        dayStart = x.lastModified() - (x.lastModified() % 86400000)/*a day*/;
                        playerLoginTime = dayStart + dayTimeInMillSeconds;
                    } else {
                        int year = Integer.parseInt(x.getName().substring(0, 4));
                        int month = Integer.parseInt(x.getName().substring(5, 7));
                        int day = Integer.parseInt(x.getName().substring(8, 10));
                        Calendar c = Calendar.getInstance();
                        c.set(year, month - 1/*JANUARY is 0*/, day);
                        playerLoginTime = c.getTimeInMillis() + dayTimeInMillSeconds;
                    }
                    addLoginRecord(playerUniqueID, playerINetAddress, playerLoginTime);
                } else if (isUniqueIDMessage) {
                    String playerName = line.substring(line.indexOf("UUID of player ") + 15, line.indexOf(" is "));
                    String playerUniqueID = line.substring(line.indexOf(" is ") + 4);
                    nameToUniqueIDMap.put(playerName, UUID.fromString(playerUniqueID));
                }
            }
            this.getLogger().info(ChatColor.DARK_AQUA + "Reading server log files... " + (++readed) + "/" + fileNumber + " completed.");
        }
        this.getLogger().info(ChatColor.DARK_AQUA + "Done! " + uniqueIDIndexMap.size() + " players with " + addressIndexMap.size() + " IP address loaded.");

    }

    @Override
    public void onDisable(){
        save();
    }

    public synchronized HashMap<Long,InetAddress> lookup(UUID uniqueID){
        if(!uniqueIDIndexMap.containsValue(uniqueID)) return new HashMap<>();
        HashMap<Integer,Long> playerAddressDataMap = getPlayerAddressData(uniqueID);
        HashMap<Long,InetAddress> result = new HashMap<>();
        for(int x:playerAddressDataMap.keySet()){
            result.put(playerAddressDataMap.get(x),addressIndexMap.get(x));
        }
        return result;
    }

    public synchronized HashMap<Long,UUID> lookup(InetAddress inetAddress){
        if(!addressIndexMap.containsValue(inetAddress)) return new HashMap<>();
        HashMap<Integer,Long> addressServedPlayerDataMap = getAddressServedPlayerData(inetAddress);
        HashMap<Long,UUID> result = new HashMap<>();
        for(int x: addressServedPlayerDataMap.keySet()){
            result.put(addressServedPlayerDataMap.get(x),uniqueIDIndexMap.get(x));
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
