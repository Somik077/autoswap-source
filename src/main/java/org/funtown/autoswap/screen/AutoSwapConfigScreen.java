package org.funtown.autoswap.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
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
    Button       keyBindButton = null;

    private static final int TAB_Y    = 28;
    private static final int TAB_H    = 18;
    private static final int LIST_TOP = TAB_Y + TAB_H + 4;

    public AutoSwapConfigScreen(Screen parent) {
        super(ModTranslation.t("autoswap.screen.config.title"));
        this.parent = parent;
    }

    public void refresh() { rebuildWidgets(); }

    void startKeyBind(SwapEntry entry, Button btn) {
        if (keyBindTarget != null)
            keyBindButton.setMessage(Component.literal(keyBindTarget.getKeyDisplayName()));
        keyBindTarget = entry;
        keyBindButton = btn;
        btn.setMessage(ModTranslation.t("autoswap.screen.config.press_key")
                .copy().withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected void init() {
        swapList = new SwapListWidget(minecraft, width, height - LIST_TOP - 30, LIST_TOP, 32);
        addRenderableWidget(swapList);

        addRenderableWidget(Button.builder(
                ModTranslation.t("autoswap.screen.config.add"),
                btn -> { AutoSwapConfig.getInstance().getEntries().add(new SwapEntry()); AutoSwapConfig.save(); swapList.reload(); }
        ).bounds(width / 2 - 156, height - 24, 100, 20).build());

        addRenderableWidget(Button.builder(
                ModTranslation.t("autoswap.screen.config.settings"),
                btn -> minecraft.setScreen(new SettingsScreen(this))
        ).bounds(width / 2 - 50, height - 24, 100, 20).build());

        addRenderableWidget(Button.builder(
                ModTranslation.t("autoswap.screen.config.done"),
                btn -> onClose()
        ).bounds(width / 2 + 56, height - 24, 100, 20).build());
    }

    private int tabWidth() {
        return Math.min(80, (width - 20) / Math.max(AutoSwapConfig.getInstance().profiles.size() + 1, 1));
    }
    private int tabStartX(int i) {
        int tw = tabWidth(), total = AutoSwapConfig.getInstance().profiles.size() + 1;
        return (width - tw * total) / 2 + i * tw;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        super.extractRenderState(ctx, mx, my, delta);
        Font font = getFont();
        ctx.centeredText(font, ModTranslation.t("autoswap.screen.config.title"), width / 2, 10, 0xFFFFFFFF);

        List<Profile> profiles = AutoSwapConfig.getInstance().profiles;
        int active = AutoSwapConfig.getInstance().activeProfile, tw = tabWidth();
        for (int i = 0; i < profiles.size(); i++) {
            int tx = tabStartX(i); boolean sel = (i == active);
            ctx.fill(tx+1, TAB_Y, tx+tw-1, TAB_Y+TAB_H, sel ? 0xCC5588CC : 0x88333333);
            String name = profiles.get(i).name;
            if (name.length() > 9) name = name.substring(0,8)+"…";
            ctx.centeredText(font, Component.literal(name), tx+tw/2, TAB_Y+4, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
            if (!sel && mx>=tx && mx<tx+tw && my>=TAB_Y && my<TAB_Y+TAB_H)
                ctx.fill(tx+1, TAB_Y, tx+tw-1, TAB_Y+TAB_H, 0x30FFFFFF);
            if (sel && profiles.size()>1)
                ctx.text(font, Component.literal("✕").withStyle(ChatFormatting.RED), tx+tw-11, TAB_Y+4, 0xFFFF5555);
        }
        int addX = tabStartX(profiles.size());
        ctx.fill(addX+1, TAB_Y, addX+tw-1, TAB_Y+TAB_H, 0x88333333);
        ctx.centeredText(font, Component.literal("+").withStyle(ChatFormatting.GREEN), addX+tw/2, TAB_Y+4, 0xFF55FF55);
        if (AutoSwapConfig.getInstance().getEntries().isEmpty())
            ctx.centeredText(font, ModTranslation.t("autoswap.screen.config.empty"), width/2, height/2-8, 0xFF666666);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
        double mx = click.x(), my = click.y();
        if (my >= TAB_Y && my < TAB_Y + TAB_H) {
            List<Profile> profiles = AutoSwapConfig.getInstance().profiles;
            int tw = tabWidth(), addX = tabStartX(profiles.size());
            if (mx >= addX && mx < addX + tw) {
                profiles.add(new Profile("Profile " + (profiles.size()+1)));
                AutoSwapConfig.getInstance().activeProfile = profiles.size()-1;
                AutoSwapConfig.save(); swapList.reload(); return true;
            }
            for (int i = 0; i < profiles.size(); i++) {
                int tx = tabStartX(i);
                if (mx >= tx && mx < tx + tw) {
                    int active = AutoSwapConfig.getInstance().activeProfile;
                    if (i == active && profiles.size() > 1 && mx >= tx+tw-14) {
                        profiles.remove(i);
                        AutoSwapConfig.getInstance().activeProfile = Math.max(0, active-1);
                        AutoSwapConfig.save(); swapList.reload(); return true;
                    }
                    AutoSwapConfig.getInstance().activeProfile = i;
                    AutoSwapConfig.save(); swapList.reload(); return true;
                }
            }
        }
        return super.mouseClicked(click, consumed);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (keyBindTarget != null) {
            InputConstants.Key key = InputConstants.getKey(input);
            if (key.getValue() != GLFW.GLFW_KEY_ESCAPE) {
                keyBindTarget.keyName = key.getName();
                keyBindButton.setMessage(Component.literal(keyBindTarget.getKeyDisplayName()));
                AutoSwapConfig.save();
            } else {
                keyBindButton.setMessage(Component.literal(keyBindTarget.getKeyDisplayName()));
            }
            keyBindTarget = null; keyBindButton = null;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() { AutoSwapConfig.save(); minecraft.setScreen(parent); }

    class SwapListWidget extends ObjectSelectionList<SwapListWidget.EntryRow> {

        SwapListWidget(Minecraft mc, int w, int h, int top, int itemH) {
            super(mc, w, h, top, itemH); reload();
        }

        void reload() {
            clearEntries();
            List<SwapEntry> entries = AutoSwapConfig.getInstance().getEntries();
            for (int i = 0; i < entries.size(); i++) addEntry(new EntryRow(entries.get(i), i));
        }

        @Override public int getRowWidth() { return Math.min(width - 20, 400); }

        class EntryRow extends ObjectSelectionList.Entry<EntryRow> {

            private final SwapEntry entry;
            private final int       idx;
            private       Button    keyBtn;
            private final Button    editBtn;
            private final Button    deleteBtn;

            EntryRow(SwapEntry entry, int idx) {
                this.entry = entry; this.idx = idx;
                keyBtn = Button.builder(Component.literal(entry.getKeyDisplayName()),
                        btn -> AutoSwapConfigScreen.this.startKeyBind(entry, keyBtn)
                ).size(70, 20).build();
                editBtn = Button.builder(ModTranslation.t("autoswap.screen.config.edit"),
                        btn -> minecraft.setScreen(new EditSwapScreen(AutoSwapConfigScreen.this, entry))
                ).size(60, 20).build();
                deleteBtn = Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), btn -> {
                    AutoSwapConfig.getInstance().getEntries().remove(idx);
                    AutoSwapConfig.save(); swapList.reload();
                }).size(20, 20).build();
            }

            @Override
            public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, boolean hov, float d) {
                Font font = getFont();
                int x = getX(), ew = getWidth();
                int y = getY();
                int midY = y + (32 - 20) / 2;

                if (hov) ctx.fill(x, y, x+ew, y+32, 0x18FFFFFF);

                String label = entry.getDisplayLabel();
                ctx.text(font, Component.literal(label).withStyle(
                                entry.pairs.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.WHITE),
                        x+4, midY+5, 0xFFFFFFFF);
                int count = entry.pairs.size();
                if (count == 0)
                    ctx.text(font, ModTranslation.t("autoswap.screen.config.no_pairs")
                                    .copy().withStyle(ChatFormatting.DARK_RED),
                            x+4+font.width(label)+2, midY+5, 0xFFFF5555);
                else if (count > 1)
                    ctx.text(font, Component.literal(" ("+count+")").withStyle(ChatFormatting.DARK_GRAY),
                            x+4+font.width(label), midY+5, 0xFF666666);

                deleteBtn.setX(x+ew-24);  deleteBtn.setY(midY);
                editBtn.setX(x+ew-88);    editBtn.setY(midY);
                keyBtn.setX(x+ew-162);    keyBtn.setY(midY);
                keyBtn.extractRenderState(ctx, mouseX, mouseY, d);
                editBtn.extractRenderState(ctx, mouseX, mouseY, d);
                deleteBtn.extractRenderState(ctx, mouseX, mouseY, d);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
                if (keyBtn.mouseClicked(click, consumed)) return true;
                if (editBtn.mouseClicked(click, consumed)) return true;
                if (deleteBtn.mouseClicked(click, consumed)) return true;
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal(entry.getDisplayLabel());
            }
        }
    }
}
