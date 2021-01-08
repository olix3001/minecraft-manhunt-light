package pl.olix3001.manhunt;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public final class Manhunt extends JavaPlugin implements Listener {

    private static Manhunt instance;
    public Player runner;
    public String prefix;
    public beforeEvent event;
    public WorldRegen regen;

    @Override
    public void onEnable() {
        Manhunt.instance = this;
        getCommand("hunt").setExecutor(new huntCommand());
        event = new beforeEvent();
        regen = new WorldRegen();
        prefix = ChatColor.WHITE + "[" + ChatColor.AQUA + "MANHUNT" + ChatColor.WHITE + "] ";
        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (runner != null) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ItemStack mainHand = p.getInventory().getItemInMainHand();
                        ItemStack offHand = p.getInventory().getItemInOffHand();
                        if ((mainHand.getType() == Material.COMPASS && mainHand.getItemMeta().isUnbreakable()) || (offHand.getType() == Material.COMPASS && offHand.getItemMeta().isUnbreakable())) {
                            p.setCompassTarget(runner.getLocation());
                            for (ItemStack item : p.getInventory().getContents()) {
                                if (item == null) continue;
                                if (item.getType() == Material.COMPASS && item.getItemMeta().isUnbreakable()) {
                                    ItemMeta meta = item.getItemMeta();
                                    meta.setDisplayName(ChatColor.GOLD + "Lokalizator");
                                    item.setItemMeta(meta);
                                }
                            }
                        }
                    }
                }
            }
        }, 0L, 20L);
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {
        if (runner == null && !e.getPlayer().isOp()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        if (runner == null) return;
        Player p = e.getPlayer();
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Lokalizator");
        meta.setLore(Arrays.asList(ChatColor.AQUA + "wskazuje uciekającego", ChatColor.AQUA + "Działa tylko w ręce"));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        compass.setItemMeta(meta);
        p.getInventory().addItem(compass);
    }

    public void RemoveAdvancements(Player p) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke " + p.getName() + " everything");
    }

    @EventHandler
    public void explosion(BlockExplodeEvent e) { if (runner == null) e.setCancelled(true);}
    @EventHandler
    public void explosion(EntityExplodeEvent e) { if (runner == null) e.setCancelled(true);}
    @EventHandler
    public void damage(EntityDamageEvent e) { if (runner == null) e.setCancelled(true);}

    @EventHandler
    public void win(PlayerAdvancementDoneEvent e) {
        if (runner == null) return;
        if (e.getAdvancement().getKey().getKey().equals("end/kill_dragon")) {
            Bukkit.broadcastMessage(prefix + ChatColor.GOLD + runner.getName() + " Pokonał smoka");
            Bukkit.broadcastMessage(prefix + ChatColor.GOLD + " świat zostanie zresetowany za 5 sekund");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().clear();
                p.sendTitle(ChatColor.RED + "KONIEC", ChatColor.RED + "Uciekający pokonał smoka", 1, 70, 1);
            }
            runner = null;
            Bukkit.getScheduler().scheduleSyncDelayedTask(Manhunt.getInstance(), new Runnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.teleport(p.getBedSpawnLocation());
                    }
                    regenerateMap();
                }
            }, 20*5);
        }
    }

    private void regenerateMap() {
        Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "Regenerowanie swiata");
        regen.regenerate();
        Bukkit.broadcastMessage(prefix + ChatColor.GREEN + "Gra zresetowana");
        Bukkit.broadcastMessage(prefix + ChatColor.WHITE + "[" + ChatColor.RED + "UWAGA" + ChatColor.WHITE + "] " + ChatColor.RED + "ze względu na ograniczone możliwości bukkita żaden ze światów nie zostanie wygenerowany od nowa (zresetowane zostaną jedynie zmiany wprowadzone przez gracza w trakcie gry, smoka należy zespawnować ręcznie)");
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if (runner == null) return;
        Player player = e.getPlayer();
        if (player != runner) return;
        runner = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.ADVENTURE);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(Manhunt.getInstance(), new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.teleport(p.getBedSpawnLocation());
                }
                regenerateMap();
            }
        }, 20*5);
        Bukkit.broadcastMessage(prefix + ChatColor.GOLD + player.getName() + " wyszedł z gry");
        Bukkit.broadcastMessage(prefix + ChatColor.GOLD + " świat zostanie zresetowany za 5 sekund");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            p.sendTitle(ChatColor.RED + "KONIEC", ChatColor.RED + "uciekający opuścił grę", 1, 70, 1);
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (e.getEntity() != runner) {
            List<ItemStack> drops = e.getDrops();
            drops.removeIf(i -> i.getType() == Material.COMPASS && i.getItemMeta().isUnbreakable());
            return;
        }
        if (runner == null) return;
        Player killed = e.getEntity();
        LivingEntity killer = e.getEntity().getKiller();

        runner = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.ADVENTURE);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(Manhunt.getInstance(), new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getBedSpawnLocation() == null) continue;
                    p.teleport(p.getBedSpawnLocation());
                }
                regenerateMap();
            }
        }, 20*5);
        if (killer != null) {
            Bukkit.broadcastMessage(prefix + ChatColor.GOLD + killed.getName() + " został zabity przez " + killer.getName());
            Bukkit.broadcastMessage(prefix + ChatColor.GOLD + "świat zostanie zresetowany za 5 sekund");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().clear();
                p.sendTitle(ChatColor.RED + "KONIEC", ChatColor.RED + "uciekający został zabity przez gracza " + killer.getName(), 1, 70, 1);
            }
        } else {
            Bukkit.broadcastMessage(prefix + ChatColor.GOLD + killed.getName() + " nie żyje");
            Bukkit.broadcastMessage(prefix + ChatColor.GOLD + "świat zostanie zresetowany za 5 sekund");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().clear();
                p.sendTitle(ChatColor.RED + "KONIEC", ChatColor.RED + "uciekający nie żyje", 1, 70, 1);
            }
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (runner == null) {
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
            e.getPlayer().sendMessage(ChatColor.GOLD + "Wpisz /hunt <nazwa gracza> żeby rozpocząć nową grę");
            TextComponent discordLink = new TextComponent(ChatColor.DARK_RED + "DISCORD");
            discordLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.com/invite/rmHZWbqCxE"));
            TextComponent prefix = new TextComponent(ChatColor.DARK_PURPLE + "Jeżeli chcesz plugin na swój serwer sprawdź \n#pluginy na [");
            TextComponent suffix = new TextComponent(ChatColor.DARK_PURPLE + "] <- KLIKNIJ");
            prefix.addExtra(discordLink);
            prefix.addExtra(suffix);
            e.getPlayer().spigot().sendMessage(prefix);
        } else {
            e.getPlayer().setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void beforeJoin(PlayerLoginEvent e) {
        if (runner != null) {
            if (e.getPlayer().isOp()) return;
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Teraz trwa inna gra");
        }
    }

    @EventHandler
    public void ping(ServerListPingEvent e) {
        if (runner == null) {
            e.setMotd(ChatColor.RED + "Witaj!" + ChatColor.AQUA + "\nZaproś znajomych i zagrajcie w manhunt");
        } else {
            e.setMotd(ChatColor.RED + "Witaj!" + ChatColor.AQUA + "\nTeraz trwa gra, nie możesz dołączyć");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventoryDrag(InventoryDragEvent e) {
        for (ItemStack i : e.getNewItems().values()) {
            if (i.getType() == Material.COMPASS && i.getItemMeta().isUnbreakable()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventoryClick(InventoryClickEvent e) {
        ItemStack i = e.getCurrentItem();
        if (i.getType() == Material.COMPASS && i.getItemMeta().isUnbreakable()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void itemDrop(PlayerDropItemEvent e) {
        Item i = e.getItemDrop();
        if (i.getItemStack().getType() == Material.COMPASS && i.getItemStack().getItemMeta().isUnbreakable()) {
            e.setCancelled(true);
        }
    }

    @Override
    public void onDisable() {

    }

    public static Manhunt getInstance() {
        return Manhunt.instance;
    }
}
