// 파일 위치: src/main/java/com/yourname/goldcurrency/listeners/InteractListener.java
package com.yourname.goldcurrency.listeners;

import com.yourname.goldcurrency.CurrencyItemManager;
import com.yourname.goldcurrency.MoneyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {
    private final MoneyManager moneyManager;
    private final CurrencyItemManager itemManager;

    public InteractListener(MoneyManager moneyManager, CurrencyItemManager itemManager) {
        this.moneyManager = moneyManager;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        int value = itemManager.getCurrencyValue(item);
        if (value > 0) {
            event.setCancelled(true); // 아이템 설치 방지 등

            int amount = 1;
            if (event.getPlayer().isSneaking()) {
                amount = item.getAmount(); // 웅크리기 시 모두 입금
            }

            item.setAmount(item.getAmount() - amount);
            moneyManager.addMoney(event.getPlayer().getUniqueId(), (long) value * amount);

            event.getPlayer().sendMessage("§e" + (value * amount) + "⛁가 계좌에 입금되었습니다.");
        }
    }
}