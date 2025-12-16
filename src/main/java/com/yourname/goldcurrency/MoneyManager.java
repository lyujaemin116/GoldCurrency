package com.yourname.goldcurrency;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoneyManager {
    private final JavaPlugin plugin;
    private final Map<UUID, Long> balances = new HashMap<>();
    private final File file;
    private FileConfiguration config;

    public MoneyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // 파일 이름은 "balances.yml" 입니다.
        this.file = new File(plugin.getDataFolder(), "balances.yml");
        loadData();
    }

    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    public void addMoney(UUID uuid, long amount) {
        balances.put(uuid, getBalance(uuid) + amount);
        saveData(); // 데이터 변경 시 즉시 저장 (안정성 확보)
    }

    public boolean removeMoney(UUID uuid, long amount) {
        long current = getBalance(uuid);
        if (current < amount) return false;
        balances.put(uuid, current - amount);
        saveData(); // 데이터 변경 시 즉시 저장
        return true;
    }

    public void setMoney(UUID uuid, long amount) {
        balances.put(uuid, amount);
        saveData();
    }

    public void loadData() {
        // 1. 플러그인 데이터 폴더가 없으면 생성 (plugins/GoldCurrency)
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 2. 파일이 없으면 빈 파일 생성
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("balances.yml 파일을 생성할 수 없습니다!");
                e.printStackTrace();
                return;
            }
        }

        // 3. 파일 로드
        config = YamlConfiguration.loadConfiguration(file);

        // 4. 데이터 맵으로 불러오기
        if (config.contains("balances")) {
            for (String key : config.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long amount = config.getLong("balances." + key);
                    balances.put(uuid, amount);
                } catch (IllegalArgumentException e) {
                    // 유효하지 않은 UUID는 무시
                }
            }
        }
    }

    public void saveData() {
        // 맵의 데이터를 config 객체에 기록
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            config.set("balances." + entry.getKey().toString(), entry.getValue());
        }

        // 파일에 쓰기
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("balances.yml 파일을 저장하는 중 오류가 발생했습니다!");
            e.printStackTrace();
        }
    }
}