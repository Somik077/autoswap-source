package org.funtown.autoswap.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ItemFilter;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.swap.SlotDetector;

import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EditSwapScreen extends Screen {

    private final Screen    parent;
    private final SwapEntry entry;
    private TextFieldWidget labelField;
    private PairListWidget  pairList;

    public EditSwapScreen(Screen parent, SwapEntry entry) {
        super(ModTranslation.t("autoswap.screen.edit.title"));
        this.parent = parent;
        this.entry  = entry;
    }

    @Override
    protected void init() {
        String currentLabel = entry.label.equals("autoswap.entry.default_label")
                ? "" : entry.label;

        labelField = new TextFieldWidget(textRenderer,
                width / 2 - 110, 18, 220, 20,
                ModTranslation.t("autoswap.screen.edit.label_placeholder"));
        labelField.setText(currentLabel);
        labelField.setMaxLength(40);
        labelField.setPlaceholder(ModTranslation.t("autoswap.screen.edit.label_placeholder")
                .copy().formatted(Formatting.DARK_GRAY));
        labelField.setChangedListener(text -> {
            entry.label = text.isEmpty() ? "autoswap.entry.default_label" : text;
            AutoSwapConfig.save();
        });
        addDrawableChild(labelField);

        pairList = new PairListWidget(client, width, height - 80, 48, 44);
        addDrawableChild(pairList);

        addDrawableChild(ButtonWidget.builder(
                ModTranslation.t("autoswap.screen.edit.add_pair"),
                btn -> {
                    entry.pairs.add(new SwapPair());
                    AutoSwapConfig.save();
                    pairList.reload();
                }
        ).dimensions(width / 2 - 102, height - 28, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                ModTranslation.t("autoswap.screen.edit.done"),
                btn -> close()
        ).dimensions(width / 2 + 2, height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, 0xFFFFFF);
        if (entry.pairs.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                    ModTranslation.t("autoswap.screen.edit.empty"),
                    width / 2, height / 2 - 8, 0x888888);
    }

    @Override
    public void close() {
        AutoSwapConfig.save();
        assert client != null;
        client.setScreen(parent);
    }

    

    private String filterLabel(ItemFilter f) {
        String text = (f == null || f.isEmpty())
                ? ModTranslation.get("autoswap.filter.none")
                : "⚙ " + f.describe();
        return textRenderer.trimToWidth(text, 92);
    }

    private boolean shiftHeld() {
        if (client == null) return false;
        long handle = client.getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    class PairListWidget extends EntryListWidget<PairListWidget.PairRow> {

        PairListWidget(MinecraftClient mc, int w, int h, int top, int itemH) {
            super(mc, w, h, top, itemH);
            reload();
        }

        @Override public void appendClickableNarrations(NarrationMessageBuilder b) {}

        void reload() {
            clearEntries();
            for (int i = 0; i < entry.pairs.size(); i++)
                addEntry(new PairRow(entry.pairs.get(i), i));
        }

        @Override public int getRowWidth() { return Math.min(width - 20, 500); }

        class PairRow extends EntryListWidget.Entry<PairRow> {

            private final SwapPair pair;
            private final int      pairIndex;

            private ButtonWidget itemABtn;
            private ButtonWidget itemBBtn;
            private ButtonWidget slotBtn;
            private ButtonWidget filterABtn;
            private ButtonWidget filterBBtn;
            private final ButtonWidget deleteBtn;

            PairRow(SwapPair pair, int pairIndex) {
                this.pair      = pair;
                this.pairIndex = pairIndex;

                itemABtn = ButtonWidget.builder(
                        Text.literal(pair.getItemAName()),
                        btn -> client.setScreen(new ItemPickerScreen(
                                EditSwapScreen.this,
                                item -> {
                                    pair.itemId = Registries.ITEM.getId(item).toString();
                                    
                                    if (!pair.targetSlot.isHotbarLike()) pair.targetSlot = SlotDetector.detectSlot(item);
                                    itemABtn.setMessage(Text.literal(pair.getItemAName()));
                                    slotBtn.setMessage(Text.literal(pair.targetSlot.getDisplayName()));
                                    AutoSwapConfig.save();
                                }))
                ).size(100, 20).build();

                itemBBtn = ButtonWidget.builder(
                        Text.literal(pair.getItemBName()),
                        btn -> client.setScreen(new ItemPickerScreen(
                                EditSwapScreen.this,
                                item -> {
                                    pair.itemId2 = Registries.ITEM.getId(item).toString();
                                    
                                    if (!pair.targetSlot.isHotbarLike()) pair.targetSlot = SlotDetector.detectSlot(item);
                                    itemBBtn.setMessage(Text.literal(pair.getItemBName()));
                                    slotBtn.setMessage(Text.literal(pair.targetSlot.getDisplayName()));
                                    AutoSwapConfig.save();
                                }))
                ).size(100, 20).build();

                filterABtn = ButtonWidget.builder(
                        Text.literal(filterLabel(pair.filterA)),
                        btn -> client.setScreen(new FilterScreen(EditSwapScreen.this, pair.filterA, f -> {
                            pair.filterA = f;
                            AutoSwapConfig.save();
                        }))
                ).size(100, 14).build();

                filterBBtn = ButtonWidget.builder(
                        Text.literal(filterLabel(pair.filterB)),
                        btn -> client.setScreen(new FilterScreen(EditSwapScreen.this, pair.filterB, f -> {
                            pair.filterB = f;
                            AutoSwapConfig.save();
                        }))
                ).size(100, 14).build();

                slotBtn = ButtonWidget.builder(
                        Text.literal(pair.targetSlot.getDisplayName()),
                        btn -> {
                            pair.targetSlot = shiftHeld() ? pair.targetSlot.prev() : pair.targetSlot.next();
                            slotBtn.setMessage(Text.literal(pair.targetSlot.getDisplayName()));
                            AutoSwapConfig.save();
                        }
                ).size(82, 20).build();

                deleteBtn = ButtonWidget.builder(
                        Text.literal("✕").formatted(Formatting.RED),
                        btn -> {
                            entry.pairs.remove(pairIndex);
                            AutoSwapConfig.save();
                            pairList.reload();
                        }
                ).size(20, 20).build();
            }

            @Override
            public void render(DrawContext ctx, int index, int y, int x,
                               int ew, int eh, int mx, int my, boolean hov, float d) {
                int midY  = y + 4;
                int iconY = y + 6;

                if (hov) ctx.fill(x, y, x + ew, y + eh, 0x18FFFFFF);

                
                safeDrawItem(ctx, pair.itemId, x + 2, iconY);
                itemABtn.setPosition(x + 21, midY);
                itemABtn.render(ctx, mx, my, d);

                int p2 = x + 21 + 100 + 4;
                ctx.drawTextWithShadow(client.textRenderer,
                        Text.literal("↔"), p2, midY + 5, 0xAAAAAA);

                
                int bx = p2 + 13;
                safeDrawItem(ctx, pair.itemId2, bx, iconY);
                itemBBtn.setPosition(bx + 19, midY);
                itemBBtn.render(ctx, mx, my, d);

                filterABtn.setPosition(x + 21, y + 27);
                filterABtn.render(ctx, mx, my, d);
                filterBBtn.setPosition(bx + 19, y + 27);
                filterBBtn.render(ctx, mx, my, d);

                int p3 = bx + 19 + 100 + 4;
                ctx.drawTextWithShadow(client.textRenderer,
                        Text.literal("→"), p3, midY + 5, 0x888888);

                slotBtn.setPosition(p3 + 12, midY);
                slotBtn.render(ctx, mx, my, d);

                deleteBtn.setPosition(x + ew - 24, midY);
                deleteBtn.render(ctx, mx, my, d);
            }
            private void safeDrawItem(DrawContext ctx, String itemId, int x, int y) {
                if (client.player == null) {
                    ctx.fill(x, y, x + 16, y + 16, 0x44AAAAAA);
                    return;
                }
                try {
                    ctx.drawItem(
                            new ItemStack(Registries.ITEM.get(Identifier.of(itemId))), x, y);
                } catch (Exception ignored) {}
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                for (var c : children()) if (c.mouseClicked(mx, my, btn)) return true;
                return false;
            }
            public List<? extends net.minecraft.client.gui.Element> children() {
                return List.of(itemABtn, itemBBtn, filterABtn, filterBBtn, slotBtn, deleteBtn);
            }
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
                return List.of(itemABtn, itemBBtn, filterABtn, filterBBtn, slotBtn, deleteBtn);
            }
            public void appendNarrations(NarrationMessageBuilder b) {}
        }
    }
}