package com.yourname.goldcurrency.listeners;

import com.yourname.goldcurrency.MoneyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class MiningListener implements Listener {
    private final JavaPlugin plugin;
    private final MoneyManager moneyManager;
    private final NamespacedKey oreKey;

    public MiningListener(JavaPlugin plugin, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.moneyManager = moneyManager;
        this.oreKey = new NamespacedKey(plugin, "placed_ores");
    }

    // 블록 위치를 문자열로 변환 (x,y,z)
    private String locToString(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE) {
            // 청크 데이터에 설치된 위치 저장
            PersistentDataContainer chunkData = event.getBlock().getChunk().getPersistentDataContainer();
            String currentData = chunkData.getOrDefault(oreKey, PersistentDataType.STRING, "");

            // "x,y,z/x,y,z/..." 형태로 저장
            String locStr = locToString(event.getBlock());
            if (!currentData.isEmpty()) currentData += "/";
            currentData += locStr;

            chunkData.set(oreKey, PersistentDataType.STRING, currentData);
        }
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;

        Block block = event.getBlock();
        Material blockType = block.getType();

        if (blockType == Material.GOLD_ORE || blockType == Material.DEEPSLATE_GOLD_ORE) {
            PersistentDataContainer chunkData = block.getChunk().getPersistentDataContainer();
            String currentData = chunkData.getOrDefault(oreKey, PersistentDataType.STRING, "");
            String locStr = locToString(block);

            // 데이터에 위치가 포함되어 있는지 확인 (플레이어가 설치한 블록)
            // 정확한 매칭을 위해 구분자(/) 처리
            boolean isPlaced = false;
            if (!currentData.isEmpty()) {
                String[] locs = currentData.split("/");
                StringBuilder newData = new StringBuilder();
                boolean first = true;

                for (String s : locs) {
                    if (s.equals(locStr)) {
                        isPlaced = true; // 찾음! (돈 주지 않음)
                        // 리스트에서 제거 (다시 캐면 없으므로)
                        continue;
                    }
                    if (!first) newData.append("/");
                    newData.append(s);
                    first = false;
                }

                // 데이터 갱신 (해당 블록 제거된 상태로)
                if (isPlaced) {
                    chunkData.set(oreKey, PersistentDataType.STRING, newData.toString());
                }
            }

            if (isPlaced) {
                return; // 플레이어가 설치했던 블록임 -> 돈 지급 X
            }

            // 자연 블록임 -> 돈 지급
            ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
            if (!tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                moneyManager.addMoney(event.getPlayer().getUniqueId(), 10);
                event.getPlayer().sendActionBar(Component.text("§e+10⛁ (채굴)"));
            }
        }
    }
}