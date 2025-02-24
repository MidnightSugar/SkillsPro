package org.skills.managers;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.ParticleDisplay;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.skills.data.managers.SkilledPlayer;
import org.skills.main.SkillsConfig;
import org.skills.main.SkillsPro;
import org.skills.main.locale.SkillsLang;
import org.skills.services.manager.ServiceHandler;
import org.skills.utils.Cooldown;
import org.skills.utils.MathUtils;
import org.skills.utils.NoEpochDate;
import org.skills.utils.StringUtils;
import org.skills.utils.nbt.ItemNBT;
import org.skills.utils.nbt.NBTType;
import org.skills.utils.nbt.NBTWrappers;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SkillItemManager implements Listener {
    public static final String SKILL_ITEM = "Skill_Item";
    private static final String XP = "SKILLS_XP";

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void proj(ProjectileLaunchEvent event) {
        Projectile bottle = event.getEntity();
        if (bottle.getType() != EntityType.THROWN_EXP_BOTTLE) return;

        if (!(bottle.getShooter() instanceof Player)) return;
        Player player = (Player) bottle.getShooter();

        NBTWrappers.NBTTagCompound nbt = ItemNBT.getTag(player.getItemInHand());
        String tag = nbt.get(SKILL_ITEM, NBTType.STRING);
        if (tag == null || !tag.startsWith("XP:")) return;
        String amt = tag.substring(3);
        bottle.setMetadata(XP, new FixedMetadataValue(SkillsPro.get(), amt));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        NBTWrappers.NBTTagCompound nbt = ItemNBT.getTag(item);
        String tag = nbt.get(SKILL_ITEM, NBTType.STRING);
        if (tag == null || !tag.startsWith("ENERGY_")) return;

        Player player = event.getPlayer();
        SkilledPlayer info = SkilledPlayer.getSkilledPlayer(player);
        boolean pot = tag.startsWith("ENERGY_POTION");
        String boost = StringUtils.deleteWhitespace(pot ? tag.substring(14) : tag.substring(15));

        if (pot) {
            int addition = (int) MathUtils.evaluateEquation(ServiceHandler.translatePlaceholders(player, boost));
            info.chargeEnergy(addition);
            ParticleDisplay display = ParticleDisplay.simple(player.getLocation(), Particle.SPELL_WITCH);
            display.count = 50;
            display.offset(0.5, 0.5, 0.5).spawn();
        } else {
            String[] split = StringUtils.split(boost, ',');
            int addition = (int) MathUtils.evaluateEquation(ServiceHandler.translatePlaceholders(player, split[0]));
            long time = Long.parseLong(split[1]);
            info.setEnergyBooster(addition);
            SkillsLang.SKILLS_ITEM_ENERGY_BOOSTER.sendMessage(player, "%booster%", addition, "%time%",
                    new NoEpochDate(time, TimeUnit.SECONDS).format(SkillsConfig.TIME_FORMAT.getString()));
            new Cooldown(player.getUniqueId(), "ENERGY_BOOSTER", time, TimeUnit.SECONDS);
            XSound.BLOCK_BEACON_ACTIVATE.play(player);
        }
    }

    @EventHandler
    public void onHit(ExpBottleEvent event) {
        Projectile bottle = event.getEntity();
        List<MetadataValue> meta = bottle.getMetadata(XP);
        if (meta.isEmpty()) return;
        event.setExperience(0);

        Player player = (Player) bottle.getShooter();
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);

        String amt = meta.get(0).asString();
        SkilledPlayer info = SkilledPlayer.getSkilledPlayer(player);
        int xp = (int) MathUtils.evaluateEquation(ServiceHandler.translatePlaceholders(player, amt));
        info.addXP(xp);
    }
}
