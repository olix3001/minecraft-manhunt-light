package pl.olix3001.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class huntCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Manhunt.getInstance().runner != null) {
            sender.sendMessage(ChatColor.RED + "Nie możesz rozpocząć innej gry w trakcie trwania innej");
            return true;
        }
        for (Entity e : ((Player) sender).getWorld().getEntities()) {
            if (e.getType() != EntityType.PLAYER) {
                e.remove();
            }
        }
        Player runner = Bukkit.getPlayer(args[0]);
        runner.setGameMode(GameMode.SURVIVAL);
        runner.getWorld().setTime(3000);
        Manhunt.getInstance().runner = runner;
        Manhunt.getInstance().start = runner.getLocation().clone();
        Manhunt plugin = Manhunt.getInstance();
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.RemoveAdvancements(p);
            p.getInventory().clear();
            p.setBedSpawnLocation(runner.getLocation(), true);
            for (PotionEffect e : p.getActivePotionEffects()) {
                p.removePotionEffect(e.getType());
            }
            p.setHealth(20);
            p.setFoodLevel(20);
            if (p != runner) {
                p.teleport(runner.getLocation());
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 100));
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "=====< HUNTER >=====");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "Jesteś hunterem!");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "Twoim zadaniem jest zabicie uciekającego");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "zanim on pokona smoka endu.");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "Kompas wskazuje ci uciekającego (tylko kiedy go trzymasz).");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "=====< HUNTER >=====");
            } else {
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "=====< RUNNER >=====");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "Jesteś uciekającym!");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "Twoim zadaniem jest pokonanie smoka endu");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "zanim hunterzy cię zabiją.");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "Kiedy zginiesz gra się kończy");
                p.sendMessage(plugin.prefix + ChatColor.GREEN + "=====< RUNNER >=====");
            }
        }
        plugin.getServer().getPluginManager().registerEvents(plugin.event, plugin);
        plugin.regen.start();
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p == runner) continue;
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    ItemMeta meta = compass.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + "Lokalizator");
                    meta.setLore(Arrays.asList(ChatColor.AQUA + "wskazuje uciekającego", ChatColor.AQUA + "Działa tylko w ręce"));
                    meta.setUnbreakable(true);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
                    compass.setItemMeta(meta);
                    p.getInventory().addItem(compass);
                    p.setGameMode(GameMode.SURVIVAL);
                }
                runner.sendTitle(ChatColor.RED + "START", ChatColor.GOLD + "Hunterzy mogą cię teraz gonić", 1, 20, 1);
                Bukkit.broadcastMessage(ChatColor.GREEN + "START");
                HandlerList.unregisterAll(plugin.event);
            }
        }, 20*15);

        Bukkit.broadcastMessage(Manhunt.getInstance().prefix + ChatColor.GOLD + "=====< Manhunt by olix3001 >=====");
        Bukkit.broadcastMessage(Manhunt.getInstance().prefix + ChatColor.GOLD + args[0] + " jest teraz goniony (15 sekund na ucieczkę)");
        return true;
    }
}
