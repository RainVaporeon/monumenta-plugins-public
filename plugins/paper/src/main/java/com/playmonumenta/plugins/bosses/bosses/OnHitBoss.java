package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class OnHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_onhit";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public boolean CAN_BLOCK = true;

		@BossParam(help = "Effects apply to the player when got hit by the boss")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particle & Sounds!
		@BossParam(help = "Particle summoned when the player got hit by the boss")
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,20,0,0,0,1,#ffffff,2)]");

		@BossParam(help = "Sound played when the player got hit by the boss")
		public SoundsList SOUND = SoundsList.fromString("[(BLOCK_PORTAL_TRIGGER,0.25,2)]");

		@BossParam(help = "Executes a Command as the CONSOLE when player gets hit.")
		public String COMMAND_AS_BOSS = "";

		@BossParam(help = "Executes a Command as the Player")
		public String COMMAND_AS_PLAYER = "";

		@BossParam(help = "if set, makes boss_onhit only trigger when the spell with this name deals damage")
		public String SPELL_NAME = "";
	}

	private final Parameters mParams;

	public OnHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		//this boss has no ability
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mParams.CAN_BLOCK && event.isBlockedByShield()) {
			// Attack was blocked
			return;
		}

		if (!mParams.SPELL_NAME.equals("") && !mParams.SPELL_NAME.equals(event.getBossSpellName())) {
			// If it isn't the spell that we want, don't trigger anything
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!event.isCancelled()) {
					execute(damagee);
				}
			}
		}.runTask(mPlugin);
	}

	private void execute(LivingEntity damagee) {
		Location loc = damagee.getLocation().add(0, 1, 0);
		mParams.EFFECTS.apply(damagee, mBoss);
		if (!mParams.COMMAND_AS_BOSS.equals("")) {
			try {
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(
					"execute as " + mBoss.getUniqueId() + " at @s run " + mParams.COMMAND_AS_BOSS);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!mParams.COMMAND_AS_PLAYER.equals("")) {
			try {
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(
					"execute as " + damagee.getUniqueId() + " at @s run " + mParams.COMMAND_AS_PLAYER);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Particle & Sound
		mParams.SOUND.play(loc);
		mParams.PARTICLE.spawn(mBoss, loc, 0d, 0d, 0d);
	}
}
