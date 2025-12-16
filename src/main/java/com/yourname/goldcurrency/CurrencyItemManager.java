package com.yourname.goldcurrency;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CurrencyItemManager {
    private final NamespacedKey currencyKey;

    public CurrencyItemManager(JavaPlugin plugin) {
        // 화폐 아이템을 식별하기 위한 고유 키 생성 ("currency_value")
        this.currencyKey = new NamespacedKey(plugin, "currency_value");
    }

    /**
     * 금액에 맞는 화폐 아이템을 생성합니다.
     * @param value 금액 (10, 100, 1000)
     * @return 생성된 ItemStack 또는 null (잘못된 금액일 경우)
     */
    public ItemStack createCurrencyItem(int value) {
        Material mat;
        String name;

        // 단위별 아이템 종류 및 이름 설정
        if (value == 10) {
            mat = Material.GOLD_NUGGET;
            name = "10⛁";
        } else if (value == 100) {
            mat = Material.GOLD_INGOT;
            name = "100⛁";
        } else if (value == 1000) {
            mat = Material.GOLD_BLOCK;
            name = "1000⛁";
        } else {
            return null; // 정의되지 않은 단위는 null 반환
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 1. 이름 설정 (노란색)
            meta.displayName(Component.text("§e" + name));

            // 2. 인챈트 효과 (반짝임) 추가
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

            // 3. 인챈트 설명 숨기기 (깔끔하게 보이도록)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // 4. **핵심**: 데이터 태그(PDC) 추가
            // 일반 아이템과 플러그인 화폐를 구분하는 유일한 수단입니다.
            meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.INTEGER, value);

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 해당 아이템이 이 플러그인의 화폐인지 확인하고, 화폐라면 가치를 반환합니다.
     * @param item 확인할 아이템
     * @return 화폐 가치 (화폐가 아니면 0)
     */
    public int getCurrencyValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        // 아이템에 심어둔 키 값을 읽어옴 (없으면 0 반환)
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(currencyKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * 해당 아이템이 화폐 아이템인지 여부만 확인합니다.
     */
    public boolean isCurrencyItem(ItemStack item) {
        return getCurrencyValue(item) > 0;
    }
}