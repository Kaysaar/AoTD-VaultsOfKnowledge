package data.kaysaar.aotd.vok.ui.research;

import ashlib.data.plugins.ui.models.PopUpUI;
import ashlib.data.plugins.ui.models.resizable.PartialImageViewer;
import ashlib.data.plugins.ui.models.ProgressBarComponentV2;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpec;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import data.kaysaar.aotd.vok.misc.AoTDMisc;
import data.kaysaar.aotd.vok.plugins.AoDUtilis;
import data.kaysaar.aotd.vok.plugins.AoTDSettingsManager;
import data.kaysaar.aotd.vok.scripts.research.AoTDFactionResearchManager;
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager;
import data.kaysaar.aotd.vok.scripts.research.models.ResearchOption;
import data.kaysaar.aotd.vok.scripts.research.models.ResearchRewardType;
import kaysaar.bmo.buildingmenu.BuildingMenuMisc;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.kaysaar.aotd.vok.plugins.AoTDVokModPlugin.clearListenersFromTemporaryMarket;

public class ResearchInfoUI extends PopUpUI {
    ResearchOption option;
    CustomPanelAPI mainPanel;
    ButtonAPI currButton;
    String currentCommand;
    float lastYPos;
    double multiplier = AoTDSettingsManager.getFloatValue(AoTDSettingsManager.AOTD_RESEARCH_SPEED_MULTIPLIER);
    ResearchZoomPanel zoomPanel;
    float iconsize = 120;
    private static final float SCREEN_MARGIN = 10f;
    private static final float SCROLLBAR_SPACE = 12f;
    private TooltipMakerAPI contentViewport;
    private float contentHeight;

    public ResearchInfoUI(ResearchOption option, ResearchZoomPanel zoomPanel) {
        this.option = option;
        this.zoomPanel = zoomPanel;
    }


    @Override
    public void init(CustomPanelAPI panelAPI, float x, float y, boolean isDialog) {
        float maxHeight = Math.max(1f, Global.getSettings().getScreenHeight() - SCREEN_MARGIN * 2f);
        panelAPI.getPosition().setSize(panelAPI.getPosition().getWidth(),
                Math.min(panelAPI.getPosition().getHeight(), maxHeight));
        UIPanelAPI parent = ProductionUtil.getCoreUI();
        if (parent != null) {
            float top = parent.getPosition().getHeight() - y;
            float maxTop = Global.getSettings().getScreenHeight() - SCREEN_MARGIN - panelAPI.getPosition().getHeight();
            top = Math.max(SCREEN_MARGIN, Math.min(top, maxTop));
            y = parent.getPosition().getHeight() - top;
        }
        super.init(panelAPI, x, y, isDialog);
    }

    @Override
    public void createUI(CustomPanelAPI panelAPI) {
        float height = createUIMockup(panelAPI);
        goalSizeY = height;
        panelAPI.getPosition().setSize(panelAPI.getPosition().getWidth(), height);
        contentViewport = panelAPI.createUIElement(panelAPI.getPosition().getWidth(), height,
                contentHeight > height);
        contentViewport.addCustom(mainPanel, 0f).getPosition().inTL(0, 0);
        contentViewport.setHeightSoFar(contentHeight);
        panelAPI.addUIElement(contentViewport).inTL(7, 0);
        addTooltip(contentViewport);
        addPanel(mainPanel);
    }

