package org.funtown.autoswap.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.Profile;
import org.funtown.autoswap.config.SwapEntry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoSwapConfigScreen extends Screen {

    private final Screen   parent;
    private SwapListWidget swapList;

    SwapEntry    keyBindTarget = null;
    ButtonWidget keyBindButton = null;

    private static final int TAB_Y    = 28;
    private static final int TAB_H    = 18;
    private static final int LIST_TOP = TAB_Y + TAB_H + 4;

    public AutoSwapConfigScreen(Screen parent) {
        super(ModTranslation.t("autoswap.screen.config.title"));
        this.parent = parent;
    }

    public void refresh() {
        clearAndInit();
    }

    void startKeyBind(SwapEntry entry, ButtonWidget btn) {
        if (keyBindTarget != null)
            keyBindButton.setMessage(Text.literal(keyBindTarget.getKeyDisplayName()));
        keyBindTarget = entry;
        keyBindButton = btn;
        btn.setMessage(ModTranslation.t("autoswap.screen.config.press_key")
                .copy().formatted(Formatting.YELLOW));
    }

    @Override
    protected void init() {
        swapList = new SwapListWidget(client, width, height - LIST_TOP - 30, LIST_TOP, 32);
        addDrawableChild(swapList);

        addDrawableChild(ButtonWidget.builder(
                ModTranslation.t("autoswap.screen.config.add"),
                btn -> {
                    AutoSwapConfig.getInstance().getEntries().add(new SwapEntry());
                    AutoSwapConfig.save();
                    swapList.reload();
                }
        ).dimensions(width / 2 - 156, height - 24, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                ModTranslation.t("autoswap.screen.config.settings"),
                btn -> client.setScreen(new SettingsScreen(this))
        ).dimensions(width / 2 - 50, height - 24, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                ModTranslation.t("autoswap.screen.config.done"),
                btn -> close()
        ).dimensions(width / 2 + 56, height - 24, 100, 20).build());
    }

    

    private int tabWidth() {
        int count = AutoSwapConfig.getInstance().profiles.size() + 1;
        return Math.min(80, (width - 20) / Math.max(count, 1));
    }
    private int tabStartX(int i) {
        int tw = tabWidth();
        int total = AutoSwapConfig.getInstance().profiles.size() + 1;
        return (width - tw * total) / 2 + i * tw;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer,
                ModTranslation.t("autoswap.screen.config.title"), width / 2, 10, 0xFFFFFF);

        List<Profile> profiles = AutoSwapConfig.getInstance().profiles;
        int active = AutoSwapConfig.getInstance().activeProfile;
        int tw     = tabWidth();

        for (int i = 0; i < profiles.size(); i++) {
            int tx  = tabStartX(i);
            boolean sel = (i == active);
            ctx.fill(tx + 1, TAB_Y, tx + tw - 1, TAB_Y + TAB_H,
                    sel ? 0xCC5588CC : 0x88333333);
            String name = profiles.get(i).name;
            if (name.length() > 9) name = name.substring(0, 8) + "…";
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                    tx + tw / 2, TAB_Y + 4, sel ? 0xFFFFFF : 0xAAAAAA);
            if (!sel && mx >= tx && mx < tx + tw && my >= TAB_Y && my < TAB_Y + TAB_H)
                ctx.fill(tx + 1, TAB_Y, tx + tw - 1, TAB_Y + TAB_H, 0x30FFFFFF);
            if (sel && profiles.size() > 1)
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal("✕").formatted(Formatting.RED),
                        tx + tw - 11, TAB_Y + 4, 0xFF5555);
        }

        int addX = tabStartX(profiles.size());
        ctx.fill(addX + 1, TAB_Y, addX + tw - 1, TAB_Y + TAB_H, 0x88333333);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("+").formatted(Formatting.GREEN),
                addX + tw / 2, TAB_Y + 4, 0x55FF55);

        if (AutoSwapConfig.getInstance().getEntries().isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                    ModTranslation.t("autoswap.screen.config.empty"),
                    width / 2, height / 2 - 8, 0x666666);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (my >= TAB_Y && my < TAB_Y + TAB_H) {
            List<Profile> profiles = AutoSwapConfig.getInstance().profiles;
            int tw = tabWidth();

            int addX = tabStartX(profiles.size());
            if (mx >= addX && mx < addX + tw) {
                profiles.add(new Profile("Profile " + (profiles.size() + 1)));
                AutoSwapConfig.getInstance().activeProfile = profiles.size() - 1;
                AutoSwapConfig.save();
                swapList.reload();
                return true;
            }
            for (int i = 0; i < profiles.size(); i++) {
                int tx = tabStartX(i);
                if (mx >= tx && mx < tx + tw) {
                    int active = AutoSwapConfig.getInstance().activeProfile;
                    if (i == active && profiles.size() > 1 && mx >= tx + tw - 14) {
                        profiles.remove(i);
                        AutoSwapConfig.getInstance().activeProfile = Math.max(0, active - 1);
                        AutoSwapConfig.save();
                        swapList.reload();
                        return true;
                    }
                    AutoSwapConfig.getInstance().activeProfile = i;
                    AutoSwapConfig.save();
                    swapList.reload();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyBindTarget != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
                keyBindTarget.keyName = key.getTranslationKey();
                keyBindButton.setMessage(Text.literal(keyBindTarget.getKeyDisplayName()));
                AutoSwapConfig.save();
            } else {
                keyBindButton.setMessage(Text.literal(keyBindTarget.getKeyDisplayName()));
            }
            keyBindTarget = null;
            keyBindButton = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        AutoSwapConfig.save();
        assert client != null;
        client.setScreen(parent);
    }

    

    class SwapListWidget extends EntryListWidget<SwapListWidget.EntryRow> {

        SwapListWidget(MinecraftClient mc, int w, int h, int top, int itemH) {
            super(mc, w, h, top, itemH);
            reload();
        }

        @Override public void appendClickableNarrations(NarrationMessageBuilder b) {}

        void reload() {
            clearEntries();
            List<SwapEntry> entries = AutoSwapConfig.getInstance().getEntries();
            for (int i = 0; i < entries.size(); i++)
                addEntry(new EntryRow(entries.get(i), i));
        }

        @Override public int getRowWidth() { return Math.min(width - 20, 400); }

        class EntryRow extends EntryListWidget.Entry<EntryRow> {

            private final SwapEntry entry;
            private final int       idx;
            private       ButtonWidget keyBtn;
            private final ButtonWidget editBtn;
            private final ButtonWidget deleteBtn;

            EntryRow(SwapEntry entry, int idx) {
                this.entry = entry;
                this.idx   = idx;

                keyBtn = ButtonWidget.builder(
                        Text.literal(entry.getKeyDisplayName()),
                        btn -> AutoSwapConfigScreen.this.startKeyBind(entry, keyBtn)
                ).size(70, 20).build();

                editBtn = ButtonWidget.builder(
                        ModTranslation.t("autoswap.screen.config.edit"),
                        btn -> client.setScreen(new EditSwapScreen(AutoSwapConfigScreen.this, entry))
                ).size(60, 20).build();

                deleteBtn = ButtonWidget.builder(
                        Text.literal("✕").formatted(Formatting.RED),
                        btn -> {
                            AutoSwapConfig.getInstance().getEntries().remove(idx);
                            AutoSwapConfig.save();
                            swapList.reload();
                        }
                ).size(20, 20).build();
            }

            @Override
            public void render(DrawContext ctx, int index, int y, int x,
                               int ew, int eh, int mx, int my, boolean hov, float d) {
                int midY = y + (eh - 20) / 2;
                if (hov) ctx.fill(x, y, x + ew, y + eh, 0x18FFFFFF);

                String label = entry.getDisplayLabel();
                ctx.drawTextWithShadow(client.textRenderer,
                        Text.literal(label).formatted(
                                entry.pairs.isEmpty() ? Formatting.GRAY : Formatting.WHITE),
                        x + 4, midY + 5, 0xFFFFFF);

                int count = entry.pairs.size();
                if (count == 0)
                    ctx.drawTextWithShadow(client.textRenderer,
                            ModTranslation.t("autoswap.screen.config.no_pairs")
                                    .copy().formatted(Formatting.DARK_RED),
                            x + 4 + textRenderer.getWidth(label) + 2, midY + 5, 0xFF5555);
                else if (count > 1)
                    ctx.drawTextWithShadow(client.textRenderer,
                            Text.literal(" (" + count + ")").formatted(Formatting.DARK_GRAY),
                            x + 4 + textRenderer.getWidth(label), midY + 5, 0x666666);

                deleteBtn.setPosition(x + ew - 24, midY);
                editBtn.setPosition(x + ew - 88, midY);
                keyBtn.setPosition(x + ew - 162, midY);
                keyBtn.render(ctx, mx, my, d);
                editBtn.render(ctx, mx, my, d);
                deleteBtn.render(ctx, mx, my, d);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                for (var c : children()) if (c.mouseClicked(mx, my, btn)) return true;
                return false;
            }
            public List<? extends net.minecraft.client.gui.Element> children() {
                return List.of(keyBtn, editBtn, deleteBtn);
            }
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
                return List.of(keyBtn, editBtn, deleteBtn);
            }
            public void appendNarrations(NarrationMessageBuilder b) {}
        }
    }
}