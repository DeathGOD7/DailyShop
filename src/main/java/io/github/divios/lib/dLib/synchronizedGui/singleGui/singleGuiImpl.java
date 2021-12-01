package io.github.divios.lib.dLib.synchronizedGui.singleGui;

import io.github.divios.core_lib.events.Events;
import io.github.divios.core_lib.events.Subscription;
import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.dailyShop.events.searchStockEvent;
import io.github.divios.dailyShop.events.updateItemEvent;
import io.github.divios.dailyShop.lorestategy.loreStrategy;
import io.github.divios.dailyShop.lorestategy.shopItemsLore;
import io.github.divios.dailyShop.utils.PlaceholderAPIWrapper;
import io.github.divios.dailyShop.utils.Utils;
import io.github.divios.lib.dLib.dInventory;
import io.github.divios.lib.dLib.dShop;
import io.github.divios.lib.dLib.synchronizedGui.taskPool.updatePool;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Class that holds a {@link dInventory} for a unique player and
 * also its base.
 * <p>
 * Subscribes to the updatePool to update placeholders
 */

public class singleGuiImpl implements singleGui {

    private final Player p;
    private final dShop shop;
    private final dInventory base;
    private final dInventory own;
    private final Set<Subscription> events = new HashSet<>();

    protected singleGuiImpl(Player p, dShop shop, singleGui base) {
        this(p, shop, base.getInventory());
    }

    protected singleGuiImpl(Player p, dShop shop, dInventory base) {
        this.p = p;
        this.shop = shop;
        this.base = base;
        this.own = base.copy();

        if (p != null) {
            updateTask();
            updatePool.subscribe(this);
            this.own.openInventory(p);
        } else ready();
    }

    private void ready() {

        events.add(
                Events.subscribe(searchStockEvent.class)                // Respond to search events
                        .filter(o -> o.getShop().equals(shop))
                        .handler(o ->
                                own.getButtons().values().stream()
                                        .filter(dItem -> dItem.getUid().equals(o.getUuid()))
                                        .findFirst()
                                        .ifPresent(dItem -> {
                                            if (!dItem.hasStock()) o.respond(-1);
                                            else o.respond(dItem.getStock().get(o.getPlayer()));
                                        }))
        );

    }

    @Override
    public synchronized void updateItem(updateItemEvent o) {
        own.updateItem(p, o);
    }

    @Override
    public synchronized void updateTask() {

        loreStrategy strategy = new shopItemsLore();
        IntStream.range(0, own.getInventorySize())
                .filter(value -> !ItemUtils.isEmpty(own.getInventory().getItem(value)))
                .forEach(value -> {

                    try {
                        Inventory inv = own.getInventory();
                        ItemStack oldItem = base.getDailyItemsSlots().contains(value) ?
                                strategy.applyLore(base.getButtons().get(value).getItem().clone(), p)
                                : base.getInventory().getItem(value);
                        ItemBuilder newItem = ItemBuilder.of(oldItem.clone()).setLore(Collections.emptyList());

                        newItem = newItem.setName(PlaceholderAPIWrapper.setPlaceholders(p, ItemUtils.getName(oldItem)));

                        for (String s : ItemUtils.getLore(oldItem))
                            newItem = newItem.addLore(PlaceholderAPIWrapper.setPlaceholders(p, s));

                        inv.setItem(value, newItem);
                    } catch (Exception ignored) {
                    }

                });
    }

    @Override
    public synchronized void renovate() {
        own.restock(p);
    }

    @Override
    public Player getPlayer() {
        return p;
    }

    @Override
    public dInventory getBase() {
        return base;
    }

    @Override
    public dInventory getInventory() {
        return own;
    }

    @Override
    public dShop getShop() {
        return shop;
    }

    @Override
    public synchronized void destroy() {
        events.forEach(Subscription::unregister);
        own.destroy();
        updatePool.unsubscribe(this);
    }

    @Override
    public synchronized int hash() {
        return Arrays.stream(own.getInventory().getContents())
                .mapToInt(value -> Utils.isEmpty(value) ? 0 : value.hashCode())
                .sum();
    }

    @Override
    public synchronized singleGui clone() {
        return new singleGuiImpl(p, shop, base);
    }

}
