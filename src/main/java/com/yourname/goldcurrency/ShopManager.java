package com.yourname.goldcurrency;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager implements InventoryHolder {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    // 레이스 컨디션 방지용 키
    private final NamespacedKey shopItemIdKey;

    private final List<ShopItem> shopItems = new ArrayList<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();

    public static class ShopItem {
        public UUID uniqueId; // 아이템 고유 식별자 (순서가 바뀌어도 식별 가능)
        public ItemStack itemStack;
        public long price;
        public UUID sellerUUID;
        public String sellerName;

        public ShopItem(UUID uniqueId, ItemStack itemStack, long price, UUID sellerUUID, String sellerName) {
            this.uniqueId = uniqueId != null ? uniqueId : UUID.randomUUID();
            this.itemStack = itemStack;
            this.price = price;
            this.sellerUUID = sellerUUID;
            this.sellerName = sellerName;
        }
    }

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
        this.shopItemIdKey = new NamespacedKey(plugin, "shop_item_id");
        loadShop();
    }

    public NamespacedKey getShopItemIdKey() {
        return shopItemIdKey;
    }

    public void addItemToShop(Player seller, ItemStack item, long price) {
        // 새 아이템 등록 시 고유 ID 생성
        shopItems.add(new ShopItem(UUID.randomUUID(), item.clone(), price, seller.getUniqueId(), seller.getName()));
        saveShop();
    }

    public void removeShopItem(int index) {
        if (index >= 0 && index < shopItems.size()) {
            shopItems.remove(index);
            saveShop();
        }
    }

    public void resetShop() {
        shopItems.clear();
        if (file.exists()) file.delete();
        saveShop();
    }

    public void openShop(Player player, int page) {
        if (page < 0) page = 0;

        int totalItems = shopItems.size();
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;

        playerCurrentPage.put(player.getUniqueId(), page);

        Inventory shopInv = Bukkit.createInventory(this, 54, Component.text("Marketplace (Page " + (page + 1) + ")"));

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = shopItems.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = shopItem.itemStack.clone();
            ItemMeta meta = displayItem.getItemMeta();

            // [중요] 화면에 보이는 아이템에 고유 ID 심기 (보이지 않는 태그)
            meta.getPersistentDataContainer().set(shopItemIdKey, PersistentDataType.STRING, shopItem.uniqueId.toString());

            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();

            lore.add(Component.text(""));
            lore.add(Component.text("§8------------------"));
            lore.add(Component.text("§7판매자: §f" + shopItem.sellerName));
            lore.add(Component.text("§6가격: §e" + shopItem.price + "⛁"));
            lore.add(Component.text("§e클릭하여 구매/회수"));
            lore.add(Component.text("§8------------------"));

            meta.lore(lore);
            displayItem.setItemMeta(meta);

            shopInv.setItem(slot, displayItem);
        }

        // 하단 메뉴바 설정
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 45; i < 54; i++) {
            shopInv.setItem(i, glass);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta pm = prev.getItemMeta();
            pm.displayName(Component.text("§c<< 이전 페이지"));
            prev.setItemMeta(pm);
            shopInv.setItem(45, prev);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.displayName(Component.text("§e페이지: " + (page + 1) + " / " + totalPages));
        info.setItemMeta(im);
        shopInv.setItem(49, info);

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta nm = next.getItemMeta();
            nm.displayName(Component.text("§a다음 페이지 >>"));
            next.setItemMeta(nm);
            shopInv.setItem(53, next);
        }

        player.openInventory(shopInv);
    }

    public int getItemIndex(Player player, int slot) {
        int page = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        return (page * 45) + slot;
    }

    public int getPlayerPage(Player player) {
        return playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
    }

    public ShopItem getShopItemByIndex(int index) {
        if (index >= 0 && index < shopItems.size()) {
            return shopItems.get(index);
        }
        return null;
    }

    // --- 저장/로드 ---

    public void loadShop() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        shopItems.clear();

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            List<String> keys = new ArrayList<>(itemsSection.getKeys(false));
            // 숫자 키 순서대로 정렬
            keys.sort((a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                } catch (NumberFormatException e) {
                    return 0;
                }
            });

            for (String key : keys) {
                try {
                    ItemStack item = itemsSection.getItemStack(key + ".item");
                    long price = itemsSection.getLong(key + ".price");
                    String sellerUUIDStr = itemsSection.getString(key + ".sellerUUID");
                    String sellerName = itemsSection.getString(key + ".sellerName");
                    // 저장된 ID가 없으면 새로 생성 (구버전 호환)
                    String uidStr = itemsSection.getString(key + ".uniqueId");
                    UUID uid = uidStr != null ? UUID.fromString(uidStr) : UUID.randomUUID();

                    if (item != null && sellerUUIDStr != null) {
                        shopItems.add(new ShopItem(uid, item, price, UUID.fromString(sellerUUIDStr), sellerName));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("상점 아이템 로드 실패: " + key);
                }
            }
        }
    }

    public void saveShop() {
        config.set("items", null);
        for (int i = 0; i < shopItems.size(); i++) {
            ShopItem item = shopItems.get(i);
            String path = "items." + i;
            config.set(path + ".uniqueId", item.uniqueId.toString());
            config.set(path + ".item", item.itemStack);
            config.set(path + ".price", item.price);
            config.set(path + ".sellerUUID", item.sellerUUID.toString());
            config.set(path + ".sellerName", item.sellerName);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}