package com.plr.paimon.mixin;

import com.mojang.authlib.GameProfile;
import com.plr.paimon.common.api.IPaimonOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player implements IPaimonOwner {
    @Unique
    private UUID paimon$paimonUuid = null;
    @Unique
    private boolean paimon$rewardGained = false;

    public MixinServerPlayer(Level pLevel, BlockPos pPos, float pYRot, GameProfile pGameProfile) {
        super(pLevel, pPos, pYRot, pGameProfile);
    }

    @Override
    public void paimon$setPaimonUuid(UUID uuid) {
        this.paimon$paimonUuid = uuid;
    }

    @Override
    public UUID paimon$getPaimonUuid() {
        return paimon$paimonUuid;
    }

    @Override
    public void paimon$setRewardGained(boolean gained) {
        this.paimon$rewardGained = gained;
    }

    @Override
    public boolean paimon$rewardGained() {
        return paimon$rewardGained;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void inject$addAdditionalSaveData(CompoundTag pCompound, CallbackInfo ci) {
        if (paimon$paimonUuid != null) {
            pCompound.putUUID("paimon_uuid", paimon$paimonUuid);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void inject$readAdditionalSaveData(CompoundTag pCompound, CallbackInfo ci) {
        paimon$paimonUuid = pCompound.hasUUID("paimon_uuid") ? pCompound.getUUID("paimon_uuid") : null;
    }
}