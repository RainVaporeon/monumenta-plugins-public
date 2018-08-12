package com.playmonumenta.bossfights.bosses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.Arrays;

import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

import com.playmonumenta.bossfights.BossBarManager;
import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellBaseLaser;
import com.playmonumenta.bossfights.spells.SpellChangeFloor;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;

public class Virius extends Boss
{
	public static final String identityTag = "boss_virius";
	public static final int detectionRange = 50;

	LivingEntity mBoss;
	Location mSpawnLoc;
	Location mEndLoc;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		String content = SerializationUtils.retrieveDataFromEntity(boss);

		if (content == null || content.isEmpty())
			throw new Exception("Can't instantiate " + identityTag + " with no serialized data");

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(content, JsonObject.class);

		if (!(object.has("spawnX") && object.has("spawnY") && object.has("spawnZ") &&
		      object.has("endX") && object.has("endY") && object.has("endZ")))
			throw new Exception("Failed to instantiate " + identityTag + ": missing required data element");

		Location spawnLoc = new Location(boss.getWorld(), object.get("spawnX").getAsDouble(),
		                                 object.get("spawnY").getAsDouble(), object.get("spawnZ").getAsDouble());
		Location endLoc = new Location(boss.getWorld(), object.get("endX").getAsDouble(),
		                               object.get("endY").getAsDouble(), object.get("endZ").getAsDouble());

		return new Virius(plugin, boss, spawnLoc, endLoc);
	}

	public Virius(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc)
	{
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellChangeFloor(plugin, mBoss, detectionRange, 3, Material.MAGMA),
		                                                 new SpellBaseLaser(plugin, boss, detectionRange, 100, false, false,
				  // Tick action per player
				  (Player player, int ticks, boolean blocked) ->
					{
						player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, (0.5f + ((float)ticks / 80f) * 1.5f));
						boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, 2, (0.5f + ((float)ticks / 80f) * 1.5f));
						if (ticks == 0)
							boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 110, 4), true);
					},
				  // Particles generated by the laser
				  (Location loc) ->
					{
						loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0);
						loc.getWorld().spawnParticle(Particle.WATER_SPLASH, loc, 1, 0.04, 0.04, 0.04, 1);
					},
				  // Damage generated at the end of the attack
				  (Player player, Location loc, boolean blocked) ->
					{
						loc.getWorld().playSound(loc, Sound.ENTITY_ENDERDRAGON_FIREBALL_EXPLODE, 1f, 1.5f);
						loc.getWorld().spawnParticle(Particle.WATER_WAKE, loc, 300, 0.8, 0.8, 0.8, 0);
						if (!blocked)
							player.damage(16f);
					})));

		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BarColor.RED, BarStyle.SOLID, null);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, bossBar);
	}

	@Override
	public void init()
	{
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hp_del = 256;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);
	}

	@Override
	public void death()
	{
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public String serialize()
	{
		Gson gson = new GsonBuilder().create();
		JsonObject root = new JsonObject();

		root.addProperty("spawnX", mSpawnLoc.getX());
		root.addProperty("spawnY", mSpawnLoc.getY());
		root.addProperty("spawnZ", mSpawnLoc.getZ());
		root.addProperty("endX", mEndLoc.getX());
		root.addProperty("endY", mEndLoc.getY());
		root.addProperty("endZ", mEndLoc.getZ());

		return gson.toJson(root);
	}
}
