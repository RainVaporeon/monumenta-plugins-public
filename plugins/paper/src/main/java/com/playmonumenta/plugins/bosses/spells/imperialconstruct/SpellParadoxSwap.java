package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.TemporalFlux;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellParadoxSwap extends Spell {

	private static final EnumSet<DamageEvent.DamageType> AFFECTED_TYPES = EnumSet.of(DamageEvent.DamageType.MELEE, DamageEvent.DamageType.MELEE_SKILL, DamageEvent.DamageType.MELEE_ENCH, DamageEvent.DamageType.PROJECTILE, DamageEvent.DamageType.PROJECTILE_SKILL, DamageEvent.DamageType.MAGIC);
	private static final int COOLDOWN = 20 * 5;
	private final int mRange;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private boolean mOnCooldown;

	public SpellParadoxSwap(Plugin mPlugin, int mRange, LivingEntity boss) {
		this.mPlugin = mPlugin;
		this.mRange = mRange;
		this.mBoss = boss;
		this.mOnCooldown = false;
	}

	@Override
	public void run() {
		mBoss.setGlowing(!mOnCooldown);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (mOnCooldown || !(damager instanceof Player player) || !AFFECTED_TYPES.contains(event.getType())) {
			return;
		}

		List<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);
		if (nearbyPlayers.size() <= 1) {
			return;
		}

		boolean playerWithoutBuff = nearbyPlayers.stream().anyMatch(p -> !com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.hasEffect(p, TemporalFlux.class));
		if (!playerWithoutBuff) {
			return;
		}

		Set<Effect> clearedEffects = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(player, TemporalFlux.GENERIC_NAME);
		if (clearedEffects != null && !clearedEffects.isEmpty()) {
			deployEffect(player);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void deployEffect(Player damager) {
		PlayerUtils.playersInRange(damager.getLocation(), 50, true).forEach(player -> player.sendMessage(Component.text("[Temporal Exchanger]", NamedTextColor.GOLD).append(Component.text(" TEMPORAL ANOMALY TRANSFERRED - ENTERING TEMPORARY ENERGY REGENERATION STATE", NamedTextColor.WHITE))));
		List<Player> nearbyPlayers = EntityUtils.getNearestPlayers(mBoss.getLocation(), mRange);
		Collections.reverse(nearbyPlayers);
		nearbyPlayers.remove(damager);
		for (Player p : nearbyPlayers) {
			if (p != damager &&
				    !com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.hasEffect(p, TemporalFlux.class)) {
				new PartialParticle(Particle.SOUL, mBoss.getLocation(), 20, 1, 1, 1).spawnAsEntityActive(mBoss);
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.HOSTILE, 30, 1);
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, TemporalFlux.GENERIC_NAME, new TemporalFlux(20 * 30));

				mOnCooldown = true;
				BukkitRunnable runnable = new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						if (mT >= COOLDOWN) {
							mOnCooldown = false;
							this.cancel();
						}
						mT += 1;
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runnable);
				return;
			}
		}
	}
}
