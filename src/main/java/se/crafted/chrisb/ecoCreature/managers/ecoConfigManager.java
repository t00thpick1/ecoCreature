package se.crafted.chrisb.ecoCreature.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.CreatureType;
import org.bukkit.util.config.Configuration;

import se.crafted.chrisb.ecoCreature.ecoCreature;
import se.crafted.chrisb.ecoCreature.models.ecoDrop;
import se.crafted.chrisb.ecoCreature.models.ecoMessage;
import se.crafted.chrisb.ecoCreature.models.ecoReward;
import se.crafted.chrisb.ecoCreature.utils.ecoEntityUtil.TimePeriod;
import se.crafted.chrisb.ecoCreature.utils.ecoLogger;

public class ecoConfigManager
{
    private static final String OLD_CONFIG_FILE = "ecoCreature.yml";
    private static final String DEFAULT_CONFIG_FILE = "default.yml";

    private static final String DEFAULT_WORLD = "default";

    private final ecoCreature plugin;
    private final ecoLogger log;
    private Boolean isEnabled;

    public static boolean debug;

    public ecoConfigManager(ecoCreature plugin)
    {
        this.plugin = plugin;
        log = this.plugin.getLogger();
    }

    public Boolean isEnabled()
    {
        return isEnabled;
    }

    public void load() throws IOException
    {
        Configuration defaultConfig;

        File defaultConfigFile = new File(ecoCreature.dataFolder, DEFAULT_CONFIG_FILE);
        File oldConfigFile = new File(ecoCreature.dataFolder, OLD_CONFIG_FILE);

        if (defaultConfigFile.exists()) {
            defaultConfig = new Configuration(defaultConfigFile);
        }
        else if (oldConfigFile.exists()) {
            defaultConfig = new Configuration(oldConfigFile);
        }
        else {
            defaultConfig = getConfig(defaultConfigFile);
        }

        defaultConfig.load();
        loadConfig(DEFAULT_WORLD, defaultConfig);

        for (World world : plugin.getServer().getWorlds()) {
            ecoCreature.messageManagers.put(world.getName(), ecoCreature.messageManagers.get(DEFAULT_WORLD).clone());
            ecoCreature.rewardManagers.put(world.getName(), ecoCreature.rewardManagers.get(DEFAULT_WORLD).clone());
            File worldConfigFile = new File(ecoCreature.dataWorldsFolder, world.getName() + ".yml");
            Configuration worldConfig = getConfig(worldConfigFile);
            worldConfig.load();
            loadConfig(world.getName(), worldConfig);
        }
    }

