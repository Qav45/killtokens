package com.example.killtokens.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Identifies plugin GUIs by inventory holder instead of by window title.
 *
 * Title matching is fragile: it breaks if the configured title changes while a GUI
 * is open (leaving an uncancellable inventory players can take items from), it can
 * collide with other plugins' windows, and translated/truncated titles on Bedrock
 * clients via Geyser make title-based assumptions risky. The holder reference is
 * server-side only, so it is immune to all of that.
 */
public final class GuiHolder implements InventoryHolder {

    public enum View { MAIN, AMOUNT, SHOP }

    private final View view;
    private final AmountGui.Type amountType;
    private final String shopSection;
    private Inventory inventory;

    private GuiHolder(View view, AmountGui.Type amountType, String shopSection) {
        this.view = view;
        this.amountType = amountType;
        this.shopSection = shopSection;
    }

    public static GuiHolder main() {
        return new GuiHolder(View.MAIN, null, null);
    }

    public static GuiHolder amount(AmountGui.Type type) {
        return new GuiHolder(View.AMOUNT, type, null);
    }

    public static GuiHolder shop(String sectionId) {
        return new GuiHolder(View.SHOP, null, sectionId);
    }

    public View getView() {
        return view;
    }

    /** Only set for {@link View#AMOUNT}. */
    public AmountGui.Type getAmountType() {
        return amountType;
    }

    /** Only set for {@link View#SHOP}. */
    public String getShopSection() {
        return shopSection;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
