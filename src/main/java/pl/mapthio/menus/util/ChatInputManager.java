package pl.mapthio.menus.util;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Pobiera od gracza tekst z czatu (nazwa slotu, komenda, nazwa bloku). */
public final class ChatInputManager implements Listener {

    private record Pending(Predicate<String> handler, Runnable after) {
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInputManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Zamyka inventory i czeka na wiadomość gracza.
     *
     * @param handler zwraca true, gdy wpis został przyjęty (false = pytamy dalej)
     * @param after   uruchamiane po przyjęciu wpisu lub anulowaniu (np. ponowne otwarcie edytora)
     */
    public void prompt(Player player, String hint, Predicate<String> handler, Runnable after) {
        player.closeInventory();
        pending.put(player.getUniqueId(), new Pending(handler, after));
        player.sendMessage(Text.info(hint));
        player.sendMessage(Text.info("Wpisz 'anuluj', aby wrócić bez zmian."));
    }

    public void cancelIfPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            pending.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Pending entry = pending.get(id);
        if (entry == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (pending.get(id) != entry) {
                return;
            }
            if (text.equalsIgnoreCase("anuluj") || text.equalsIgnoreCase("cancel")) {
                pending.remove(id);
                player.sendMessage(Text.info("Anulowano."));
                entry.after().run();
                return;
            }
            if (entry.handler().test(text)) {
                pending.remove(id);
                entry.after().run();
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
