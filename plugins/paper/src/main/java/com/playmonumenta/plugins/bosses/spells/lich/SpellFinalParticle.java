package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellFinalParticle extends Spell {

	private Plugin mPlugin;
	private Location mCenter;
	private double mRange;
	private LivingEntity mBoss;
	private static final String WEAKNESS_SRC = "MiasmaWeakness";
	private static final String SLOWNESS_SRC = "MiasmaSlowness";
	private final int DEBUFF_DURATION = 20 * 15;
	private int mCylRadius = 8;
	private boolean mPTick = true;
	private List<Player> mWarned = new ArrayList<Player>();
	private FallingBlock mBlock;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<Player>();
	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f);
	private PartialParticle mBlack;
	private PPCircle mIndicator;

	public SpellFinalParticle(Plugin plugin, LivingEntity boss, Location loc, double range, FallingBlock block) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
		mBlock = block;
		mBlack = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0, BLACK);
		mIndicator = new PPCircle(Particle.REDSTONE, mCenter, mCylRadius).count(20).delta(0.1).data(BLACK);
	}

	@Override
	public void run() {
		mBlock.setTicksLived(1);
		World world = mBoss.getWorld();
		//smoke ring particle
		if (mPTick) {
			mPTick = false;
			for (double j = 0; j < 20; j += 1.5) {
				mIndicator.location(mCenter.clone().add(0, j, 0)).spawnAsBoss();
			}
		} else {
			mPTick = true;
		}

		//hurt players within the circle
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}
		for (Player p : mPlayers) {
			Location pGroundLoc = p.getLocation();
			pGroundLoc.setY(mCenter.getY());
			if (pGroundLoc.distance(mCenter) < 8) {
				if (!mWarned.contains(p)) {
					mWarned.add(p);
					p.sendMessage(Component.text("Looks like a dense cloud of miasma formed at the center of the arena.", NamedTextColor.AQUA));
				}
				BossUtils.bossDamagePercent(mBoss, p, 0.05, "Miasma");
				//death bloom nod >:3

				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, WEAKNESS_SRC,
					new PercentDamageDealt(DEBUFF_DURATION, -0.3));
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, SLOWNESS_SRC,
					new PercentSpeed(DEBUFF_DURATION, -0.15, SLOWNESS_SRC));
				p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, DEBUFF_DURATION, 2));
				p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, DEBUFF_DURATION, 2));
				p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 0));
			}
		}
		//kill all projectiles close to boss
		//prevent easy shooting of corner crystals
		Location ballLoc = mBoss.getLocation().add(0, 4.5, 0);
		BukkitRunnable run = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				// 1 tick overlap
				if (mT > 6 || Lich.bossDead()) {
					this.cancel();
				}
				Collection<AbstractArrow> projs = mBoss.getLocation().getNearbyEntitiesByType(AbstractArrow.class, 8);
				for (AbstractArrow proj : projs) {
					Location loc = proj.getLocation();
					Vector dir = LocationUtils.getDirectionTo(ballLoc, loc).multiply(0.5);
					double dist = proj.getLocation().distance(ballLoc);
					for (int k = 0; k < 40; k++) {
						Location pLoc = loc.clone().add(dir.multiply(k));
						if (pLoc.distance(loc) > dist) {
							break;
						}
						mBlack.location(pLoc).spawnAsBoss();
					}
					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1, 1);
					proj.remove();
				}
			}

		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
