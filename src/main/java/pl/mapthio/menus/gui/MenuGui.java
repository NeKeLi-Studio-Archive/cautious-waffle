package pl.mapthio.menus.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.mapthio.menus.menu.Menu;
import pl.mapthio.menus.menu.SlotData;
import pl.mapthio.menus.util.Text;

import java.util.ArrayList;
import java.util.List;

/** Buduje i otwiera inventory: menu, podgląd edycji i edytor slotu. */
public final class MenuGui {

    // Pozycje przycisków w edytorze slotu (3 rzędy)
    public static final int BTN_BLOCK = 10;
    public static final int BTN_NAME = 11;
    public static final int BTN_GLOW = 12;
    public static final int BTN_COMMAND = 13;
    public static final int BTN_EXECUTOR = 14;
    public static final int BTN_CLOSE = 15;
    public static final int BTN_CLEAR = 16;
    public static final int BTN_BACK = 22;

    private MenuGui() {
    }

    /** Menu tak, jak widzi je gracz. */
    public static void openView(Player player, Menu menu) {
        MenuHolder holder = new MenuHolder(menu, MenuHolder.Mode.VIEW, -1);
        Inventory inventory = Bukkit.createInventory(holder, menu.size(), Text.parse(menu.getTitle()));
        holder.setInventory(inventory);
        for (var entry : menu.slots().entrySet()) {
            if (entry.getKey() < menu.size()) {
                inventory.setItem(entry.getKey(), entry.getValue().toItem());
            }
        }
        player.openInventory(inventory);
    }

    /** Podgląd edycji: kliknij slot, aby go edytować. */
    public static void openEdit(Player player, Menu menu) {
        MenuHolder holder = new MenuHolder(menu, MenuHolder.Mode.EDIT, -1);
        Inventory inventory = Bukkit.createInventory(holder, menu.size(),
                Text.parse("&8Edycja: &0" + menu.getName()));
        holder.setInventory(inventory);

        for (int i = 0; i < menu.size(); i++) {
            SlotData data = menu.getSlot(i);
            if (data == null) {
                inventory.setItem(i, button(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Slot " + i + " &8(pusty)",
                        false, "&eKliknij, aby edytować", "&7Trzymasz przedmiot na kursorze?",
                        "&7Kliknij nim, aby od razu ustawić blok."));
                continue;
            }
            ItemStack item = data.toItem();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(Text.plain("Slot " + i, NamedTextColor.DARK_GRAY));
                if (!data.getCommand().isEmpty()) {
                    lore.add(Text.plain("Komenda: /" + data.getCommand(), NamedTextColor.GRAY));
                }
                lore.add(Text.plain("Kliknij, aby edytować", NamedTextColor.YELLOW));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    /** Edytor jednego slotu: blok, nazwa, enchant effect, komenda. */
    public static void openSlotEditor(Player player, Menu menu, int slot) {
        MenuHolder holder = new MenuHolder(menu, MenuHolder.Mode.SLOT_EDITOR, slot);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Text.parse("&8Slot " + slot + " &7• &8" + menu.getName()));
        holder.setInventory(inventory);

        ItemStack filler = button(Material.BLACK_STAINED_GLASS_PANE, " ", false);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        SlotData data = menu.getSlot(slot);
        boolean exists = data != null;
        Material material = exists ? data.getMaterial() : Material.STONE;
        String name = exists ? data.getName() : "";
        boolean glow = exists && data.isGlow();
        String command = exists ? data.getCommand() : "";
        SlotData.Executor executor = exists ? data.getExecutor() : SlotData.Executor.PLAYER;
        boolean close = !exists || data.isCloseOnClick();

        inventory.setItem(BTN_BLOCK, button(material, "&aZmień blok", false,
                "&7Aktualny: &f" + material.name(),
                "",
                "&eKliknij przedmiot w SWOIM ekwipunku",
                "&eaby ustawić go jako blok slotu.",
                "&7Prawy klik: wpisz nazwę na czacie"));

        String shownName = name.isEmpty() ? "&7(domyślna)" : (name.isBlank() ? "&7(pusta)" : name);
        inventory.setItem(BTN_NAME, button(Material.NAME_TAG, "&aZmień nazwę", false,
                "&7Aktualna: &f" + shownName,
                "",
                "&eLewy klik: wpisz nazwę na czacie",
                "&7Prawy klik: przywróć domyślną"));

        inventory.setItem(BTN_GLOW, button(Material.ENCHANTED_BOOK, "&aEnchant effect", false,
                "&7Połysk: " + (glow ? "&aWŁĄCZONY" : "&cWYŁĄCZONY"),
                "",
                "&eKliknij, aby przełączyć"));

        inventory.setItem(BTN_COMMAND, button(Material.COMMAND_BLOCK, "&aKomenda slotu", false,
                "&7Aktualna: &f" + (command.isEmpty() ? "&7(brak)" : "/" + command),
                "",
                "&eLewy klik: wpisz komendę na czacie",
                "&7Prawy klik: usuń komendę",
                "&8%player% = nick gracza, ;; = wiele komend"));

        inventory.setItem(BTN_EXECUTOR, button(Material.COMPARATOR, "&aWykonuje komendę", false,
                "&7Jako: " + (executor == SlotData.Executor.PLAYER ? "&bGRACZ" : "&dKONSOLA"),
                "",
                "&eKliknij, aby przełączyć"));

        inventory.setItem(BTN_CLOSE, button(Material.OAK_DOOR, "&aZamknij menu po kliknięciu", false,
                "&7Zamyka: " + (close ? "&aTAK" : "&cNIE"),
                "",
                "&eKliknij, aby przełączyć"));

        inventory.setItem(BTN_CLEAR, button(Material.TNT, "&cWyczyść slot", false,
                "&7Usuwa wszystkie ustawienia tego slotu."));

        inventory.setItem(BTN_BACK, button(Material.ARROW, "&eWróć do menu", false));

        player.openInventory(inventory);
    }

    /** Przycisk z kolorowanym tytułem i lore. */
    private static ItemStack button(Material material, String name, boolean glow, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Text.parse(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Text.parse(line));
            }
            meta.lore(lines);
        }
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
