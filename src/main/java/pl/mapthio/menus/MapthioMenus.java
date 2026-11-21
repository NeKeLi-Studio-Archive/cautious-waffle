package pl.mapthio.menus;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mapthio.menus.command.MapthioCommand;
import pl.mapthio.menus.gui.MenuListener;
import pl.mapthio.menus.menu.MenuManager;
import pl.mapthio.menus.util.ChatInputManager;

/** Mapthio Menus - twórz menu/GUI bezpośrednio w grze. */
public final class MapthioMenus extends JavaPlugin {

    private MenuManager menuManager;
    private ChatInputManager chatInput;

    @Override
    public void onEnable() {
        menuManager = new MenuManager(this);
        chatInput = new ChatInputManager(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(chatInput, this);

        PluginCommand command = getCommand("mapthio");
        if (command != null) {
            MapthioCommand executor = new MapthioCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // Menu ładujemy po starcie serwera, żeby komendy innych pluginów były już zarejestrowane.
        getServer().getScheduler().runTask(this, () -> {
            menuManager.loadAll();
            getLogger().info("Wczytano menu: " + menuManager.all().size());
        });
    }

    @Override
    public void onDisable() {
        if (menuManager != null) {
            menuManager.saveAll();
            menuManager.unloadAll();
        }
    }

    public MenuManager menus() {
        return menuManager;
    }

    public ChatInputManager chat() {
        return chatInput;
    }
}