    @Override
    public float createUIMockup(CustomPanelAPI panelAPI) {
        mainTooltips.clear();
        mainPanels.clear();
        mainPanel = panelAPI.createCustomPanel(panelAPI.getPosition().getWidth() - SCROLLBAR_SPACE,
                panelAPI.getPosition().getHeight(), null);

        createImageSection(mainPanel);
        createNameOfTechSection(mainPanel);
        CustomPanelAPI progress = getProgressionBarTooltip(mainPanel);
        float lastY = 0;
        mainPanel.addComponent(progress).inTL(0, iconsize - 15);
        lastY += iconsize - 15 + progress.getPosition().getHeight() + 30;
        CustomPanelAPI widget = createWidgetForIndustries(mainPanel);
        TooltipMakerAPI tooltip = mainPanel.createUIElement(mainPanel.getPosition().getWidth(), 20, false);
        tooltip.addSectionHeading("Unlocks", Alignment.MID, 0f);
        mainPanel.addUIElement(tooltip).inTL(0, lastY);
        lastY += 20;
        if (widget != null) {
            mainPanel.addComponent(widget).inTL(0, lastY);
            lastY += widget.getPosition().getHeight() + 5;
        }
        CustomPanelAPI structures = createWidgetForRewards(mainPanel, ResearchRewardType.STABLESTRUCTURES);
        if (structures != null) {
            mainPanel.addComponent(structures).inTL(0, lastY);
            lastY += structures.getPosition().getHeight() + 5;
        }
        CustomPanelAPI widget2 = createSectionForSpecialEffects(mainPanel);
        if (widget2 != null) {
            mainPanel.addComponent(widget2).inTL(0, lastY);
            lastY += widget2.getPosition().getHeight() + 5;
        }
        addTooltip(tooltip);
        tooltip = mainPanel.createUIElement(mainPanel.getPosition().getWidth(), 20, false);
        if (option.otherReq != null || !option.ReqTechsToResearchFirst.isEmpty() || !option.ReqItemsToResearchFirst.isEmpty()) {
            tooltip.addSectionHeading("Requirements", Alignment.MID, 0f);
            mainPanel.addUIElement(tooltip).inTL(0, lastY);
            lastY += 25;
            if (!option.ReqTechsToResearchFirst.isEmpty()) {
                CustomPanelAPI comp = createSectionForReqResearch(mainPanel);
                mainPanel.addComponent(comp).inTL(0, lastY);
                lastY += comp.getPosition().getHeight() + 5;
            }
            if (!option.ReqItemsToResearchFirst.isEmpty()) {
                CustomPanelAPI comp = createSectionForReqResearchItem(mainPanel);
                mainPanel.addComponent(comp).inTL(0, lastY);
                lastY += comp.getPosition().getHeight() + 10;
            }
            if (option.otherReq != null) {
                CustomPanelAPI comp = createSectionForOtherReq(mainPanel);
                mainPanel.addComponent(comp).inTL(0, lastY);
                lastY += comp.getPosition().getHeight() + 5;
            }
        }
        addTooltip(tooltip);

        CustomPanelAPI panelAPI1 = createButtonForResearchOrQueue(mainPanel);
        mainPanel.addComponent(panelAPI1).inTL(0, lastY);
        lastY += panelAPI1.getPosition().getHeight() + 5;
        contentHeight = lastY;
        mainPanel.getPosition().setSize(mainPanel.getPosition().getWidth(), contentHeight);
        return Math.min(contentHeight, Math.max(1f, Global.getSettings().getScreenHeight() - SCREEN_MARGIN * 2f));
    }
    public void resetUI() {
        currButton = null;
        if (contentViewport != null) getPanelToInfluence().removeComponent(contentViewport);
        contentViewport = null;
        mainPanel = null;
        createUI(getPanelToInfluence());
        if (zoomPanel != null) {
            zoomPanel.refresh();
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (currButton != null && !pressedConfirmCancel && currButton.isChecked()) {
            currButton.setChecked(false);
            String data = (String) currButton.getCustomData();
            String[] splitted = data.split(":");
            if (splitted[0].equals("research")) {
                AoTDMainResearchManager.getInstance().getManagerForPlayer().pickResearchFocus(option.Id);
                AoTDMainResearchManager.getInstance().getManagerForPlayer().notifyFactionBeginResearch(option);
                Global.getSoundPlayer().playUISound("aotd_research_started", 1f, 1f);
                resetUI();
                this.zoomPanel.plugin.resetCurrentResearch();
                return;
            }
            if (splitted[0].equals("queue_add")) {
                AoTDMainResearchManager.getInstance().getManagerForPlayer().getQueueManager().addToQueue(option.Id);
                Global.getSoundPlayer().playUISound("aotd_research_started", 1f, 1f);
                resetUI();
                return;
            }
            if (splitted[0].equals("queue_remove")) {
                AoTDMainResearchManager.getInstance().getManagerForPlayer().getQueueManager().removeFromQueue(option.Id);
                resetUI();
                return;
            }
            if (splitted[0].equals("stop")) {
                AoTDFactionResearchManager manager = AoTDMainResearchManager.getInstance().getManagerForPlayer();
                manager.setCurrentFocus(null);
                if (!manager.getQueueManager().getQueuedResearchOptions().isEmpty()) {
                    manager.setCurrentFocus(AoTDMainResearchManager.getInstance().getManagerForPlayer().getQueueManager().removeFromTop().Id);
                }
                this.zoomPanel.plugin.resetCurrentResearch();
                resetUI();
                return;
            }
        }
    }

    public CustomPanelAPI createButtonForResearchOrQueue(CustomPanelAPI originPanel) {
        CustomPanelAPI other = originPanel.createCustomPanel(originPanel.getPosition().getWidth(), 30, null);
        TooltipMakerAPI tooltip = other.createUIElement(originPanel.getPosition().getWidth(), 30, false);
        if (AoTDMainResearchManager.getInstance().getManagerForPlayer().getCurrentFocus() == null) {
            currButton = tooltip.addButton("Start Research", "research:" + option.getSpec().getId(), 200, 30, 0f);
        } else {
            if (AoTDMainResearchManager.getInstance().getManagerForPlayer().getCurrentFocus().getSpec().getId().equals(option.getSpec().getId())) {
                currButton = tooltip.addButton("Stop Research", "stop:" + option.getSpec().getId(), 200, 30, 0f);

            } else {
                if (!AoTDMainResearchManager.getInstance().getManagerForPlayer().getQueueManager().isInQueue(option.getSpec().getId())) {
                    currButton = tooltip.addButton("Queue Research", "queue_add:" + option.getSpec().getId(), 200, 30, 0f);

                } else {
                    currButton = tooltip.addButton("Remove from Queue", "queue_remove:" + option.getSpec().getId(), 200, 30, 0f);

                }


            }
        }
        currButton.setEnabled(AoTDMainResearchManager.getInstance().getManagerForPlayer().canResearch(option.Id, false));
        currButton.getPosition().inTL(originPanel.getPosition().getWidth() - 205, 0);
        other.addUIElement(tooltip).inTL(0, 0);
        return other;

    }

    public void createImageSection(CustomPanelAPI originPanel) {
        String iconId = option.getSpec().getIconId();
        UILinesRenderer renderer = new UILinesRenderer(0f);
        CustomPanelAPI imagePanel = originPanel.createCustomPanel(iconsize, iconsize, null);
        TooltipMakerAPI tooltip = imagePanel.createUIElement(iconsize, iconsize, false);
        tooltip.addImage(Global.getSettings().getSpriteName("ui_icons_tech_tree", iconId), iconsize, iconsize, 0);
        imagePanel.addUIElement(tooltip).inTL(0, 0);
        originPanel.addComponent(imagePanel).inTL(-10, -5);

    }

    public void createNameOfTechSection(CustomPanelAPI originPanel) {
        float width = originPanel.getPosition().getWidth() - iconsize;
        CustomPanelAPI imagePanel = originPanel.createCustomPanel(width, iconsize - 10, null);
        TooltipMakerAPI tooltip = imagePanel.createUIElement(width, iconsize - 10, false);
        tooltip.setTitleOrbitronVeryLarge();
        tooltip.addTitle(option.getSpec().getName());
        imagePanel.addUIElement(tooltip).inTL(0, 0);
        originPanel.addComponent(imagePanel).inTL(iconsize - 10, 5);

    }

    public CustomPanelAPI createIndustryWidgetForUI(String indId) {
        return createRewardWidgetForUI(indId, ResearchRewardType.INDUSTRY);
    }

    private CustomPanelAPI createRewardWidgetForUI(String rewardId, ResearchRewardType type) {
        String image;
        String name;
        if (type == ResearchRewardType.STABLESTRUCTURES) {
            AoTDStableStructureSpec spec = AoTDStableStructureSpecManager.getSpec(rewardId);
            AoTDStableStructureImpl structure = spec == null ? null : spec.getImplClassInstance();
            name = structure == null ? rewardId : structure.getName();
            image = structure == null ? null : structure.getImageName();
            if (image == null || image.isEmpty()) image = AoTDMisc.getImagePathForTechIcon(rewardId);
        } else {
            image = Global.getSettings().getIndustrySpec(rewardId).getImageName();
            name = Global.getSettings().getIndustrySpec(rewardId).getName();
        }
        UILinesRenderer renderer = new UILinesRenderer(0f);
        CustomPanelAPI panelAPI = Global.getSettings().createCustom(190, 95, renderer);
        renderer.setPanel(panelAPI);
        TooltipMakerAPI tooltip = panelAPI.createUIElement(195, 95, false);
        if (type == ResearchRewardType.STABLESTRUCTURES) {
            float width = panelAPI.getPosition().getWidth();
            float height = panelAPI.getPosition().getHeight();
            SpriteAPI sprite = Global.getSettings().getSprite(image);
            float sourceWidth = sprite.getWidth();
            float sourceHeight = sprite.getHeight();
            PartialImageViewer viewer = new PartialImageViewer(width, height, image, sourceWidth, sourceHeight);
            if (sourceWidth > 0f && sourceHeight > 0f) {
                // Scale to cover the card, preserving proportions, then crop equally on both sides.
                float scale = Math.max(width / sourceWidth, height / sourceHeight);
                float cropWidth = width / scale;
                float cropHeight = height / scale;
                viewer.setRegion((sourceWidth - cropWidth) / 2f, (sourceHeight - cropHeight) / 2f,
                        cropWidth, cropHeight);
            }
            tooltip.addCustom(viewer.getComponentPanel(), 0f);
        } else {
            tooltip.addImage(image, 190, 95, 0f);
        }
        LabelAPI title = type == ResearchRewardType.STABLESTRUCTURES
                ? tooltip.addTitle(name, Color.ORANGE) : tooltip.addTitle(name);
        title.getPosition().inTL(5, -20);
        panelAPI.addUIElement(tooltip).inTL(-5, 0);
        return panelAPI;
    }

    public CustomPanelAPI getProgressionBarTooltip(CustomPanelAPI originPanel) {
        CustomPanelAPI progression = originPanel.createCustomPanel(originPanel.getPosition().getWidth(), 50, null);
        ProgressBarComponentV2 component = new ProgressBarComponentV2(progression.getPosition().getWidth() - 10, 21, option.getPercentageProgress()/100f, Misc.getDarkPlayerColor().brighter().brighter());
        TooltipMakerAPI tooltip = progression.createUIElement(originPanel.getPosition().getWidth(), 50, false);
        int defaultDays = (int) AoDUtilis.getDaysFromResearch(option);

        tooltip.addPara("This technology takes %s to research", 5f, Color.ORANGE, AoTDMisc.convertDaysToString((int) (defaultDays-option.daysSpentOnResearching)));
        if (option.isResearched) {
            tooltip.addPara("Current research progress :%s", 5f, Color.ORANGE, "Finished");
        } else {
            AoTDFactionResearchManager manager = AoTDMainResearchManager.getInstance().getManagerForPlayerFaction();
            if (manager.getCurrentFocus() != null && manager.getCurrentFocus().getSpec().getId().equals(option.getSpec().getId())) {
                tooltip.addPara("Current research progress : %s ( %s left )", 5f, Color.ORANGE, option.getPercentageProgress() + "%", AoTDMisc.convertDaysToString((int) (AoDUtilis.getDaysFromResearch(option)-option.daysSpentOnResearching)));

            } else {
                tooltip.addPara("Current research progress : %s", 5f, Color.ORANGE, option.getPercentageProgress() + "%");

            }

        }
        tooltip.addCustom(component.getRenderingPanel(), 5f);
        progression.addUIElement(tooltip).inTL(0, 0);
        return progression;
    }

    public CustomPanelAPI createWidgetForIndustries(CustomPanelAPI originPanel) {
        return createWidgetForRewards(originPanel, ResearchRewardType.INDUSTRY);
    }

    private CustomPanelAPI createWidgetForRewards(CustomPanelAPI originPanel, ResearchRewardType type) {
        ArrayList<String> rewardIds = new ArrayList<>();
        for (Map.Entry<String, ResearchRewardType> entry : option.Rewards.entrySet()) {
            if (entry.getValue() == type) rewardIds.add(entry.getKey());
        }
        if (rewardIds.isEmpty()) return null;

        float width = originPanel.getPosition().getWidth();
        float cardWidth = 190f;
        float cardHeight = 95f;
        float titleSpace = 20f;
        float rowGap = 5f;
        int columns = width >= cardWidth * 2f + 10f ? 2 : 1;
        int rows = (rewardIds.size() + columns - 1) / columns;
        float rowHeight = titleSpace + cardHeight + rowGap;
        float height = rows * rowHeight;
        CustomPanelAPI panel = originPanel.createCustomPanel(width, height, null);
        TooltipMakerAPI tooltip = panel.createUIElement(width, height, false);
        for (int i = 0; i < rewardIds.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int cardsInRow = Math.min(columns, rewardIds.size() - row * columns);
            float slotWidth = width / cardsInRow;
            CustomPanelAPI card = createRewardWidgetForUI(rewardIds.get(i), type);
            tooltip.addCustom(card, 0f).getPosition().inTL(
                    slotWidth * column + (slotWidth - cardWidth) / 2f, row * rowHeight + titleSpace);
            createOnHoverTooltipForReward(tooltip, rewardIds.get(i), type, card);
        }
        tooltip.setHeightSoFar(height);
        panel.addUIElement(tooltip).inTL(0, 0);
        return panel;
    }

    public static void createOnHoverTooltipForIndustry(TooltipMakerAPI tooltip, String industryId, UIComponentAPI attatchTo) {
        createOnHoverTooltipForReward(tooltip, industryId, ResearchRewardType.INDUSTRY, attatchTo);
    }

    public static void createOnHoverTooltipForReward(TooltipMakerAPI tooltip, String industryId,
                                                     ResearchRewardType type, UIComponentAPI attatchTo) {
        tooltip.addTooltipTo(new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return 500;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                if (type == ResearchRewardType.STABLESTRUCTURES) {
                    AoTDStableStructureSpec spec = AoTDStableStructureSpecManager.getSpec(industryId);
                    if (spec == null) {
                        tooltip.addTitle("Unknown system structure");
                        tooltip.addPara("No system structure specification is loaded for %s.",
                                5f, Misc.getNegativeHighlightColor(), industryId);
                        return;
                    }
                    // Preview through the existing construction tooltip, without spawning an entity.
                    AoTDStableStructureImpl structure = spec.getImplClassInstance();
                    tooltip.addTitle(structure.getName(), Color.ORANGE);
                    structure.createTooltip(tooltip, AoTDStableStructureImpl.AoTDStableStructureTooltipMode.BUILDING_MENU);
                    return;
                }
                MarketAPI marketAPI = initalizeMarket();
                marketAPI.addIndustry(Industries.POPULATION);
                marketAPI.addIndustry(Industries.SPACEPORT);
                marketAPI.getStability().modifyFlat("test",1000,"test");
                marketAPI.getIncomeMult().modifyFlat("test",1,"test");
                marketAPI.addCondition("aotd_toolbox_food_corrector");
                marketAPI.reapplyConditions();
                marketAPI.reapplyIndustries();

                Industry ind = marketAPI.instantiateIndustry(industryId);
                marketAPI.reapplyConditions();
                marketAPI.reapplyIndustries();
                ind.reapply();
                if (ind.getId().equals(Industries.FARMING)) {
                    ind.getSupply(Commodities.FOOD).getQuantity().modifyFlat("test", 6);
                }
                if (ind.getId().equals(Industries.AQUACULTURE)) {
                    ind.getSupply(Commodities.FOOD).getQuantity().modifyFlat("test", 6);
                }
                if (ind.getId().equals(Industries.MINING)) {
                    ind.getSupply(Commodities.ORE).getQuantity().modifyFlat("test", 6);
                    ind.getSupply(Commodities.RARE_ORE).getQuantity().modifyFlat("test", 4);
                    ind.getSupply(Commodities.ORGANICS).getQuantity().modifyFlat("test", 6);
                    ind.getSupply(Commodities.VOLATILES).getQuantity().modifyFlat("test", 4);
                }

                BuildingMenuMisc.createTooltipForIndustry((BaseIndustry) ind, Industry.IndustryTooltipMode.ADD_INDUSTRY,tooltip,expanded,true,getTooltipWidth(tooltipParam),true,true,false);
                tooltip.addPara("*Size of example market is 6 and all resource conditions are set to give none of bonuses to any industry.", Misc.getTooltipTitleAndLightHighlightColor(), 5f);


                marketAPI.removeIndustry(industryId, MarketAPI.MarketInteractionMode.REMOTE,false);
                clearListenersFromTemporaryMarket();

            }
        }, attatchTo, TooltipMakerAPI.TooltipLocation.RIGHT,false);
    }

    public CustomPanelAPI createSectionForSpecialEffects(CustomPanelAPI originPanel) {
        CustomPanelAPI other = originPanel.createCustomPanel(originPanel.getPosition().getWidth(), 50, null);
        TooltipMakerAPI tooltip = other.createUIElement(originPanel.getPosition().getWidth(), 50, false);
        boolean addedSth = false;
        float pad = 3f;
        for (Map.Entry<String, ResearchRewardType> entry : option.Rewards.entrySet()) {
            if (entry.getValue().equals(ResearchRewardType.MODIFIER)) {
                tooltip.addPara(entry.getKey(), Misc.getPositiveHighlightColor(), pad);
                pad = 10f;
                addedSth = true;
            }
        }
        if (addedSth) {
            other.getPosition().setSize(other.getPosition().getWidth(),tooltip.getHeightSoFar());
            other.addUIElement(tooltip).inTL(0, 0);
            return other;
        }

        return null;

    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);
    }

    public CustomPanelAPI createSectionForOtherReq(CustomPanelAPI originPanel) {
        CustomPanelAPI other = originPanel.createCustomPanel(originPanel.getPosition().getWidth(), 30, null);
        TooltipMakerAPI tooltip = other.createUIElement(originPanel.getPosition().getWidth(), 30, true);
        if (option.otherReq != null) {
            if (!option.metOtherReq) {
                tooltip.addPara(option.otherReq.two, Misc.getNegativeHighlightColor(), 0f);
            } else {
                tooltip.addPara(option.otherReq.two, Misc.getPositiveHighlightColor(), 0f);
            }

        }
        tooltip.addSpacer(5f);
        other.addUIElement(tooltip).inTL(0, 0);
        if (tooltip.getHeightSoFar() <= 30) {
            other.getPosition().setSize(other.getPosition().getWidth(), tooltip.getHeightSoFar()+5);
            tooltip.getPosition().setSize(other.getPosition().getWidth(),tooltip.getHeightSoFar()+5);

        } else {
            other.getPosition().setSize(other.getPosition().getWidth(), 30);
        }
        return other;

    }

    public CustomPanelAPI createSectionForReqResearch(CustomPanelAPI originPanel) {
        CustomPanelAPI other = originPanel.createCustomPanel(originPanel.getPosition().getWidth(), 120, null);
        TooltipMakerAPI tooltip = other.createUIElement(originPanel.getPosition().getWidth(), 120, true);
        float pad = 0f;
        float height = 0;
        for (String s : option.ReqTechsToResearchFirst) {
            tooltip.addCustom(populateReqResearch(AoTDMainResearchManager.getInstance().getManagerForPlayerFaction(), s), pad);
            height += tooltip.getPrev().getPosition().getHeight() + pad;
            pad = 5f;
        }
        if (tooltip.getHeightSoFar() <= 120) {
            other.getPosition().setSize(other.getPosition().getWidth(), tooltip.getHeightSoFar());

        } else {
            other.getPosition().setSize(other.getPosition().getWidth(), 120);
        }


        other.addUIElement(tooltip).inTL(0, 0);
        return other;

    }

    public CustomPanelAPI createSectionForReqResearchItem(CustomPanelAPI originPanel) {
        CustomPanelAPI other = originPanel.createCustomPanel(originPanel.getPosition().getWidth(), 140, null);
        TooltipMakerAPI tooltip = other.createUIElement(originPanel.getPosition().getWidth(), 140, true);
        float pad = 10f;
        for (Map.Entry<String, Integer> entry : option.ReqItemsToResearchFirst.entrySet()) {
            CustomPanelAPI panel = populateReqItems(AoTDMainResearchManager.getInstance().getManagerForPlayerFaction(), entry);
            if (panel == null) continue;
            tooltip.addCustom(panel, pad);
        }

        other.addUIElement(tooltip).inTL(0, 0);
        if (tooltip.getHeightSoFar() <= 140) {
            other.getPosition().setSize(other.getPosition().getWidth(), tooltip.getHeightSoFar());

        } else {
            other.getPosition().setSize(other.getPosition().getWidth(), 140);
        }
        return other;

    }

    private CustomPanelAPI populateReqItems(final AoTDFactionResearchManager manager, final Map.Entry<String, Integer> entry) {
        if (entry.getValue() == 0) return null;
        CustomPanelAPI panel = Global.getSettings().createCustom(400, 60, null);
        TooltipMakerAPI tooltipMakerAPI = panel.createUIElement(60, 60, false);
        TooltipMakerAPI labelTooltip = panel.createUIElement(320, 60, false);
        LabelAPI labelAPI1 = null;

        if (Global.getSettings().getCommoditySpec(entry.getKey()) != null) {
            tooltipMakerAPI.addImage(Global.getSettings().getCommoditySpec(entry.getKey()).getIconName(), 60, 60, 10f);
            labelAPI1 = labelTooltip.addPara(Global.getSettings().getCommoditySpec(entry.getKey()).getName() + " : " + entry.getValue(), 10f);
            labelTooltip.addPara("You have %s stored in Research Storages", 10f, Color.ORANGE, "" + (int) manager.retrieveAmountOfItems(entry.getKey()));

        }
        if (Global.getSettings().getSpecialItemSpec(entry.getKey()) != null) {
            tooltipMakerAPI.addImage(Global.getSettings().getSpecialItemSpec(entry.getKey()).getIconName(), 60, 60, 10f);
            labelAPI1 = labelTooltip.addPara(Global.getSettings().getSpecialItemSpec(entry.getKey()).getName() + " : " + entry.getValue(), 10f);
            labelTooltip.addPara("You have %s stored in Research Storages", 10f, Color.ORANGE, "" + (int) manager.retrieveAmountOfItems(entry.getKey()));

        }
        if (manager.haveMetReqForItem(entry.getKey(), entry.getValue()) || manager.getResearchOptionFromRepo(option.Id).havePaidForResearch) {
            labelAPI1.setColor(Misc.getPositiveHighlightColor());

        } else {
            labelAPI1.setColor(Misc.getNegativeHighlightColor());
        }
        if (option.isResearched) {
            labelAPI1.setColor(Misc.getPositiveHighlightColor());
        }
        labelAPI1.autoSizeToWidth(320);
        panel.addUIElement(tooltipMakerAPI).inTL(-10, -20);
        panel.addUIElement(labelTooltip).inTL(60, -14);
        return panel;
    }

    private CustomPanelAPI populateReqResearch(final AoTDFactionResearchManager manager, final String researchID) {
        CustomPanelAPI panel = Global.getSettings().createCustom(360, 80, null);
        TooltipMakerAPI tooltipMakerAPI = panel.createUIElement(80, 80, false);
        TooltipMakerAPI labelTooltip = panel.createUIElement(300, 60, false);
        LabelAPI labelAPI1 = null;

        tooltipMakerAPI.addImage(Global.getSettings().getSpriteName("ui_icons_tech_tree", manager.getResearchOptionFromRepo(researchID).getSpec().getIconId()), 80, 80, 0);
        labelTooltip.addTitle(manager.getResearchOptionFromRepo(researchID).getSpec().getName());
        if (manager.getResearchOptionFromRepo(researchID).isResearched()) {
            labelAPI1 = labelTooltip.addPara("Already researched !", Misc.getPositiveHighlightColor(), 20f);
        } else {
            labelAPI1 = labelTooltip.addPara("Must be researched!", Misc.getNegativeHighlightColor(), 20f);
        }

        labelAPI1.autoSizeToWidth(320);
        panel.addUIElement(tooltipMakerAPI).inTL(-12, 0);
        panel.addUIElement(labelTooltip).inTL(70, 7);
        return panel;
    }

    public static MarketAPI initalizeMarket() {
        if (Global.getSector().getMemory().get("$aotd_checkout_market")!=null) {
            return (MarketAPI) Global.getSector().getMemory().get("$aotd_checkout_market");
        } else {
            MarketAPI marketToShowTooltip = Global.getFactory().createMarket("to_delete", "TEst", 6);
            marketToShowTooltip.addCondition(Conditions.FARMLAND_ADEQUATE);
            marketToShowTooltip.addCondition(Conditions.ORE_MODERATE);
            marketToShowTooltip.addCondition(Conditions.RARE_ORE_MODERATE);
            marketToShowTooltip.addCondition(Conditions.ORGANICS_COMMON);
            marketToShowTooltip.addCondition(Conditions.VOLATILES_DIFFUSE);

            marketToShowTooltip.addCondition("AoDFoodDemand");
            marketToShowTooltip.addCondition(Conditions.VOLATILES_DIFFUSE);
            marketToShowTooltip.addIndustry("dummy_industry");
            marketToShowTooltip.setFactionId(Global.getSector().getPlayerFaction().getId());
            marketToShowTooltip.reapplyConditions();
            marketToShowTooltip.setFreePort(true);

            for (CommodityOnMarketAPI allCommodity : marketToShowTooltip.getAllCommodities()) {
                allCommodity.getAvailableStat().addTemporaryModFlat(10000, "src", 30);
            }
            marketToShowTooltip.setUseStockpilesForShortages(true);
            Global.getSector().getMemory().set("$aotd_checkout_market", marketToShowTooltip);

            return marketToShowTooltip;
        }
    }


    @Override
    public void onExit() {
        super.onExit();
        zoomPanel.unlockButtonsFromHover();
    }

    public void clearUI() {
        if (contentViewport != null) getPanelToInfluence().removeComponent(contentViewport);
        contentViewport = null;
        createUI(getPanelToInfluence());
    }

}
