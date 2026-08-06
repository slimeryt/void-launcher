package com.voidlauncher.app.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size

/**
 * AGSL liquid glass (API 33+): optical refraction, Fresnel rim, Blinn-Phong specular.
 *
 * Backdrop is expected already Gaussian-blurred (σ≈25–40) via RenderEffect before this
 * shader runs. This stage warps UVs from the shape-mask gradient, adds chromatic fringe,
 * Fresnel edge catch, and a sharp wet specular lobe.
 */
object LiquidRefractionShader {

    /**
     * Checklist targets:
     * - η (eta): 0.03–0.07, max ~5% UV at edges
     * - Fresnel alpha: 0.05 center → 0.45 perimeter
     * - Specular power n: ~50
     * - Light: top-left
     */
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

        // Signed distance to rounded-rect edge (negative inside, 0 on boundary).
        float sdRoundRect(float2 p, float2 halfSize, float r) {
            float2 q = abs(p) - halfSize + float2(r, r);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        }

        // Height / thickness field M: 0 in flat center, rises toward curved rim.
        float maskM(float2 fragCoord) {
            float2 halfSize = resolution * 0.5;
            float2 p = fragCoord - halfSize;
            float r = clamp(cornerRadius, 1.0, min(halfSize.x, halfSize.y));
            float sd = sdRoundRect(p, halfSize, r);
            float edgeDist = max(-sd, 0.0);
            float bezel = max(r * 0.95, 12.0);
            // High at perimeter, ~0 deep inside (∇M drives refraction at edges only).
            return exp(-edgeDist / bezel);
        }

        float2 maskGrad(float2 fragCoord) {
            float e = 1.5;
            float dx = maskM(fragCoord + float2(e, 0.0)) - maskM(fragCoord - float2(e, 0.0));
            float dy = maskM(fragCoord + float2(0.0, e)) - maskM(fragCoord - float2(0.0, e));
            return float2(dx, dy) / (2.0 * e);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            float2 halfSize = resolution * 0.5;
            float2 p = fragCoord - halfSize;
            float r = clamp(cornerRadius, 1.0, min(halfSize.x, halfSize.y));
            float sd = sdRoundRect(p, halfSize, r);
            float edgeDist = max(-sd, 0.0);
            float maxDist = min(halfSize.x, halfSize.y);
            // 0 at center, 1 at perimeter (for Fresnel cosθ approximation).
            float perimeter = 1.0 - clamp(edgeDist / maxDist, 0.0, 1.0);

            float2 g = maskGrad(fragCoord);
            float gLen = length(g);
            float2 gHat = gLen > 0.0001 ? g / gLen : float2(0.0, 0.0);

            // UV_refracted = UV + ∇M · η  (cap ~5% UV)
            float etaClamped = clamp(eta, 0.0, 0.07);
            float2 offsetUv = g * etaClamped;
            float offsetLen = length(offsetUv);
            if (offsetLen > 0.05) {
                offsetUv *= 0.05 / offsetLen;
            }
            float2 refractCoord = fragCoord + offsetUv * resolution;

            // Chromatic fringe along gradient (pixel-scale, weighted by edge mask).
            float m = maskM(fragCoord);
            float ca = chromatic * m;
            half4 cR = content.eval(refractCoord + gHat * ca);
            half4 cG = content.eval(refractCoord);
            half4 cB = content.eval(refractCoord - gHat * ca);
            half3 rgb = half3(cR.r, cG.g, cB.b);
            half a = cG.a;

            // Soft frost veil toward cool white.
            rgb = mix(rgb, half3(0.94, 0.97, 1.0), half(clamp(frost, 0.0, 1.5) * 0.14));

            // Fresnel: I = Imin + (Imax-Imin) * (1-cosθ)^5 , cosθ ≈ 1-perimeter
            float fresnelTerm = pow(perimeter, 5.0);
            float fresnelA = mix(fresnelMin, fresnelMax, fresnelTerm);
            rgb = rgb + half3(fresnelA, fresnelA, fresnelA);

            // Pillow normal from height field for Blinn-Phong specular.
            float2 hGrad = maskGrad(fragCoord) * 40.0;
            float3 N = normalize(float3(-hGrad.x, -hGrad.y, 1.0));
            float3 L = normalize(float3(lightDir.x, lightDir.y, 0.65));
            float3 V = float3(0.0, 0.0, 1.0);
            float3 H = normalize(L + V);
            float ndoth = max(dot(N, H), 0.0);
            float spec = pow(ndoth, max(specularPower, 1.0)) * specularStrength * (0.35 + 0.65 * m);
            rgb = rgb + half3(spec, spec, spec);

            // Keep fragments outside the shape transparent-ish (clip handles most).
            float inside = smoothstep(1.5, -1.5, sd);
            return half4(rgb, a * half(inside));
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
        fresnelMin: Float = 0.05f,
        fresnelMax: Float = 0.45f,
        specularPower: Float = 50f,
        specularStrength: Float = 0.55f,
        chromatic: Float = 1.4f,
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
