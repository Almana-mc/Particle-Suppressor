package me.almana.particlesuppresor.gui;

import me.almana.particlesuppresor.Rules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class InvButton extends AbstractWidget {
    private static final ResourceLocation ICON =
        ResourceLocation.fromNamespaceAndPath("particlesuppresor", "textures/gui/button.png");
    private static final WidgetSprites SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/button"),
        ResourceLocation.withDefaultNamespace("widget/button_disabled"),
        ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
    private static final int SIZE = 16;
    private static final int PAD = 3;

    private final int guiLeft;
    private final int guiTop;
    private boolean dragging;

    public InvButton(int guiLeft, int guiTop) {
        super(guiLeft + Rules.invButtonX, guiTop + Rules.invButtonY, SIZE, SIZE,
            Component.translatable("particlesuppresor.title"));
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        if (button == 0) {
            Minecraft.getInstance().setScreen(new SuppressorScreen());
            return true;
        }
        if (button == 1) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return false;
        setX((int) mouseX - width / 2);
        setY((int) mouseY - height / 2);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 1 || !dragging) return false;
        dragging = false;
        Rules.invButtonX = getX() - guiLeft;
        Rules.invButtonY = getY() - guiTop;
        Rules.save();
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.blitSprite(SPRITES.get(active, isHoveredOrFocused()), getX(), getY(), width, height);
        g.blit(ICON, getX() + PAD, getY() + PAD, width - 2 * PAD, height - 2 * PAD, 0.0F, 0.0F, 512, 512, 512, 512);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
