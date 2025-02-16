package top.mrxiaom.catsense;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static top.mrxiaom.catsense.LocksListener.getFace;

public class ContainerListener implements Listener {
    public static class Holder implements InventoryHolder {
        private final Inventory inventory;
        public Holder(int size, String title) {
            inventory = Bukkit.createInventory(this, size, title);
        }

        @NotNull
        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public static boolean is(Inventory inv) {
            return inv != null && inv.getHolder() instanceof Holder;
        }
    }
    CatSense plugin;
    public static final BlockFace[] faces = new BlockFace[] { BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST };
    String signKey, title;
    public ContainerListener(CatSense plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig(){
        FileConfiguration config = plugin.getConfig();
        signKey = config.getString("container-preview-key", "查看容器");
        String rawTitle = config.getString("container-preview-title", null);
        title = ChatColor.translateAlternateColorCodes('&', rawTitle != null ? rawTitle : "&3&l查看容器");
    }

    public boolean hasCheckFlag(Block block) {
        if (block == null) return false;
        for (BlockFace face : faces) {
            Block temp = block.getRelative(face, 1);
            if (!temp.getType().name().contains("WALL_SIGN")) continue;
            Sign sign = (Sign) temp.getState();
            // 要求牌子贴在目标方块上
            // 即 目标方块指向牌子的方向 和 牌子面向的方向 相同
            BlockFace signFace = getFace(sign);
            if (face.equals(signFace)) {
                for (String line : sign.getLines()) {
                    if (line.equals(signKey)) return true;
                }
            }
        }
        return false;
    }
    private static final List<String> containers = Lists.newArrayList(
            "CHEST", "TRAPPED_CHEST", "DISPENSER", "DROPPER", "HOPPER", "BARREL"
    );
    public boolean isTargetContainer(Block block) {
        return containers.contains(block.getType().name());
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        // 点击牌子
        if (block.getType().name().contains("WALL_SIGN") &&
                (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || (player.isSneaking() && event.getAction().equals(Action.LEFT_CLICK_BLOCK)))) {
            Sign sign = (Sign) block.getState();
            // 获取牌子贴着的方块
            BlockFace face = getFace(sign);
            if (face == null) return;
            block = block.getRelative(face.getOppositeFace(), 1);
            if (!isTargetContainer(block)) return;
            for (String line : sign.getLines()) {
                if (line.equals(signKey)) {
                    event.setCancelled(true);
                    openInv(player, block);
                    break;
                }
            }
            return;
        }
        // 点击容器
        if (player.isSneaking() && event.getAction().equals(Action.LEFT_CLICK_BLOCK)
                && isTargetContainer(block)) {
            if (hasCheckFlag(block)) {
                event.setCancelled(true);
                openInv(player, block);
            }
        }
    }

    public void openInv(Player player, Block block) {
        BlockState state = block.getState();
        if(state instanceof Chest) {
            Inventory chest = ((Chest) state).getInventory();
            openInv(player, chest instanceof DoubleChestInventory ? 6 : 3, chest.getContents());
        }
        if(state instanceof Dispenser) {
            openInv(player, 1, ((Dispenser) state).getInventory().getContents());
        }
        if(state instanceof Dropper) {
            openInv(player, 1, ((Dropper) state).getInventory().getContents());
        }
        if (state instanceof Hopper) {
            openInv(player, 1, ((Hopper) state).getInventory().getContents());
        }
    }

    public void openInv(Player player, int row, ItemStack[] contents) {
        if (Holder.is(player.getOpenInventory().getTopInventory())) {
            return;
        }
        Inventory inv = new Holder(row * 9, title).getInventory();
        inv.setContents(contents);
        player.closeInventory();
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (Holder.is(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (Holder.is(event.getDestination())
        || Holder.is(event.getInitiator())
        || Holder.is(event.getSource())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (Holder.is(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
