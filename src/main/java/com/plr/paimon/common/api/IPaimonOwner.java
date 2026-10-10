package com.plr.paimon.common.api;

import java.util.UUID;

public interface IPaimonOwner {
    void paimon$setPaimonUuid(UUID uuid);

    UUID paimon$getPaimonUuid();

    boolean paimon$rewardGained();

    void paimon$setRewardGained(boolean gained);
}