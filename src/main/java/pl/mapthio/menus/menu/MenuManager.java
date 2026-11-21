package pl.mapthio.menus.menu;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mapthio.menus.gui.MenuGui;
import pl.mapthio.menus.gui.MenuHolder;
import pl.mapthio.menus.util.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

/** Wczytuje, zapisuje menu (menus/*.yml) i rejestruje komendy otwierające menu. */
public final class MenuManager {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");

    private final JavaPlugin plugin;
    private final File dir;
    private final Map<String, Menu> menus = new TreeMap<>();
    private final Map<String, MenuCommand> commands = new HashMap<>();

    public MenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "menus");
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    // ---------------------------------------------------------------- ładowanie / zapis

    public void loadAll() {
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Nie udało się utworzyć folderu " + dir);
        }
        File[] files = dir.listFiles((d, fileName) -> fileName.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            String name = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
            if (!isValidName(name)) {
                plugin.getLogger().warning("Pomijam plik z niepoprawną nazwą menu: " + fileName);
                continue;
            }
            try {
                Menu menu = Menu.load(name, YamlConfiguration.loadConfiguration(file));
                menus.put(name, menu);
                if (menu.getOpenCommand() != null) {
                    String error = registerCommand(menu.getOpenCommand());
                    if (error != null) {
                        plugin.getLogger().warning("Menu '" + name + "': " + error);
                        menu.setOpenCommand(null);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Nie udało się wczytać menu " + fileName, ex);
            }
        }
        syncCommands();
    }

    public void save(Menu menu) {
        YamlConfiguration yaml = new YamlConfiguration();
        menu.save(yaml);
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                plugin.getLogger().warning("Nie udało się utworzyć folderu " + dir);
            }
            yaml.save(new File(dir, menu.getName() + ".yml"));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się zapisać menu " + menu.getName(), ex);
        }
    }

    public void saveAll() {
        menus.values().forEach(this::save);
    }

    /** Zamyka otwarte menu, wyrejestrowuje komendy i czyści pamięć (przed reloadem / wyłączeniem). */
    public void unloadAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                player.closeInventory();
            }
        }
        for (String label : new ArrayList<>(commands.keySet())) {
            unregisterCommand(label);
        }
        menus.clear();
        syncCommands();
    }

    // ---------------------------------------------------------------- menu

    public Menu get(String name) {
        return name == null ? null : menus.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Menu> all() {
        return menus.values();
    }

    public Menu create(String name, int rows) {
        Menu menu = new Menu(name, "&8" + name, rows);
        menus.put(name, menu);
        save(menu);
        return menu;
    }

    public void delete(Menu menu) {
        closeViewers(menu);
        if (menu.getOpenCommand() != null) {
            unregisterCommand(menu.getOpenCommand());
            syncCommands();
        }
        menus.remove(menu.getName());
        File file = new File(dir, menu.getName() + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Nie udało się usunąć pliku " + file);
        }
    }

    public void setRows(Menu menu, int rows) {
        closeViewers(menu);
        menu.setRows(rows);
        save(menu);
    }

    public void setTitle(Menu menu, String title) {
        closeViewers(menu);
        menu.setTitle(title);
        save(menu);
    }

    public void closeViewers(Menu menu) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder
                    && holder.menu() == menu) {
                player.closeInventory();
            }
        }
    }

    // ---------------------------------------------------------------- komenda otwierająca menu

    public Menu byCommand(String label) {
        for (Menu menu : menus.values()) {
            if (label.equalsIgnoreCase(menu.getOpenCommand())) {
                return menu;
            }
        }
        return null;
    }

    /**
     * Przypisuje do menu komendę otwierającą (np. "tryby" = /tryby).
     *
     * @param label nazwa komendy albo null, aby usunąć
     * @return komunikat błędu albo null, gdy się udało
     */
    public String setOpenCommand(Menu menu, String label) {
        if (label == null) {
            if (menu.getOpenCommand() != null) {
                unregisterCommand(menu.getOpenCommand());
            }
            menu.setOpenCommand(null);
            save(menu);
            syncCommands();
            return null;
        }

        String clean = label.toLowerCase(Locale.ROOT);
        if (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (!NAME.matcher(clean).matches()) {
            return "Niepoprawna nazwa komendy. Dozwolone: a-z, 0-9, _ i - (maks. 32 znaki).";
        }
        Menu other = byCommand(clean);
        if (other != null && other != menu) {
            return "Komenda /" + clean + " jest już przypisana do menu '" + other.getName() + "'.";
        }
        String error = registerCommand(clean);
        if (error != null) {
            return error;
        }
        if (menu.getOpenCommand() != null && !menu.getOpenCommand().equals(clean)) {
            unregisterCommand(menu.getOpenCommand());
        }
        menu.setOpenCommand(clean);
        save(menu);
        syncCommands();
        return null;
    }

    private String registerCommand(String label) {
        if (commands.containsKey(label)) {
            return null;
        }
        CommandMap map = Bukkit.getCommandMap();
        if (map.getCommand(label) != null) {
            return "Komenda /" + label + " już istnieje na serwerze - wybierz inną nazwę.";
        }
        MenuCommand command = new MenuCommand(label);
        map.register("mapthio", command);
        commands.put(label, command);
        return null;
    }

    private void unregisterCommand(String label) {
        MenuCommand command = commands.remove(label);
        if (command == null) {
            return;
        }
        CommandMap map = Bukkit.getCommandMap();
        command.unregister(map);
        map.getKnownCommands().values().removeIf(known -> known == command);
    }

    /** Odświeża listę komend (podpowiedzi) u graczy. */
    private void syncCommands() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    /** Dynamiczna komenda otwierająca konkretne menu. */
    private final class MenuCommand extends Command {

        private final String menuLabel;

        private MenuCommand(String menuLabel) {
            super(menuLabel, "Otwiera menu " + menuLabel, "/" + menuLabel, List.of());
            this.menuLabel = menuLabel;
            setPermission("mapthio.use");
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!testPermission(sender)) {
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.error("Tę komendę może użyć tylko gracz."));
                return true;
            }
            Menu menu = byCommand(menuLabel);
            if (menu == null) {
                player.sendMessage(Text.error("To menu już nie istnieje."));
                return true;
            }
            MenuGui.openView(player, menu);
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return List.of();
        }
    }
}
