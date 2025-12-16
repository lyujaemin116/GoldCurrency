// 파일 위치: src/main/java/com/yourname/goldcurrency/listeners/DeathListener.java
package com.yourname.goldcurrency.listeners;

import com.yourname.goldcurrency.CurrencyItemManager;
import com.yourname.goldcurrency.LostItemManager; // 추가
import com.yourname.goldcurrency.MoneyManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeathListener implements Listener {
    private final MoneyManager moneyManager;
    private final CurrencyItemManager itemManager;
    private final LostItemManager lostItemManager; // 추가

    public DeathListener(MoneyManager moneyManager, CurrencyItemManager itemManager, LostItemManager lostItemManager) {
        this.moneyManager = moneyManager;
        this.itemManager = itemManager;
        this.lostItemManager = lostItemManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 1. 계좌 잔액 차감 (30%)
        long balance = moneyManager.getBalance(player.getUniqueId());
        long lost = (long) (balance * 0.3);
        if (lost > 0) {
            moneyManager.removeMoney(player.getUniqueId(), lost);
            player.sendMessage("§c사망하여 계좌 잔액의 30%인 " + lost + "⛁를 잃었습니다.");
        }

        // 2. 인벤토리 세이브 로직
        event.setKeepInventory(false);
        event.setDroppedExp(0);
        event.setKeepLevel(true);

        List<ItemStack> itemsToSave = new ArrayList<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();

            if (itemManager.isCurrencyItem(item)) continue; // 화폐는 드랍
            if (item.getType() == Material.PLAYER_HEAD) continue; // 머리는 드랍

            itemsToSave.add(item); // 저장할 아이템 목록에 추가
            iterator.remove();     // 바닥에 떨어지지 않게 제거
        }

        // [핵심 변경] 사망 즉시 LostItemManager를 통해 파일에 안전하게 저장
        if (!itemsToSave.isEmpty()) {
            lostItemManager.addLostItems(player.getUniqueId(), itemsToSave);
            player.sendMessage("§e아이템이 안전하게 임시 보관되었습니다.");
            player.sendMessage("§e부활 시 자동으로 지급되며, 인벤토리가 부족하면 /lost로 찾을 수 있습니다.");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // 부활 시 저장된 아이템 자동 수령 시도
        // (타이머가 있더라도 부활 순간에는 자동으로 넣어주는 것이 편의상 좋음)
        // 1틱 뒤에 실행해야 인벤토리 초기화 후 정상적으로 들어감
        event.getPlayer().getServer().getScheduler().runTaskLater(
                event.getPlayer().getServer().getPluginManager().getPlugin("GoldCurrency"), // 플러그인 인스턴스 가져오기 (혹은 생성자로 전달받은 plugin 사용)
                () -> lostItemManager.tryRestoreItems(event.getPlayer()),
                1L
        );
    }
}