package wtf.choco.arrows.listeners;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.Iterables;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

import wtf.choco.arrows.AlchemicalArrows;
import wtf.choco.arrows.api.AlchemicalArrow;
import wtf.choco.arrows.api.AlchemicalArrowEntity;
import wtf.choco.arrows.api.event.AlchemicalArrowShootEvent;
import wtf.choco.arrows.api.property.ArrowProperty;
import wtf.choco.arrows.registry.ArrowRegistry;
import wtf.choco.arrows.registry.ArrowStateManager;

public final class ProjectileShootListener implements Listener {

    private static final Random RANDOM = new Random();

    private final FileConfiguration config;
    private final ArrowRegistry arrowRegistry;
    private final ArrowStateManager stateManager;

    public ProjectileShootListener(@NotNull AlchemicalArrows plugin) {
        this.config = plugin.getConfig();
        this.arrowRegistry = plugin.getArrowRegistry();
        this.stateManager = plugin.getArrowStateManager();
    }

    @EventHandler
    public void onShootArrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();
        ProjectileSource source = arrow.getShooter();

        if (source instanceof Player) {
            Player player = (Player) source;
            PlayerInventory inventory = player.getInventory();
            if (!containsArrow(inventory)) return;

            // Register the arrow if it's in the arrow registry
            int arrowSlot = (isShotFromMainHand(player) ? findFirstArrow(inventory) : inventory.getHeldItemSlot());
            ItemStack arrowItem = inventory.getItem(arrowSlot);

            if (arrowItem == null || ArrowRegistry.ARROW_MATERIALS.contains(arrowItem.getType())) {
                arrowItem = inventory.getItem(arrowSlot = findFirstArrow(inventory));

                if (arrowItem == null) { // If the arrow is STILL null, just give up
                    return;
                }
            }

            AlchemicalArrow type = arrowRegistry.get(arrowItem);
            if (type == null) return;

            AlchemicalArrowEntity alchemicalArrow = type.createNewArrow(arrow);
            type.shootEventHandler(alchemicalArrow, event);
            if (!type.onShootFromPlayer(alchemicalArrow, player)) {
                event.setCancelled(true);
                return;
            }

            // Handle infinity
            ItemStack mainHand = inventory.getItemInMainHand(), offHand = inventory.getItemInOffHand();
            if ((mainHand != null && mainHand.containsEnchantment(Enchantment.ARROW_INFINITE))
                || (offHand != null && offHand.containsEnchantment(Enchantment.ARROW_INFINITE))) {

                if (!type.getProperties().getProperty(ArrowProperty.ALLOW_INFINITY).orElse(true) && player.getGameMode() != GameMode.CREATIVE) {
                    if (arrowItem.getAmount() > 1) {
                        arrowItem.setAmount(arrowItem.getAmount() - 1);
                        inventory.setItem(arrowSlot, arrowItem);
                    } else {
                        inventory.setItem(arrowSlot, null);
                    }
                } else {
                    arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

                    // Special condition for non-regular arrows
                    if (arrowItem.getType() != Material.ARROW) {
                        inventory.setItem(arrowSlot, arrowItem); // Keep arrow count the same
                    }
                }
            }

            AlchemicalArrowShootEvent aasEvent = new AlchemicalArrowShootEvent(alchemicalArrow, source);
            Bukkit.getPluginManager().callEvent(aasEvent);
            if (aasEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            this.stateManager.add(alchemicalArrow);
        }

        else if (source instanceof Skeleton && RANDOM.nextInt(100) < config.getDouble("Skeletons.ShootPercentage", 10.0)) {
            Collection<AlchemicalArrow> arrows = arrowRegistry.getRegisteredArrows();
            AlchemicalArrow type = Iterables.get(arrows, RANDOM.nextInt(arrows.size()));
            if (type == null || type.getProperties().getProperty(ArrowProperty.SKELETONS_CAN_SHOOT).orElse(false)) return;

            AlchemicalArrowEntity alchemicalArrow = type.createNewArrow(arrow);
            if (!type.onShootFromSkeleton(alchemicalArrow, (Skeleton) source)) {
                event.setCancelled(true);
                return;
            }

            AlchemicalArrowShootEvent aasEvent = new AlchemicalArrowShootEvent(alchemicalArrow, source);
            Bukkit.getPluginManager().callEvent(aasEvent);
            if (aasEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            this.stateManager.add(alchemicalArrow);
        }

        else if (arrow.getShooter() instanceof BlockProjectileSource) {
//            TODO:
//            BlockProjectileSource bps = (BlockProjectileSource) arrow.getShooter();
//            plugin.getArrowRegistry().getAlchemicalArrow(arrow).onShootFromBlockSource(bps);
        }
    }

    private boolean isShotFromMainHand(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();

        return ((mainHand != null && ArrowRegistry.BOW_MATERIALS.contains(mainHand.getType())) ||
                (mainHand == null && offHand != null && ArrowRegistry.BOW_MATERIALS.contains(offHand.getType())));
    }

    private boolean containsArrow(PlayerInventory inventory) {
    	return ArrowRegistry.ARROW_MATERIALS.stream().anyMatch(inventory::contains);
    }

    private int findFirstArrow(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && ArrowRegistry.ARROW_MATERIALS.contains(item.getType())) {
            	return i;
            }
        }

        return -1;
    }

}