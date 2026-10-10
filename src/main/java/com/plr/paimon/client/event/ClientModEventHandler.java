package com.plr.paimon.client.event;

import com.plr.paimon.client.model.ModelPaimon;
import com.plr.paimon.client.renderer.RenderPaimon;
import com.plr.paimon.common.entities.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ClientModEventHandler {
    public static void registerModels(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerEntityRenderer(ModEntities.PAIMON.get(), RenderPaimon::new);
    }

    public static void registerLayerDefs(EntityRenderersEvent.RegisterLayerDefinitions evt) {
        evt.registerLayerDefinition(ModelPaimon.MODEL_LAYER_LOCATION, ModelPaimon::createBodyLayer);
    }
}