package pl.mapthio.menus.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.mapthio.menus.MapthioMenus;
import pl.mapthio.menus.menu.Menu;
import pl.mapthio.menus.menu.SlotData;
import pl.mapthio.menus.util.Text;

import java.util.function.Consumer;

/** Obsługa kliknięć w menu, podglądzie edycji i edytorze slotu. */
public final class MenuListener implements Listener {

    private final MapthioMenus plugin;

    public MenuListener(MapthioMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        // W menu nic nie wolno wyciągać ani wkładać.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean topClick = event.getClickedInventory() != null && event.getClickedInventory().equals(top);

        switch (holder.mode()) {
            case VIEW -> {
                if (topClick) {
                    handleView(player, holder, event);
                }
            }
            case EDIT -> {
                if (!player.hasPermission("mapthio.admin")) {
                    return;
                }
                if (topClick) {
                    handleEdit(player, holder, event);
                }
            }
            case SLOT_EDITOR -> {
                if (!player.hasPermission("mapthio.admin")) {
                    return;
                }
                if (topClick) {
                    handleEditorButton(player, holder, event);
                } else {
                    handleEditorPick(player, holder, event);
                }
            }
        }
    }

    // ---------------------------------------------------------------- menu dla gracza

    private void handleView(Player player, MenuHolder holder, InventoryClickEvent event) {
        SlotData data = holder.menu().getSlot(event.getRawSlot());
        if (data == null || data.getCommand().isBlank()) {
            return;
        }
        String command = data.getCommand();
        SlotData.Executor executor = data.getExecutor();
        boolean close = data.isCloseOnClick();

        later(() -> {
            if (close) {
                player.closeInventory();
            }
            for (String part : command.split(";;")) {
                String cmd = part.trim();
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                if (cmd.isEmpty()) {
                    continue;
                }
                cmd = cmd.replace("%player%", player.getName());
                if (executor == SlotData.Executor.CONSOLE) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } else {
                    player.performCommand(cmd);
                }
            }
        });
    }

    // ---------------------------------------------------------------- podgląd edycji

    private void handleEdit(Player player, MenuHolder holder, InventoryClickEvent event) {
        Menu menu = holder.menu();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= menu.size()) {
            return;
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType().isItem() && !cursor.getType().isAir()) {
            // Szybka zmiana bloku: klik slotu przedmiotem z kursora.
            menu.getOrCreate(slot).setMaterial(cursor.getType());
            plugin.menus().save(menu);
            later(() -> MenuGui.openEdit(player, menu));
            return;
        }
        later(() -> MenuGui.openSlotEditor(player, menu, slot));
    }

    // ---------------------------------------------------------------- edytor slotu

    private void handleEditorButton(Player player, MenuHolder holder, InventoryClickEvent event) {
        Menu menu = holder.menu();
        int slot = holder.slot();
        boolean right = event.isRightClick();

        switch (event.getRawSlot()) {
            case MenuGui.BTN_BLOCK -> {
                if (right) {
                    promptMaterial(player, menu, slot);
                } else {
                    player.sendMessage(Text.info(
                            "Kliknij przedmiot w swoim ekwipunku (dolna część okna), aby ustawić go jako blok slotu."));
                }
            }
            case MenuGui.BTN_NAME -> {
                if (right) {
                    modify(player, menu, slot, data -> data.setName(""));
                } else {
                    promptName(player, menu, slot);
                }
            }
            case MenuGui.BTN_GLOW -> modify(player, menu, slot, data -> data.setGlow(!data.isGlow()));
            case MenuGui.BTN_COMMAND -> {
                if (right) {
                    modify(player, menu, slot, data -> data.setCommand(""));
                } else {
                    promptCommand(player, menu, slot);
                }
            }
            case MenuGui.BTN_EXECUTOR -> modify(player, menu, slot, data -> data.setExecutor(
                    data.getExecutor() == SlotData.Executor.PLAYER
                            ? SlotData.Executor.CONSOLE : SlotData.Executor.PLAYER));
            case MenuGui.BTN_CLOSE -> modify(player, menu, slot, data -> data.setCloseOnClick(!data.isCloseOnClick()));
            case MenuGui.BTN_CLEAR -> {
                menu.removeSlot(slot);
                plugin.menus().save(menu);
                later(() -> MenuGui.openEdit(player, menu));
            }
            case MenuGui.BTN_BACK -> later(() -> MenuGui.openEdit(player, menu));
            default -> {
                // tło edytora
            }
        }
    }

    /** Klik w dolny ekwipunek gracza = wybór bloku dla slotu. */
    private void handleEditorPick(Player player, MenuHolder holder, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.getType().isItem()) {
            return;
        }
        Material material = clicked.getType();
        modify(player, holder.menu(), holder.slot(), data -> data.setMaterial(material));
    }

    private void modify(Player player, Menu menu, int slot, Consumer<SlotData> change) {
        change.accept(menu.getOrCreate(slot));
        plugin.menus().save(menu);
        later(() -> MenuGui.openSlotEditor(player, menu, slot));
    }

    // ---------------------------------------------------------------- wpisywanie na czacie

    private void promptName(Player player, Menu menu, int slot) {
        later(() -> plugin.chat().prompt(player,
                "Wpisz nową nazwę slotu (kolory: &a, hex: &#ff8800). '-' = pusta nazwa, 'reset' = domyślna.",
                text -> {
                    SlotData data = menu.getOrCreate(slot);
                    if (text.equalsIgnoreCase("reset")) {
                        data.setName("");
                    } else if (text.equals("-")) {
                        data.setName(" ");
                    } else {
                        data.setName(text);
                    }
                    plugin.menus().save(menu);
                    return true;
                },
                () -> MenuGui.openSlotEditor(player, menu, slot)));
    }

    private void promptCommand(Player player, Menu menu, int slot) {
        later(() -> plugin.chat().prompt(player,
                "Wpisz komendę dla slotu (bez /, np. gamemode creative %player%). "
                        + "Kilka komend oddziel ';;'. 'brak' = usuń komendę.",
                text -> {
                    SlotData data = menu.getOrCreate(slot);
                    String cmd = text.startsWith("/") ? text.substring(1) : text;
                    data.setCommand(cmd.equalsIgnoreCase("brak") ? "" : cmd);
                    plugin.menus().save(menu);
                    return true;
                },
                () -> MenuGui.openSlotEditor(player, menu, slot)));
    }

    private void promptMaterial(Player player, Menu menu, int slot) {
        later(() -> plugin.chat().prompt(player,
                "Wpisz nazwę bloku/przedmiotu, np. DIAMOND_BLOCK.",
                text -> {
                    Material material = Text.parseMaterial(text);
                    if (material == null) {
                        player.sendMessage(Text.error("Nie ma takiego przedmiotu: " + text + ". Spróbuj ponownie."));
                        return false;
                    }
                    menu.getOrCreate(slot).setMaterial(material);
                    plugin.menus().save(menu);
                    return true;
                },
                () -> MenuGui.openSlotEditor(player, menu, slot)));
    }

    private void later(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
