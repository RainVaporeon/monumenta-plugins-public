package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class InfernoDamage extends Effect {
	public static final String effectID = "InfernoDamage";

	private final int mLevel;
	private final @Nullable Player mPlayer;
	private final @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;

	public InfernoDamage(int duration, int level, @Nullable Player player, @Nullable ItemStatManager.PlayerItemStats playerItemStats) {
		super(duration, effectID);
		mLevel = level;
		mPlayer = player;
		mPlayerItemStats = playerItemStats;
	}

	public InfernoDamage(int duration, int level, @Nullable Player player) {
		this(duration, level, player, player == null ? null : Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(player));
	}

	@Override
	public double getMagnitude() {
		return mLevel;
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof LivingEntity le) {
			double damage = Math.pow(mLevel, 0.95);
			if (mPlayer != null) {
				damage = CharmManager.calculateFlatAndPercentValue(mPlayer, Inferno.CHARM_DAMAGE, damage);
			}
			DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageType.AILMENT, ClassAbility.INFERNO, mPlayerItemStats), damage, true, false, false);
			new PartialParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05).spawnAsEnemyBuff();
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("level", mLevel);
		if (mPlayer != null) {
			object.addProperty("player", mPlayer.getUniqueId().toString());
		}

		return object;
	}

	public static InfernoDamage deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int level = object.get("level").getAsInt();

		@Nullable Player player = null;
		if (object.has("player")) {
			player = plugin.getPlayer(UUID.fromString(object.get("player").getAsString()));
		}

		return new InfernoDamage(duration, level, player);
	}

	@Override
	public String toString() {
		return String.format("Inferno duration:%d modifier:%s level:%d", this.getDuration(), "CustomDamageOverTime", mLevel);
	}

}
