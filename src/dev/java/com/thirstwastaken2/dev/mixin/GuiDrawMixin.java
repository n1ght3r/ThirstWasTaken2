package com.thirstwastaken2.dev.mixin;

//? if >=1.21.2 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//?}
import com.thirstwastaken2.dev.agent.thirst.HudRecord;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the rectangles of vanilla's food and air sprites for numeric HUD layout checks, in screen
 * space: the sprite's position is put through the current pose, because a status bar can be moved by
 * translating the pose rather than by the coordinates it is drawn at. Fabric API's height registry
 * moves vanilla's air row that way from 1.21.6, and {@code GuiMixin} does on 1.21.1.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiDrawMixin {
    // The render pipeline parameter was added with the rendering changes after 1.21.1.
    //? if >=1.21.2 {
    @Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
            at = @At("HEAD"))
    private void thirst$recordHudSprite(RenderPipeline pipeline, Identifier sprite, int x, int y,
                                        int width, int height, CallbackInfo info) {
        HudRecord.sprite(sprite.toString(), ((GuiGraphicsExtractor) (Object) this).pose().transformPosition(x, y, new org.joml.Vector2f()), width, height);
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "blitSprite(Lnet/minecraft/resources/Identifier;IIII)V", at = @At("HEAD"))
    private void thirst$recordHudSprite(Identifier sprite, int x, int y, int width, int height,
                                        CallbackInfo info) {
        HudRecord.sprite(sprite.toString(), ((GuiGraphicsExtractor) (Object) this).pose().last().pose().transformPosition(x, y, 0.0F, new org.joml.Vector3f()), width, height);
    }
    *///?} else {
    /*// 1.20.1 has no GUI atlas: the food and air icons are regions of icons.png, named here by where
    // they sit on it, the plain food background and the hunger one, then a full bubble and a bursting one.
    @Inject(method = "blit(Lnet/minecraft/resources/Identifier;IIIIII)V", at = @At("HEAD"))
    private void thirst$recordHudSprite(Identifier texture, int x, int y, int u, int v, int width, int height,
                                        CallbackInfo info) {
        if (!texture.getPath().equals("textures/gui/icons.png")) return;
        String sprite = v == 27 && (u == 16 || u == 133) ? "minecraft:hud/food_empty"
                : v == 18 && (u == 16 || u == 25) ? "minecraft:hud/air" : null;
        if (sprite == null) return;
        HudRecord.sprite(sprite, ((GuiGraphicsExtractor) (Object) this).pose().last().pose().transformPosition(x, y, 0.0F, new org.joml.Vector3f()), width, height);
    }
    *///?}
}
