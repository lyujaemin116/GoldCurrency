// 파일 위치: src/main/java/com/yourname/goldcurrency/TabListManager.java
package com.yourname.goldcurrency;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TabListManager {
    private final JavaPlugin plugin;
    private final MoneyManager moneyManager;
    private final Scoreboard scoreboard;

    public TabListManager(JavaPlugin plugin, MoneyManager moneyManager) {
        this.plugin = plugin;
        this.moneyManager = moneyManager;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateTabList, 20L, 20L); // 1초마다 갱신
    }

    private void updateTabList() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            long balance = moneyManager.getBalance(player.getUniqueId());

            // 1. 탭 리스트 이름 앞에 돈 표시
            player.playerListName(Component.text("§e" + balance + "⛁ " + player.getName()));

            // 2. 정렬 로직 (스코어보드 팀 이름 이용)
            // 내림차순 정렬을 위해 돈이 많을수록 알파벳 순서가 빠른 팀 이름 생성
            // MaxLong에서 현재 돈을 빼서, 작은 숫자가 되게 함 (문자열 정렬 시 앞쪽에 옴)
            // 자리수를 맞춰야 정상 정렬됨 (예: 20자리)
            String priority = String.format("%020d", Long.MAX_VALUE - balance);

            // 팀 이름은 고유해야 하므로 뒤에 플레이어 이름 붙임
            String teamName = priority + player.getName();
            // 팀 이름 길이 제한 안전장치 (오래된 버전 대비, 1.21은 넉넉함)
            if (teamName.length() > 64) teamName = teamName.substring(0, 64);

            Team team = scoreboard.getTeam(player.getName()); // 플레이어 이름으로 팀 관리
            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
            }

            // 실제 정렬 키가 되는 팀 이름 변경은 불가능하므로,
            // 매번 새로운 팀을 만들거나 관리하는 방식 대신,
            // 여기서는 간단하게 "순위용 팀"에 플레이어를 넣는 방식을 사용해야 합니다.
            // 하지만 팀을 계속 생성/삭제하는 것은 비효율적이므로,
            // 탭 리스트 정렬을 위한 가상 팀 이름을 생성합니다.

            // 개선된 정렬 로직:
            // 플레이어마다 하나의 팀을 할당하고, 그 팀의 이름을 업데이트하는 것은 불가능합니다.
            // 따라서, 기존 팀을 해체하고 매번 갱신하거나, 이름 자체를 정렬 키로 써야 합니다.
            // 가장 깔끔한 방법:

            String sortTeamName = "R_" + priority;
            if (sortTeamName.length() > 60) sortTeamName = sortTeamName.substring(0, 60);

            Team rankTeam = scoreboard.getTeam(sortTeamName);
            if (rankTeam == null) {
                rankTeam = scoreboard.registerNewTeam(sortTeamName);
            }

            // 플레이어가 다른 팀에 있다면 옮기기
            if (!rankTeam.hasEntry(player.getName())) {
                rankTeam.addEntry(player.getName());
            }
        }
    }
}