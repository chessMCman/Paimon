package com.plr.paimon;

import com.plr.paimon.client.event.ClientForgeEventHandler;
import com.plr.paimon.client.event.ClientModEventHandler;
import com.plr.paimon.common.core.ConfigHandler;
import com.plr.paimon.common.core.EquipmentHandler;
import com.plr.paimon.common.core.ModSounds;
import com.plr.paimon.common.entities.ModEntities;
import com.plr.paimon.common.event.ForgeEventHandler;
import com.plr.paimon.common.items.ModItems;
import com.plr.paimon.common.tab.ModCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;


@Mod(Constants.MOD_ID)
public class Paimon {
    public static boolean curiosLoaded = false;

    public Paimon(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ConfigHandler.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.COMMON, ConfigHandler.COMMON_SPEC);
        bus.addListener(this::commonSetup);
        ModSounds.SOUNDS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModCreativeTabs.TABS.register(bus);

        // Game-bus event listeners (registered directly; @EventBusSubscriber is deprecated).
        NeoForge.EVENT_BUS.addListener(ForgeEventHandler::onFished);
        NeoForge.EVENT_BUS.addListener(ForgeEventHandler::onPlayerJoinWorld);

        // Client-only listeners (mod bus renderer registration + game-bus client tick).
        if (FMLEnvironment.dist.isClient()) {
            bus.addListener(ClientModEventHandler::registerModels);
            bus.addListener(ClientModEventHandler::registerLayerDefs);
            NeoForge.EVENT_BUS.addListener(ClientForgeEventHandler::clientTickEnd);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        curiosLoaded = ModList.get().isLoaded("curios");
        EquipmentHandler.init();
    }
}