package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid-glass edge treatment (API 33+).
 *
 * Deliberately does NOT displace/magnify content. An earlier version warped UVs
 * near the edge to fake a convex lens bulge; on any wallpaper whose color changes
 * within a bezel's width of the panel edge (which is most wallpapers, most of the
 * time), that displacement visibly drags unrelated content in and stretches it —
 * reading as "the wallpaper got zoomed/pushed outward," not glass. Real glass at
 * normal viewing distance mostly reads via blur + a soft rim highlight + a
 * whisper-thin color fringe right at the boundary — not moved pixels. So: fixed,
 * tiny (~1-2px) chromatic fringe confined to a thin edge ring, plus rim brightening.
 * No animation; only the separate sheen highlight (driven from GlassPanel) moves.
 */
object LiquidRefractionShader {

    const val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float intensity;
        uniform float chromatic;
        uniform float time;
        uniform float frost;
        uniform float bezel;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;

            float ed = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
            float bz = max(bezel, 0.04);
            float t = clamp(ed / bz, 0.0, 1.0);

            // Ring-shaped weight: 0 right at the true edge and well before the
            // interior, peaking mid-bezel. Only used to fade the fringe/highlight —
            // never to move sample coordinates.
            float rise = smoothstep(0.0, 0.4, t);
            float fall = 1.0 - smoothstep(0.6, 1.0, t);
            float weight = rise * fall;

            // Chromatic uniform is a PIXEL offset now (not a UV fraction), so it can
            // never pull content from meaningfully outside the true edge no matter
            // what's in the source image.
            float caPx = chromatic * weight;
            float2 dir = normalize(uv - float2(0.5, 0.5) + 0.0001);
            half4 cR = content.eval(fragCoord + dir * caPx);
            half4 cG = content.eval(fragCoord);
            half4 cB = content.eval(fragCoord - dir * caPx);

            half3 rgb = half3(cR.r, cG.g, cB.b);
            half a = cG.a;

            half3 frosted = mix(rgb, half3(0.94, 0.97, 1.0), half(frost) * 0.12);
            rgb = frost > 0.001 ? frosted : rgb;

            // Static rim highlight — brightest at the bezel, no time-based shimmer.
            // `intensity` now controls rim brightness strength, not displacement.
            half spec = half((weight + 0.15) * intensity);
            rgb = rgb + half3(spec, spec, spec);

            return half4(rgb, a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun create(): RuntimeShader = RuntimeShader(SOURCE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun update(
        shader: RuntimeShader,
        size: Size,
        intensity: Float,
        chromatic: Float,
        frost: Float,
        time: Float,
        bezel: Float
    ) {
        shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
        shader.setFloatUniform("intensity", intensity)
        shader.setFloatUniform("chromatic", chromatic)
        shader.setFloatUniform("frost", frost)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("bezel", bezel)
    }
}
