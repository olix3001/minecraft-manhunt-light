package pl.olix3001.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WorldRegen implements Listener {

    List<BlockCopy> toRegenerate;

    public WorldRegen() {
        toRegenerate = new ArrayList<>();
    }

    public void start() {
        Manhunt.getInstance().getServer().getPluginManager().registerEvents(this, Manhunt.getInstance());
    }

    public void restart() {
        toRegenerate = new ArrayList<>();
    }

    public void regenerate() {
        HandlerList.unregisterAll(this);
        int regenerated = 0;
        for (BlockCopy b : toRegenerate) {
            b.place();
            regenerated++;
        }
        Bukkit.broadcastMessage(Manhunt.getInstance().prefix + ChatColor.DARK_GRAY + "Zregenerowano " + ChatColor.DARK_RED + regenerated + ChatColor.DARK_GRAY + " bloków");
        restart();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockBreak(BlockBreakEvent e) {
        if (hasPosition(e.getBlock().getLocation())) return;
        toRegenerate.add(new BlockCopy(e.getBlock(), true));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void blockPlace(BlockPlaceEvent e) {
        if (hasPosition(e.getBlock().getLocation())) return;
        toRegenerate.add(new BlockCopy(Material.AIR, e.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void blockBurn(BlockBurnEvent e) {
        if (hasPosition(e.getBlock().getLocation())) return;
        toRegenerate.add(new BlockCopy(e.getBlock(), false));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void grow(StructureGrowEvent e) {
        for (BlockState bs : e.getBlocks()) {
            Block b = bs.getBlock();
            if (hasPosition(b.getLocation())) continue;
            toRegenerate.add(new BlockCopy(b, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void explosion(BlockExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (hasPosition(b.getLocation())) continue;
            toRegenerate.add(new BlockCopy(b, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void entityExplosion(EntityExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (hasPosition(b.getLocation())) continue;
            toRegenerate.add(new BlockCopy(b, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void interact(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (hasPosition(e.getClickedBlock().getLocation())) return;
        toRegenerate.add(new BlockCopy(e.getClickedBlock(), true));
    }

    boolean hasPosition(Location l) {
        return (toRegenerate.stream().anyMatch(block -> (l.getX() == block.loc.getX() && l.getY() == block.loc.getY() && l.getZ() == block.loc.getZ())));
    }
}

class BlockCopy {
    Material mat;
    BlockData data;
    Inventory inv;
    public Location loc;

    public BlockCopy(Block b, boolean saveData) {
        mat = b.getType();
        if (saveData) {
            data = b.getBlockData().clone();
        } else {
            data = Bukkit.createBlockData(mat);
        }
        BlockState state = b.getState();
        if (state instanceof Container) {
            Inventory container = ((Container) state).getInventory();
            inv = Bukkit.createInventory(null, container.getSize(), "copy");
            inv.setContents(((Container) state).getInventory().getContents());
        }
        loc = b.getLocation();
    }

    public BlockCopy(Material m, Location l) {
        mat = m;
        data = Bukkit.createBlockData(m);
        loc = l;
    }

    public void place() {
        loc.getBlock().setType(mat);
        loc.getBlock().setBlockData(data);
        if (inv != null) {
            if (loc.getBlock().getState() instanceof Container) {
                ((Container) loc.getBlock().getState()).getInventory().setContents(inv.getContents());
                inv = null;
            }
        }
    }
}
