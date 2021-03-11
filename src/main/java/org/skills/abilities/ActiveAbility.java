package org.skills.abilities;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.skills.api.events.SkillActiveStateChangeEvent;
import org.skills.data.managers.SkilledPlayer;
import org.skills.main.SkillsConfig;
import org.skills.main.locale.MessageHandler;
import org.skills.services.manager.ServiceHandler;

import java.util.List;

public abstract class ActiveAbility extends Ability {
    public boolean activateOnReady;

    public ActiveAbility(String skill, String name, boolean activateOnReady) {
        super(skill, name);
        this.activateOnReady = activateOnReady;
    }

    public String getAbilityReady(SkilledPlayer info) {
        return getExtra(info).getString("activation.messages.ready");
    }

    public String getAbilityIdle(SkilledPlayer info) {
        return getExtra(info).getString("activation.messages.idle");
    }

    public String getAbilityActivated(SkilledPlayer info) {
        return getExtra(info).getString("activation.messages.activated");
    }

    public String getAbilityFinished(SkilledPlayer info) {
        return getExtra(info).getString("activation.messages.finished");
    }

    public int getIdle(SkilledPlayer info) {
        return getExtra(info).getInt("activation.idle");
    }

    public boolean isAbilityReady(Player p) {
        return SkilledPlayer.getSkilledPlayer(p).isActiveReady();
    }

    public String getActivationKey(SkilledPlayer info) {
        return getExtra(info).getString("activation.key");
    }

    protected void useSkill(Player player) {
        activeCheckup(player);
    }

    public double getEnergy(SkilledPlayer info) {
        String energy = getExtra(info).getString("activation.energy");
        return super.getAbsoluteScaling(info, energy);
    }

    public boolean isWeaponAllowed(SkilledPlayer info, ItemStack item) {
        List<String> list = getExtra(info).getStringList("activation.items");
        if (list.isEmpty()) return true;
        return XMaterial.matchXMaterial(item).isOneOf(list);
    }

    public double getCooldown(SkilledPlayer info) {
        String cooldown = getExtra(info).getString("activation.cooldown");
        return super.getAbsoluteScaling(info, cooldown);
    }

    public void sendMessage(Player player, String message, Object... edits) {
        if (Strings.isNullOrEmpty(message)) return;
        String msg = MessageHandler.replaceVariables(ServiceHandler.translatePlaceholders(player, message), edits);
        MessageHandler.sendMessage(player, msg, SkillsConfig.PREFIX.getBoolean());
    }

    public SkilledPlayer activeCheckup(Player player) {
        SkilledPlayer info = super.checkup(player);
        if (info == null) return null;
        if (!info.isActiveReady(this)) return null;

        SkillActiveStateChangeEvent event = new SkillActiveStateChangeEvent(player, this, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;
        info.deactivateReady();

        info.setEnergy(info.getEnergy() - getEnergy(info));
        info.setCooldown((long) getCooldown(info) * 1000L);

        sendMessage(player, getAbilityActivated(info));
        return info;
    }
}