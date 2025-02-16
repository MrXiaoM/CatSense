package top.mrxiaom.catsense;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CatSense extends JavaPlugin {
    LocksListener locks;
    ContainerListener container;
    private Economy econ = null;
    private String prefix;
    private int costRepair;
    private int costClearEnch;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            this.getLogger().warning("无法与 Vault 经济挂钩，正在卸载插件");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public String getPrefix() {
        return prefix;
    }

    public void reloadConfig() {
        saveDefaultConfig();
        super.reloadConfig();
        prefix = this.getConfig().getString("prefix", "&7[&b小猫感应&7] &e");
        costRepair = this.getConfig().getInt("repair.cost", 500);
        costClearEnch = this.getConfig().getInt("repair.cost-clear-ench", 500);
        if (locks == null)
            locks = new LocksListener(this);
        if (container == null)
            container = new ContainerListener(this);
        locks.reloadConfig();
        container.reloadConfig();
    }

    public String resTranslate(String key) {
        return ChatColor.translateAlternateColorCodes('&', Residence.getInstance().getLocaleManager().getLocaleConfig().getString(key));
    }

    private void m(Player player, String key) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString(key, "[404:" + key + "]")));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("catsense")) {
            if (args.length == 1) {
                if (sender.isOp() && args[0].equalsIgnoreCase("reload")) {
                    this.reloadConfig();
                    sender.sendMessage(prefix + "配置文件已重载");
                    return true;
                }
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (args[0].equalsIgnoreCase("repair")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item == null || item.getType().equals(Material.AIR)) {
                            m(player, "repair.message.repair.no-item");
                            return true;
                        }
                        if (item.getDurability() >= item.getType().getMaxDurability()) {
                            m(player, "repair.message.repair.no-need");
                            return true;
                        }
                        double money = econ.getBalance(player);
                        if (costRepair > money) {
                            m(player, "repair.message.repair.no-money");
                            return true;
                        }
                        econ.withdrawPlayer(player, costRepair);
                        item.setDurability(item.getType().getMaxDurability());
                        player.getInventory().setItemInMainHand(item);
                        m(player, "repair.message.repair.ok");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("clearEnch")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item == null || item.getType().equals(Material.AIR)) {
                            m(player, "repair.message.clear-ench.no-item");
                            return true;
                        }
                        if (item.getEnchantments().isEmpty()) {
                            m(player, "repair.message.clear-ench.no-need");
                            return true;
                        }
                        double money = econ.getBalance(player);
                        if (costClearEnch > money) {
                            m(player, "repair.message.clear-ench.no-money");
                            return true;
                        }
                        econ.withdrawPlayer(player, costClearEnch);
                        for (Enchantment ench : item.getEnchantments().keySet()) {
                            item.removeEnchantment(ench);
                        }
                        player.getInventory().setItemInMainHand(item);
                        m(player, "repair.message.clear-ench.ok");
                        return true;
                    }
                }
            }
            sender.sendMessage("§7[§b小猫感应§7] §e懒怠的小猫§a友情提供插件 §7for §b&o§l零都市");
            sender.sendMessage("§7[§b小猫感应§7] §a/catsense repair §7花费" + costRepair + "金币修理物品");
            sender.sendMessage("§7[§b小猫感应§7] §a/catsense clearEnch §7花费" + costClearEnch + "金币清除附魔");
            if (sender.isOp()) sender.sendMessage("§7[§b小猫感应§7] §a/catsense reload §7重载配置文件");
            sender.sendMessage("§7[§b小猫感应§7] §7(这里 ores 是 Original Residence 的缩写， 不是矿石…)");
            sender.sendMessage("§7[§b小猫感应§7] §a/ores tpset §7设置领地传送点(不跨服， 支持子领地)");
            sender.sendMessage("§7[§b小猫感应§7] §a/ores tp <领地> §7传送到领地(不跨服， 支持子领地)");
            sender.sendMessage("§7[§b小猫感应§7] §a/locks §7收费门帮助命令");
            return true;
        }
        // 收费门帮助
        if (label.equalsIgnoreCase("locks")) {
            locks.n(sender, "help");
            return true;
        }
        return true;
    }

    @SafeVarargs
    public static void runCommand(Player player, List<String> commands, Pair<String, String>... replacement) {
        for (String cmd : commands) {
            cmd = PlaceholderAPI.setPlaceholders(player, cmd);
            for (Pair<String, String> replace : replacement) {
                cmd = cmd.replace(replace.getKey(), replace.getValue());
            }
            if (cmd.startsWith("player:")) {
                Bukkit.dispatchCommand(player, cmd.substring("player:".length()));
            }
            if (cmd.startsWith("console:")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring("console:".length()));
            }
            if (cmd.startsWith("msg:")) {
                player.sendMessage(cmd.substring("msg:".length()));
            }
        }
    }
}
