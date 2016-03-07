package me.choco.arrows.utils.arrows;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.choco.arrows.AlchemicalArrows;
import me.choco.arrows.api.AlchemicalArrow;

public class ConfusionArrow extends AlchemicalArrow{
	public ConfusionArrow(Arrow arrow) {
		super(arrow);
	}
	
	@Override
	public void displayParticle(Player player){
		//TODO
	}
	
	@Override
	public void onHitPlayer(Player player) {
		PotionEffect confusion = PotionEffectType.CONFUSION.createEffect(500, 0);
		player.addPotionEffect(confusion);
		player.teleport(new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw() + 180, player.getLocation().getPitch()));
	}
	
	@Override
	public void onHitEntity(Entity entity) {
		if (entity instanceof Creature){
			((Creature) entity).setTarget(null);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onShootFromPlayer(Player player) {
		if (!player.hasPermission("arrows.shoot.confusion")){
			AlchemicalArrows.getPlugin().getArrowRegistry().unregisterAlchemicalArrow(this);
		}
	}
	
	@Override
	public boolean allowInfinity() {
		return AlchemicalArrows.getPlugin().getConfig().getBoolean("Arrows.ConfusionArrow.AllowInfinity");
	}
	
	@Override
	public boolean skeletonsCanShoot() {
		return AlchemicalArrows.getPlugin().getConfig().getBoolean("Arrows.ConfusionArrow.SkeletonsCanShoot");
	}
}