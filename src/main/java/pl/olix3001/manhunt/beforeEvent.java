package pl.olix3001.manhunt;


import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class beforeEvent implements Listener {

    @EventHandler
    public void moveEvent(PlayerMoveEvent e) {
        if (e.getPlayer() != Manhunt.getInstance().runner) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void openInv(InventoryOpenEvent e) {
        if (e.getPlayer() != Manhunt.getInstance().runner) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() != Manhunt.getInstance().runner && e.getEntity().getType() == EntityType.PLAYER) {
           e.setCancelled(true);
        }
    }

}
