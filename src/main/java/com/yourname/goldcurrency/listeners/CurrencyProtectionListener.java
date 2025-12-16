package com.yourname.goldcurrency.listeners;

import com.yourname.goldcurrency.CurrencyItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*; // 모든 인벤토리 이벤트 import
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class CurrencyProtectionListener implements Listener {
    private final CurrencyItemManager itemManager;

    public CurrencyProtectionListener(CurrencyItemManager itemManager) {
        this.itemManager = itemManager;
    }

    // [기존 방지 로직들 유지]
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (itemManager.isCurrencyItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("§c화폐는 버릴 수 없습니다."));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (itemManager.isCurrencyItem(event.getItemInHand())) event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            ItemStack handItem = event.getPlayer().getInventory().getItem(event.getHand());
            if (itemManager.isCurrencyItem(handItem)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (itemManager.isCurrencyItem(event.getMainHandItem()) || itemManager.isCurrencyItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // [기존 꾸러미/외부이동 방지 로직 유지]
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean currencyInvolved = (current != null && itemManager.isCurrencyItem(current)) || (cursor != null && itemManager.isCurrencyItem(cursor));
        if (!currencyInvolved) return;

        if (event.getClick() == ClickType.RIGHT) {
            ItemStack clicked = event.getCurrentItem();
            ItemStack held = event.getCursor();
            if ((clicked != null && clicked.getType() == Material.BUNDLE) || (held != null && held.getType() == Material.BUNDLE)) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) return;
        if (event.getClickedInventory() == event.getView().getTopInventory() || event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (itemManager.isCurrencyItem(event.getOldCursor())) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // --- [추가된 버그 수정] ---

    // 1. 제작대/모루 사용 방지 (화폐 파손 방지)
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && itemManager.isCurrencyItem(item)) {
                event.getInventory().setResult(null); // 제작 결과물 없음
                return;
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        // 모루의 첫 번째 또는 두 번째 슬롯에 화폐가 있으면 차단
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();

        if ((first != null && itemManager.isCurrencyItem(first)) ||
                (second != null && itemManager.isCurrencyItem(second))) {
            event.setResult(null);
        }
    }

    // 2. 호퍼(깔때기) 흡수 방지
    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (itemManager.isCurrencyItem(event.getItem().getItemStack())) {
            event.setCancelled(true); // 호퍼가 화폐를 빨아들이지 못하게 함
            // 선택사항: event.getItem().remove(); // 바닥의 아이템을 아예 삭제? (너무 가혹하므로 흡수만 차단)
        }
    }
}