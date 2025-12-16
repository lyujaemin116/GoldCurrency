// 파일 위치: src/main/java/com/yourname/goldcurrency/commands/WithdrawCommand.java
package com.yourname.goldcurrency.commands;

import com.yourname.goldcurrency.CurrencyItemManager;
import com.yourname.goldcurrency.MoneyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WithdrawCommand implements CommandExecutor {
    private final MoneyManager moneyManager;
    private final CurrencyItemManager itemManager;

    public WithdrawCommand(MoneyManager moneyManager, CurrencyItemManager itemManager) {
        this.moneyManager = moneyManager;
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length != 1) return false;

        long amount;
        try {
            amount = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c올바른 숫자를 입력해주세요.");
            return true;
        }

        if (amount < 10) {
            player.sendMessage("§c최소 출금 단위는 10⛁입니다");
            return true;
        }

        if (amount % 10 != 0) {
            player.sendMessage("§c10⛁ 단위로만 출금할 수 있습니다.");
            return true;
        }

        if (moneyManager.getBalance(player.getUniqueId()) < amount) {
            player.sendMessage("§c잔액이 부족합니다.");
            return true;
        }

        // 돈 차감
        moneyManager.removeMoney(player.getUniqueId(), amount);

        // 아이템 계산
        List<ItemStack> itemsToGive = new ArrayList<>();
        long remaining = amount;

        long blocks = remaining / 1000;
        remaining %= 1000;
        long ingots = remaining / 100;
        remaining %= 100;
        long nuggets = remaining / 10;

        addItems(itemsToGive, 1000, (int) blocks);
        addItems(itemsToGive, 100, (int) ingots);
        addItems(itemsToGive, 10, (int) nuggets);

        // 아이템 지급 및 드랍
        boolean dropped = false;
        for (ItemStack item : itemsToGive) {
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
            if (!leftOver.isEmpty()) {
                for (ItemStack drop : leftOver.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                dropped = true;
            }
        }

        player.sendMessage("§e" + amount + "⛁을 출금했습니다.");
        if (dropped) {
            player.sendMessage("§c인벤토리가 부족하여 일부 화폐가 바닥에 떨어졌습니다!");
        }

        return true;
    }

    private void addItems(List<ItemStack> list, int value, int count) {
        if (count <= 0) return;
        ItemStack item = itemManager.createCurrencyItem(value);
        int maxStack = item.getMaxStackSize();
        while (count > 0) {
            int amount = Math.min(count, maxStack);
            ItemStack clone = item.clone();
            clone.setAmount(amount);
            list.add(clone);
            count -= amount;
        }
    }
}