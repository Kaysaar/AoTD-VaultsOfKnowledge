package com.fs.starfarer.api.impl.campaign.aotd_entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.kaysaar.aotd.vok.misc.AoTDMisc;

public class CrystalCasinoEntityPlugin extends BaseCustomEntityPlugin {
    private transient SpriteAPI lights;
    private transient boolean shadowSized;

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (!shadowSized && layer == CampaignEngineLayers.BELOW_STATIONS) shadowSized = AoTDMisc.sizeStationShadow(entity);
        if (layer != CampaignEngineLayers.STATIONS) return;
        float alpha = viewport.getAlphaMult() * entity.getSensorFaderBrightness() * entity.getSensorContactFaderBrightness();
        if (alpha <= 0f) return;

        if (lights == null) {
            CustomEntitySpecAPI spec = entity.getCustomEntitySpec();
            lights = Global.getSettings().getSprite("rendering", "aotd_crystal_casino_glow");
            lights.setSize(spec.getSpriteWidth(), spec.getSpriteHeight());
            lights.setAdditiveBlend();
        }
        lights.setAngle(entity.getFacing() - 90f);
        lights.setAlphaMult(alpha);
        lights.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
    }
}
