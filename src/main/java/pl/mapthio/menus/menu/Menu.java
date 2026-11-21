package pl.mapthio.menus.menu;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.TreeMap;

/** Jedno menu: tytuł, liczba rzędów, komenda otwierająca i sloty. */
public final class Menu {

    private final String name;
    private String title;
    private int rows;
    private String openCommand;
    private final Map<Integer, SlotData> slots = new TreeMap<>();

    public Menu(String name, String title, int rows) {
        this.name = name;
        this.title = title;
        this.rows = rows;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getRows() {
        return rows;
    }

    public int size() {
        return rows * 9;
    }

    /** Zmienia liczbę rzędów i usuwa sloty, które wypadły poza menu. */
    public void setRows(int rows) {
        this.rows = rows;
        int size = rows * 9;
        slots.keySet().removeIf(index -> index >= size);
    }

    public String getOpenCommand() {
        return openCommand;
    }

    public void setOpenCommand(String openCommand) {
        this.openCommand = openCommand;
    }

    public Map<Integer, SlotData> slots() {
        return slots;
    }

    public SlotData getSlot(int index) {
        return slots.get(index);
    }

    public SlotData getOrCreate(int index) {
        return slots.computeIfAbsent(index, i -> new SlotData());
    }

    public void removeSlot(int index) {
        slots.remove(index);
    }

    public void save(YamlConfiguration yaml) {
        yaml.set("title", title);
        yaml.set("rows", rows);
        yaml.set("command", openCommand);
        for (Map.Entry<Integer, SlotData> entry : slots.entrySet()) {
            entry.getValue().save(yaml.createSection("slots." + entry.getKey()));
        }
    }

    public static Menu load(String name, YamlConfiguration yaml) {
        int rows = Math.max(1, Math.min(6, yaml.getInt("rows", 3)));
        Menu menu = new Menu(name, yaml.getString("title", "&8" + name), rows);
        String command = yaml.getString("command");
        menu.openCommand = (command == null || command.isBlank()) ? null : command;

        ConfigurationSection section = yaml.getConfigurationSection("slots");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    ConfigurationSection slotSection = section.getConfigurationSection(key);
                    if (slotSection != null && index >= 0 && index < menu.size()) {
                        menu.slots.put(index, SlotData.load(slotSection));
                    }
                } catch (NumberFormatException ignored) {
                    // pomijamy niepoprawne klucze
                }
            }
        }
        return menu;
    }
}