    public void loadConfig(String worldName, Configuration config)
    {
        ecoMessageManager messageManager;
        ecoRewardManager rewardManager;

        if (ecoCreature.messageManagers.containsKey(worldName)) {
            messageManager = ecoCreature.messageManagers.get(worldName);
        }
        else {
            messageManager = new ecoMessageManager(plugin);
        }

        if (ecoCreature.rewardManagers.containsKey(worldName)) {
            rewardManager = ecoCreature.rewardManagers.get(worldName);
        }
        else {
            rewardManager = new ecoRewardManager(plugin);
        }
        isEnabled = config.getBoolean("DidYou.Read.Understand.Configure", true);

        debug = config.getBoolean("System.debug", false);

        rewardManager.isIntegerCurrency = config.getBoolean("System.Economy.IntegerCurrency", false);

        rewardManager.canCampSpawner = config.getBoolean("System.Hunting.AllowCamping", false);
        rewardManager.shouldClearCampDrops = config.getBoolean("System.Hunting.ClearCampDrops", true);
        rewardManager.shouldOverrideDrops = config.getBoolean("System.Hunting.OverrideDrops", true);
        rewardManager.isFixedDrops = config.getBoolean("System.Hunting.FixedDrops", false);
        rewardManager.campRadius = config.getInt("System.Hunting.CampRadius", 7);
        rewardManager.hasBowRewards = config.getBoolean("System.Hunting.BowRewards", true);
        rewardManager.hasDeathPenalty = config.getBoolean("System.Hunting.PenalizeDeath", false);
        rewardManager.hasPVPReward = config.getBoolean("System.Hunting.PVPReward", true);
        rewardManager.isPercentPenalty = config.getBoolean("System.Hunting.PenalizeType", true);
        rewardManager.isPercentPvpReward = config.getBoolean("System.Hunting.PVPRewardType", true);
        rewardManager.penaltyAmount = config.getDouble("System.Hunting.PenalizeAmount", 0.05D);
        rewardManager.pvpRewardAmount = config.getDouble("System.Hunting.PenalizeAmount", 0.05D);
        rewardManager.canHuntUnderSeaLevel = config.getBoolean("System.Hunting.AllowUnderSeaLVL", true);
        rewardManager.isWolverineMode = config.getBoolean("System.Hunting.WolverineMode", true);
        rewardManager.hasDTPRewards = config.getBoolean("System.Hunting.DTPRewards", true);
        rewardManager.dtpPenaltyAmount = config.getDouble("System.Hunting.DTPDeathStreakPenalty", 5.0D);
        rewardManager.dtpPenaltyAmount = config.getDouble("System.Hunting.DTPKillStreakPenalty", 10.0D);
        rewardManager.noFarm = config.getBoolean("System.Hunting.NoFarm", false);

        messageManager.shouldOutputMessages = config.getBoolean("System.Messages.Output", true);
        messageManager.noBowRewardMessage = new ecoMessage(convertMessage(config.getString("System.Messages.NoBowMessage", ecoMessageManager.NO_BOW_REWARD_MESSAGE)), true);
        messageManager.noCampMessage = new ecoMessage(convertMessage(config.getString("System.Messages.NoCampMessage", ecoMessageManager.NO_CAMP_MESSAGE)), config.getBoolean("System.Messages.Spawner", false));
        messageManager.deathPenaltyMessage = new ecoMessage(convertMessage(config.getString("System.Messages.DeathPenaltyMessage", ecoMessageManager.DEATH_PENALTY_MESSAGE)), true);
        messageManager.pvpRewardMessage = new ecoMessage(convertMessage(config.getString("System.Messages.PVPRewardMessage", ecoMessageManager.PVP_REWARD_MESSAGE)), true);
        messageManager.dtpDeathStreakMessage = new ecoMessage(convertMessage(config.getString("System.Messages.DTPDeathStreakMessage", ecoMessageManager.DTP_DEATHSTREAK_MESSAGE)), true);
        messageManager.dtpKillStreakMessage = new ecoMessage(convertMessage(config.getString("System.Messages.DTPKillStreakMessage", ecoMessageManager.DTP_KILLSTREAK_MESSAGE)), true);

        if (config.getKeys("Gain.Groups") != null) {
            for (String group : config.getKeys("Gain.Groups")) {
                rewardManager.groupMultiplier.put(group.toLowerCase(), Double.valueOf(config.getDouble("Gain.Groups." + group + ".Amount", 0.0D)));
            }
        }

        if (config.getKeys("Gain.Time") != null) {
            for (String period : config.getKeys("Gain.Time")) {
                rewardManager.timeMultiplier.put(TimePeriod.fromName(period), Double.valueOf(config.getDouble("Gain.Time." + period + ".Amount", 1.0D)));
            }
        }

        if (config.getKeys("Gain.Environment") != null) {
            for (String environment : config.getKeys("Gain.Environment")) {
                rewardManager.envMultiplier.put(Environment.valueOf(environment), Double.valueOf(config.getDouble("Gain.Environment." + environment + ".Amount", 1.0D)));
            }
        }

        if (config.getKeys("RewardTable") != null) {
            for (String creatureName : config.getKeys("RewardTable")) {
                ecoReward reward = new ecoReward();
                reward.setCreatureName(creatureName);
                reward.setCreatureType(CreatureType.fromName(creatureName));

                String root = "RewardTable." + creatureName;
                reward.setDrops(parseDrops(config.getString(root + ".Drops"), rewardManager.isFixedDrops));
                reward.setCoinMax(config.getDouble(root + ".Coin_Maximum", 0));
                reward.setCoinMin(config.getDouble(root + ".Coin_Minimum", 5));
                reward.setCoinPercentage(config.getDouble(root + ".Coin_Percent", 50));

                reward.setNoRewardMessage(new ecoMessage(convertMessage(config.getString(root + ".NoReward_Message", ecoMessageManager.NO_REWARD_MESSAGE)), config.getBoolean("System.Messages.NoReward", false)));
                reward.setRewardMessage(new ecoMessage(convertMessage(config.getString(root + ".Reward_Message", ecoMessageManager.REWARD_MESSAGE)), true));
                reward.setPenaltyMessage(new ecoMessage(convertMessage(config.getString(root + ".Penalty_Message", ecoMessageManager.PENALTY_MESSAGE)), true));

                if (creatureName.equals("Spawner")) {
                    rewardManager.spawnerReward = reward;
                }
                else {
                    rewardManager.rewards.put(reward.getCreatureType(), reward);
                }
            }
        }

        ecoCreature.messageManagers.put(worldName, messageManager);
        ecoCreature.rewardManagers.put(worldName, rewardManager);
    }

