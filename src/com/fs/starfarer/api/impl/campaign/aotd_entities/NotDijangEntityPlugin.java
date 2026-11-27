package com.fs.starfarer.api.impl.campaign.aotd_entities;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.kaysaar.aotd.vok.misc.AoTDMisc;

public class NotDijangEntityPlugin extends BaseCustomEntityPlugin {
    private transient boolean shadowSized;

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (!shadowSized && layer == CampaignEngineLayers.BELOW_STATIONS) shadowSized = AoTDMisc.sizeStationShadow(entity);
    }
}
