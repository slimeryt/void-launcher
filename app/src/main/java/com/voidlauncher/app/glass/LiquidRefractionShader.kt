package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid-glass refraction (API 33+).
 * Warps the wallpaper sample like a convex lens and adds light chromatic aberration
 * toward the edges so the dock/panels actually refract what's behind them.
 */
object LiquidRefractionShader {

    const val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float intensity;
        uniform float chromatic;
        uniform float time;
        uniform float frost;

        float2 distort(float2 uv, float amount) {
            float2 c = uv * 2.0 - 1.0;
            float r2 = dot(c, c);
            float breathe = 1.0 + 0.02 * sin(time * 1.25);
            float k = amount * breathe;
            // Barrel / lens warp — stronger toward edges
            c *= 1.0 + k * r2 + 0.45 * k * r2 * r2;
            return clamp(c * 0.5 + 0.5, 0.0, 1.0);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 centered = uv * 2.0 - 1.0;
            float edge = smoothstep(0.05, 1.1, length(centered));

            float2 base = distort(uv, intensity);

            float2 dir = length(centered) > 0.001 ? normalize(centered) : float2(0.0, 0.0);
            float ca = chromatic * edge;

            half4 cR = content.eval((base + dir * ca) * resolution);
            half4 cG = content.eval(base * resolution);
            half4 cB = content.eval((base - dir * ca) * resolution);

            half4 color = half4(cR.r, cG.g, cB.b, max(max(cR.a, cG.a), cB.a));

            // Soft frost wash kept mild so refraction stays readable
            half3 frostTint = half3(0.92, 0.95, 0.98);
            color.rgb = mix(color.rgb, frostTint, half(frost) * 0.35);

            // Specular catch near top
            float spec = smoothstep(0.55, 0.0, uv.y) * 0.22;
            color.rgb += half(spec);

            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun create(): RuntimeShader = RuntimeShader(SOURCE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun update(
        shader: RuntimeShader,
        size: Size,
        intensity: Float = 0.18f,
        chromatic: Float = 0.012f,
        frost: Float = 0.45f,
        time: Float = 0f
    ) {
        shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
        shader.setFloatUniform("intensity", intensity)
        shader.setFloatUniform("chromatic", chromatic)
        shader.setFloatUniform("frost", frost)
        shader.setFloatUniform("time", time)
    }
}
