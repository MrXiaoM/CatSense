package top.mrxiaom.catsense;

import com.google.common.collect.Lists;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocksListener implements Listener {
    final CatSense plugin;
    String header;
    String headerCheck;
    Map<String, Object> translate = new HashMap<>();
    List<String> enterCommands;
    int maxCost;
    double taxRate;
    public LocksListener(CatSense plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        header = config.getString("locks.header", "locks");
        headerCheck = ChatColor.translateAlternateColorCodes('&', config.getString("locks.header-check", "&b&l收费门"));
        maxCost = config.getInt("locks.max-cost", 100000);
        taxRate = config.getDouble("locks.tax", 0.01D);
        enterCommands = config.getStringList("locks.enter-commands");
        translate.clear();
        for (String key : config.getConfigurationSection("locks.message").getKeys(false)) {
            translate.put(key, config.get("locks.message." + key));
        }
    }

    private String m(String key) {
        return translate.getOrDefault(key, "[404:locks.message." + key + "]").toString();
    }

    /**
     * 发送消息
     * @param player 玩家
     * @param key 语言键
     */
    @SafeVarargs
    private final void m(Player player, String key, Pair<String, String>... replacement) {
        String msg = m(key);
        for(Pair<String, String> replace : replacement) {
            msg = msg.replace(replace.getKey(), replace.getValue());
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getPrefix() + msg));
    }
    /**
     * 发送消息 (列表)
     * @param sender 接收者
     * @param key 语言键
     */
    @SafeVarargs
    protected final void n(CommandSender sender, String key, Pair<String, String>... replacement) {
        Object obj = translate.getOrDefault(key, Lists.newArrayList("[404:locks.message." + key + "]"));
        List<?> list = obj instanceof List ? (List<?>) obj : Lists.newArrayList("[404:locks.message." + key + "]");
        for (Object line : list) {
            String s = line.toString();
            for (Pair<String, String> replace : replacement){
                s = s.replace(replace.getKey(), replace.getValue());
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getPrefix() + s));
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().getType().name().contains("WALL_SIGN")
                && event.getLine(0).equals(header)) {
            Player player = event.getPlayer();
            // 获取贴着牌子的方块另一边是否有收费门牌子
            Block oppositeBlock = event.getBlock().getRelative(((org.bukkit.material.Sign) event.getBlock().getState().getData()).getFacing().getOppositeFace(), 2);
            if(oppositeBlock.getType().equals(Material.WALL_SIGN)) {
                BlockState sign = oppositeBlock.getState();
                if ((sign instanceof Sign) && ((Sign) sign).getLine(0).equalsIgnoreCase(headerCheck)) {
                    m(player, "error-create-both-side");
                    event.getBlock().breakNaturally();
                    return;
                }
            }
            int cost = -1;
            try {
                cost = Integer.parseInt(event.getLine(1));
            } catch (Throwable ignored) {
            }
            if (cost < 0 && cost > maxCost) {
                m(player, "error-create-wrong-cost", Pair.of("$maxCost", String.valueOf(maxCost)));
                event.getBlock().breakNaturally();
                return;
            }
            String rawExtraOptions = event.getLine(2);
            // 进 出 空 效
            boolean isEmptyInv = rawExtraOptions.contains("空");
            boolean isEmptyEffect = rawExtraOptions.contains("效");
            boolean canEnter = rawExtraOptions.contains("进");
            boolean canLeave = rawExtraOptions.contains("出");
            event.setLine(1, String.valueOf(cost));
            event.setLine(2, ChatColor.translateAlternateColorCodes('&', " &r"+(canEnter ?"&0&l":"&4&m")+"进&r"+(canLeave?"&0&l":"&4&m")+"出&r"+(isEmptyInv?"&0&l":"&4&m")+"空&r"+(isEmptyEffect?"&0&l":"&4&m")+"效&r "));
            event.setLine(3, player.getName());
            event.setLine(0, headerCheck);
            m(player, "created");
        }
    }


    private boolean isIronDoor(Block block) {
        return block != null && block.getType().name().contains("IRON_DOOR");
    }

    private boolean isLocksSign(Block baseBlock, Block block) {
        if (block == null) return false;
        BlockState sign = block.getState();

        if (sign instanceof Sign) {
            // 要求牌子贴在目标方块上
            // 即 目标方块指向牌子的方向 和 牌子面向的方向 相同
            if (baseBlock == null || baseBlock.getFace(block).equals(((org.bukkit.material.Sign) sign.getData()).getFacing()))
                return ((Sign) sign).getLine(0).equals(headerCheck);
        }
        return false;
    }

    private boolean isInventoryEmpty(Player player) {
        for(ItemStack i : player.getInventory().getContents()) {
            if (i != null && i.getAmount() > 0) return false;
        }
        return true;
    }

    private OfflinePlayer getOfflinePlayer(String name) {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if(player != null && player.getName() != null && player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        // 点铁门
        if (isIronDoor(block)) {
            BlockFace face = event.getBlockFace();
            Block temp = block.getRelative(BlockFace.UP, 1);
            // 始终选择铁门上面的方块
            if (isIronDoor(temp)) temp = temp.getRelative(BlockFace.UP, 1);
            // 获取对玩家而言是前面还是后面的牌子
            Block front = temp.getRelative(face, 1);
            Block back = temp.getRelative(face.getOppositeFace(), 1);
            boolean frontAccess = isLocksSign(temp, front);
            boolean backAccess = isLocksSign(temp, back);
            // 如果两边都有牌子
            if (frontAccess && backAccess) {
                m(player, "error-both-side");
                event.setCancelled(true);
                return;
            }
            Sign sign = (frontAccess ? (Sign) front.getState() : (backAccess ? (Sign) back.getState() : null));
            if (sign == null) return;
            event.setCancelled(true);
            OfflinePlayer owner = getOfflinePlayer(sign.getLine(3));
            if(owner==null){
                m(player, "error-no-owner");
                return;
            }
            int cost = -1;
            try {
                cost = Integer.parseInt(sign.getLine(1));
            } catch (Throwable ignored) {
            }
            if (cost < 0) {
                m(player, "error-wrong-cost");
                return;
            }

            double money = plugin.getEconomy().getBalance(player);
            if (money < cost) {
                m(player, "error-no-money");
                return;
            }
            String rawExtraOptions = sign.getLine(2);
            // 进 出 空 效
            // 收费门主人不受限制
            boolean isEmptyInv = !player.getName().equals(owner.getName()) && rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l空");
            boolean isEmptyEffect = !player.getName().equals(owner.getName()) && rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l效");
            boolean canEnter = player.getName().equals(owner.getName()) || rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l进");
            boolean canLeave = player.getName().equals(owner.getName()) || rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l出");
            if (isEmptyEffect && !player.getActivePotionEffects().isEmpty()) {
                m(player, "error-has-effect");
                return;
            }
            if (isEmptyInv && !isInventoryEmpty(player)) {
                m(player, "error-has-item");
                return;
            }
            if (frontAccess) {
                if (!canEnter) {
                    m(player, "error-can-not-enter");
                    return;
                }
            }
            else {
                if (!canLeave) {
                    m(player, "error-can-not-leave");
                    return;
                }
            }
            if (!player.isSneaking()) {
                m(player, "error-no-shift");
                return;
            }
            // 牌子贴着的方块往下走两格，再往前走一格
            Location target = temp.getRelative(BlockFace.DOWN, 2).getRelative(face.getOppositeFace(), 1).getLocation();
            target = new Location(target.getWorld(),
                    target.getX() + 0.5,
                    target.getY(),
                    target.getZ() + 0.5,
                    player.getLocation().getYaw(), player.getLocation().getPitch());
            // 主人不收费，但也会有收费命令执行
            if(cost > 0 && frontAccess && !player.getName().equals(owner.getName())) {
                plugin.getEconomy().withdrawPlayer(player, cost);
                plugin.getEconomy().depositPlayer(owner, cost * taxRate);
            }
            player.teleport(target);
            if (frontAccess) CatSense.runCommand(player, enterCommands, Pair.of("$cost", String.valueOf(cost)), Pair.of("$costWithTax", String.valueOf(cost * taxRate)), Pair.of("$owner", owner.getName()));
            return;
        }
        // 点牌子
        if (block.getType().name().contains("WALL_SIGN"))  {
            if (isLocksSign(null, block)) {
                Sign sign = (Sign) block.getState();

                OfflinePlayer owner = getOfflinePlayer(sign.getLine(3));
                String ownerStr = owner == null || owner.getName() == null ? m("error-value") : owner.getName();
                int cost = -1;
                try {
                    cost = Integer.parseInt(sign.getLine(1));
                } catch (Throwable ignored) {
                }
                String costStr = cost < 0 ? m("error-value") : String.valueOf(cost);
                String rawExtraOptions = sign.getLine(2);
                // 进 出 空 效
                String isEmptyInv = m("info-" + rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l空"));
                String isEmptyEffect = m("info-" + rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l效"));
                String canEnter = m("info-" + rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l进"));
                String canLeave = m("info-" + rawExtraOptions.contains(ChatColor.COLOR_CHAR + "l出"));

                n(player, "info", Pair.of("$owner", ownerStr), Pair.of("$cost", costStr),
                        Pair.of("$canEnter", canEnter), Pair.of("$canLeave", canLeave),
                        Pair.of("$noItem", isEmptyInv), Pair.of("$noEffect", isEmptyEffect));
            }
        }
    }
}
