package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL lens refraction for API 33+. Distorts UV toward the edges and splits RGB
 * for a chromatic liquid-glass look over the wallpaper buffer.
 */
object LiquidRefractionShader {

    const val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float intensity;
        uniform float chromatic;
        uniform float time;

        float2 distort(float2 uv, float amount) {
            float2 centered = uv * 2.0 - 1.0;
            float r2 = dot(centered, centered);
            // Convex lens + subtle breathing
            float breathe = 1.0 + 0.015 * sin(time * 1.4);
            float k = amount * breathe;
            centered *= 1.0 + k * r2 + (k * 0.35) * r2 * r2;
            return centered * 0.5 + 0.5;
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 base = distort(uv, intensity);

            float2 centered = uv * 2.0 - 1.0;
            float edge = smoothstep(0.15, 1.05, length(centered));
            float ca = chromatic * edge;

            float2 dir = length(centered) > 0.001
                ? normalize(centered)
                : float2(0.0, 0.0);

            float2 uvR = base + dir * ca;
            float2 uvG = base;
            float2 uvB = base - dir * ca;

            half4 cR = content.eval(uvR * resolution);
            half4 cG = content.eval(uvG * resolution);
            half4 cB = content.eval(uvB * resolution);

            return half4(cR.r, cG.g, cB.b, max(max(cR.a, cG.a), cB.a));
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun create(): RuntimeShader = RuntimeShader(SOURCE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun update(shader: RuntimeShader, size: Size, intensity: Float, chromatic: Float, time: Float) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("intensity", intensity)
        shader.setFloatUniform("chromatic", chromatic)
        shader.setFloatUniform("time", time)
    }
}
