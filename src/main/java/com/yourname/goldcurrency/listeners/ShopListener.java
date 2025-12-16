package com.yourname.goldcurrency.listeners;

import com.yourname.goldcurrency.MoneyManager;
import com.yourname.goldcurrency.ShopManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class ShopListener implements Listener {
    private final ShopManager shopManager;
    private final MoneyManager moneyManager;

    public ShopListener(ShopManager shopManager, MoneyManager moneyManager) {
        this.shopManager = shopManager;
        this.moneyManager = moneyManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getInventory();
        if (!(clickedInv.getHolder() instanceof ShopManager)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != clickedInv) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getSlot();
        int currentPage = shopManager.getPlayerPage(player);

        // 페이지 버튼 처리
        if (slot == 45) {
            shopManager.openShop(player, currentPage - 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 53) {
            shopManager.openShop(player, currentPage + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot >= 45) return;

        // --- 아이템 유효성 검사 (Race Condition 방지) ---

        // 1. 클릭한 아이콘 확인
        ItemStack clickedIcon = event.getCurrentItem();
        if (clickedIcon == null || !clickedIcon.hasItemMeta()) return;

        // 2. 아이콘에 심긴 고유 ID 추출
        NamespacedKey key = shopManager.getShopItemIdKey();
        String idOnIcon = clickedIcon.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (idOnIcon == null) return; // 상점 물건이 아님

        // 3. 현재 데이터베이스 상의 아이템 확인
        int realIndex = shopManager.getItemIndex(player, slot);
        ShopManager.ShopItem shopItem = shopManager.getShopItemByIndex(realIndex);

        // 4. ID 비교 (내가 보고 있는 물건이 지금 그 자리에 있는 물건과 같은가?)
        if (shopItem == null || !shopItem.uniqueId.toString().equals(idOnIcon)) {
            player.sendMessage("§c상점 목록이 갱신되어 구매에 실패했습니다. (이미 팔린 상품일 수 있습니다)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            shopManager.openShop(player, currentPage); // 새로고침
            return;
        }

        // --- 본인 아이템 회수 ---
        if (shopItem.sellerUUID.equals(player.getUniqueId())) {
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(shopItem.itemStack);

            if (!leftOver.isEmpty()) {
                for (ItemStack drop : leftOver.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage("§e인벤토리가 가득 차 아이템이 바닥에 떨어졌습니다.");
            } else {
                player.sendMessage("§e등록했던 아이템을 회수했습니다.");
            }

            shopManager.removeShopItem(realIndex);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            shopManager.openShop(player, currentPage);
            return;
        }

        // --- 타인 아이템 구매 ---
        long price = shopItem.price;
        long balance = moneyManager.getBalance(player.getUniqueId());

        if (balance < price) {
            player.sendMessage("§c잔액이 부족합니다! (보유: " + balance + "⛁)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            player.closeInventory();
            return;
        }

        moneyManager.removeMoney(player.getUniqueId(), price);
        moneyManager.addMoney(shopItem.sellerUUID, price);

        Player seller = player.getServer().getPlayer(shopItem.sellerUUID);
        if (seller != null) {
            seller.sendMessage("§e" + player.getName() + "님이 당신의 아이템을 구매하여 " + price + "⛁가 입금되었습니다.");
        }

        HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(shopItem.itemStack);
        if (!leftOver.isEmpty()) {
            for (ItemStack drop : leftOver.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§c인벤토리가 가득 차 아이템이 바닥에 떨어졌습니다.");
        }

        player.sendMessage("§e아이템을 구매했습니다! (-" + price + "⛁)");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        shopManager.removeShopItem(realIndex);
        shopManager.openShop(player, currentPage);
    }
}