    private Configuration getConfig(File configFile) throws IOException
    {
        if (!configFile.exists()) {
            InputStream inputStream = ecoCreature.class.getResourceAsStream(DEFAULT_CONFIG_FILE);
            FileOutputStream outputStream = new FileOutputStream(configFile);

            byte[] buffer = new byte[8192];
            int length = 0;
            while ((length = inputStream.read(buffer)) > 0)
                outputStream.write(buffer, 0, length);

            inputStream.close();
            outputStream.close();

            log.info("Default settings file written: " + DEFAULT_CONFIG_FILE);
        }

        return new Configuration(new File(configFile.getPath()));
    }

    private static String convertMessage(String message)
    {
        if (message != null) {
            return message.replaceAll("&&", "\b").replaceAll("&", "§").replaceAll("\b", "&");
        }

        return null;
    }

    private List<ecoDrop> parseDrops(String dropsString, Boolean isFixedDrops)
    {
        if (dropsString != null && !dropsString.isEmpty()) {
            List<ecoDrop> drops = new ArrayList<ecoDrop>();

            try {
                for (String dropString : dropsString.split(";")) {
                    String[] dropParts = dropString.split(":");
                    ecoDrop drop = new ecoDrop();
                    String[] itemParts = dropParts[0].split("\\.");
                    try {
                        drop.setItem(Material.getMaterial(Integer.parseInt(itemParts.length > 0 ? itemParts[0] : dropParts[0])));
                    }
                    catch (NumberFormatException e) {
                        drop.setItem(Material.matchMaterial(itemParts.length > 0 ? itemParts[0] : dropParts[0]));
                    }
                    drop.setData(itemParts.length > 1 ? Byte.parseByte(itemParts[1]) : 0);
                    String[] amountRange = dropParts[1].split("-");
                    if (amountRange.length == 2) {
                        drop.setMinAmount(Integer.parseInt(amountRange[0]));
                        drop.setMaxAmount(Integer.parseInt(amountRange[1]));
                    }
                    else {
                        drop.setMaxAmount(Integer.parseInt(dropParts[1]));
                    }
                    drop.setPercentage(Double.parseDouble(dropParts[2]));
                    drop.setIsFixedDrops(isFixedDrops);
                    drops.add(drop);
                }

                return drops;
            }
            catch (Exception exception) {
                log.warning("Failed to parse drops: " + dropsString);
            }
        }

        return null;
    }
}