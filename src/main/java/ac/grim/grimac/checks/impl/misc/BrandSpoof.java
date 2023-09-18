package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BrandSpoof extends Check implements PacketCheck {
    boolean alerted = false;
    public BrandSpoof(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (this.alerted) return;
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alert-brand-spoof", true)) {
            if (this.player.isLikelySpoofingBrand()) {
                String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("brand-spoof-alert", "%prefix% &f%player% &7is likely spoofing their brand (claimed %brand_raw%)");
                message = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(getPlayer(), message, true);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("grim.brand") || player.hasPermission("grim.alerts")) {
                        player.sendMessage(message);
                    }
                }

                this.alerted = true;
            }
        }
    }
}
