package org.funtown.autoswap.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.swap.SlotDetector;

import java.util.List;

public class EditSwapScreen extends Screen {

    private final Screen    parent;
    private final SwapEntry entry;
    private EditBox         labelField;
    private PairListWidget  pairList;

    public EditSwapScreen(Screen parent, SwapEntry entry) {
        super(ModTranslation.t("autoswap.screen.edit.title"));
        this.parent = parent; this.entry = entry;
    }

    @Override
    protected void init() {
        String cur = entry.label.equals("autoswap.entry.default_label") ? "" : entry.label;
        labelField = new EditBox(getFont(), width/2-110, 18, 220, 20,
                ModTranslation.t("autoswap.screen.edit.label_placeholder"));
        labelField.setValue(cur); labelField.setMaxLength(40);
        labelField.setHint(ModTranslation.t("autoswap.screen.edit.label_placeholder")
                .copy().withStyle(ChatFormatting.DARK_GRAY));
        labelField.setResponder(t -> { entry.label = t.isEmpty() ? "autoswap.entry.default_label" : t; AutoSwapConfig.save(); });
        addRenderableWidget(labelField);
        pairList = new PairListWidget(minecraft, width, height-80, 48, 44);
        addRenderableWidget(pairList);
        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.edit.add_pair"), btn -> {
            entry.pairs.add(new SwapPair()); AutoSwapConfig.save(); pairList.reload();
        }).bounds(width/2-102, height-28, 100, 20).build());
        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.edit.done"), btn -> onClose())
                .bounds(width/2+2, height-28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(getFont(), title, width/2, 6, 0xFFFFFFFF);
        if (entry.pairs.isEmpty())
            ctx.centeredText(getFont(), ModTranslation.t("autoswap.screen.edit.empty"), width/2, height/2-8, 0xFF888888);
    }

    @Override
    public void onClose() { AutoSwapConfig.save(); minecraft.setScreen(parent); }

    class PairListWidget extends ObjectSelectionList<PairListWidget.PairRow> {

        PairListWidget(Minecraft mc, int w, int h, int top, int itemH) {
            super(mc, w, h, top, itemH); reload();
        }

        void reload() {
            clearEntries();
            for (int i = 0; i < entry.pairs.size(); i++) addEntry(new PairRow(entry.pairs.get(i), i));
        }

        @Override public int getRowWidth() { return Math.min(width-20, 500); }

        class PairRow extends ObjectSelectionList.Entry<PairRow> {

            private final SwapPair pair;
            private final int      pairIndex;
            private Button         itemABtn, itemBBtn, slotBtn;
            private final Button   deleteBtn;

            PairRow(SwapPair pair, int pairIndex) {
                this.pair = pair; this.pairIndex = pairIndex;

                itemABtn = Button.builder(Component.literal(pair.getItemAName()),
                        btn -> minecraft.setScreen(new ItemPickerScreen(EditSwapScreen.this, item -> {
                            pair.itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                            pair.targetSlot = SlotDetector.detectSlot(item);
                            itemABtn.setMessage(Component.literal(pair.getItemAName()));
                            slotBtn.setMessage(Component.literal(pair.targetSlot.getDisplayName()));
                            AutoSwapConfig.save();
                        }))).size(100, 20).build();

                itemBBtn = Button.builder(Component.literal(pair.getItemBName()),
                        btn -> minecraft.setScreen(new ItemPickerScreen(EditSwapScreen.this, item -> {
                            pair.itemId2 = BuiltInRegistries.ITEM.getKey(item).toString();
                            pair.targetSlot = SlotDetector.detectSlot(item);
                            itemBBtn.setMessage(Component.literal(pair.getItemBName()));
                            slotBtn.setMessage(Component.literal(pair.targetSlot.getDisplayName()));
                            AutoSwapConfig.save();
                        }))).size(100, 20).build();

                slotBtn = Button.builder(Component.literal(pair.targetSlot.getDisplayName()), btn -> {
                    pair.targetSlot = pair.targetSlot.next();
                    slotBtn.setMessage(Component.literal(pair.targetSlot.getDisplayName()));
                    AutoSwapConfig.save();
                }).size(82, 20).build();

                deleteBtn = Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), btn -> {
                    entry.pairs.remove(pairIndex); AutoSwapConfig.save(); pairList.reload();
                }).size(20, 20).build();
            }

            @Override
            public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, boolean hov, float d) {
                Font font = getFont();
                int x = getX(), ew = getWidth();
                int y = getY();
                int midY = y+(44-20)/2, iconY = y+(44-16)/2;
                if (hov) ctx.fill(x, y, x+ew, y+44, 0x18FFFFFF);

                safeDrawItem(ctx, pair.itemId, x+2, iconY);
                itemABtn.setX(x+21); itemABtn.setY(midY);
                itemABtn.extractRenderState(ctx, mouseX, mouseY, d);
                int gapA = 24, gapB = 24;
                int aw = font.width("↔");
                int bx = x+21+100+gapA;
                ctx.text(font, Component.literal("↔"),
                        x+121+(gapA-aw)/2, midY+5, 0xFFAAAAAA);
                safeDrawItem(ctx, pair.itemId2, bx, iconY);
                itemBBtn.setX(bx+19); itemBBtn.setY(midY);
                itemBBtn.extractRenderState(ctx, mouseX, mouseY, d);
                int bEnd = bx+19+100;
                int gw = font.width("→");
                ctx.text(font, Component.literal("→"),
                        bEnd+(gapB-gw)/2, midY+5, 0xFF888888);
                slotBtn.setX(bEnd+gapB); slotBtn.setY(midY);
                slotBtn.extractRenderState(ctx, mouseX, mouseY, d);
                deleteBtn.setX(x+ew-24); deleteBtn.setY(midY);
                deleteBtn.extractRenderState(ctx, mouseX, mouseY, d);
            }

            private void safeDrawItem(GuiGraphicsExtractor ctx, String id, int x, int y) {
                if (minecraft.player == null) { ctx.fill(x, y, x+16, y+16, 0x44AAAAAA); return; }
                try { ctx.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))), x, y); }
                catch (Exception ignored) {}
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
                if (itemABtn.mouseClicked(click, consumed)) return true;
                if (itemBBtn.mouseClicked(click, consumed)) return true;
                if (slotBtn.mouseClicked(click, consumed)) return true;
                if (deleteBtn.mouseClicked(click, consumed)) return true;
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal(pair.getItemAName() + " ↔ " + pair.getItemBName());
            }
        }
    }
}
