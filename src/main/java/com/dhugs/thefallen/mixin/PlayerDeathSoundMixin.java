package com.dhugs.thefallen.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Vector3f;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;

import com.dhugs.NoteBlockAPI.model.RepeatMode;
import com.dhugs.NoteBlockAPI.model.Song;
import com.dhugs.NoteBlockAPI.player.EntitySongPlayer;
import com.dhugs.thefallen.TheFallenMod;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerDeathSoundMixin {

    @Shadow @Final public MinecraftServer server;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Vec3d calculateScreenUpVector(float pitch, float yaw) {
        // Handle edge cases first
        if (pitch >= 89.9f) {  // Looking straight down
            return Vec3d.fromPolar(0, yaw); // Use forward direction as "up"
        }
        if (pitch <= -89.9f) { // Looking straight up
            return Vec3d.fromPolar(0, yaw + 180); // Use backward direction as "up"
        }

        // Normal case: use cross product method
        Vec3d lookDir = Vec3d.fromPolar(pitch, yaw);
        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = lookDir.crossProduct(worldUp).normalize();
        return right.crossProduct(lookDir).normalize();
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        server.getPlayerManager().getPlayerList().forEach(p -> {
            Song song = TheFallenMod.getTheFallenSong();
            if (song == null) {
                return; // Skip if song failed to load
            }

            // Create and play the song using Nota
            EntitySongPlayer songPlayer = new EntitySongPlayer(song);
            songPlayer.setEntity(p);
            songPlayer.addPlayer(p);
            songPlayer.setRepeatMode(RepeatMode.NONE);
            songPlayer.setAutoDestroy(true);
            songPlayer.setPlaying(true);

            // Create player head item
            ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
            playerHead.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));

            // Calculate screen-space vectors
            Vec3d lookDir = Vec3d.fromPolar(p.getPitch(), p.getYaw());
            Vec3d screenUp = calculateScreenUpVector(p.getPitch(), p.getYaw());

            // Position calculation
            Vec3d eyePos = p.getEyePos();
            Vec3d targetPos = eyePos
                .add(lookDir.multiply(2.5))         // Push out further (was 2.0)
                .add(screenUp.multiply(1.0));       // Move up more (was 0.7)

            ItemDisplayEntity blockDisplay = new ItemDisplayEntity(EntityType.ITEM_DISPLAY, p.getWorld());
            blockDisplay.setItemStack(playerHead);
            blockDisplay.setPos(targetPos.x, targetPos.y, targetPos.z); // Offset down slightly to be at eye level
            blockDisplay.setNoGravity(true);
            blockDisplay.setCustomName(player.getName());
            blockDisplay.setCustomNameVisible(true);
            blockDisplay.setBrightness(new Brightness(15, 15)); // Set both block and sky brightness to max
            blockDisplay.setShadowRadius(0.0f);     // Disable shadow
            blockDisplay.setShadowStrength(0.0f);   // Disable shadow strength
            
            // Set scale transformation
            Vector3f scale = new Vector3f(0.5f, 0.5f, 0.5f);
            blockDisplay.setTransformation(new AffineTransformation(null, null, scale, null));

            // Calculate rotation to face player, then flip it around
            Vec3d toPlayer = eyePos.subtract(targetPos).normalize();
            float yaw = (float) Math.toDegrees(MathHelper.atan2(-toPlayer.z, -toPlayer.x)) - 90.0F;
            // Invert the pitch calculation
            float pitch = -(float) Math.toDegrees(MathHelper.atan2(toPlayer.y, Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z)));

            blockDisplay.setYaw(yaw);
            blockDisplay.setPitch(pitch);

            p.getWorld().spawnEntity(blockDisplay);
            TheFallenMod.trackDisplayEntity(blockDisplay);

            // Send destroy packet to all other players to hide the entity
            for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
                if (otherPlayer != p) {
                    otherPlayer.networkHandler.sendPacket(
                        new EntitiesDestroyS2CPacket(blockDisplay.getId())
                    );
                }
            }

            // Schedule the block to follow the player
            scheduler.scheduleAtFixedRate(() -> {
                server.execute(() -> {
                    Vec3d newLookDir = Vec3d.fromPolar(p.getPitch(), p.getYaw());
                    Vec3d newScreenUp = calculateScreenUpVector(p.getPitch(), p.getYaw());

                    Vec3d newEyePos = p.getEyePos();
                    Vec3d newTargetPos = newEyePos
                        .add(newLookDir.multiply(2.5))
                        .add(newScreenUp.multiply(1.0));
                    
                    blockDisplay.setPos(newTargetPos.x, newTargetPos.y, newTargetPos.z);

                    Vec3d newToPlayer = newEyePos.subtract(newTargetPos).normalize();
                    float newYaw = (float) Math.toDegrees(MathHelper.atan2(-newToPlayer.z, -newToPlayer.x)) - 90.0F;
                    float newPitch = (float) Math.toDegrees(MathHelper.atan2(newToPlayer.y, 
                        Math.sqrt(newToPlayer.x * newToPlayer.x + newToPlayer.z * newToPlayer.z)));

                    blockDisplay.setYaw(newYaw);
                    blockDisplay.setPitch(newPitch);
                });
            }, 0, 10, TimeUnit.MILLISECONDS);

            // Schedule entity removal on the main server thread
            scheduler.schedule(() -> {
                server.execute(() -> {
                    blockDisplay.remove(Entity.RemovalReason.DISCARDED);
                    TheFallenMod.untrackDisplayEntity(blockDisplay);
                });
            }, 35, TimeUnit.SECONDS);
        });
    }
}
