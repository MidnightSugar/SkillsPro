package org.skills.abilities.arbalist;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.skills.abilities.ActiveAbility;
import org.skills.data.managers.SkilledPlayer;
import org.skills.main.SkillsPro;
import org.skills.utils.MathUtils;

import java.util.concurrent.ThreadLocalRandom;

public class ArbalistFireCrossbow extends ActiveAbility {
    public static final String ARBALIST_FIRECROSSBOW = "ARBALIST_CROSS";

    public ArbalistFireCrossbow() {
        super("Arbalist", "fire_crossbow", true);
        if (!XMaterial.supports(14)) Bukkit.getPluginManager().registerEvents(new Old(), SkillsPro.get());
    }

    @Override
    public void useSkill(Player p) {
        SkilledPlayer info = this.activeCheckup(p);
        if (info == null) return;

        Vector vector = p.getEyeLocation().getDirection();
        double extraScaling = getExtraScaling(info, "range");
        int kb = (int) getExtraScaling(info, "knockback");
        int fire = (int) getExtraScaling(info, "fire");
        vector.multiply(extraScaling);

        Arrow arrow = p.launchProjectile(Arrow.class, vector);
        arrow.setInvulnerable(true);
        arrow.setBounce(false);
        arrow.setFireTicks(fire);
        arrow.setKnockbackStrength(kb);
        arrow.setMetadata(ARBALIST_FIRECROSSBOW, new FixedMetadataValue(SkillsPro.get(), null));
        if (XMaterial.supports(14)) arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);


        int shotgunChance = (int) getExtraScaling(info, "shotgun.chance");
        if (MathUtils.hasChance(shotgunChance)) {
            int min = (int) getExtraScaling(info, "shotgun.amount.min");
            int max = (int) getExtraScaling(info, "shotgun.amount.max");
            double offset = (int) getExtraScaling(info, "shotgun.offset");
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < random.nextInt(min, max); i++) {
                Arrow extra = p.launchProjectile(Arrow.class, vector.clone().add(new Vector(
                        random.nextDouble(-offset, offset), random.nextDouble(-offset, offset), random.nextDouble(-offset, offset))));
                if (XMaterial.supports(14)) extra.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }
        }

        arrow.setGlowing(true);
        if (MathUtils.hasChance((int) getExtraScaling(info, "critical-chance"))) arrow.setCritical(true);
        XSound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR.play(p);

        p.spawnParticle(Particle.LAVA, p.getLocation(), (int) (extraScaling * 2) + 10, 0.1, 0.1, 0.1, 1);
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                i++;
                if (i > 4) cancel();
                if (arrow.isOnGround()) cancel();

                if (XMaterial.isNewVersion()) p.playNote(arrow.getLocation(), Instrument.CHIME, Note.natural(1, Note.Tone.values()[i]));
                p.spawnParticle(Particle.FLAME, arrow.getLocation(), (int) (extraScaling * 2), 0.01, 0.01, 0.01, 0.1);
            }
        }.runTaskTimer(SkillsPro.get(), 5L, 5L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onTargetHit(EntityDamageByEntityEvent event) {
        Entity arrow = event.getDamager();
        if (!(arrow instanceof Projectile)) return;
        if (!arrow.hasMetadata(ARBALIST_FIRECROSSBOW)) return;

        Player shooter = (Player) ((Projectile) arrow).getShooter();
        if (shooter == null) return;
        SkilledPlayer info = SkilledPlayer.getSkilledPlayer(shooter);

        event.setDamage((int) this.getScaling(info));
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(shooter, 2, 0);
        shooter.spawnParticle(Particle.LAVA, event.getEntity().getLocation(), 30, 0.1, 0.1, 0.1, 0.1);
    }

    @EventHandler
    public void onBlockHit(ProjectileHitEvent event) {
        Projectile arrow = event.getEntity();
        if (event.getHitBlock() == null) return;
        if (!arrow.hasMetadata(ARBALIST_FIRECROSSBOW)) return;
        arrow.removeMetadata(ARBALIST_FIRECROSSBOW, SkillsPro.get());

        Player shooter = (Player) arrow.getShooter();
        if (shooter == null) return;
        SkilledPlayer info = SkilledPlayer.getSkilledPlayer(shooter);
        if (MathUtils.hasChance((int) getExtraScaling(info, "explosion-chance"))) {
            TNTPrimed TNT = (TNTPrimed) shooter.getLocation().getWorld().spawnEntity(event.getHitBlock().getLocation(), EntityType.PRIMED_TNT);
            TNT.setFuseTicks(1);
        }
    }

    private static class Old implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onPlayerPickupItem(PlayerPickupArrowEvent event) {
            if (event.getItem().hasMetadata(ArbalistPassive.ARBALIST_ARROW)) event.setCancelled(true);
        }
    }
}