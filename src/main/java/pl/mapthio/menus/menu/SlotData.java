package pl.mapthio.menus.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.mapthio.menus.util.Text;

/** Dane jednego slotu menu: blok, nazwa, enchant effect i komenda. */
public final class SlotData {

    public enum Executor {
        PLAYER,
        CONSOLE
    }

    private Material material = Material.STONE;
    /** Pusty = domyślna nazwa przedmiotu, " " = całkowicie pusta nazwa. */
    private String name = "";
    private boolean glow;
    /** Komenda bez "/", wiele komend oddzielonych ";;". Pusta = brak akcji. */
    private String command = "";
    private Executor executor = Executor.PLAYER;
    private boolean closeOnClick = true;

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command == null ? "" : command;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public boolean isCloseOnClick() {
        return closeOnClick;
    }

    public void setCloseOnClick(boolean closeOnClick) {
        this.closeOnClick = closeOnClick;
    }

    /** Buduje przedmiot widoczny w menu. */
    public ItemStack toItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (!name.isEmpty()) {
            meta.displayName(Text.parse(name));
        }
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public void save(ConfigurationSection section) {
        section.set("material", material.name());
        if (!name.isEmpty()) {
            section.set("name", name);
        }
        section.set("glow", glow);
        if (!command.isEmpty()) {
            section.set("command", command);
        }
        section.set("executor", executor.name());
        section.set("close", closeOnClick);
    }

    public static SlotData load(ConfigurationSection section) {
        SlotData data = new SlotData();
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        data.material = (material != null && material.isItem() && !material.isAir()) ? material : Material.STONE;
        data.name = section.getString("name", "");
        data.glow = section.getBoolean("glow", false);
        data.command = section.getString("command", "");
        try {
            data.executor = Executor.valueOf(section.getString("executor", "PLAYER").toUpperCase());
        } catch (IllegalArgumentException ex) {
            data.executor = Executor.PLAYER;
        }
        data.closeOnClick = section.getBoolean("close", true);
        return data;
    }
}
