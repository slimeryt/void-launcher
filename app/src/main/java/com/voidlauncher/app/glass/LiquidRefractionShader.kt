package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid glass closer to Apple Liquid Glass optics (iOS 26):
 *
 * - Circular-arc **surface slope** (steep at rim → flat deep inside)
 * - Snell-style bend: `(1 - 1/IOR) * slope * bevel`
 * - Soft whole-slab residual zoom so short bars (search/dock) lens across the face,
 *   not only a thin edge fringe
 * - Per-channel IOR dispersion (soft chromatic fringe)
 * - Soft Fresnel rim + Blinn-Phong specular
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

        // t = 0 at rim, 1 past the bevel. Slope ∞ at rim → 0 inside.
        float surfaceSlope(float t) {
            float cl = clamp(t, 0.001, 0.999);
            float x = 1.0 - cl;
            return x / max(sqrt(max(1.0 - x * x, 0.0)), 0.001);
        }

        half4 sampleRgb(float2 coord, float2 n, float bend, float ca) {
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

            // Thick glass band: short pills (search/dock) are almost all “rim”.
            float bezel = max(max(r * 1.15, 26.0), maxDist * 0.55);
            float t = clamp(edgeDist / bezel, 0.0, 1.0);
            float slope = surfaceSlope(t);
            // Soften only the deepest interior — keep bend alive across dock face.
            float rimMask = 1.0 - smoothstep(0.72, 1.0, t);

            float ior = 1.0 + clamp(eta, 0.0, 0.2) * 5.2;
            float snell = 1.0 - 1.0 / ior;
            // Slope already → 0 at center; don't kill mid-face with rimMask.
            float bend = min(slope * snell * bezel * 0.5, bezel * 0.95);

            // Residual slab zoom: thin bars need this or only the edge “spreads”.
            float2 rn = p / max(halfSize, float2(1.0));
            float rHat = min(length(rn), 1.0);
            float zoomCap = 1.0 - sqrt(max(1.0 - rHat * rHat, 0.0));
            float centerZoom = clamp(eta, 0.0, 0.16) * 0.55 * zoomCap;
            float2 zoomPull = p * centerZoom;

            float ca = clamp(chromatic, 0.0, 4.0) * 0.022 * max(rimMask, 0.35);
            half4 sampled = sampleRgb(fragCoord - zoomPull, n, bend, ca);
            half3 rgb = sampled.rgb;
            half a = sampled.a;

            rgb = mix(rgb, half3(0.96, 0.98, 1.0), half(clamp(frost, 0.0, 1.5) * 0.055));

            float fresnelTerm = pow(perimeter, 2.8) * rimMask;
            float fresnelA = mix(fresnelMin, fresnelMax, fresnelTerm);
            rgb = rgb + half3(fresnelA, fresnelA, fresnelA);

            float3 N = normalize(float3(
                -n.x * (0.28 + 1.0 * rimMask * min(slope * 0.07, 1.0)),
                -n.y * (0.28 + 1.0 * rimMask * min(slope * 0.07, 1.0)),
                1.0
            ));
            float3 L = normalize(float3(lightDir.x, lightDir.y, 0.72));
            float3 V = float3(0.0, 0.0, 1.0);
            float3 H = normalize(L + V);
            float spec = pow(max(dot(N, H), 0.0), max(specularPower, 1.0))
                * specularStrength * (0.25 + 0.75 * rimMask);
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
