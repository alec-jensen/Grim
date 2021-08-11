package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.TransactionKnockbackData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentLinkedQueue;

// We are making a velocity sandwich between two pieces of transaction packets (bread)
public class KnockbackHandler {
    ConcurrentLinkedQueue<TransactionKnockbackData> firstBreadMap = new ConcurrentLinkedQueue<>();
    GrimPlayer player;

    ConcurrentLinkedQueue<VelocityData> lastKnockbackKnownTaken = new ConcurrentLinkedQueue<>();
    VelocityData firstBreadOnlyKnockback = null;

    public KnockbackHandler(GrimPlayer player) {
        this.player = player;
    }

    public void addPlayerKnockback(int entityID, int breadOne, Vector knockback) {
        double minimumMovement = 0.003D;
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_8))
            minimumMovement = 0.005D;

        if (Math.abs(knockback.getX()) < minimumMovement) {
            knockback.setX(0D);
        }

        if (Math.abs(knockback.getY()) < minimumMovement) {
            knockback.setY(0D);
        }

        if (Math.abs(knockback.getZ()) < minimumMovement) {
            knockback.setZ(0D);
        }

        firstBreadMap.add(new TransactionKnockbackData(breadOne, entityID, knockback));
    }

    public VelocityData getRequiredKB(int entityID, int transaction) {
        tickKnockback(transaction);

        VelocityData returnLastKB = null;
        for (VelocityData data : lastKnockbackKnownTaken) {
            if (data.entityID == entityID)
                returnLastKB = data;
        }

        lastKnockbackKnownTaken.clear();

        return returnLastKB;
    }

    private void tickKnockback(int transactionID) {
        TransactionKnockbackData data = firstBreadMap.peek();
        while (data != null) {
            if (data.transactionID == transactionID) { // First bread knockback
                firstBreadOnlyKnockback = new VelocityData(data.entityID, data.knockback);
                break; // All knockback after this will have not been applied
            } else if (data.transactionID < transactionID) { // This kb has 100% arrived to the player
                lastKnockbackKnownTaken.add(new VelocityData(data.entityID, data.knockback));
                firstBreadOnlyKnockback = null;
                firstBreadMap.poll();
                data = firstBreadMap.peek();
            } else { // We are too far ahead in the future
                break;
            }
        }
    }

    public void handlePlayerKb(double offset) {
        if (player.likelyKB == null && player.firstBreadKB == null) {
            return;
        }

        if (player.predictedVelocity.hasVectorType(VectorData.VectorType.Knockback)) {
            // Unsure knockback was taken
            if (player.firstBreadKB != null) {
                player.firstBreadKB.offset = Math.min(player.firstBreadKB.offset, offset);
            }

            // 100% known kb was taken
            if (player.likelyKB != null) {
                player.likelyKB.offset = Math.min(player.likelyKB.offset, offset);
            }
        }

        if (player.likelyKB != null) {
            ChatColor color = ChatColor.GREEN;
            if (player.likelyKB.offset > 0.05) {
                color = ChatColor.RED;
            }
            // Add offset to violations
            Bukkit.broadcastMessage(color + "Kb offset is " + player.likelyKB.offset);
        }
    }

    public VelocityData getFirstBreadOnlyKnockback(int entityID, int transaction) {
        tickKnockback(transaction);
        if (firstBreadOnlyKnockback != null && firstBreadOnlyKnockback.entityID == entityID)
            return firstBreadOnlyKnockback;
        return null;
    }
}
