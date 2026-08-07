package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid glass closer to Apple Liquid Glass optics (iOS 26):
 *
 * Per Ken Sorrell / Apple lens model — **flat center, steep rim**:
 * - Circular-arc **surface slope** (∞ at rim → 0 deep inside), not whole-surface zoom
 * - Snell-style bend: `(1 - 1/IOR) * slope * bevel`
 * - Per-channel IOR dispersion (soft chromatic fringe)
 * - Soft Fresnel rim + Blinn-Phong specular
 *
 * Backdrop should stay lightly blurred; heavy blur kills the lens read.
 */
object LiquidRefractionShader {

    const val SOURCE = """
        uniform shader content;
        uniform float2 resolution;
        uniform float cornerRadius;
        uniform float eta;
        uniform float frost;
        uniform float fresnelMin;
        uniform float fresnelMax;
        uniform float specularPower;
        uniform float specularStrength;
        uniform float chromatic;
        uniform float2 lightDir;

        float sdRoundRect(float2 p, float2 halfSize, float r) {
            float2 q = abs(p) - halfSize + float2(r, r);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        }

        float2 sdfNormal(float2 p, float2 halfSize, float r) {
            float e = 1.25;
            float2 ex = float2(e, 0.0);
            float2 ey = float2(0.0, e);
            float2 g = float2(
                sdRoundRect(p + ex, halfSize, r) - sdRoundRect(p - ex, halfSize, r),
                sdRoundRect(p + ey, halfSize, r) - sdRoundRect(p - ey, halfSize, r)
            );
            float len = length(g);
            return len > 0.0001 ? g / len : float2(0.0);
        }

        // t = 0 at rim, 1 past the bevel (center). Slope ∞ at rim → 0 inside.
        float surfaceSlope(float t) {
            float cl = clamp(t, 0.001, 0.999);
            float x = 1.0 - cl;
            return x / max(sqrt(max(1.0 - x * x, 0.0)), 0.001);
        }

        half4 sampleRgb(float2 coord, float2 n, float bend, float ca) {
            // Per-channel IOR split (dispersion), not a crude RGB pixel offset.
            float2 cR = coord - n * (bend * (1.0 - ca));
            float2 cG = coord - n * bend;
            float2 cB = coord - n * (bend * (1.0 + ca));
            half4 sR = content.eval(cR);
            half4 sG = content.eval(cG);
            half4 sB = content.eval(cB);
            return half4(sR.r, sG.g, sB.b, sG.a);
        }

        half4 main(float2 fragCoord) {
            float2 halfSize = resolution * 0.5;
            float2 p = fragCoord - halfSize;
            float r = clamp(cornerRadius, 1.0, min(halfSize.x, halfSize.y));
            float sd = sdRoundRect(p, halfSize, r);
            float edgeDist = max(-sd, 0.0);
            float maxDist = min(halfSize.x, halfSize.y);
            float perimeter = 1.0 - clamp(edgeDist / maxDist, 0.0, 1.0);

            float2 n = sdfNormal(p, halfSize, r);

            // Bezel band: wide enough to read as thick glass (~corner or 28% of half-min).
            float bezel = max(max(r * 1.05, 22.0), maxDist * 0.28);
            float t = clamp(edgeDist / bezel, 0.0, 1.0);
            float slope = surfaceSlope(t);
            // Soft falloff past the bevel so the interior stays flat (Apple: center ≈ passthrough).
            float rimMask = 1.0 - smoothstep(0.55, 1.0, t);

            // Map Polar eta (≈0.05–0.15) → glass IOR ≈ 1.35–1.72.
            float ior = 1.0 + clamp(eta, 0.0, 0.2) * 4.8;
            float snell = 1.0 - 1.0 / ior;
            // Cap so the rim doesn't fishbowl; keep a strong but readable warp.
            float bend = min(slope * snell * bezel * 0.42, bezel * 0.9) * rimMask;

            // Tiny residual center pull — real thick glass magnifies slightly, Apple is subtle.
            float2 rn = p / max(halfSize, float2(1.0));
            float rHat = min(length(rn), 1.0);
            float centerZoom = clamp(eta, 0.0, 0.16) * 0.22 * (1.0 - sqrt(max(1.0 - rHat * rHat, 0.0)));
            float2 zoomPull = p * centerZoom;

            float ca = clamp(chromatic, 0.0, 4.0) * 0.018 * rimMask;
            half4 sampled = sampleRgb(fragCoord - zoomPull, n, bend, ca);
            half3 rgb = sampled.rgb;
            half a = sampled.a;

            // Light frost — keep detail so the lens warp stays readable.
            rgb = mix(rgb, half3(0.96, 0.98, 1.0), half(clamp(frost, 0.0, 1.5) * 0.06));

            // Soft Fresnel rim (glow at grazing edge, not a white wash).
            float fresnelTerm = pow(perimeter, 3.2) * rimMask;
            float fresnelA = mix(fresnelMin, fresnelMax, fresnelTerm);
            rgb = rgb + half3(fresnelA, fresnelA, fresnelA);

            // Wet specular lobe concentrated on the curved rim.
            float3 N = normalize(float3(
                -n.x * (0.25 + 1.1 * rimMask * min(slope * 0.08, 1.0)),
                -n.y * (0.25 + 1.1 * rimMask * min(slope * 0.08, 1.0)),
                1.0
            ));
            float3 L = normalize(float3(lightDir.x, lightDir.y, 0.72));
            float3 V = float3(0.0, 0.0, 1.0);
            float3 H = normalize(L + V);
            float spec = pow(max(dot(N, H), 0.0), max(specularPower, 1.0))
                * specularStrength * (0.2 + 0.8 * rimMask);
            rgb = rgb + half3(spec, spec, spec);

            float inside = smoothstep(1.5, -1.5, sd);
            return half4(clamp(rgb, 0.0, 1.0), a * half(inside));
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun create(): RuntimeShader = RuntimeShader(SOURCE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun update(
        shader: RuntimeShader,
        size: Size,
        cornerRadiusPx: Float,
        eta: Float,
        frost: Float,
        fresnelMin: Float = 0.03f,
        fresnelMax: Float = 0.26f,
        specularPower: Float = 56f,
        specularStrength: Float = 0.55f,
        chromatic: Float = 1.0f,
        lightDirX: Float = -0.55f,
        lightDirY: Float = -0.75f
    ) {
        shader.setFloatUniform("resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
        shader.setFloatUniform("cornerRadius", cornerRadiusPx.coerceAtLeast(1f))
        shader.setFloatUniform("eta", eta)
        shader.setFloatUniform("frost", frost)
        shader.setFloatUniform("fresnelMin", fresnelMin)
        shader.setFloatUniform("fresnelMax", fresnelMax)
        shader.setFloatUniform("specularPower", specularPower)
        shader.setFloatUniform("specularStrength", specularStrength)
        shader.setFloatUniform("chromatic", chromatic)
        shader.setFloatUniform("lightDir", lightDirX, lightDirY)
    }
}
