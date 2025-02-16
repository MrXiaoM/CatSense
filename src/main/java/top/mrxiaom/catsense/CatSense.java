package top.mrxiaom.catsense;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CatSense extends JavaPlugin {
    LocksListener locks;
    ContainerListener container;
    private Economy econ = null;
    private String prefix;
    private int costRepair;
    private int costClearEnchants;
    private List<String> help, helpOp;

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
        return true;
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
        FileConfiguration config = this.getConfig();
        prefix = config.getString("prefix", "&7[&b小猫感应&7] &e");
        costRepair = config.getInt("repair.cost", 500);
        costClearEnchants = config.getInt("repair.cost-clear-ench", 500);
        help = config.getStringList("help");
        helpOp = config.getStringList("help-op");
        if (locks == null)
            locks = new LocksListener(this);
        if (container == null)
            container = new ContainerListener(this);
        locks.reloadConfig();
        container.reloadConfig();
    }

    private void m(Player player, String key) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString(key, "[404:" + key + "]")));
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().equals(Material.AIR);
    }

    @Override
    @SuppressWarnings({"deprecation"})
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
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
                        ItemStack item = player.getInventory().getItemInHand();
                        if (isEmpty(item)) {
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
                        ItemStack item = player.getInventory().getItemInHand();
                        if (isEmpty(item)) {
                            m(player, "repair.message.clear-ench.no-item");
                            return true;
                        }
                        if (item.getEnchantments().isEmpty()) {
                            m(player, "repair.message.clear-ench.no-need");
                            return true;
                        }
                        double money = econ.getBalance(player);
                        if (costClearEnchants > money) {
                            m(player, "repair.message.clear-ench.no-money");
                            return true;
                        }
                        econ.withdrawPlayer(player, costClearEnchants);
                        for (Enchantment ench : item.getEnchantments().keySet()) {
                            item.removeEnchantment(ench);
                        }
                        player.getInventory().setItemInMainHand(item);
                        m(player, "repair.message.clear-ench.ok");
                        return true;
                    }
                }
            }
            List<String> helpMessage = sender.isOp() ? helpOp : help;
            String strRepairCost = String.valueOf(costRepair);
            String strClearCost = String.valueOf(costClearEnchants);
            for (String s : helpMessage) {
                String s1 = ChatColor.translateAlternateColorCodes('&', s)
                        .replace("%repair_cost%", strRepairCost)
                        .replace("%clear_cost%", strClearCost);
                sender.sendMessage(s1);
            }
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

    public static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
