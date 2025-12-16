// 파일 위치: src/main/java/com/yourname/goldcurrency/commands/PayCommand.java
package com.yourname.goldcurrency.commands;

import com.yourname.goldcurrency.MoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PayCommand implements CommandExecutor {
    private final MoneyManager moneyManager;

    public PayCommand(MoneyManager moneyManager) {
        this.moneyManager = moneyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length != 2) {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§c해당 플레이어를 찾을 수 없습니다.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§c자신에게 보낼 수 없습니다.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c올바른 숫자를 입력해주세요.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("§c0보다 큰 금액을 입력해주세요.");
            return true;
        }

        if (moneyManager.removeMoney(player.getUniqueId(), amount)) {
            moneyManager.addMoney(target.getUniqueId(), amount);
            player.sendMessage("§e" + target.getName() + "님에게 " + amount + "⛁을 보냈습니다.");
            target.sendMessage("§e" + player.getName() + "님으로부터 " + amount + "⛁을 받았습니다.");
        } else {
            player.sendMessage("§c잔액이 부족합니다.");
        }

        return true;
    }
}