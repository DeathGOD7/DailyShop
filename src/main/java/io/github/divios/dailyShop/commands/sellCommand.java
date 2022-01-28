package io.github.divios.dailyShop.commands;

import com.cryptomorin.xseries.XMaterial;
import io.github.divios.core_lib.events.Events;
import io.github.divios.core_lib.events.Subscription;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.core_lib.scheduler.Schedulers;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.economies.Economy;
import io.github.divios.dailyShop.files.Messages;
import io.github.divios.dailyShop.utils.DebugLog;
import io.github.divios.dailyShop.utils.PrettyPrice;
import io.github.divios.dailyShop.utils.Timer;
import io.github.divios.dailyShop.utils.Utils;
import io.github.divios.jcommands.JCommand;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dShop;
import io.github.divios.lib.dLib.dTransaction.SingleTransaction;
import io.github.divios.lib.dLib.dTransaction.Transactions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.IntStream;

public class sellCommand {

    public JCommand getCommand() {
        return JCommand.create("sell")
                .assertPermission("DailyRandomShop.sell")
                .assertUsage("/ds sell")
                .withSubcommands(getSellAllCommand(), getSellHandCommand(), getSellGuiCommand());
    }

    private JCommand getSellHandCommand() {
        return new JCommand("hand")
                .assertPermission("DailyRandomShop.sell.hand")
                .assertUsage(FormatUtils.color("&8- &6/rdshop open [shop] [player] &8- &7Opens a gui for yourself or for the given player"))
                .executesPlayer((player, valueMap) -> {
                    ItemStack itemToSell = player.getItemInHand();

                    ItemEntry entry;
                    if (ItemUtils.isEmpty(itemToSell) || (entry = searchItem(itemToSell)) == null
                            || entry.getItem().getSellPrice() <= 0) {
                        Messages.MSG_INVALID_SELL.send(player);
                        return;
                    }

                    Transactions.Custom()
                            .withType(SingleTransaction.Type.SELL)
                            .withPlayer(player)
                            .withShop(entry.getShop())
                            .withItem(entry.getItem())
                            .withAmount(itemToSell.getAmount())
                            .execute();
                });
    }

    private JCommand getSellAllCommand() {
        return new JCommand("all")
                .assertPermission("DailyRandomShop.sell.all")
                .assertUsage(FormatUtils.color("&8- &6/rdshop open [shop] [player] &8- &7Opens a gui for yourself or for the given player"))
                .executesPlayer((player, valueMap) -> {

                    Utils.sendRawMsg(player, "&7+-----------------------------+");
                    for (ItemStack itemToSell : Arrays.copyOf(player.getInventory().getContents(), 36)) {
                        ItemEntry entry;
                        if (ItemUtils.isEmpty(itemToSell) || (entry = searchItem(itemToSell)) == null
                                || entry.getItem().getSellPrice() <= 0) continue;

                        Transactions.Custom()
                                .withType(SingleTransaction.Type.SELL)
                                .withPlayer(player)
                                .withShop(entry.getShop())
                                .withItem(entry.getItem())
                                .withAmount(itemToSell.getAmount())
                                .execute();
                    }
                    Utils.sendRawMsg(player, "&7+-----------------------------+");

                });
    }

    private JCommand getSellGuiCommand() {
        return new JCommand("gui")
                .assertPermission("DailyRandomShop.sell.gui")
                .assertUsage(FormatUtils.color("&8- &6/rdshop open [shop] [player] &8- &7Opens a gui for yourself or for the given player"))
                .executesPlayer((player, valueMap) -> new dragAndSellGui(player));
    }

    private static ItemEntry searchItem(ItemStack itemToSearch) {
        ItemEntry entry = null;
        Timer timer = Timer.create();

        shopLoop:
        for (dShop shop : DailyShop.get().getShopsManager().getShops()) {
            for (dItem shopItem : shop.getItems()) {
                if (shopItem.getItem().isSimilar(itemToSearch)) {
                    entry = ItemEntry.from(shop, shopItem);
                    break shopLoop;
                }
            }
        }

        timer.stop();
        DebugLog.info("Time elapsed to searchItem: " + timer.getTime() + " ms");
        return entry;
    }

    private static final class ItemEntry {

        public static ItemEntry from(dShop shop, dItem item) {
            return new ItemEntry(shop, item);
        }

        private final dShop shop;
        private final dItem item;

        public ItemEntry(dShop shop, dItem item) {
            this.shop = shop;
            this.item = item;
        }

        public dShop getShop() {
            return shop;
        }

        public dItem getItem() {
            return item;
        }
    }

    public static class dragAndSellGui {

