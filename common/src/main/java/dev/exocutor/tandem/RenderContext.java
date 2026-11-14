package dev.exocutor.tandem;

import dev.exocutor.tandem.mixin.GameRenderStateAccess;
import dev.exocutor.tandem.mixin.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public final class RenderContext {
    public final LevelRenderer renderer;
    public final LevelExtractor extractor;
    public final LevelRenderState state;
    private final boolean owned;
    private boolean closed;
    private int width, height;

    private RenderContext(LevelRenderer renderer, LevelExtractor extractor, LevelRenderState state, boolean owned) {
        this.renderer = renderer;
        this.extractor = extractor;
        this.state = state;
        this.owned = owned;
        width = Tandem.client().getWindow().getWidth();
        height = Tandem.client().getWindow().getHeight();
    }

    public static RenderContext current(Minecraft mc) {
        return new RenderContext(mc.levelRenderer, mc.levelExtractor, mc.gameRenderer.gameRenderState().levelRenderState, false);
    }

    public static RenderContext create(Minecraft mc) {
        var gameState = mc.gameRenderer.gameRenderState();
        LevelRenderState previous = gameState.levelRenderState;
        LevelRenderState state = new LevelRenderState();
        ((GameRenderStateAccess) gameState).tandem$level(state);
        try {
            LevelRenderer renderer = new LevelRenderer(mc.getEntityRenderDispatcher(), mc.getBlockEntityRenderDispatcher(), mc.getModelManager(),
                    mc.getTextureManager(), mc.getAtlasManager(), mc.getShaderManager(), mc.gameRenderer, mc.getWindow().getWidth(), mc.getWindow().getHeight());
            return new RenderContext(renderer, new LevelExtractor(mc, state, renderer), state, true);
        } finally {
            ((GameRenderStateAccess) gameState).tandem$level(previous);
        }
    }

    public void bind(Minecraft mc) {
        MinecraftAccess access = (MinecraftAccess) mc;
        access.tandem$renderer(renderer);
        access.tandem$extractor(extractor);
        ((GameRenderStateAccess) mc.gameRenderer.gameRenderState()).tandem$level(state);
    }

    public void close() {
        if (owned && !closed) {
            closed = true;
            renderer.close();
        }
    }

    public void resize(Minecraft mc) {
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != width || h != height) {
            renderer.resize(w, h);
            width = w;
            height = h;
        }
    }
}
