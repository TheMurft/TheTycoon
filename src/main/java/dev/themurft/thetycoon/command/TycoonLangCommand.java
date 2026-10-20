package dev.themurft.thetycoon.command;

import dev.themurft.thetycoon.service.LanguageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TycoonLangCommand implements CommandExecutor, TabCompleter {
    private final LanguageService languages;

    public TycoonLangCommand(LanguageService languages) {
        this.languages = languages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            languages.send(sender, "language-list", "<languages>", String.join(", ", languages.availableLanguages()));
            languages.send(sender, "usage-lang");
            return true;
        }

        if (args[0].equalsIgnoreCase("global")) {
            if (!sender.hasPermission("thetycoon.lang.global")) {
                languages.send(sender, "no-permission");
                return true;
            }
            if (args.length < 2 || !languages.isAvailable(args[1])) {
                String requested = args.length < 2 ? "" : args[1];
                languages.send(sender, "unknown-language", "<language>", requested);
                return true;
            }
            languages.setGlobalLanguage(args[1]);
            languages.send(sender, "global-language-changed", "<language>", languages.displayName(args[1]));
            return true;
        }

        if (!(sender instanceof Player player)) {
            languages.send(sender, "usage-lang");
            return true;
        }
        if (!sender.hasPermission("thetycoon.lang")) {
            languages.send(sender, "no-permission");
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            languages.resetPlayerLanguage(player.getUniqueId());
            languages.send(player, "language-reset", "<language>", languages.displayName(languages.globalLanguage()));
            return true;
        }
        if (!languages.isAvailable(args[0])) {
            languages.send(sender, "unknown-language", "<language>", args[0]);
            return true;
        }
        languages.setPlayerLanguage(player.getUniqueId(), args[0]);
        languages.send(player, "language-changed", "<language>", languages.displayName(args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(languages.availableLanguages());
            if (sender instanceof Player) options.add("reset");
            if (sender.hasPermission("thetycoon.lang.global")) options.add("global");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("global")) {
            options.addAll(languages.availableLanguages());
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
