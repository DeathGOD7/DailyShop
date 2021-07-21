package io.github.divios.dailyShop.lorestategy;

import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.core_lib.misc.Msg;
import io.github.divios.core_lib.misc.XSymbols;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dPrice;
import io.github.divios.lib.dLib.dShop;
import org.bukkit.inventory.ItemStack;

public class shopItemsLore implements loreStrategy {
    private static final DailyShop plugin = DailyShop.getInstance();
    private final dShop.dShopT type;

    public shopItemsLore(dShop.dShopT type) {
        this.type = type;
    }

    @Override
    public void setLore(ItemStack item) {
        ItemUtils.translateAllItemData(applyLore(item), item);
    }

    @Override
    public ItemStack applyLore(ItemStack item) {
        dItem aux = dItem.of(item);

        ItemBuilder newItem = new ItemBuilder(item)
                .addLore("")
                .addLore(Msg.singletonMsg(
                        plugin.configM.getLangYml().DAILY_ITEMS_BUY_PRICE)
                        .add("\\{buyPrice}",
                                (aux.getBuyPrice().isPresent()
                                        && aux.getBuyPrice().get().getPrice() != -1) ? "" +
                                        aux.getBuyPrice().orElse(new dPrice(-1)).getPrice()
                                        : FormatUtils.color("&c&l" + XSymbols.TIMES_3.parseSymbol())).build())

                .addLore(Msg.singletonMsg(
                        plugin.configM.getLangYml().DAILY_ITEMS_SELL_PRICE)
                        .add("\\{sellPrice}", (aux.getSellPrice().isPresent()
                                && aux.getSellPrice().get().getPrice() != -1) ? "" +
                                aux.getSellPrice().orElse(new dPrice(-1)).getPrice()
                                : FormatUtils.color("&c&l" + XSymbols.TIMES_3.parseSymbol())).build())

                .addLore("");

        if (aux.getStock().isPresent()) {
            newItem = newItem.addLore(plugin.configM.getLangYml().DAILY_ITEMS_STOCK +
                    (aux.getStock().get() == -1 ?
                            FormatUtils.color("&c" + XSymbols.TIMES_3.parseSymbol()):
                            aux.getStock().get()));
        }

        newItem = newItem.addLore(Msg.singletonMsg(plugin.configM.getLangYml().DAILY_ITEMS_CURRENCY)
                .add("\\{currency}", aux.getEconomy().getName()).build());

        newItem = newItem.addLore(Msg.singletonMsg(plugin.configM.getLangYml().DAILY_ITEMS_RARITY)
                .add("\\{rarity}", aux.getRarity().toString()).build());

        return newItem;
    }

    @Override
    public void removeLore(ItemStack item) {

    }

    @Override
    public void update(ItemStack item) {

    }
}
