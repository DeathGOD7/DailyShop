package io.github.divios.dailyrandomshop.guis;

import io.github.divios.dailyrandomshop.conf_msg;
import io.github.divios.dailyrandomshop.utils.utils;
import io.github.divios.dailyrandomshop.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class confirmIH implements InventoryHolder, Listener {

    private final Player p;
    private final BiConsumer<Player, Boolean> bi;
    private final ItemStack item;
    private final String title;
    private final String confirmLore;
    private final String cancelLore;
    private final Consumer<Player> back;
    private boolean backFlag = true;
    private static final io.github.divios.dailyrandomshop.main main = io.github.divios.dailyrandomshop.main.getInstance();

    /**
     * @param p          Player to show the GUI
     * @param true_false Block of code to execute
     * @param title      Title of the GUI
     */

    public confirmIH(Player p,
                     BiConsumer<Player , Boolean> true_false,
                     Consumer<Player> back,
                     ItemStack item,
                     String title,
                     String confirmLore,
                     String cancelLore) {

        Bukkit.getPluginManager().registerEvents(this, main);
        this.p = p;
        this.back = back;
        this.item = item;
        bi = true_false;
        this.title = utils.formatString(title);
        this.confirmLore = confirmLore;
        this.cancelLore = cancelLore;
        p.openInventory(getInventory());
    }

    @Override
    public Inventory getInventory() {

        Inventory inv = Bukkit.createInventory(this, 27, title);

        ItemStack true_ = XMaterial.EMERALD_BLOCK.parseItem();
        utils.setDisplayName(true_, confirmLore);


        ItemStack false_ = XMaterial.REDSTONE_BLOCK.parseItem();
        utils.setDisplayName(false_, cancelLore);

        if (!utils.isEmpty(item))
            inv.setItem(4, item);
        inv.setItem(15, false_);
        inv.setItem(11, true_);

        return inv;
    }

    @EventHandler
    private void onClick(InventoryClickEvent e) {

        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);


        if (utils.isEmpty(e.getCurrentItem())) return;
        if (e.getSlot() != e.getRawSlot()) return;

        switch (e.getSlot()) {
            case 15:
                bi.accept(p, false);
                backFlag = false;
                break;

            case 11:
                backFlag = false;
                bi.accept(p, true);
                break;

            default:
                break;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        if(backFlag) utils.runTaskLater(() -> back.accept(p), 1L);
    }

}
