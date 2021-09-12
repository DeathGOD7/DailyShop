package io.github.divios.lib.storage.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.divios.core_lib.utils.Log;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.utils.FileUtils;
import io.github.divios.lib.dLib.dInventory;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dShop;
import io.github.divios.lib.managers.shopsManager;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ParserApi {


    public static void serialize(dShop shop) {

        Map<UUID, dItemState> itemsCollect = new LinkedHashMap<>();

        shop.getItems().forEach(dItem -> itemsCollect.put(dItem.getUid(), dItemState.of(dItem.getItem())));

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(new jsonEntry(shop.getName(), itemsCollect, shop));
        File data = new File(DailyShop.getInstance().getDataFolder() + File.separator + "parser", shop.getName() + ".yml");
        if (!data.exists()) {
            FileUtils.createFile(data);
        }

        Map map = new Gson().fromJson(json, Map.class);
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setAllowUnicode(true);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        try (FileWriter fw = new FileWriter(data)) {
            new Yaml(options).dump(map, fw);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.info("Converted all items correctly of shop " + shop.getName() + " into the file " + data.getPath());

    }

    public static void deserialize(String fileName) {

        Set<dItem> newItems = new LinkedHashSet<>();
        String json = null;

        File data = new File(DailyShop.getInstance().getDataFolder() + File.separator + "parser", fileName + ".yml");
        if (!data.exists()) {
            Log.info("That shop doesn't exist on the parser folder");
            return;
        }

        jsonEntry wrapper = null;
        Object o = null;

        try (FileInputStream fis = new FileInputStream(data)) {         // Load Yaml and parse to a Map
            o = new Yaml().load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        json = new Gson().toJson(o, LinkedHashMap.class);               // Load the map into json
        wrapper = new Gson().fromJson(json, jsonEntry.class);           // Parse the json to jsonEntry

        for (Map.Entry<UUID, dItemState> entry : wrapper.getItems().entrySet()) {       // Parser the ItemStates to dItems
            try {
                newItems.add(entry.getValue().parseItem(entry.getKey()));
            } catch (Exception e) {
                Log.info("There was a problem parsing the item of uuid " + entry.getKey().toString());
            }
        }

        dShop shop = shopsManager.getInstance().getShop(wrapper.id).orElse(null);   // Get the shop, if it doesn't exist create it
        if (shop == null) {
            try {
                shopsManager.getInstance().createShop(wrapper.id, dShop.dShopT.buy).get();  // Wait for the shop to be created
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            shop = shopsManager.getInstance().getShop(wrapper.id).get();
        }

        wrapper.getShopState().apply(shop);         // Get the shop default Gui and display

        boolean result = true;                      // Check if all items are similar
        for (dItem dItem : newItems) {
            dItem shopItem = shop.getItem(dItem.getUid()).orElse(null);
            if (shopItem == null) continue;
            result &= dItem.getRawItem().isSimilar(shopItem.getRawItem());
        }
        Log.info("The result of the comparison is: " + result);

        shop.setItems(newItems);                    // Set all the items
        shop.getGuis().reStock();                   // Restock shop
        Log.info("Parsed all items correctly of shop " + shop.getName());

    }


    private static final class jsonEntry {

        private final String id;
        private final dShopState shop;
        private final Map<UUID, dItemState> items;

        protected jsonEntry(String id, Map<UUID, dItemState> items, dShop shop) {
            this.id = id;
            dInventory defaultInv = shop.getGuis().getDefault();
            this.shop = new dShopState(
                    defaultInv.getTitle(),
                    defaultInv.getSize(),
                    defaultInv.getButtons().values().stream()
                            .filter(dItem -> !defaultInv.getOpenSlots().contains(dItem.getSlot()))      // Filter only buttons, not daily items
                            .collect(Collectors.toList())
            );
            this.items = items;
        }

        public String getId() {
            return id;
        }

        public dShopState getShopState() {
            return shop;
        }

        public Map<UUID, dItemState> getItems() {
            return items;
        }

    }


}