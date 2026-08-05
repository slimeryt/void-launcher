package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid glass — bezel refraction inspired by
 * https://kube.io/blog/liquid-glass-css-svg
 *
 * Squircle height profile on the rim + convex inward sampling,
 * flat center, chromatic aberration, rim specular.
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

        float squircleHeight(float x) {
            // kube.io convex squircle: y = (1 - (1-x)^4)^(1/4)
            float t = clamp(x, 0.0, 1.0);
            float inner = 1.0 - pow(1.0 - t, 4.0);
            return pow(max(inner, 0.0), 0.25);
        }

        float squircleSlope(float x) {
            float d = 0.02;
            return (squircleHeight(x + d) - squircleHeight(x - d)) / (2.0 * d);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;

            float edX = min(uv.x, 1.0 - uv.x);
            float edY = min(uv.y, 1.0 - uv.y);
            float edgeDist = min(edX, edY);

            float bezelW = max(bezel, 0.06);
            float t = clamp(edgeDist / bezelW, 0.0, 1.0);
            float slope = abs(squircleSlope(t));
            float rim = (1.0 - smoothstep(0.72, 1.0, t));
            float breathe = 1.0 + 0.04 * sin(time * 1.15);
            float mag = intensity * slope * rim * breathe;

            float2 inward = float2(0.0, 0.0);
            if (edX < edY) {
                inward.x = uv.x < 0.5 ? 1.0 : -1.0;
            } else {
                inward.y = uv.y < 0.5 ? 1.0 : -1.0;
            }
            float2 toCenter = normalize(float2(0.5, 0.5) - uv + 1e-5);
            float2 dir = normalize(mix(inward, toCenter, 0.55) + 1e-5);

            // Convex: sample from outside the bezel so wallpaper bends inward
            float2 base = uv - dir * mag * 0.24;

            float ca = chromatic * rim * (0.55 + 0.45 * slope);
            half4 cR = content.eval((base + dir * ca) * resolution);
            half4 cG = content.eval(base * resolution);
            half4 cB = content.eval((base - dir * ca) * resolution);
            half4 color = half4(cR.r, cG.g, cB.b, max(max(cR.a, cG.a), cB.a));

            if (frost > 0.001) {
                half3 frostTint = half3(0.93, 0.96, 0.99);
                color.rgb = mix(color.rgb, frostTint, half(frost) * 0.12);
            }

            float spec = rim * slope * 0.24 * (0.55 + 0.45 * sin(time * 0.7 + uv.x * 6.0));
            color.rgb += half3(spec, spec, spec * 1.05);

            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun create(): RuntimeShader = RuntimeShader(SOURCE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun update(
        shader: RuntimeShader,
        size: Size,
        intensity: Float = 0.55f,
        chromatic: Float = 0.014f,
        frost: Float = 0f,
        time: Float = 0f,
        bezel: Float = 0.16f
    ) {
        shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
        shader.setFloatUniform("intensity", intensity)
        shader.setFloatUniform("chromatic", chromatic)
        shader.setFloatUniform("frost", frost)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("bezel", bezel)
    }
}
