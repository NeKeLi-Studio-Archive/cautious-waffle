package pl.mapthio.menus.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pl.mapthio.menus.menu.Menu;

/** Rozpoznaje inventory należące do Mapthio Menus. */
public final class MenuHolder implements InventoryHolder {

    public enum Mode {
        /** Zwykłe menu dla gracza. */
        VIEW,
        /** Podgląd edycji - klik w slot otwiera edytor slotu. */
        EDIT,
        /** Edytor jednego slotu. */
        SLOT_EDITOR
    }

    private final Menu menu;
    private final Mode mode;
    private final int slot;
    private Inventory inventory;

    public MenuHolder(Menu menu, Mode mode, int slot) {
        this.menu = menu;
        this.mode = mode;
        this.slot = slot;
    }

    public Menu menu() {
        return menu;
    }

    public Mode mode() {
        return mode;
    }

    public int slot() {
        return slot;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
