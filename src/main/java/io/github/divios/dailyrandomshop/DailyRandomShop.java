package io.github.divios.dailyrandomshop;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import io.github.divios.dailyrandomshop.GUIs.buyGui;
import io.github.divios.dailyrandomshop.GUIs.confirmGui;
import io.github.divios.dailyrandomshop.GUIs.sellGui;
import io.github.divios.dailyrandomshop.Listeners.buyGuiListener;
import io.github.divios.dailyrandomshop.Listeners.confirmGuiListener;
import io.github.divios.dailyrandomshop.Listeners.sellGuiListener;
import io.github.divios.dailyrandomshop.Utils.ConfigUtils;
import io.github.divios.dailyrandomshop.Utils.Utils;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DailyRandomShop extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    public Economy econ = null;
    public Permission perms = null;
    public Chat chat = null;
    public Map<String, Double[]> listMaterials;
    public buyGui BuyGui;
    public sellGui SellGui;
    public confirmGui ConfirmGui;
    public Utils utils;
    public Config config;
    public int time = 0;

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));

    }

    @Override
    public void onEnable() {

        int pluginId = 9721;
        Metrics metrics = new Metrics(this, pluginId);

        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            ConfigUtils.reloadConfig(this, false);
        } catch (IOException e) {
            log.severe("Something went wrong with the .yml files");
            getServer().getPluginManager().disablePlugin(this);
        }

        utils = new Utils(this);
        BuyGui = new buyGui(this);
        SellGui = new sellGui(this);
        ConfirmGui = new confirmGui(this);

        buyGuiListener buyguiListener = new buyGuiListener(this);
        sellGuiListener sellguiListener = new sellGuiListener(this, SellGui.getDailyItemsSlots());
        confirmGuiListener confirmguiListener = new confirmGuiListener(this);
        getServer().getPluginManager().registerEvents(buyguiListener, this);
        getServer().getPluginManager().registerEvents(sellguiListener, this);
        getServer().getPluginManager().registerEvents(confirmguiListener, this);

        getCommand("rdShop").setExecutor(new Commands(this));
        getCommand("rdShop").setTabCompleter(new TabComplete());
        //setupPermissions();
        //setupChat();

    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }


}
