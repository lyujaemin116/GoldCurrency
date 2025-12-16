// 파일 위치: src/main/java/com/yourname/goldcurrency/commands/ShopCommand.java
package com.yourname.goldcurrency.commands;

import com.yourname.goldcurrency.ShopManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ShopCommand implements CommandExecutor {
    private final ShopManager shopManager;

    public ShopCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        // 1. /shop - 상점 열기 (0페이지)
        if (args.length == 0) {
            shopManager.openShop(player, 0);
            return true;
        }

        // 2. /shop add <가격> [이름]
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                player.sendMessage("§c사용법: /shop add <가격> [설정할 이름]");
                return true;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getType() == Material.AIR) {
                player.sendMessage("§c손에 팔고 싶은 아이템을 들어주세요.");
                return true;
            }

            try {
                long price = Long.parseLong(args[1]);
                if (price <= 0) {
                    player.sendMessage("§c가격은 0보다 커야 합니다.");
                    return true;
                }

                ItemStack itemToSell = handItem.clone();
                // 이름 설정 로직
                if (args.length > 2) {
                    String customName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    ItemMeta meta = itemToSell.getItemMeta();
                    meta.displayName(Component.text(customName.replace("&", "§")));
                    itemToSell.setItemMeta(meta);
                }

                // 무제한 등록이므로 성공 여부 체크 불필요
                shopManager.addItemToShop(player, itemToSell, price);

                player.getInventory().setItemInMainHand(null);
                player.sendMessage("§e아이템이 상점에 " + price + "⛁으로 등록되었습니다.");

            } catch (NumberFormatException e) {
                player.sendMessage("§c올바른 숫자를 입력해주세요.");
            }
            return true;
        }

        // 3. /shop reset
        if (args[0].equalsIgnoreCase("reset")) {
            if (!player.isOp()) {
                player.sendMessage("§c권한이 없습니다.");
                return true;
            }
            shopManager.resetShop();
            player.sendMessage("§e상점 목록이 초기화되었습니다.");
            return true;
        }

        return false;
    }
}