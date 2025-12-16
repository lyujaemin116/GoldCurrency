// 파일 위치: src/main/java/com/yourname/goldcurrency/LostItemManager.java
package com.yourname.goldcurrency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class LostItemManager {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    // UUID : [남은 시간(초), 아이템 리스트]
    // 런타임 처리를 빠르게 하기 위해 메모리에 로드해두고, 변경 시마다 파일에 저장합니다.
    private final Map<UUID, Long> timers = new HashMap<>();
    private final Map<UUID, List<ItemStack>> lostItems = new HashMap<>();

    public LostItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lost_items.yml");
        loadData();
        startTimerTask();
    }

    // 사망 시 아이템 저장 (초기 시간 300초 = 5분)
    public void addLostItems(UUID uuid, List<ItemStack> items) {
        List<ItemStack> current = lostItems.getOrDefault(uuid, new ArrayList<>());
        current.addAll(items);
        lostItems.put(uuid, current);

        // 이미 타이머가 돌고 있다면 시간 초기화 하지 않음 (기존 아이템과 섞일 경우)
        // 혹은 사망 시마다 5분으로 리셋? -> 보통 리셋해주는 게 안전함.
        timers.put(uuid, 300L);

        saveData();
    }

    // 부활 또는 /lost 명령어 시 아이템 지급 시도
    public void tryRestoreItems(Player player) {
        UUID uuid = player.getUniqueId();
        if (!lostItems.containsKey(uuid) || lostItems.get(uuid).isEmpty()) {
            return;
        }

        List<ItemStack> items = new ArrayList<>(lostItems.get(uuid));
        List<ItemStack> leftovers = new ArrayList<>();

        // 인벤토리에 넣기
        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
            if (!remaining.isEmpty()) {
                leftovers.addAll(remaining.values());
            }
        }

        if (leftovers.isEmpty()) {
            // 모두 수령 완료
            lostItems.remove(uuid);
            timers.remove(uuid);
            player.sendMessage("§a모든 아이템을 수령했습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        } else {
            // 인벤토리 부족으로 남음
            lostItems.put(uuid, leftovers);
            // 시간은 그대로 유지 (또는 리셋하고 싶으면 timers.put(uuid, 300L) 추가)

            long timeLeft = timers.getOrDefault(uuid, 300L);
            notifyWarning(player, leftovers.size(), timeLeft);
        }

        saveData();
    }

    private void notifyWarning(Player player, int count, long seconds) {
        player.sendMessage("§c§l[경고] §c인벤토리가 부족하여 " + count + "개의 아이템을 받지 못했습니다!");
        player.sendMessage("§e§l/lost §e명령어로 남은 아이템을 반드시 수령하세요.");
        player.sendMessage("§c남은 시간: " + formatTime(seconds) + " (접속 중에만 시간이 줄어듭니다)");

        // 화면 중앙 타이틀 경고
        Title title = Title.title(
                Component.text("§c아이템 수령 필요!"),
                Component.text("§e/lost 명령어를 입력하세요 (" + formatTime(seconds) + " 남음)"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );
        player.showTitle(title);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
    }

    // 1초마다 온라인 플레이어의 타이머 감소
    private void startTimerTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (timers.containsKey(uuid)) {
                    long time = timers.get(uuid);
                    time--;

                    if (time <= 0) {
                        // 시간 초과 -> 삭제
                        timers.remove(uuid);
                        lostItems.remove(uuid);
                        saveData();
                        player.sendMessage("§c§l[주의] §c보관 시간이 만료되어 맡겨진 아이템이 소멸했습니다.");
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                    } else {
                        timers.put(uuid, time);

                        // 1분 단위 또는 30초, 10초 남았을 때 알림
                        if (time == 60 || time == 30 || time == 10) {
                            player.sendMessage("§c[주의] 아이템 소멸까지 " + time + "초 남았습니다! /lost로 수령하세요.");
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 2);
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    public boolean hasLostItems(UUID uuid) {
        return lostItems.containsKey(uuid) && !lostItems.get(uuid).isEmpty();
    }

    private String formatTime(long totalSeconds) {
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return String.format("%02d분 %02d초", m, s);
    }

    // --- 데이터 저장/로드 ---

    public void loadData() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        lostItems.clear();
        timers.clear();

        ConfigurationSection section = config.getConfigurationSection("data");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long time = section.getLong(key + ".time");
                    List<?> list = section.getList(key + ".items");

                    if (list != null) {
                        List<ItemStack> items = new ArrayList<>();
                        for (Object o : list) {
                            if (o instanceof ItemStack) items.add((ItemStack) o);
                        }
                        lostItems.put(uuid, items);
                        timers.put(uuid, time);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveData() {
        config.set("data", null);
        for (UUID uuid : lostItems.keySet()) {
            config.set("data." + uuid + ".time", timers.get(uuid));
            config.set("data." + uuid + ".items", lostItems.get(uuid));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}