        private final Player p;
        private final Inventory gui;
        private final Set<Subscription> listeners;

        private dragAndSellGui(Player p) {
            this.p = p;
            this.gui = Bukkit.createInventory(null, 36, "");
            this.listeners = new HashSet<>();

            fillGuiWithPanels();
            updateDoneButton();

            createListeners();
            p.openInventory(gui);
        }

        private void fillGuiWithPanels() {
            ItemStack fillItem = ItemUtils.setName(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(), "&7");
            IntStream.range(27, 36).forEach(index -> gui.setItem(index, fillItem));
        }

        private void updateDoneButton() {
            ItemStack doneButton = ItemUtils.setName(XMaterial.GREEN_STAINED_GLASS_PANE.parseItem(),
                    "Total Price: ");

            List<String> lore = new ArrayList<>();
            getItemPrices().forEach((economy, aDouble) ->
                    lore.add("&7" + PrettyPrice.pretty(aDouble) + " " + economy.getName()));

            doneButton = ItemUtils.setLore(doneButton, lore);
            gui.setItem(31, doneButton);
        }

        private Map<Economy, Double> getItemPrices() {
            Map<Economy, Double> pricesMap = new HashMap<>();

            IntStream.range(0, gui.getSize()).forEach(index -> {
                ItemStack item = gui.getItem(index);
                ItemEntry entry;
                if (ItemUtils.isEmpty(item) || (entry = searchItem(item)) == null) return;

                double basePrice = entry.getItem().getPlayerSellPrice(p, entry.getShop());
                double finalPrice = basePrice * item.getAmount();

                pricesMap.compute(entry.getItem().getEcon(), (economy, aDouble) ->
                        aDouble == null ? finalPrice : aDouble + finalPrice);
            });

            return pricesMap;
        }

        private void createListeners() {
            listeners.add(
                    Events.subscribe(InventoryClickEvent.class)
                            .filter(e -> e.getInventory().equals(gui))
                            .handler(e -> {
                                if (e.getSlot() != e.getRawSlot())
                                    buttonGuiAction(e);
                                else
                                    upperGuiAction(e);
                            })
            );

            listeners.add(
                    Events.subscribe(InventoryCloseEvent.class)
                            .filter(e -> e.getInventory().equals(gui))
                            .handler(e -> {
                                Iterator<Subscription> iterator = listeners.iterator();
                                while (iterator.hasNext()) {
                                    iterator.next().unregister();
                                    iterator.remove();
                                }
                                givePlayerItemsBack();
                            })
            );
        }

        private void buttonGuiAction(InventoryClickEvent e) {
            if (!ItemUtils.isEmpty(e.getCurrentItem())
                    && getItemPrice(e.getCurrentItem()) <= 0) {
                e.setCancelled(true);
                Messages.MSG_INVALID_SELL.send(p);
            }
            if (e.isLeftClick() && e.isShiftClick())
                Schedulers.sync().run(this::updateDoneButton);
        }

        private void upperGuiAction(InventoryClickEvent e) {
            if (e.getSlot() >= 27 && e.getSlot() < 36) {
                e.setCancelled(true);
                if (e.getSlot() == 31) {
                    startTransaction();
                }
            }

            if (e.getSlot() >= 0 && e.getSlot() < 27) {
                Schedulers.sync().run(this::updateDoneButton);
            }
        }

        /**
         * Searches the sell price of the item. If <=0, means it cannot be sold
         */
        private double getItemPrice(ItemStack itemToLook) {
            ItemEntry entry = searchItem(itemToLook);
            return entry == null
                    ? 0
                    : entry.getItem().getPlayerSellPrice(p, entry.getShop());
        }


        private void startTransaction() {
            IntStream.range(0, 27).forEach(index -> {
                ItemStack itemToSell = gui.getItem(index);
                if (ItemUtils.isEmpty(itemToSell)) return;

                ItemEntry entry = searchItem(itemToSell);
                Utils.sendRawMsg(p, "&7+-----------------------------+");
                Transactions.Custom()
                        .withType(SingleTransaction.Type.SELL)
                        .withPlayer(p)
                        .withShop(entry.getShop())
                        .withItem(entry.getItem())
                        .withAmount(itemToSell.getAmount())
                        //.withInventoryAction(false)
                        .withOnFail((dItem, transactionError) -> {})
                        .execute();
                Utils.sendRawMsg(p, "&7+-----------------------------+");

            });
        }

        private void givePlayerItemsBack() {
            IntStream.range(0, 27)
                    .mapToObj(gui::getItem)
                    .filter(Objects::nonNull)
                    .forEach(itemStack -> ItemUtils.give(p, itemStack));
        }

    }

}
