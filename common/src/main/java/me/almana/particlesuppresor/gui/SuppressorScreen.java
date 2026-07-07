package me.almana.particlesuppresor.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import me.almana.particlesuppresor.Anchor;
import me.almana.particlesuppresor.Rules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SuppressorScreen extends Screen {
    private int tab;
    private String query = "";
    private EditBox search;
    private PList list;

    public SuppressorScreen() {
        super(Component.translatable("particlesuppresor.title"));
    }

    @Override
    protected void init() {
        boolean canAdd = Rules.anchors.size() < Rules.maxAnchors;
        int tabCount = 1 + Rules.anchors.size() + (canAdd ? 1 : 0);
        int tx = width / 2 - tabCount * 11;
        addTab(tx, 0, Component.literal("G"));
        for (int i = 1; i <= Rules.anchors.size(); i++) {
            addTab(tx + i * 22, i, Component.literal(String.valueOf(i)));
        }
        if (canAdd) {
            addRenderableWidget(Button.builder(Component.literal("+"), b -> addAnchor())
                .bounds(tx + (tabCount - 1) * 22, 22, 20, 20).build());
        }

        search = new EditBox(font, width / 2 - 100, 48, 200, 18, Component.translatable("particlesuppresor.search"));
        search.setValue(query);
        search.setResponder(s -> {
            query = s;
            refill();
        });
        search.setHint(Component.translatable("particlesuppresor.search.hint"));
        addRenderableWidget(search);

        list = new PList();
        addRenderableWidget(list);

        if (tab > 0) initAnchorControls();

        addRenderableWidget(CycleButton.onOffBuilder(Rules.master).create(
            width / 2 - 154, height - 26, 100, 20,
            Component.translatable("particlesuppresor.master"), (b, v) -> {
                Rules.master = v;
                Rules.recompute();
            }));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .bounds(width / 2 + 54, height - 26, 100, 20).build());

        refill();
    }

    private void addTab(int x, int index, Component label) {
        Button b = Button.builder(label, btn -> {
            tab = index;
            rebuildWidgets();
        }).bounds(x, 22, 20, 20).build();
        b.active = tab != index;
        addRenderableWidget(b);
    }

    private void addAnchor() {
        Anchor anchor = new Anchor();
        anchor.name = "Anchor " + (Rules.anchors.size() + 1);
        anchor.setPos();
        Rules.anchors.add(anchor);
        Rules.recompute();
        tab = Rules.anchors.size();
        rebuildWidgets();
    }

    private void initAnchorControls() {
        Anchor anchor = Rules.anchors.get(tab - 1);
        int y = height - 52;
        EditBox name = new EditBox(font, width / 2 - 154, y + 1, 100, 18, Component.translatable("particlesuppresor.anchor.name"));
        name.setValue(anchor.name);
        name.setResponder(s -> anchor.name = s);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.literal("-"), b -> anchor.radius = Math.max(1, anchor.radius - 1))
            .bounds(width / 2 - 48, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> anchor.radius = Math.min(48, anchor.radius + 1))
            .bounds(width / 2 + 14, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("particlesuppresor.anchor.setpos"), b -> anchor.setPos())
            .bounds(width / 2 + 40, y, 56, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("particlesuppresor.anchor.delete"), b -> {
            Rules.anchors.remove(tab - 1);
            tab = 0;
            Rules.recompute();
            rebuildWidgets();
        }).bounds(width / 2 + 100, y, 54, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        if (tab > 0) {
            g.drawCenteredString(font, "R: " + Rules.anchors.get(tab - 1).radius, width / 2 - 7, height - 46, 0xFFFFFF);
        }
    }

    private void refill() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> ids = new ArrayList<>();
        if (q.isEmpty()) {
            Set<ResourceLocation> seen = new LinkedHashSet<>();
            configured().stream().sorted().forEach(seen::add);
            List<ResourceLocation> rec;
            synchronized (Rules.recent) {
                rec = new ArrayList<>(Rules.recent.keySet());
            }
            Collections.reverse(rec);
            seen.addAll(rec);
            ids.addAll(seen);
        } else {
            BuiltInRegistries.PARTICLE_TYPE.keySet().stream()
                .filter(id -> id.toString().contains(q)).sorted().forEach(ids::add);
        }
        list.refill(ids);
    }

    private Collection<ResourceLocation> configured() {
        if (tab == 0) return new ArrayList<>(Rules.global.keySet());
        return Rules.anchors.get(tab - 1).densities.keySet().stream().map(ResourceLocation::parse).toList();
    }

    private int density(ResourceLocation id) {
        return tab == 0 ? Rules.global.getInt(id) : Rules.anchors.get(tab - 1).getDensity(id);
    }

    private void setDensity(ResourceLocation id, int value) {
        if (tab == 0) {
            Rules.setGlobal(id, value);
        } else {
            Rules.anchors.get(tab - 1).setDensity(id, value);
            Rules.recompute();
        }
    }

    @Override
    public void removed() {
        Rules.save();
    }

    private class PList extends ContainerObjectSelectionList<Row> {
        PList() {
            super(Minecraft.getInstance(), SuppressorScreen.this.width, SuppressorScreen.this.height - 135, 75, 24);
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        void refill(List<ResourceLocation> ids) {
            clearEntries();
            ids.forEach(id -> addEntry(new Row(id)));
            setScrollAmount(0);
        }
    }

    private class Row extends ContainerObjectSelectionList.Entry<Row> {
        final ResourceLocation id;
        final DensitySlider slider;
        final Button toggle;
        int restore;

        Row(ResourceLocation id) {
            this.id = id;
            int d = density(id);
            restore = d > 0 ? d : 100;
            slider = new DensitySlider(this, d);
            toggle = Button.builder(label(d), b -> flip()).size(36, 20).build();
        }

        static Component label(int d) {
            return d > 0 ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        }

        void flip() {
            int d = density(id);
            if (d > 0) {
                restore = d;
                apply(0);
            } else {
                apply(restore);
            }
        }

        void apply(int d) {
            setDensity(id, d);
            if (d > 0) restore = d;
            slider.sync(d);
            toggle.setMessage(label(d));
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovering, float partialTick) {
            g.drawString(font, font.plainSubstrByWidth(id.toString(), width - 152), left + 4, top + 8, 0xFFFFFF);
            slider.setX(left + width - 146);
            slider.setY(top + 1);
            slider.render(g, mouseX, mouseY, partialTick);
            toggle.setX(left + width - 40);
            toggle.setY(top + 1);
            toggle.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(slider, toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(slider, toggle);
        }
    }

    private class DensitySlider extends AbstractSliderButton {
        final Row row;

        DensitySlider(Row row, int density) {
            super(0, 0, 100, 20, Component.empty(), density / 100.0);
            this.row = row;
            updateMessage();
        }

        void sync(int density) {
            value = density / 100.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(Math.round(value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            int d = (int) Math.round(value * 100);
            setDensity(row.id, d);
            if (d > 0) row.restore = d;
            row.toggle.setMessage(Row.label(d));
        }
    }
}
