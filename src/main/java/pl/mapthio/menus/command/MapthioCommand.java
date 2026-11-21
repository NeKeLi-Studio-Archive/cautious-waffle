package pl.mapthio.menus.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import pl.mapthio.menus.MapthioMenus;
import pl.mapthio.menus.gui.MenuGui;
import pl.mapthio.menus.menu.Menu;
import pl.mapthio.menus.menu.MenuManager;
import pl.mapthio.menus.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Komenda /mapthio - tworzenie i zarządzanie menu. */
public final class MapthioCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "create", "edit", "open", "list", "delete", "title", "rows", "command", "reload");
    private static final List<String> NEED_MENU = List.of(
            "edit", "open", "delete", "title", "rows", "command");

    private final MapthioMenus plugin;

    public MapthioCommand(MapthioMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mapthio.admin")) {
            sender.sendMessage(Text.error("Nie masz uprawnień (mapthio.admin)."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender, label);
            return true;
        }

        plugin.chat().cancelIfPlayer(sender);
        MenuManager menus = plugin.menus();

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 2) {
                    sender.sendMessage(Text.error("Użycie: /" + label + " create <nazwa> [rzędy 1-6]"));
                    return true;
                }
                String name = args[1].toLowerCase(Locale.ROOT);
                if (!MenuManager.isValidName(name)) {
                    sender.sendMessage(Text.error("Nazwa menu: a-z, 0-9, _ i - (maks. 32 znaki)."));
                    return true;
                }
                if (menus.get(name) != null) {
                    sender.sendMessage(Text.error("Menu '" + name + "' już istnieje."));
                    return true;
                }
                int rows = 3;
                if (args.length >= 3) {
                    Integer parsed = parseRows(args[2]);
                    if (parsed == null) {
                        sender.sendMessage(Text.error("Liczba rzędów musi być od 1 do 6."));
                        return true;
                    }
                    rows = parsed;
                }
                Menu menu = menus.create(name, rows);
                sender.sendMessage(Text.ok("Utworzono menu '" + name + "' (" + rows + " rzędy)."));
                if (sender instanceof Player player) {
                    sender.sendMessage(Text.info("Kliknij dowolny slot, aby go edytować."));
                    MenuGui.openEdit(player, menu);
                }
            }
            case "edit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.error("Tę komendę może użyć tylko gracz."));
                    return true;
                }
                Menu menu = requireMenu(sender, args, label);
                if (menu == null) {
                    return true;
                }
                sender.sendMessage(Text.info("Kliknij slot, aby go edytować. Trzymając przedmiot na kursorze, "
                        + "kliknij slot, aby od razu ustawić blok."));
                MenuGui.openEdit(player, menu);
            }
            case "open" -> {
                Menu menu = requireMenu(sender, args, label);
                if (menu == null) {
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage(Text.error("Gracz " + args[2] + " nie jest online."));
                        return true;
                    }
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage(Text.error("Użycie z konsoli: /" + label + " open <menu> <gracz>"));
                    return true;
                }
                MenuGui.openView(target, menu);
            }
            case "list" -> {
                if (menus.all().isEmpty()) {
                    sender.sendMessage(Text.info("Brak menu. Utwórz pierwsze: /" + label + " create <nazwa>"));
                    return true;
                }
                sender.sendMessage(Text.info("Menu (" + menus.all().size() + "):"));
                for (Menu menu : menus.all()) {
                    String cmd = menu.getOpenCommand() == null ? "brak komendy" : "/" + menu.getOpenCommand();
                    sender.sendMessage(Component.text(" - " + menu.getName() + " ", NamedTextColor.YELLOW)
                            .append(Component.text("(" + menu.getRows() + " rzędy, " + menu.slots().size()
                                    + " slotów, " + cmd + ")", NamedTextColor.GRAY)));
                }
            }
            case "delete" -> {
                Menu menu = requireMenu(sender, args, label);
                if (menu == null) {
                    return true;
                }
                menus.delete(menu);
                sender.sendMessage(Text.ok("Usunięto menu '" + menu.getName() + "'."));
            }
            case "title" -> {
                Menu menu = requireMenu(sender, args, label);
                if (menu == null) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Text.error("Użycie: /" + label + " title <menu> <tytuł z kolorami &>"));
                    return true;
                }
                String title = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                menus.setTitle(menu, title);
                sender.sendMessage(Text.ok("Ustawiono tytuł menu."));
            }
            case "rows" -> {
                Menu menu = requireMenu(sender, args, label);
                if (menu == null) {
                    return true;
                }
                Integer rows = args.length >= 3 ? parseRows(args[2]) : null;
                if (rows == null) {
                    sender.sendMessage(Text.error("Użycie: /" + label + " rows <menu> <1-6>"));
                    return true;
                }
                menus.setRows(menu, rows);
                sender.sendMessage(Text.ok("Ustawiono " + rows + " rzędy. Sloty poza menu zostały usunięte."));
            }
            case "command" -> {
                Menu menu = requireMenu(sender, args, label);
                if (menu == null) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Text.error("Użycie: /" + label + " command <menu> <komenda|none>"));
                    return true;
                }
                boolean remove = args[2].equalsIgnoreCase("none") || args[2].equalsIgnoreCase("brak");
                String error = menus.setOpenCommand(menu, remove ? null : args[2]);
                if (error != null) {
                    sender.sendMessage(Text.error(error));
                } else if (remove) {
                    sender.sendMessage(Text.ok("Usunięto komendę otwierającą menu."));
                } else {
                    sender.sendMessage(Text.ok("Menu '" + menu.getName() + "' otwiera teraz komenda /"
                            + menu.getOpenCommand() + "."));
                }
            }
            case "reload" -> {
                menus.unloadAll();
                menus.loadAll();
                sender.sendMessage(Text.ok("Wczytano ponownie " + menus.all().size() + " menu."));
            }
            default -> help(sender, label);
        }
        return true;
    }

    private Menu requireMenu(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sender.sendMessage(Text.error("Użycie: /" + label + " " + args[0] + " <menu> ..."));
            return null;
        }
        Menu menu = plugin.menus().get(args[1]);
        if (menu == null) {
            sender.sendMessage(Text.error("Nie ma menu '" + args[1] + "'. Lista: /" + label + " list"));
        }
        return menu;
    }

    private static Integer parseRows(String raw) {
        try {
            int rows = Integer.parseInt(raw);
            return rows >= 1 && rows <= 6 ? rows : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== Mapthio Menus ===", NamedTextColor.GOLD));
        String[][] lines = {
                {"create <nazwa> [rzędy]", "tworzy menu i otwiera edycję"},
                {"edit <menu>", "edycja: kliknij slot, aby zmienić blok/nazwę/enchant/komendę"},
                {"open <menu> [gracz]", "otwiera menu jak graczowi"},
                {"command <menu> <komenda|none>", "komenda otwierająca menu, np. tryby = /tryby"},
                {"title <menu> <tytuł>", "tytuł menu (kolory &, hex &#rrggbb)"},
                {"rows <menu> <1-6>", "liczba rzędów"},
                {"list", "lista menu"},
                {"delete <menu>", "usuwa menu"},
                {"reload", "wczytuje menu z plików"},
        };
        for (String[] line : lines) {
            sender.sendMessage(Component.text("/" + label + " " + line[0], NamedTextColor.YELLOW)
                    .append(Component.text(" - " + line[1], NamedTextColor.GRAY)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mapthio.admin")) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && NEED_MENU.contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Menu menu : plugin.menus().all()) {
                if (menu.getName().startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(menu.getName());
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "rows" -> result.addAll(List.of("1", "2", "3", "4", "5", "6"));
                case "command" -> result.add("none");
                case "open" -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                            result.add(player.getName());
                        }
                    }
                }
                default -> {
                    // brak podpowiedzi
                }
            }
        }
        return result;
    }
}
