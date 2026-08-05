package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid-glass refraction (API 33+).
 * Bezel convex warp + subtle full-panel liquid ripple so motion reads live.
 * Profile inspired by https://kube.io/blog/liquid-glass-css-svg
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
            float bz = max(bezel, 0.08);
            float t = clamp(ed / bz, 0.0, 1.0);

            float rim = 1.0 - t;
            rim = rim * rim * (3.0 - 2.0 * rim);
            float mid = 4.0 * t * (1.0 - t);
            float weight = rim * (0.4 + 0.6 * mid);

            float breathe = 1.0 + 0.12 * sin(time * 1.4);
            float mag = intensity * weight * breathe;

            float2 toCenter = normalize(float2(0.5, 0.5) - uv + 0.0001);
            // Full-panel ripple so refraction is obviously "live" even away from bezel
            float ripple = sin(time * 2.2 + uv.x * 12.0 + uv.y * 9.0) * intensity * 0.045;
            float2 base = uv - toCenter * mag + float2(ripple, -ripple * 0.7);

            float ca = chromatic * (weight + 0.35);
            half4 cR = content.eval((base + toCenter * ca) * resolution);
            half4 cG = content.eval(base * resolution);
            half4 cB = content.eval((base - toCenter * ca) * resolution);

            half4 color = half4(cR.r, cG.g, cB.b, max(max(cR.a, cG.a), cB.a));

            if (frost > 0.001) {
                color.rgb = mix(color.rgb, half3(0.94, 0.97, 1.0), half(frost) * 0.12);
            }

            float spec = (weight + 0.15) * 0.16 * (0.55 + 0.45 * sin(time * 0.9 + uv.x * 7.0));
            color.rgb += half3(spec, spec, spec);

            return color;
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
