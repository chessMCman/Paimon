package com.plr.paimon.common.entities;

import com.plr.paimon.common.core.ConfigHandler;
import com.plr.paimon.common.core.EquipmentHandler;
import com.plr.paimon.common.core.ModSounds;
import com.plr.paimon.common.items.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EntityPaimon extends ThrowableProjectile {
    private static final int TP_SOUNDS = 4;
    private static final int RANDOM_SOUNDS = 14;
    private static final int VANISH_SOUNDS = 7;
    private static final int THANK_SOUNDS = 3;
    private static final String TAG_PITCH = "pitch";
    private static final String TAG_ROTATION = "rotation";
    private static final String TAG_OWNER_UUID = "owner_uuid";
    private static final String TAG_FOLLOWING = "following";
    private static final String TAG_ANIMATION = "animation";
    private static final String TAG_VOICECD = "voicecd";
    private static final String TAG_TPCD = "tpcd";
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> ROTATION = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> ANIMATION = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> VOICECD = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> TPCD = SynchedEntityData.defineId(EntityPaimon.class, EntityDataSerializers.INT);
    private int changeTicks;
    private final int MAX_CHANGE_TICKS;
    private int stayTicks;
    private int tooFarTicks;
    private int i;
    private final int MAX_ANIMATION_TICKS;

    public EntityPaimon(EntityType<? extends EntityPaimon> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);

        this.changeTicks = 0;
        this.MAX_CHANGE_TICKS = 8;
        this.stayTicks = 0;
        this.tooFarTicks = 0;
        this.i = 0;
        this.MAX_ANIMATION_TICKS = 20;
    }

    public EntityPaimon(Level worldIn, double x, double y, double z) {
        super(ModEntities.PAIMON.get(), worldIn);
        this.changeTicks = 0;
        this.MAX_CHANGE_TICKS = 8;
        this.stayTicks = 0;
        this.tooFarTicks = 0;
        this.i = 0;
        this.MAX_ANIMATION_TICKS = 20;
        setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        setAnimation(this.MAX_ANIMATION_TICKS);
        setVoiceCD(400);
        setTPCD(200);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        clearFire();
        // This entity is server-authoritative. Running the follow/movement logic on the client mirror
        // too makes it move independently at a slightly different rate than the server's copy, which
        // renders as two overlapping Paimons drifting apart. Drive the mirror purely by server sync.
        if (this.level().isClientSide) {
            return;
        }
        Player player = null;
        if (getAnimation() > 0) {
            setAnimation(getAnimation() - 1);
            if (getAnimation() <= this.MAX_ANIMATION_TICKS) {
                setDeltaMovement(0.0D, 0.08D, 0.0D);
                if (this.level() instanceof ServerLevel level) {
                    this.i += 30;
                    float r = 1.0F;
                    double x = getX() + r * Math.cos(Math.toRadians(this.i));
                    double y = getY() + ((this.i / 24.0) * 0.05);
                    double z = getZ() + r * Math.sin(Math.toRadians(this.i));
                    level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, .0, .0, .0, .0);
                }
                Vec3 v = position().add(getLookAngle().yRot((float) Math.toRadians(60.0D)));
                facePos(v.x, v.y, v.z);
            }

            return;
        }
        if (getVoiceCD() > 0)
            setVoiceCD(getVoiceCD() - 1);
        if (getTPCD() > 0) {
            setTPCD(getTPCD() - 1);
        }
        if (getOwnerUUID().isPresent()) {
            Player owner = this.level().getPlayerByUUID(getOwnerUUID().get());
            if (owner != null) {
                player = owner;
            }
        }

        if (player == null) {
            vanish();
            return;
        }
        // Vanish as soon as the owner is no longer wearing the Paimon Medal. This covers cases where
        // onUnequip cannot find the entity (e.g. a stale stored entity/id after re-logging), so Paimon
        // never keeps following a player who has taken the medal off.
        if (!this.level().isClientSide && EquipmentHandler.findOrEmpty(ModItems.paimonMedal.get(), player).isEmpty()) {
            vanish();
            return;
        }
        Vec3 playerPos = player.position();
        Vec3 lookVec = (new Vec3((player.getLookAngle()).x, 0.0D, (player.getLookAngle()).z)).normalize().yRot((float) Math.toRadians(30.0D)).reverse().scale(1.1D);
        final boolean edge = lookVec.x == .0D && lookVec.y == .0D;
        Vec3 targetPos = playerPos.add(lookVec.x, lookVec.y, lookVec.z).add(0.0D, edge ? 2.5D : 1.2D, 0.0D);

        if (player.isCrouching()) {
            if (player.getMainHandItem().has(DataComponents.FOOD)) {
                targetPos = playerPos.add((player.getLookAngle()).x, edge ? 2.5D : 1.2D, (player.getLookAngle()).z);
            } else if (player.getOffhandItem().has(DataComponents.FOOD)) {
                lookVec = lookVec.reverse();
                targetPos = playerPos.add(lookVec.x, lookVec.y, lookVec.z).add(0.0D, edge ? 2.5D : 1.2D, 0.0D);
            }
        }

        if (position().distanceTo(targetPos) >= 16.0D) {
            this.tooFarTicks++;
        } else {
            this.tooFarTicks = 0;
        }
        if (this.tooFarTicks >= 20) {
            this.tooFarTicks = 0;
            teleportTo(targetPos.x, targetPos.y, targetPos.z);
            setDeltaMovement(Vec3.ZERO);
            if (getTPCD() == 0) {
                if (!this.level().isClientSide)
                    randomTPSound(this.level().random.nextInt(4));
                setTPCD(400);
                setVoiceCD(getTPCD() + 300);
            }
            setAnimation(this.MAX_ANIMATION_TICKS);

            return;
        }
        this.i = 0;

        if (getVoiceCD() == 0) {
            if (!this.level().isClientSide)
                randomSound(this.level().random.nextInt(14));
            setVoiceCD((int) (ConfigHandler.COMMON.soundInterval.get() + Math.random() * 400.0D));
            setTPCD(getTPCD() + 200);
        }

        if (posEqual(position(), targetPos)) {
            this.stayTicks++;
        } else {
            this.stayTicks = 0;
        }
        if (this.changeTicks >= this.MAX_CHANGE_TICKS) {
            setFollowing(this.stayTicks < 8 || !posEqual(position(), targetPos));
            this.changeTicks = 0;
        }
        this.changeTicks++;

        // Smoothly ease toward the target. The old instant velocity change made Paimon jerk (and
        // visually smear/ghost) whenever the target flipped between in-front and behind as the owner
        // toggled sneak, so now we ease the velocity toward the desired motion and cap the turn rate.
        Vec3 diff = targetPos.subtract(position());
        double distance = diff.length();
        Vec3 desired = distance > 0.05 ? diff.scale(Math.min(distance * 0.25, 0.5) / distance) : Vec3.ZERO;
        Vec3 motion = getDeltaMovement().add(desired.subtract(getDeltaMovement()).scale(0.5));
        if (motion.lengthSqr() < 1.0E-4D) {
            motion = Vec3.ZERO;
        }
        setDeltaMovement(motion);
        if (motion.lengthSqr() > 0.0D) {
            faceEntity(player, 30.0F, 30.0F);
            if (this.tickCount % 12 == 0 && level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, getX() - motion.x, getY(), getZ() - motion.z, 1, -motion.x, -0.05D, -motion.z, .0);
            }
        }
    }

    public void vanish() {
        if (level() instanceof ServerLevel level) for (this.i = 0; this.i < 720; this.i += 24) {
            float r = 0.6F;
            double x = getX() + r * Math.cos(Math.toRadians(this.i));
            double y = getY() - 0.25D + ((this.i / 15.0) * 0.05);
            double z = getZ() + r * Math.sin(Math.toRadians(this.i));
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, .0, .0, .0, .0);
        }
        if (!this.level().isClientSide) {
            setVoiceCD(200);
            setTPCD(200);
            randomVanishSound(this.level().random.nextInt(7));
        }
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROTATION, 0.0F);
        builder.define(PITCH, 0.0F);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(FOLLOWING, false);
        builder.define(ANIMATION, 0);
        builder.define(VOICECD, 0);
        builder.define(TPCD, 0);
    }

    @Override
    public boolean isPickable() {
        // Only allow Paimon to be targeted when its owner is sneaking while holding food (i.e. trying to
        // feed it). Otherwise it stays non-pickable so it never intercepts the crosshair, which would
        // otherwise make Jade target it and block mining/block interaction.
        Player owner = getOwnerUUID().map(this.level()::getPlayerByUUID).orElse(null);
        if (owner != null) {
            return owner.isSecondaryUseActive()
                    && (owner.getMainHandItem().has(DataComponents.FOOD) || owner.getOffhandItem().has(DataComponents.FOOD));
        }
        return false;
    }


    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.has(DataComponents.FOOD)) {

                if (!this.level().isClientSide) {
                    randomThankSound(this.level().random.nextInt(3));
                    // Only push back the ambient-speech timer so the next idle voice line doesn't overlap
                    // the thank-you, but never gate the thank-you itself (feeding should always respond).
                    setVoiceCD(Math.max(getVoiceCD(), (int) (ConfigHandler.COMMON.soundInterval.get() * 0.5D)));
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                if (this.level() instanceof ServerLevel level) for (int i = 0; i < 5; i++) {
                    level.sendParticles(ParticleTypes.HEART, getX() - 0.25D + 0.5D * ThreadLocalRandom.current().nextDouble(), getY() + 0.5D + 0.30000001192092896D * Math.random(), getZ() - 0.25D + 0.5D * Math.random(), 1, 0.0D, 0.029999999329447746D, 0.0D, .0);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public float getSoundVolume() {
        return 1.0F;
    }

    /**
     * Play Paimon's voice lines in the {@code voice} sound category rather than the default
     * {@code neutral}. Immersive Engineering's ear defenders (and the earmuffs Curios slot) only
     * dampen {@code ambient / weather / record / block / neutral / hostile / player}, so a genuine
     * voice should never be muffled by wearing earmuffs.
     */
    @Override
    public SoundSource getSoundSource() {
        return SoundSource.VOICE;
    }

    public void randomThankSound(int i) {
        switch (i) {
            case 0 -> playSound(ModSounds.paimon_thank_0.get(), getSoundVolume(), 1.0F);
            case 1 -> playSound(ModSounds.paimon_thank_1.get(), getSoundVolume(), 1.0F);
            case 2 -> playSound(ModSounds.paimon_thank_2.get(), getSoundVolume(), 1.0F);
        }
    }

    public void randomVanishSound(int i) {
        switch (i) {
            case 0 -> playSound(ModSounds.paimon_vanish_0.get(), getSoundVolume(), 1.0F);
            case 1 -> playSound(ModSounds.paimon_vanish_1.get(), getSoundVolume(), 1.0F);
            case 2 -> playSound(ModSounds.paimon_vanish_2.get(), getSoundVolume(), 1.0F);
            case 3 -> playSound(ModSounds.paimon_vanish_3.get(), getSoundVolume(), 1.0F);
            case 4 -> playSound(ModSounds.paimon_vanish_4.get(), getSoundVolume(), 1.0F);
            case 5 -> playSound(ModSounds.paimon_vanish_5.get(), getSoundVolume(), 1.0F);
            case 6 -> playSound(ModSounds.paimon_vanish_6.get(), getSoundVolume(), 1.0F);
        }
    }

    public void randomTPSound(int i) {
        switch (i) {
            case 0 -> playSound(ModSounds.paimon_tp_0.get(), getSoundVolume(), 1.0F);
            case 1 -> playSound(ModSounds.paimon_tp_1.get(), getSoundVolume(), 1.0F);
            case 2 -> playSound(ModSounds.paimon_tp_2.get(), getSoundVolume(), 1.0F);
            case 3 -> playSound(ModSounds.paimon_tp_3.get(), getSoundVolume(), 1.0F);
        }
    }

    public void randomSound(int i) {
        switch (i) {
            case 1 -> playSound(ModSounds.paimon_1.get(), getSoundVolume(), 1.0F);
            case 2 -> playSound(ModSounds.paimon_2.get(), getSoundVolume(), 1.0F);
            case 3 -> playSound(ModSounds.paimon_3.get(), getSoundVolume(), 1.0F);
            case 4 -> playSound(ModSounds.paimon_4.get(), getSoundVolume(), 1.0F);
            case 5 -> playSound(ModSounds.paimon_5.get(), getSoundVolume(), 1.0F);
            case 6 -> playSound(ModSounds.paimon_6.get(), getSoundVolume(), 1.0F);
            case 7 -> playSound(ModSounds.paimon_7.get(), getSoundVolume(), 1.0F);
            case 8 -> playSound(ModSounds.paimon_8.get(), getSoundVolume(), 1.0F);
            case 9 -> playSound(ModSounds.paimon_9.get(), getSoundVolume(), 1.0F);
            case 10 -> playSound(ModSounds.paimon_10.get(), getSoundVolume(), 1.0F);
            case 11 -> playSound(ModSounds.paimon_11.get(), getSoundVolume(), 1.0F);
            case 12 -> playSound(ModSounds.paimon_12.get(), getSoundVolume(), 1.0F);
            case 13 -> playSound(ModSounds.paimon_13.get(), getSoundVolume(), 1.0F);
        }
    }


    protected void updateRotation() {
    }


    public void facePos(double x, double y, double z) {
        double d0 = x - getX();
        double d2 = z - getZ();

        double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));
        float f = (float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        float f1 = (float) -(Mth.atan2(y, d3) * 57.2957763671875D);
        this.setXRot(updateRotation(this.getXRot(), f1, 360.0F));
        this.setYRot(updateRotation(this.getYRot(), f, 360.0F));
    }

    public void faceEntity(Entity entityIn, float maxYawIncrease, float maxPitchIncrease) {
        double d1, d0 = entityIn.getX() - getX();
        double d2 = entityIn.getZ() - getZ();

        if (entityIn instanceof LivingEntity livingentity) {
            d1 = livingentity.getEyeY() - getEyeY();
        } else {
            d1 = ((entityIn.getBoundingBox()).minY + (entityIn.getBoundingBox()).maxY) / 2.0D - getEyeY();
        }

        double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));
        float f = (float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        float f1 = (float) -(Mth.atan2(d1, d3) * 57.2957763671875D);
        this.setXRot(updateRotation(this.getXRot(), f1, maxPitchIncrease));
        this.setYRot(updateRotation(this.getYRot(), f, maxYawIncrease));
    }

    private float updateRotation(float angle, float targetAngle, float maxIncrease) {
        float f = Mth.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        return angle + f;
    }

    public boolean posEqual(Vec3 v1, Vec3 v2) {
        return (v1.distanceTo(v2) <= 0.25D);
    }


    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("owner_uuid")) {
            setOwnerUUID(compound.getUUID("owner_uuid"));
        }
        setRotation(compound.getFloat("rotation"));
        setPitch(compound.getFloat("pitch"));
        setFollowing(compound.getBoolean("following"));
        setAnimation(compound.getInt("animation"));
        setVoiceCD(compound.getInt("voicecd"));
        setTPCD(compound.getInt("tpcd"));
    }


    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        if (getOwnerUUID().isPresent()) {
            compound.putUUID("owner_uuid", getOwnerUUID().get());
        }
        compound.putFloat("rotation", getRotation());
        compound.putFloat("pitch", getPitch());
        compound.putBoolean("following", getFollowing());
        compound.putInt("animation", getAnimation());
        compound.putInt("voicecd", getVoiceCD());
        compound.putInt("tpcd", getTPCD());
    }

    public void setAnimation(int i) {
        this.entityData.set(ANIMATION, i);
    }

    public int getAnimation() {
        return this.entityData.get(ANIMATION);
    }

    public void setVoiceCD(int i) {
        this.entityData.set(VOICECD, i);
    }

    public int getVoiceCD() {
        return this.entityData.get(VOICECD);
    }

    public void setTPCD(int i) {
        this.entityData.set(TPCD, i);
    }

    public int getTPCD() {
        return this.entityData.get(TPCD);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public float getRotation() {
        return this.entityData.get(ROTATION);
    }

    public void setRotation(float rot) {
        this.entityData.set(ROTATION, rot);
    }

    public float getPitch() {
        return this.entityData.get(PITCH);
    }

    public void setPitch(float rot) {
        this.entityData.set(PITCH, rot);
    }

    public boolean getFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }
}