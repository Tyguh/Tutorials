package net.viraxis.tutorials;

import net.viraxis.currencies.shops.event.ShopSaleCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

final class CurrencyHooks implements Listener {
    private final TutorialsPlugin plugin;
    CurrencyHooks(TutorialsPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void onSale(ShopSaleCompleteEvent event) {
        if (event.getPaidTotal() <= 0) return;
        plugin.advance(event.getPlayer(), "ShopSale", trigger -> trigger.currency() == null
                || trigger.currency().equalsIgnoreCase(event.getCurrencyId()));
    }
}
