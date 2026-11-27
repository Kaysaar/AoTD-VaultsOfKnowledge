package com.fs.starfarer.api.impl.campaign.aotd_entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import data.kaysaar.aotd.vok.campaign.econ.stablestructures.impl.AbyssalLighthouseImpl;
import data.kaysaar.aotd.vok.misc.AoTDMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AbyssalLighthouseEntityPlugin extends BaseCustomEntityPlugin {
    private static final Vector2f[] VENT_OFFSETS = {
            new Vector2f(21f, 58f), new Vector2f(15f, 42f),
            new Vector2f(6f, 12f), new Vector2f(4f, -34f)
    };
    private static final int WISPS_PER_LAYER = 80;
    private static final int SIDES_PER_VENT = 2;
    private static final float SPRITE_FORWARD_DEGREES = 90f;
    private static final float CYCLES_PER_SECOND = 0.17f;
    private static final float GOLDEN_RATIO_FRACTION = 0.618034f;

    private static final int CLOUD_TILES_PER_ROW = 2;
    private static final float CLOUD_WHITE_MIX = 0.14f;
    private static final Color CLOUD_COLOR = Misc.interpolateColor(DwellerShroud.SHROUD_COLOR, Color.WHITE, CLOUD_WHITE_MIX);
    private static final float CLOUD_PHASE_OFFSET = 0.5f;
    private static final float CLOUD_OUTWARD_TRAVEL = 9f;
    private static final float CLOUD_START_SIZE = 17f;
    private static final float CLOUD_SIZE_GAIN = 15f;
    private static final float CLOUD_PEAK_ALPHA = 0.46f;

    private static final int MIST_TILES_PER_ROW = 4;
    private static final float MIST_WHITE_MIX = 0.28f;
    private static final Color MIST_COLOR = Misc.interpolateColor(DwellerShroud.SHROUD_GLOW_COLOR, Color.WHITE, MIST_WHITE_MIX);
    private static final float MIST_OUTWARD_TRAVEL = 4f;
    private static final float MIST_START_SIZE = 11f;
    private static final float MIST_SIZE_GAIN = 10f;
    private static final float MIST_PEAK_ALPHA = 0.22f;

    private static final int SPREAD_STEPS = 100;
    private static final int SPREAD_STRIDE = 37;
    private static final int VENT_HEIGHT_JITTER = 11;
    private static final int HEIGHT_STRIDE = 53;
    private static final float CURL_PHASE_SPREAD_RADIANS = 2f;
    private static final float CURL_DISTANCE = 7f;
    private static final float EXTRA_OUTWARD_TRAVEL = 12f;
    private static final float DOWNWARD_TRAVEL = 22f;
    private static final float EXTRA_DOWNWARD_TRAVEL = 17f;
    private static final float WISP_HEIGHT_TO_WIDTH = 1.2f;
    private static final float ANGLE_STEP_DEGREES = 73f;
    private static final float SPIN_DEGREES_PER_CYCLE = 120f;

    private transient SpriteAPI cloud;
    private transient SpriteAPI mist;
    private transient boolean shadowSized;
    private boolean encounterMusicSet;
    private float phase;

    @Override
    public void advance(float amount) {
        if (!encounterMusicSet) {
            entity.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "aotd_special");
            encounterMusicSet = true;
        }
        if (!Global.getSector().isPaused()) phase = (phase + amount * CYCLES_PER_SECOND) % 1f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        boolean behindHull = layer == CampaignEngineLayers.BELOW_STATIONS;
        if (!behindHull && layer != CampaignEngineLayers.STATIONS) return;
        if (!shadowSized && behindHull) shadowSized = AoTDMisc.sizeStationShadow(entity);
        Object structure = entity.getMemoryWithoutUpdate().get(AoTDStableStructureSpecManager.memKeyInEntities);
        if (structure instanceof AbyssalLighthouseImpl lighthouse && !lighthouse.isFunctional()) return;
        float alpha = viewport.getAlphaMult() * entity.getSensorFaderBrightness() * entity.getSensorContactFaderBrightness();
        if (alpha <= 0f || !viewport.isNearViewport(entity.getLocation(), getRenderRange())) return;
        if (cloud == null) loadShroudSprites();
        renderVentWisps(behindHull, alpha);
    }

    private void loadShroudSprites() {
        cloud = Global.getSettings().getSprite("misc", "nebula_particles2");
        cloud.setNormalBlend();
        cloud.setColor(CLOUD_COLOR);
        cloud.setTexWidth(1f / CLOUD_TILES_PER_ROW);
        cloud.setTexHeight(1f / CLOUD_TILES_PER_ROW);

        mist = Global.getSettings().getSprite("misc", "dust_particles");
        mist.setAdditiveBlend();
        mist.setColor(MIST_COLOR);
        mist.setTexWidth(1f / MIST_TILES_PER_ROW);
        mist.setTexHeight(1f / MIST_TILES_PER_ROW);
    }

    private void renderVentWisps(boolean behindHull, float alpha) {
        SpriteAPI sprite = behindHull ? cloud : mist;
        int tilesPerRow = behindHull ? CLOUD_TILES_PER_ROW : MIST_TILES_PER_ROW;
        float phaseOffset = behindHull ? CLOUD_PHASE_OFFSET : 0f;
        float outwardTravel = behindHull ? CLOUD_OUTWARD_TRAVEL : MIST_OUTWARD_TRAVEL;
        float startSize = behindHull ? CLOUD_START_SIZE : MIST_START_SIZE;
        float sizeGain = behindHull ? CLOUD_SIZE_GAIN : MIST_SIZE_GAIN;
        float peakAlpha = alpha * (behindHull ? CLOUD_PEAK_ALPHA : MIST_PEAK_ALPHA);

        Vector2f center = entity.getLocation();
        float facing = entity.getFacing();
        double hullAngle = Math.toRadians(facing - SPRITE_FORWARD_DEGREES);
        float cos = (float) Math.cos(hullAngle);
        float sin = (float) Math.sin(hullAngle);

        sprite.bindTexture();
        for (int wisp = 0; wisp < WISPS_PER_LAYER; wisp++) {
            int ventIndex = (wisp / SIDES_PER_VENT) % VENT_OFFSETS.length;
            Vector2f vent = VENT_OFFSETS[ventIndex];
            float side = wisp % SIDES_PER_VENT == 0 ? -1f : 1f;
            float age = (phase + wisp * GOLDEN_RATIO_FRACTION + phaseOffset) % 1f;
            float spread = (wisp * SPREAD_STRIDE % (SPREAD_STEPS + 1)) / (float) SPREAD_STEPS;
            int heightOffset = (wisp * HEIGHT_STRIDE) % (VENT_HEIGHT_JITTER * 2 + 1) - VENT_HEIGHT_JITTER;

            double curlAngle = age * Math.PI * 2 + ventIndex + side + spread * CURL_PHASE_SPREAD_RADIANS;
            float curl = (float) Math.sin(curlAngle);
            float driftX = outwardTravel + EXTRA_OUTWARD_TRAVEL * spread + CURL_DISTANCE * curl;
            float driftY = DOWNWARD_TRAVEL + EXTRA_DOWNWARD_TRAVEL * spread;
            float localX = side * (vent.x * spread + age * driftX);
            float localY = vent.y + heightOffset - age * driftY;

            float size = startSize + age * sizeGain;
            sprite.setSize(size, size * WISP_HEIGHT_TO_WIDTH);
            sprite.setAlphaMult(peakAlpha * (float) Math.sin(age * Math.PI));
            sprite.setAngle(facing + wisp * ANGLE_STEP_DEGREES + side * age * SPIN_DEGREES_PER_CYCLE);
            sprite.setTexX((wisp % tilesPerRow) / (float) tilesPerRow);
            sprite.setTexY((wisp / tilesPerRow % tilesPerRow) / (float) tilesPerRow);
            sprite.renderAtCenterNoBind(center.x + localX * cos - localY * sin,
                    center.y + localX * sin + localY * cos);
        }
    }
}
