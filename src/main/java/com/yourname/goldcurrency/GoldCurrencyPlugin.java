package com.yourname.goldcurrency;

import com.yourname.goldcurrency.commands.LostCommand;
import com.yourname.goldcurrency.commands.PayCommand;
import com.yourname.goldcurrency.commands.ShopCommand;
import com.yourname.goldcurrency.commands.WithdrawCommand;
import com.yourname.goldcurrency.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class GoldCurrencyPlugin extends JavaPlugin {

    private MoneyManager moneyManager;
    private TabListManager tabListManager;
    private CurrencyItemManager itemManager;
    private ShopManager shopManager;
    private LostItemManager lostItemManager;

    @Override
    public void onEnable() {
        // 1. 매니저(데이터 관리자) 초기화
        this.moneyManager = new MoneyManager(this);
        this.itemManager = new CurrencyItemManager(this);
        this.tabListManager = new TabListManager(this, moneyManager);
        this.shopManager = new ShopManager(this);
        this.lostItemManager = new LostItemManager(this);

        // 2. 명령어 등록
        if (getCommand("pay") != null) getCommand("pay").setExecutor(new PayCommand(moneyManager));
        if (getCommand("withdraw") != null) getCommand("withdraw").setExecutor(new WithdrawCommand(moneyManager, itemManager));
        if (getCommand("shop") != null) getCommand("shop").setExecutor(new ShopCommand(shopManager));
        if (getCommand("lost") != null) getCommand("lost").setExecutor(new LostCommand(lostItemManager));

        // 3. 이벤트 리스너 등록
        // [수정됨] MiningListener에 this 전달 (설치한 블록 구분을 위해 필요)
        getServer().getPluginManager().registerEvents(new MiningListener(this, moneyManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(moneyManager, itemManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(moneyManager, itemManager, lostItemManager), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, moneyManager), this);
        // [필수] 화폐 보호 리스너 등록 (꾸러미/액자 등 악용 방지)
        getServer().getPluginManager().registerEvents(new CurrencyProtectionListener(itemManager), this);

        // 4. 탭 리스트 업데이트 시작
        tabListManager.startUpdateTask();

        getLogger().info("GoldCurrency 플러그인이 성공적으로 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        // 서버 종료 시 데이터 저장
        if (moneyManager != null) moneyManager.saveData();
        if (shopManager != null) shopManager.saveShop();
        if (lostItemManager != null) lostItemManager.saveData();

        getLogger().info("GoldCurrency 플러그인이 비활성화되었습니다.");
    }
}