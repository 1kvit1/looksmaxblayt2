package com.looksmax.client;

import com.looksmax.LooksClientConfig;
import com.looksmax.Tracks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Меню LooksMax (клавиша K): выбор трека, громкость, надписи. */
public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.literal("LooksMax - настройки"));
        this.parent = parent;
    }

    private static Component trackLabel(String id) {
        return Component.literal(Tracks.RANDOM.equals(id) ? "Случайный" : Tracks.name(id));
    }

    @Override
    protected void init() {
        int w = 230;
        int x = this.width / 2 - w / 2;
        int y = this.height / 6;
        int step = 26;

        List<String> values = new ArrayList<>();
        values.add(Tracks.RANDOM);
        for (String id : Tracks.IDS) values.add(id);
        String cur = LooksClientConfig.TRACK.get();
        if (!values.contains(cur)) cur = Tracks.RANDOM;

        this.addRenderableWidget(CycleButton.<String>builder(SettingsScreen::trackLabel)
                .withValues(values)
                .withInitialValue(cur)
                .create(x, y, w, 20, Component.literal("Мой трек"), (btn, val) -> {
                    LooksClientConfig.TRACK.set(val);
                    LooksClientConfig.TRACK.save();
                    ClientGameEvents.sendPrefs();
                }));
        y += step;

        this.addRenderableWidget(Button.builder(Component.literal("▶ Прослушать"), b -> {
            String t = LooksClientConfig.TRACK.get();
            ClientMusic.preview(Tracks.isTrack(t) ? t : Tracks.randomId(new java.util.Random()));
        }).bounds(x, y, w / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("■ Стоп"), b -> ClientMusic.stop())
                .bounds(x + w / 2 + 2, y, w / 2 - 2, 20).build());
        y += step;

        this.addRenderableWidget(CycleButton.onOffBuilder(LooksClientConfig.MUSIC_ON.get())
                .create(x, y, w, 20, Component.literal("Музыка мог-батлов"), (btn, val) -> {
                    LooksClientConfig.MUSIC_ON.set(val);
                    LooksClientConfig.MUSIC_ON.save();
                    if (!val) ClientMusic.stop();
                }));
        y += step;

        this.addRenderableWidget(new VolumeSlider(x, y, w, 20));
        y += step;

        this.addRenderableWidget(CycleButton.onOffBuilder(LooksClientConfig.SHOW_TAG.get())
                .create(x, y, w, 20, Component.literal("Рейтинг и 'фармит ауру' над ником"), (btn, val) -> {
                    LooksClientConfig.SHOW_TAG.set(val);
                    LooksClientConfig.SHOW_TAG.save();
                }));
        y += step + 8;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(x, y, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        ClientMusic.stop();
        this.minecraft.setScreen(parent);
    }

    private static final class VolumeSlider extends AbstractSliderButton {
        VolumeSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), LooksClientConfig.VOLUME.get() / 100.0);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal("Громкость музыки: " + (int) Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            LooksClientConfig.VOLUME.set((int) Math.round(this.value * 100));
            LooksClientConfig.VOLUME.save();
        }
    }
}
