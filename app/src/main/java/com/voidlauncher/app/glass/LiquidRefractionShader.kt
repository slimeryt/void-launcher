package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid glass tuned toward Apple Liquid Glass optics:
 * - Spherical-cap **zoom** across the whole surface (thick-glass magnification)
 * - Circular **rim bezel** refraction along the SDF normal
 * - Soft chromatic fringe, Fresnel rim, Blinn-Phong specular
 *
 * Backdrop should be only lightly blurred; heavy blur kills the lens read.
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

        // Convex glass edge: 0 flat inside the band → 1 at the rim (infinite slope).
        float lensProfile(float edgeDist, float height) {
            if (height <= 0.0001 || edgeDist >= height) return 0.0;
            float x = 1.0 - edgeDist / height;
            return 1.0 - sqrt(max(1.0 - x * x, 0.0));
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

            // --- Apple-style whole-surface zoom (spherical cap) ---
            // Sample is pulled toward center → backdrop appears magnified.
            // Elliptical r̂ so pills/rounded rects lens correctly.
            float2 invHalf = 1.0 / max(halfSize, float2(1.0));
            float2 rn = p * invHalf;
            float rHat = min(length(rn), 1.0);
            float zoomCap = 1.0 - sqrt(max(1.0 - rHat * rHat, 0.0));
            float etaClamped = clamp(eta, 0.0, 0.16);
            // ~0.12 default feel at eta=0.1 (glasskit-like thick glass).
            float zoom = etaClamped * 1.35;
            float2 zoomPull = p * (zoom * zoomCap);

            // --- Rim bezel refraction (circular profile along SDF normal) ---
            float bezel = max(r * 0.95, 18.0);
            float rimLens = lensProfile(edgeDist, bezel);
            float bendPx = etaClamped * 220.0 * rimLens;
            float2 rimPull = n * bendPx;

            float2 sampleCoord = fragCoord - zoomPull - rimPull;

            // Soft RGB split only in the curved rim (iOS is subtle, not prismatic).
            float ca = chromatic * rimLens;
            half4 cR = content.eval(sampleCoord - n * ca);
            half4 cG = content.eval(sampleCoord);
            half4 cB = content.eval(sampleCoord + n * ca);
            half3 rgb = half3(cR.r, cG.g, cB.b);
            half a = cG.a;

            // Light frost — keep detail so magnification stays readable.
            rgb = mix(rgb, half3(0.96, 0.98, 1.0), half(clamp(frost, 0.0, 1.5) * 0.08));

            // Soft Fresnel rim (not a white wash).
            float fresnelTerm = pow(perimeter, 4.0) * rimLens;
            float fresnelA = mix(fresnelMin, fresnelMax, fresnelTerm);
            rgb = rgb + half3(fresnelA, fresnelA, fresnelA);

            // Wet specular lobe from pillow normal.
            float3 N = normalize(float3(-n.x * (0.35 + 0.85 * rimLens), -n.y * (0.35 + 0.85 * rimLens), 1.0));
            float3 L = normalize(float3(lightDir.x, lightDir.y, 0.7));
            float3 V = float3(0.0, 0.0, 1.0);
            float3 H = normalize(L + V);
            float spec = pow(max(dot(N, H), 0.0), max(specularPower, 1.0))
                * specularStrength * (0.25 + 0.75 * rimLens);
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
        fresnelMin: Float = 0.04f,
        fresnelMax: Float = 0.32f,
        specularPower: Float = 48f,
        specularStrength: Float = 0.5f,
        chromatic: Float = 1.2f,
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
