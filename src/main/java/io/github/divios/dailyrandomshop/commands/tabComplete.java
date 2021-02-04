package io.github.divios.dailyrandomshop.commands;

import io.github.divios.dailyrandomshop.commands.cmds.allCmds;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class tabComplete implements TabCompleter {

    private static final io.github.divios.dailyrandomshop.main main = io.github.divios.dailyrandomshop.main.getInstance();
    private static tabComplete instance = null;

    private tabComplete() { }

    public static tabComplete getInstance() {
        if(instance == null) {
            instance = new tabComplete();
            main.getCommand("rdshop").setTabCompleter(instance);
        }
        return instance;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> commandsList = Arrays.stream(allCmds.values())
                .map(Enum::name).collect(Collectors.toList());

        if (args.length == 0) return commandsList;
        if (args.length >= 1) {
            return commandsList.stream().filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
