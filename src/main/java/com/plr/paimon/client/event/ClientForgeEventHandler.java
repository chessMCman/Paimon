package com.plr.paimon.client.event;


import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientForgeEventHandler {
    public static int ticksInGame = 0;

    public static void clientTickEnd(ClientTickEvent.Post event) {
        ticksInGame++;
    }
}