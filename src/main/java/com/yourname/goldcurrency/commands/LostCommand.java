// 파일 위치: src/main/java/com/yourname/goldcurrency/commands/LostCommand.java
package com.yourname.goldcurrency.commands;

import com.yourname.goldcurrency.LostItemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LostCommand implements CommandExecutor {
    private final LostItemManager lostItemManager;

    public LostCommand(LostItemManager lostItemManager) {
        this.lostItemManager = lostItemManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!lostItemManager.hasLostItems(player.getUniqueId())) {
            player.sendMessage("§c찾을 아이템이 없습니다.");
            return true;
        }

        // 아이템 수령 시도
        lostItemManager.tryRestoreItems(player);
        return true;
    }
}