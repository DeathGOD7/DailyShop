package io.github.divios.lib.managers;

import io.github.divios.core_lib.utils.Log;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dShop;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

public class WrappedShop extends dShop {

    public static dShop wrap(dShop shop) {
        if (shop instanceof WrappedShop) return shop;
        return new WrappedShop(shop);
    }

    protected WrappedShop(String name) {
        super(name);
    }

    protected WrappedShop(String name, dShopT type) {
        super(name, type);
    }

    protected WrappedShop(String name, dShopT type, String base64, Timestamp timestamp, int timer) {
        super(name, type, base64, timestamp, timer);
    }

    protected WrappedShop(String name, dShopT type, String base64, Timestamp timestamp, int timer, Set<dItem> items) {
        super(name, type, base64, timestamp, timer, items);
    }

    protected WrappedShop(dShop fromShop) {
        this(fromShop.getName(), dShopT.buy, fromShop.getGuis().getDefault().toBase64(), fromShop.getTimestamp(), fromShop.getTimer(), new HashSet<>(fromShop.getItems()));
    }

    @Override
    public void rename(String s) {
        dManager.renameShopAsync(name, s.toLowerCase());
        super.rename(s);
    }

    @Override
    public synchronized void reStock() {
        super.reStock();
        dManager.updateTimeStampAsync(this.name, this.timestamp);
        dManager.updateGuiAsync(this.name, this.guis);
    }

    @Override
    public synchronized void addItem(dItem item) {
        super.addItem(item);
        super.dManager.addItemAsync(this.name, item);
    }

    @Override
    public synchronized void updateItem(UUID uid, dItem newItem) {
        super.updateItem(uid, newItem);
        dManager.updateItemAsync(getName(), newItem);
    }

    @Override
    public synchronized boolean removeItem(UUID uid) {
        if (!super.removeItem(uid)) return false;
        dManager.deleteItemAsync(this.name, uid);
        return true;
    }

    @Override
    public synchronized void setTimer(int timer) {
        super.setTimer(timer);
        dManager.updateTimerAsync(this.name, this.timer);
    }

}
