package com.Nxer.TwistSpaceTechnology.common.machine.UI.MUI2;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;

import com.Nxer.TwistSpaceTechnology.common.machine.GeneratorMultis.TST_LargeSolarBoiler;
import com.Nxer.TwistSpaceTechnology.util.TextEnums;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * MUI2 GUI for the Large Solar Boiler.
 * <p>
 * The boiler's Heat / Calcification status text and its calcification-cleaning button were originally defined in the
 * legacy ModularUI hooks {@code drawTexts(...)} and {@code addUIWidgets(...)}. The MUI2 GUI path does not call those
 * hooks, so the status text and button never appeared. This class re-adds them through the MUI2 API.
 * <p>
 * The status text is added inside the black terminal panel by overriding {@code createTerminalTextWidget(...)} (the
 * same
 * hook GregTech itself uses to draw the recipe-result / running-mode lines). The clean button is added to the small
 * button row via {@code createLeftPanelGapRow(...)}.
 * <p>
 * Heat and calcification are stored as doubles in range 0..1. They are synced to the client as ints scaled by 10 (i.e.
 * tenths of a percent) so the GUI can show one decimal place using only {@link IntSyncValue}.
 */
public class TST_Gui_LargeSolarBoiler extends TST_Gui<TST_LargeSolarBoiler> {

    /** Scale factor: double 0..1 -> int 0..1000 (tenths of a percent, i.e. value * 1000 gives e.g. 123 == 12.3%). */
    private static final double SCALE = 1000.0D;

    public TST_Gui_LargeSolarBoiler(TST_LargeSolarBoiler multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);

        // Server -> client display values, scaled to tenths of a percent.
        syncManager.syncValue("heatSyncer", new IntSyncValue(() -> (int) Math.round(multiblock.getHeat() * SCALE)));
        syncManager.syncValue(
            "calcificationSyncer",
            new IntSyncValue(() -> (int) Math.round(multiblock.getCalcification() * SCALE)));

        // Client -> server clean action.
        InteractionSyncHandler clearSyncer = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (!multiblock.getBaseMetaTileEntity()
                .isServerSide()) return;
            multiblock.clearMachine();
        });
        syncManager.syncValue("clearSyncer", clearSyncer);
    }

    /**
     * Adds the Heat and Calcification lines inside the black terminal panel, after GregTech's own terminal lines.
     * Signature matches GregTech's MTEMultiBlockBaseGui#createTerminalTextWidget (returns ListWidget&lt;IWidget,
     * ?&gt;).
     */
    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        IntSyncValue heatSyncer = (IntSyncValue) syncManager.getSyncHandlerFromMapKey("heatSyncer:0");
        IntSyncValue calcificationSyncer = (IntSyncValue) syncManager.getSyncHandlerFromMapKey("calcificationSyncer:0");

        return super.createTerminalTextWidget(syncManager, parent).child(
            IKey.dynamic(
                () -> EnumChatFormatting.WHITE
                    // #tr TST_LargeSolarBoiler.gui.02
                    // # Heat:
                    // #zh_CN 热量:
                    + TextEnums.tr("TST_LargeSolarBoiler.gui.02")
                    + " "
                    + EnumChatFormatting.GOLD
                    + percent(heatSyncer.getValue())
                    + EnumChatFormatting.RESET)
                .asWidget()
                .marginBottom(2)
                .fullWidth())
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.WHITE
                        // #tr TST_LargeSolarBoiler.gui.03
                        // # Calcification Level:
                        // #zh_CN 钙化程度:
                        + TextEnums.tr("TST_LargeSolarBoiler.gui.03")
                        + " "
                        + EnumChatFormatting.GOLD
                        + percent(calcificationSyncer.getValue())
                        + EnumChatFormatting.RESET)
                    .asWidget()
                    .marginBottom(2)
                    .fullWidth());
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        return super.createLeftPanelGapRow(parent, syncManager).child(createClearButton(syncManager));
    }

    /** "Clear the machine" button: resets calcification and running ticks (ports the old addUIWidgets button). */
    private IWidget createClearButton(PanelSyncManager syncManager) {
        InteractionSyncHandler clearSyncer = (InteractionSyncHandler) syncManager
            .getSyncHandlerFromMapKey("clearSyncer:0");
        return new ButtonWidget<>().size(18, 18)
            .marginLeft(4)
            // OVERLAY_BUTTON_STRUCTURE_UPDATE is confirmed present in GregTech's MUI2 textures. If you prefer a
            // checkmark and OVERLAY_BUTTON_CHECKMARK compiles in your version, swap it here.
            .overlay(GTGuiTextures.OVERLAY_BUTTON_STRUCTURE_UPDATE)
            .syncHandler(clearSyncer)
            .onMousePressed(mouseButton -> clearSyncer.onMousePressed(mouseButton))
            .playClickSound(true)
            .tooltipBuilder(
                // #tr TST_LargeSolarBoiler.gui.01
                // # Press to clear the machine
                // #zh_CN 点击以清洁机器的钙化
                t -> t.addLine(IKey.lang("TST_LargeSolarBoiler.gui.01")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Format a scaled int (tenths of a percent) back into a "12.3%" string. */
    private static String percent(int scaledValue) {
        return String.format(Locale.ROOT, "%.1f%%", scaledValue / 10.0D);
    }
}
