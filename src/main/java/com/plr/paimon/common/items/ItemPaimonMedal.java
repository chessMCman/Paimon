package com.plr.paimon.common.items;

import com.plr.paimon.common.api.IPaimonOwner;
import com.plr.paimon.common.core.ModSounds;
import com.plr.paimon.common.entities.EntityPaimon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class ItemPaimonMedal extends Item implements ICurioItem {

    public ItemPaimonMedal() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1).setNoRepair());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("paimon.info.paimon_medal").withStyle(ChatFormatting.ITALIC));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide() || player.level().getGameTime() % 20 != 0) return;
        if (player.getCooldowns().isOnCooldown(this)) return;
        final IPaimonOwner owner = (IPaimonOwner) player;
        // Never spawn while a live Paimon already belongs to this player. Match on the *owner* uuid
        // rather than the single tracked entity uuid: after a teleport the tracked uuid can point at a
        // copy left behind in an unloaded chunk / another dimension while the real Paimon is right
        // here. When we find it, re-sync the tracked uuid and skip spawning.
        EntityPaimon existing = findOwnedPaimon(player);
        if (existing != null) {
            if (!existing.getUUID().equals(owner.paimon$getPaimonUuid())) {
                owner.paimon$setPaimonUuid(existing.getUUID());
            }
            return;
        }
        Vec3 lookVec = player.getLookAngle().normalize().scale(1.5D);
        // Spawn slightly behind the player so Paimon doesn't pop up right in front of them.
        Vec3 spawnPoint = player.position().add(-lookVec.x, 1.0D, -lookVec.z);
        EntityPaimon paimon = new EntityPaimon(player.level(), spawnPoint.x, spawnPoint.y, spawnPoint.z);
        paimon.setOwnerUUID(player.getUUID());
        paimon.faceEntity(player, 360.0F, 360.0F);
        player.level().addFreshEntity(paimon);
        randomSpawnSound(paimon, player.level().random.nextInt(2));
        player.getCooldowns().addCooldown(this, 100);
        owner.paimon$setPaimonUuid(paimon.getUUID());
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (!(slotContext.entity() instanceof ServerPlayer player)) return;
        final Level level = player.level();
        if (level.isClientSide()) return;
        final IPaimonOwner owner = (IPaimonOwner) player;
        for (EntityPaimon paimon : findOwnedPaimons(player)) {
            paimon.vanish();
        }
        // Always clear the tracked uuid, even when no entity could be found (it may be in an
        // unloaded chunk; the entity itself will vanish on its next tick once it loads back).
        owner.paimon$setPaimonUuid(null);
    }

    private static List<EntityPaimon> findOwnedPaimons(ServerPlayer player) {
        final UUID ownerUuid = player.getUUID();
        return player.level().getEntitiesOfClass(EntityPaimon.class, player.getBoundingBox().inflate(64.0),
                e -> e.getOwnerUUID().isPresent() && e.getOwnerUUID().get().equals(ownerUuid));
    }

    private static EntityPaimon findOwnedPaimon(ServerPlayer player) {
        for (EntityPaimon paimon : findOwnedPaimons(player)) {
            return paimon;
        }
        return null;
    }

    public float getSoundVolume() {
        return 1.0F;
    }

    public void randomSpawnSound(Entity entity, int i) {
        switch (i) {
            case 0 -> entity.playSound(ModSounds.paimon_spawn_0.get(), getSoundVolume(), 1.0F);
            case 1 -> entity.playSound(ModSounds.paimon_0.get(), getSoundVolume(), 1.0F);
        }
    }

    @NotNull
    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}