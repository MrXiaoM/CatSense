package top.mrxiaom.catsense;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.inventory.ItemStack;

public class ContainerListener implements Listener {
    CatSense plugin;
    public static final BlockFace[] faces = new BlockFace[] { BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST };
    String signKey;
    String title = "§1§1§4§3§l容器查看";
    public ContainerListener(CatSense plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig(){
        FileConfiguration config = plugin.getConfig();
        signKey = config.getString("container-preview-key", "查看容器");
    }

    public boolean hasCheckFlag(Block block) {
        if (block == null) return false;
        for (BlockFace face : faces) {
            Block temp = block.getRelative(face, 1);
            if (!temp.getType().equals(Material.WALL_SIGN)) continue;
            Sign sign = (Sign) temp.getState();
            // 要求牌子贴在目标方块上
            // 即 目标方块指向牌子的方向 和 牌子面向的方向 相同
            BlockFace signFace = ((org.bukkit.material.Sign) sign.getData()).getFacing();
            if (signFace.equals(face)) {
                for (String line : sign.getLines()) {
                    if (line.equals(signKey)) return true;
                }
            }
        }
        return false;
    }

    public boolean isTargetContainer(Block block) {
        return block.getType().equals(Material.CHEST) || block.getType().equals(Material.TRAPPED_CHEST)
                || block.getType().equals(Material.DISPENSER) || block.getType().equals(Material.DROPPER)
                || block.getType().equals(Material.HOPPER);
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        // 点击牌子
        if (block.getType().equals(Material.WALL_SIGN) &&
                (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || (player.isSneaking() && event.getAction().equals(Action.LEFT_CLICK_BLOCK)))) {
            Sign sign = (Sign) block.getState();
            // 获取牌子贴着的方块
            block = block.getRelative(((org.bukkit.material.Sign) sign.getData()).getFacing().getOppositeFace(), 1);
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
        Inventory inv = Bukkit.createInventory(null, row * 9, title);
        inv.setContents(contents);
        player.closeInventory();
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(title)) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getDestination().getTitle().equalsIgnoreCase(title)
        || event.getInitiator().getTitle().equalsIgnoreCase(title)
        || event.getSource().getTitle().equalsIgnoreCase(title)) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(title)) event.setCancelled(true);
    }
}
