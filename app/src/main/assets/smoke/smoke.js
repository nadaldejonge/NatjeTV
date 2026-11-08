/*
 * Realistic Smoke Wisps
 * Copyright (c) 2026 Lorenzo Fattò Offidani
 * Released under the MIT License.
 *
 * The embedded 3D simplex noise routine is derived from webgl-noise.
 * Copyright (C) 2011 by Ashima Arts. Distributed under the MIT License.
 * See THIRD_PARTY_NOTICES.md.
 */

const MAX_LIFESPANS = 8;
const META_STRIDE = 64;

const STRUCTURAL_OPTIONS = new Set([
  "particleGridWidth",
  "particleGridHeight",
  "wispCount",
  "dissipationTimes",
  "randomSeed",
]);

const CONSTRUCTOR_ONLY_OPTIONS = new Set(["powerPreference"]);

const SMOKE_PRESETS = Object.freeze({
  cinematic: Object.freeze({
    particleGridWidth: 512,
    particleGridHeight: 256,
    smokeDensity: 0.005,
    maxPixelRatio: 2,
  }),
  balanced: Object.freeze({
    particleGridWidth: 384,
    particleGridHeight: 192,
    smokeDensity: 0.008,
    maxPixelRatio: 1.75,
  }),
  mobile: Object.freeze({
    particleGridWidth: 256,
    particleGridHeight: 128,
    smokeDensity: 0.018,
    maxPixelRatio: 1.25,
  }),
});

const DEFAULT_OPTIONS = Object.freeze({
  particleGridWidth: 512,
  particleGridHeight: 256,

  originX: 0,
  originY: -1.05,
  originJitter: 0.001,
  wispCount: 3,
  wispSpread: 0.025,

  upwardSpeed: 0.12,
  upwardVariation: 0.1,
  minimumUpwardSpeed: 0.06,
  laminarHeight: -0.8,
  turbulentHeight: -0.1,
  turbulenceEndHeight: 1,
  turbulenceStrength: 0.14,
  curlScale: 3.2,
  curlSampleDistance: 0.05,
  noiseTravelSpeed: 0.05,
  noiseEvolutionSpeed: 0.08,
  swayFrequency: 3.2,
  swaySpeed: 0.5,
  swayAmplitude: 0.06,
  outwardDrift: 0.05,

  smokeDensity: 0.005,
  dissipationTimes: Object.freeze([16, 18, 20, 22, 24]),

  sharpParticleFraction: 0.1,
  minBlur: 0.2,
  maxBlur: 1,
  blurStartHeight: -0.4,
  blurEndHeight: 1,
  initialPointSize: 3,
  sharpPointSize: 25,
  softPointSize: 90,
  blurSizeBoost: 40,
  sizeExpansionPower: 2.2,
  sizeReferenceHeight: 800,
  maxSizeScale: 2,
  preserveAspectRatio: false,

  fadeInStart: -1.05,
  fadeInEnd: -1,
  fadeOutAgeStart: 0.8,
  fadeOutAgeEnd: 1,
  hotColor: "#ebf0f5",
  coolColor: "#99a1ad",
  colorShiftAgeMin: 0.15,
  colorShiftAgeMax: 0.7,
  colorShiftDuration: 0.2,
  textureDarkness: 0.85,
  textureBrightness: 1.15,
  sharpProfile: 28,
  softProfile: 4,
  blurredOpacity: 0.4,

  pixelRatio: "auto",
  maxPixelRatio: 2,
  prewarmSeconds: 19.8,
  prewarmStep: 0.033,
  maxDeltaSeconds: 0.1,
  maxFps: 0,
  autoStart: true,
  pauseWhenHidden: true,
  respectReducedMotion: true,
  powerPreference: "high-performance",
  randomSeed: 23817,
});

const UPDATE_UNIFORMS = [
  "u_stateTex",
  "u_time",
  "u_dt",
  "u_originX",
  "u_originY",
  "u_originJitter",
  "u_wispCount",
  "u_wispSpread",
  "u_upwardSpeed",
  "u_upwardVariation",
  "u_minimumUpwardSpeed",
  "u_laminarHeight",
  "u_turbulentHeight",
  "u_turbulenceEndHeight",
  "u_turbulenceStrength",
  "u_curlScale",
  "u_curlSampleDistance",
  "u_noiseTravelSpeed",
  "u_noiseEvolutionSpeed",
  "u_swayFrequency",
  "u_swaySpeed",
  "u_swayAmplitude",
  "u_outwardDrift",
  "u_lifespans[0]",
];

const RENDER_UNIFORMS = [
  "u_stateTex",
  "u_stateSize",
  "u_cssResolution",
  "u_pixelRatio",
  "u_pointSizeLimit",
  "u_originX",
  "u_preserveAspectRatio",
  "u_density",
  "u_sharpParticleFraction",
  "u_minBlur",
  "u_maxBlur",
  "u_blurStartHeight",
  "u_blurEndHeight",
  "u_initialPointSize",
  "u_sharpPointSize",
  "u_softPointSize",
  "u_blurSizeBoost",
  "u_sizeExpansionPower",
  "u_sizeReferenceHeight",
  "u_maxSizeScale",
  "u_fadeInStart",
  "u_fadeInEnd",
  "u_fadeOutAgeStart",
  "u_fadeOutAgeEnd",
  "u_hotColor",
  "u_coolColor",
  "u_colorShiftAgeMin",
  "u_colorShiftAgeMax",
  "u_colorShiftDuration",
  "u_textureDarkness",
  "u_textureBrightness",
  "u_sharpProfile",
  "u_softProfile",
  "u_blurredOpacity",
];

const SIMPLEX_NOISE_3D = `
  // Array and textureless GLSL 3D simplex noise.
  // Author: Ian McEwan, Ashima Arts.
  // Distributed under the MIT License.
  vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
  }

  vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
  }

  vec4 permute(vec4 x) {
    return mod289(((x * 34.0) + 1.0) * x);
  }

  vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
  }

  float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    i = mod289(i);
    vec4 p = permute(
      permute(
        permute(i.z + vec4(0.0, i1.z, i2.z, 1.0))
        + i.y + vec4(0.0, i1.y, i2.y, 1.0)
      )
      + i.x + vec4(0.0, i1.x, i2.x, 1.0)
    );

    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt(vec4(
      dot(p0, p0),
      dot(p1, p1),
      dot(p2, p2),
      dot(p3, p3)
    ));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 m = max(
      0.6 - vec4(
        dot(x0, x0),
        dot(x1, x1),
        dot(x2, x2),
        dot(x3, x3)
      ),
      0.0
    );
    m *= m;
    return 42.0 * dot(
      m * m,
      vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3))
    );
  }
`;

const UPDATE_VERTEX_SHADER = `#version 300 es
  in vec2 a_position;
  out vec2 v_uv;

  void main() {
    v_uv = a_position * 0.5 + 0.5;
    gl_Position = vec4(a_position, 0.0, 1.0);
  }
`;

const UPDATE_FRAGMENT_SHADER = `#version 300 es
  precision highp float;

  uniform sampler2D u_stateTex;
  uniform float u_time;
  uniform float u_dt;
  uniform float u_originX;
  uniform float u_originY;
  uniform float u_originJitter;
  uniform float u_wispCount;
  uniform float u_wispSpread;
  uniform float u_upwardSpeed;
  uniform float u_upwardVariation;
  uniform float u_minimumUpwardSpeed;
  uniform float u_laminarHeight;
  uniform float u_turbulentHeight;
  uniform float u_turbulenceEndHeight;
  uniform float u_turbulenceStrength;
  uniform float u_curlScale;
  uniform float u_curlSampleDistance;
  uniform float u_noiseTravelSpeed;
  uniform float u_noiseEvolutionSpeed;
  uniform float u_swayFrequency;
  uniform float u_swaySpeed;
  uniform float u_swayAmplitude;
  uniform float u_outwardDrift;
  uniform float u_lifespans[${MAX_LIFESPANS}];

  in vec2 v_uv;
  out vec4 outColor;

  ${SIMPLEX_NOISE_3D}

  float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
  }

  void main() {
    vec4 state = texture(u_stateTex, v_uv);
    vec2 pos = state.xy;
    float age = state.z;

    float packedMetadata = floor(state.w);
    float wispIndex = mod(packedMetadata, ${META_STRIDE}.0);
    int lifeIndex = int(floor(packedMetadata / ${META_STRIDE}.0));
    float randomSeed = fract(state.w);
    float particleLifespan = u_lifespans[lifeIndex];

    age += u_dt / particleLifespan;

    if (age >= 1.0) {
      age = 0.0;
      pos.x = u_originX + (rand(v_uv + u_time) - 0.5) * u_originJitter;

      float speedVariation = mix(
        1.0 - u_upwardVariation,
        1.0 + u_upwardVariation,
        randomSeed
      );
      float localUpward = u_upwardSpeed * speedVariation;
      float subFrameOffset = rand(v_uv + u_time + 1.0) * u_dt * localUpward;
      pos.y = u_originY + subFrameOffset;
    } else {
      float height = pos.y;
      float speedVariation = mix(
        1.0 - u_upwardVariation,
        1.0 + u_upwardVariation,
        randomSeed
      );
      float localUpward = u_upwardSpeed * speedVariation;
      vec2 velocity = vec2(0.0, localUpward);

      float swayFactor = smoothstep(
        u_laminarHeight,
        u_turbulentHeight,
        height
      );
      float turbulenceFactor = smoothstep(
        u_turbulentHeight,
        u_turbulenceEndHeight,
        height
      );

      float wispCenter = (u_wispCount - 1.0) * 0.5;
      float splitDirection = wispIndex - wispCenter;
      velocity.x += (
        splitDirection
        * u_wispSpread
        * swayFactor
        * (1.0 - turbulenceFactor * 0.5)
      );

      float phaseOffset = wispIndex * 6.28318530718 / max(u_wispCount, 1.0);
      float swayStrength = (
        u_swayAmplitude
        * swayFactor
        * (1.0 - turbulenceFactor * 0.2)
      );
      velocity.x += sin(
        height * u_swayFrequency
        - u_time * u_swaySpeed
        + phaseOffset
      ) * swayStrength;

      vec3 noisePosition = vec3(
        pos.x * u_curlScale,
        (pos.y - u_time * u_noiseTravelSpeed) * u_curlScale,
        u_time * u_noiseEvolutionSpeed
      );
      float epsilon = u_curlSampleDistance;
      float north = snoise(noisePosition + vec3(0.0, epsilon, 0.0));
      float south = snoise(noisePosition - vec3(0.0, epsilon, 0.0));
      float east = snoise(noisePosition + vec3(epsilon, 0.0, 0.0));
      float west = snoise(noisePosition - vec3(epsilon, 0.0, 0.0));
      vec2 curl = vec2(north - south, -(east - west)) / (2.0 * epsilon);

      velocity.x += curl.x * u_turbulenceStrength * turbulenceFactor;
      velocity.y += (
        curl.y
        * u_turbulenceStrength
        * 0.5
        * turbulenceFactor
      );
      velocity.y = max(velocity.y, u_minimumUpwardSpeed);

      float distanceFromOrigin = pos.x - u_originX;
      velocity.x += (
        distanceFromOrigin
        * age
        * u_outwardDrift
        * turbulenceFactor
      );

      pos += velocity * u_dt;
    }

    outColor = vec4(pos, age, state.w);
  }
`;

const RENDER_VERTEX_SHADER = `#version 300 es
  uniform sampler2D u_stateTex;
  uniform vec2 u_stateSize;
  uniform vec2 u_cssResolution;
  uniform float u_pixelRatio;
  uniform float u_pointSizeLimit;
  uniform float u_originX;
  uniform float u_preserveAspectRatio;
  uniform float u_sharpParticleFraction;
  uniform float u_minBlur;
  uniform float u_maxBlur;
  uniform float u_blurStartHeight;
  uniform float u_blurEndHeight;
  uniform float u_initialPointSize;
  uniform float u_sharpPointSize;
  uniform float u_softPointSize;
  uniform float u_blurSizeBoost;
  uniform float u_sizeExpansionPower;
  uniform float u_sizeReferenceHeight;
  uniform float u_maxSizeScale;
  uniform float u_fadeInStart;
  uniform float u_fadeInEnd;
  uniform float u_fadeOutAgeStart;
  uniform float u_fadeOutAgeEnd;

  out float v_age;
  out float v_opacity;
  out float v_blur;
  out float v_seed;

  void main() {
    int textureWidth = int(u_stateSize.x);
    int x = gl_VertexID % textureWidth;
    int y = gl_VertexID / textureWidth;
    vec2 uv = (
      vec2(float(x) + 0.5, float(y) + 0.5)
      / u_stateSize
    );

    vec4 state = texture(u_stateTex, uv);
    vec2 pos = state.xy;
    float age = state.z;
    v_seed = fract(state.w);

    float aspectScale = u_cssResolution.y / max(u_cssResolution.x, 1.0);
    float aspectCorrectedX = (
      u_originX + (pos.x - u_originX) * aspectScale
    );
    float displayX = mix(pos.x, aspectCorrectedX, u_preserveAspectRatio);
    gl_Position = vec4(displayX, pos.y, 0.0, 1.0);

    float blurSeed = fract(v_seed * 89.123);
    float maxParticleBlur = 0.0;
    if (blurSeed >= u_sharpParticleFraction) {
      float blurRangePosition = (
        (blurSeed - u_sharpParticleFraction)
        / max(1.0 - u_sharpParticleFraction, 0.0001)
      );
      maxParticleBlur = mix(u_minBlur, u_maxBlur, blurRangePosition);
    }

    float heightBlur = smoothstep(
      u_blurStartHeight,
      u_blurEndHeight,
      pos.y
    );
    v_blur = heightBlur * maxParticleBlur;

    float expansion = pow(max(0.0, age), u_sizeExpansionPower);
    float targetSize = mix(
      u_sharpPointSize,
      u_softPointSize,
      maxParticleBlur
    );
    float size = (
      mix(u_initialPointSize, targetSize, expansion)
      + v_blur * u_blurSizeBoost
    );
    float viewportScale = min(
      u_maxSizeScale,
      u_cssResolution.y / u_sizeReferenceHeight
    );
    gl_PointSize = min(
      size * viewportScale * u_pixelRatio,
      u_pointSizeLimit
    );

    v_age = age;
    float alphaIn = smoothstep(u_fadeInStart, u_fadeInEnd, pos.y);
    float alphaOut = 1.0 - smoothstep(
      u_fadeOutAgeStart,
      u_fadeOutAgeEnd,
      age
    );
    v_opacity = alphaIn * alphaOut;
  }
`;

const RENDER_FRAGMENT_SHADER = `#version 300 es
  precision highp float;

  in float v_age;
  in float v_opacity;
  in float v_blur;
  in float v_seed;

  uniform float u_density;
  uniform vec3 u_hotColor;
  uniform vec3 u_coolColor;
  uniform float u_colorShiftAgeMin;
  uniform float u_colorShiftAgeMax;
  uniform float u_colorShiftDuration;
  uniform float u_textureDarkness;
  uniform float u_textureBrightness;
  uniform float u_sharpProfile;
  uniform float u_softProfile;
  uniform float u_blurredOpacity;

  out vec4 fragColor;

  void main() {
    if (v_age >= 1.0 || v_opacity <= 0.0) {
      discard;
    }

    vec2 pointCoordinate = gl_PointCoord - 0.5;
    float distanceSquared = dot(pointCoordinate, pointCoordinate);
    if (distanceSquared > 0.25) {
      discard;
    }

    float sharpProfile = exp(-distanceSquared * u_sharpProfile);
    float softProfile = exp(-distanceSquared * u_softProfile);
    float profile = mix(sharpProfile, softProfile, v_blur);
    float expansionFade = mix(1.0, u_blurredOpacity, v_blur);
    float alpha = profile * v_opacity * u_density * expansionFade;

    float graySeed = fract(v_seed * 43.758);
    float colorShiftStart = mix(
      u_colorShiftAgeMin,
      u_colorShiftAgeMax,
      graySeed
    );
    float colorBlend = smoothstep(
      colorShiftStart,
      colorShiftStart + u_colorShiftDuration,
      v_age
    );
    vec3 baseColor = mix(u_hotColor, u_coolColor, colorBlend);
    baseColor *= mix(u_textureDarkness, u_textureBrightness, v_seed);

    fragColor = vec4(baseColor * alpha, alpha);
  }
`;

function resolveCanvas(value) {
  if (typeof value === "string") {
    const element = document.querySelector(value);
    if (!(element instanceof HTMLCanvasElement)) {
      throw new TypeError(`No canvas matched the selector "${value}".`);
    }
    return element;
  }

  if (!(value instanceof HTMLCanvasElement)) {
    throw new TypeError("RealisticSmoke requires a canvas element.");
  }

  return value;
}

function resolvePreset(name) {
  if (!Object.prototype.hasOwnProperty.call(SMOKE_PRESETS, name)) {
    throw new RangeError(
      `Unknown preset "${name}". Use cinematic, balanced, or mobile.`,
    );
  }
  return SMOKE_PRESETS[name];
}

function buildOptions(input = {}, base = DEFAULT_OPTIONS) {
  const presetName = input.preset;
  const preset = presetName ? resolvePreset(presetName) : {};
  const next = {
    ...base,
    ...preset,
    ...input,
  };
  delete next.preset;
  next.dissipationTimes = [...next.dissipationTimes];
  return validateOptions(next);
}

function validateOptions(options) {
  const integerRanges = {
    particleGridWidth: [16, 2048],
    particleGridHeight: [16, 2048],
    wispCount: [1, META_STRIDE],
    randomSeed: [0, 0xffffffff],
  };

  for (const [key, [minimum, maximum]] of Object.entries(integerRanges)) {
    if (
      !Number.isInteger(options[key])
      || options[key] < minimum
      || options[key] > maximum
    ) {
      throw new RangeError(
        `${key} must be an integer from ${minimum} to ${maximum}.`,
      );
    }
  }

  if (
    options.particleGridWidth * options.particleGridHeight
    > 1048576
  ) {
    throw new RangeError("The particle grid may not exceed 1,048,576 points.");
  }

  const positiveNumbers = [
    "upwardSpeed",
    "minimumUpwardSpeed",
    "curlScale",
    "curlSampleDistance",
    "smokeDensity",
    "initialPointSize",
    "sharpPointSize",
    "softPointSize",
    "sizeExpansionPower",
    "sizeReferenceHeight",
    "maxSizeScale",
    "prewarmStep",
    "maxDeltaSeconds",
    "maxPixelRatio",
    "sharpProfile",
    "softProfile",
    "colorShiftDuration",
  ];

  for (const key of positiveNumbers) {
    if (!Number.isFinite(options[key]) || options[key] <= 0) {
      throw new RangeError(`${key} must be greater than zero.`);
    }
  }

  const finiteNumbers = [
    "originX",
    "originY",
    "originJitter",
    "wispSpread",
    "upwardVariation",
    "laminarHeight",
    "turbulentHeight",
    "turbulenceEndHeight",
    "turbulenceStrength",
    "noiseTravelSpeed",
    "noiseEvolutionSpeed",
    "swayFrequency",
    "swaySpeed",
    "swayAmplitude",
    "outwardDrift",
    "minBlur",
    "maxBlur",
    "blurStartHeight",
    "blurEndHeight",
    "blurSizeBoost",
    "fadeInStart",
    "fadeInEnd",
    "fadeOutAgeStart",
    "fadeOutAgeEnd",
    "colorShiftAgeMin",
    "colorShiftAgeMax",
    "colorShiftDuration",
    "textureDarkness",
    "textureBrightness",
    "blurredOpacity",
    "prewarmSeconds",
    "maxFps",
  ];

  for (const key of finiteNumbers) {
    if (!Number.isFinite(options[key])) {
      throw new TypeError(`${key} must be a finite number.`);
    }
  }

  const nonNegativeNumbers = [
    "originJitter",
    "wispSpread",
    "turbulenceStrength",
    "blurSizeBoost",
    "prewarmSeconds",
    "maxFps",
  ];
  for (const key of nonNegativeNumbers) {
    if (options[key] < 0) {
      throw new RangeError(`${key} must not be negative.`);
    }
  }

  const booleanOptions = [
    "preserveAspectRatio",
    "autoStart",
    "pauseWhenHidden",
    "respectReducedMotion",
  ];
  for (const key of booleanOptions) {
    if (typeof options[key] !== "boolean") {
      throw new TypeError(`${key} must be true or false.`);
    }
  }

  if (options.laminarHeight >= options.turbulentHeight) {
    throw new RangeError("laminarHeight must be below turbulentHeight.");
  }
  if (options.turbulentHeight >= options.turbulenceEndHeight) {
    throw new RangeError(
      "turbulentHeight must be below turbulenceEndHeight.",
    );
  }
  if (options.blurStartHeight >= options.blurEndHeight) {
    throw new RangeError("blurStartHeight must be below blurEndHeight.");
  }
  if (options.fadeInStart >= options.fadeInEnd) {
    throw new RangeError("fadeInStart must be below fadeInEnd.");
  }
  if (options.fadeOutAgeStart >= options.fadeOutAgeEnd) {
    throw new RangeError("fadeOutAgeStart must be below fadeOutAgeEnd.");
  }
  if (options.colorShiftAgeMin > options.colorShiftAgeMax) {
    throw new RangeError(
      "colorShiftAgeMin must not exceed colorShiftAgeMax.",
    );
  }
  if (
    options.sharpParticleFraction < 0
    || options.sharpParticleFraction > 1
  ) {
    throw new RangeError("sharpParticleFraction must be from 0 to 1.");
  }
  if (options.upwardVariation < 0 || options.upwardVariation > 1) {
    throw new RangeError("upwardVariation must be from 0 to 1.");
  }
  if (options.minBlur < 0 || options.maxBlur < options.minBlur) {
    throw new RangeError("Blur values must satisfy 0 <= minBlur <= maxBlur.");
  }
  if (
    options.fadeOutAgeStart < 0
    || options.fadeOutAgeEnd > 1
  ) {
    throw new RangeError("Fade-out ages must remain within 0 and 1.");
  }
  if (
    options.colorShiftAgeMin < 0
    || options.colorShiftAgeMax > 1
  ) {
    throw new RangeError("Color-shift ages must remain within 0 and 1.");
  }
  if (options.blurredOpacity < 0 || options.blurredOpacity > 1) {
    throw new RangeError("blurredOpacity must be from 0 to 1.");
  }
  if (
    options.textureDarkness < 0
    || options.textureBrightness < options.textureDarkness
  ) {
    throw new RangeError(
      "Texture values must satisfy 0 <= textureDarkness <= textureBrightness.",
    );
  }
  if (options.prewarmSeconds < 0 || options.prewarmSeconds > 60) {
    throw new RangeError("prewarmSeconds must be from 0 to 60.");
  }
  if (options.maxFps < 0 || options.maxFps > 240) {
    throw new RangeError("maxFps must be from 0 to 240.");
  }
  if (
    options.pixelRatio !== "auto"
    && (!Number.isFinite(options.pixelRatio) || options.pixelRatio <= 0)
  ) {
    throw new RangeError('pixelRatio must be "auto" or greater than zero.');
  }
  if (
    !Array.isArray(options.dissipationTimes)
    || options.dissipationTimes.length < 1
    || options.dissipationTimes.length > MAX_LIFESPANS
    || options.dissipationTimes.some(
      (value) => !Number.isFinite(value) || value <= 0,
    )
  ) {
    throw new RangeError(
      `dissipationTimes must contain 1 to ${MAX_LIFESPANS} positive numbers.`,
    );
  }
  if (!["default", "high-performance", "low-power"].includes(
    options.powerPreference,
  )) {
    throw new RangeError(
      "powerPreference must be default, high-performance, or low-power.",
    );
  }

  parseColor(options.hotColor, "hotColor");
  parseColor(options.coolColor, "coolColor");
  return options;
}

function parseColor(value, name) {
  if (
    Array.isArray(value)
    && value.length === 3
    && value.every((channel) => Number.isFinite(channel))
  ) {
    return value.map((channel) => Math.min(1, Math.max(0, channel)));
  }

  if (typeof value === "string") {
    const compact = value.trim();
    const shortMatch = /^#([\da-f])([\da-f])([\da-f])$/i.exec(compact);
    const longMatch = /^#([\da-f]{2})([\da-f]{2})([\da-f]{2})$/i.exec(
      compact,
    );

    if (shortMatch) {
      return shortMatch.slice(1).map(
        (channel) => parseInt(channel + channel, 16) / 255,
      );
    }
    if (longMatch) {
      return longMatch.slice(1).map(
        (channel) => parseInt(channel, 16) / 255,
      );
    }
  }

  throw new TypeError(
    `${name} must be a hexadecimal color or an RGB array from 0 to 1.`,
  );
}

function createSeededRandom(seed) {
  let state = seed >>> 0;
  return function random() {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function getUniformLocations(gl, program, names) {
  const locations = {};
  for (const name of names) {
    const key = name.replace("[0]", "");
    locations[key] = gl.getUniformLocation(program, name);
  }
  return locations;
}

function sourceWithLineNumbers(source) {
  return source
    .split("\n")
    .map((line, index) => `${String(index + 1).padStart(4, " ")} | ${line}`)
    .join("\n");
}

/**
 * Dependency-free WebGL2 incense smoke simulation.
 */
class RealisticSmoke {
  constructor(input = {}) {
    const {
      canvas,
      onError = null,
      ...optionInput
    } = input;

    this.canvas = resolveCanvas(canvas);
    this.options = buildOptions(optionInput);
    this.onError = typeof onError === "function" ? onError : null;

    this.gl = null;
    this.updateProgram = null;
    this.renderProgram = null;
    this.updateUniforms = null;
    this.renderUniforms = null;
    this.updateVao = null;
    this.renderVao = null;
    this.quadBuffer = null;
    this.stateTextures = [];
    this.framebuffers = [];
    this.currentRead = 0;
    this.particleCount = 0;
    this.globalTime = 0;
    this.lastFrameTime = 0;
    this.frameHandle = null;
    this.resizeObserver = null;
    this.cssWidth = 1;
    this.cssHeight = 1;
    this.currentPixelRatio = 1;
    this.pointSizeLimit = 1;
    this.destroyed = false;
    this.contextLost = false;
    this.manualPaused = !this.options.autoStart;
    this.visibilityPaused = false;
    this.reducedMotionPaused = false;

    this._boundRender = (time) => this._render(time);
    this._boundResize = () => this.resize();
    this._boundVisibility = () => this._handleVisibilityChange();
    this._boundMotionChange = () => this._handleMotionPreference();
    this._boundContextLost = (event) => this._handleContextLost(event);
    this._boundContextRestored = () => this._handleContextRestored();

    this.canvas.setAttribute("aria-hidden", "true");
    this.canvas.addEventListener("webglcontextlost", this._boundContextLost);
    this.canvas.addEventListener(
      "webglcontextrestored",
      this._boundContextRestored,
    );

    this._createContext();
    this._initializeGpuResources();
    this._installEnvironmentListeners();
    this.resize();
    this._prewarm();
    this.renderOnce();
    this.lastFrameTime = performance.now();
    this._scheduleFrame();
  }

  static isSupported() {
    if (typeof document === "undefined") {
      return false;
    }

    const canvas = document.createElement("canvas");
    const gl = canvas.getContext("webgl2");
    if (!gl) {
      return false;
    }
    const supported = Boolean(gl.getExtension("EXT_color_buffer_float"));
    gl.getExtension("WEBGL_lose_context")?.loseContext();
    return supported;
  }

  setOptions(partial = {}) {
    this._assertActive();

    for (const key of CONSTRUCTOR_ONLY_OPTIONS) {
      if (
        Object.prototype.hasOwnProperty.call(partial, key)
        && partial[key] !== this.options[key]
      ) {
        throw new Error(`${key} can only be set in the constructor.`);
      }
    }

    const previous = this.options;
    const next = buildOptions(partial, previous);
    const structuralChange = Object.keys(partial).some((key) => {
      if (!STRUCTURAL_OPTIONS.has(key)) {
        return false;
      }
      if (key === "dissipationTimes") {
        return JSON.stringify(previous[key]) !== JSON.stringify(next[key]);
      }
      return previous[key] !== next[key];
    });

    this.options = next;
    this._handleMotionPreference();
    this.resize();

    if (structuralChange) {
      this.reset();
    } else {
      this._applyStaticUniforms();
      if (!this._shouldAnimate()) {
        this.renderOnce();
      }
    }

    return this;
  }

  setPreset(name, overrides = {}) {
    return this.setOptions({
      ...resolvePreset(name),
      ...overrides,
    });
  }

  reset() {
    this._assertActive();
    this._cancelFrame();
    this._disposeStateResources();
    this._createStateResources();
    this._applyStaticUniforms();
    this.globalTime = 0;
    this._prewarm();
    this.renderOnce();
    this.lastFrameTime = performance.now();
    this._scheduleFrame();
    return this;
  }

  pause() {
    this._assertActive();
    this.manualPaused = true;
    this._cancelFrame();
    return this;
  }

  resume() {
    this._assertActive();
    this.manualPaused = false;
    this.lastFrameTime = performance.now();
    this._scheduleFrame();
    return this;
  }

  resize() {
    this._assertActive();

    const rect = this.canvas.getBoundingClientRect();
    const width = Math.max(1, Math.round(rect.width || window.innerWidth || 1));
    const height = Math.max(
      1,
      Math.round(rect.height || window.innerHeight || 1),
    );
    const requestedRatio = this.options.pixelRatio === "auto"
      ? window.devicePixelRatio || 1
      : this.options.pixelRatio;
    const pixelRatio = Math.min(requestedRatio, this.options.maxPixelRatio);
    const drawingWidth = Math.max(1, Math.round(width * pixelRatio));
    const drawingHeight = Math.max(1, Math.round(height * pixelRatio));

    this.cssWidth = width;
    this.cssHeight = height;
    this.currentPixelRatio = pixelRatio;

    if (
      this.canvas.width !== drawingWidth
      || this.canvas.height !== drawingHeight
    ) {
      this.canvas.width = drawingWidth;
      this.canvas.height = drawingHeight;
    }

    if (!this._shouldAnimate() && !this.contextLost) {
      this.renderOnce();
    }
    return this;
  }

  renderOnce() {
    this._assertActive();
    if (!this.contextLost) {
      this._drawToScreen();
    }
    return this;
  }

  destroy() {
    if (this.destroyed) {
      return;
    }

    this._cancelFrame();
    this.resizeObserver?.disconnect();
    window.removeEventListener("resize", this._boundResize);
    document.removeEventListener("visibilitychange", this._boundVisibility);
    this.motionQuery?.removeEventListener?.("change", this._boundMotionChange);
    this.canvas.removeEventListener(
      "webglcontextlost",
      this._boundContextLost,
    );
    this.canvas.removeEventListener(
      "webglcontextrestored",
      this._boundContextRestored,
    );
    this._disposeGpuResources();
    this.destroyed = true;
  }

  _assertActive() {
    if (this.destroyed) {
      throw new Error("This RealisticSmoke instance has been destroyed.");
    }
  }

  _createContext() {
    const gl = this.canvas.getContext("webgl2", {
      alpha: true,
      antialias: false,
      premultipliedAlpha: true,
      preserveDrawingBuffer: false,
      powerPreference: this.options.powerPreference,
    });

    if (!gl) {
      throw new Error("WebGL2 is required for Realistic Smoke Wisps.");
    }
    if (!gl.getExtension("EXT_color_buffer_float")) {
      throw new Error(
        "This device cannot render floating-point WebGL2 framebuffers.",
      );
    }
    this.gl = gl;
    this.pointSizeLimit = gl.getParameter(gl.ALIASED_POINT_SIZE_RANGE)[1];
  }

  _initializeGpuResources() {
    const gl = this.gl;
    this.updateProgram = this._createProgram(
      UPDATE_VERTEX_SHADER,
      UPDATE_FRAGMENT_SHADER,
      "simulation",
    );
    this.renderProgram = this._createProgram(
      RENDER_VERTEX_SHADER,
      RENDER_FRAGMENT_SHADER,
      "rendering",
    );
    this.updateUniforms = getUniformLocations(
      gl,
      this.updateProgram,
      UPDATE_UNIFORMS,
    );
    this.renderUniforms = getUniformLocations(
      gl,
      this.renderProgram,
      RENDER_UNIFORMS,
    );

    this.updateVao = gl.createVertexArray();
    this.renderVao = gl.createVertexArray();
    this.quadBuffer = gl.createBuffer();

    gl.bindVertexArray(this.updateVao);
    gl.bindBuffer(gl.ARRAY_BUFFER, this.quadBuffer);
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]),
      gl.STATIC_DRAW,
    );
    const positionLocation = gl.getAttribLocation(
      this.updateProgram,
      "a_position",
    );
    gl.enableVertexAttribArray(positionLocation);
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);
    gl.bindVertexArray(null);
    gl.bindBuffer(gl.ARRAY_BUFFER, null);

    this._createStateResources();
    this._applyStaticUniforms();
  }

  _createStateResources() {
    const gl = this.gl;
    const {
      particleGridWidth,
      particleGridHeight,
      wispCount,
      dissipationTimes,
      originX,
      originY,
      randomSeed,
    } = this.options;

    this.particleCount = particleGridWidth * particleGridHeight;
    const initialData = new Float32Array(this.particleCount * 4);
    const random = createSeededRandom(randomSeed);

    for (let index = 0; index < this.particleCount; index += 1) {
      const wispIndex = index % wispCount;
      const lifeIndex = Math.floor(random() * dissipationTimes.length);
      const particleSeed = Math.max(random(), 0.000001);
      const offset = index * 4;

      initialData[offset] = originX;
      initialData[offset + 1] = originY;
      initialData[offset + 2] = random();
      initialData[offset + 3] = (
        wispIndex + lifeIndex * META_STRIDE + particleSeed
      );
    }

    this.stateTextures = [gl.createTexture(), gl.createTexture()];
    this.framebuffers = [gl.createFramebuffer(), gl.createFramebuffer()];

    for (let index = 0; index < 2; index += 1) {
      gl.bindTexture(gl.TEXTURE_2D, this.stateTextures[index]);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      gl.texImage2D(
        gl.TEXTURE_2D,
        0,
        gl.RGBA32F,
        particleGridWidth,
        particleGridHeight,
        0,
        gl.RGBA,
        gl.FLOAT,
        initialData,
      );

      gl.bindFramebuffer(gl.FRAMEBUFFER, this.framebuffers[index]);
      gl.framebufferTexture2D(
        gl.FRAMEBUFFER,
        gl.COLOR_ATTACHMENT0,
        gl.TEXTURE_2D,
        this.stateTextures[index],
        0,
      );

      if (
        gl.checkFramebufferStatus(gl.FRAMEBUFFER)
        !== gl.FRAMEBUFFER_COMPLETE
      ) {
        throw new Error(
          "The floating-point particle framebuffer is incomplete.",
        );
      }
    }

    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    gl.bindTexture(gl.TEXTURE_2D, null);
    this.currentRead = 0;
  }

  _applyStaticUniforms() {
    const gl = this.gl;
    const options = this.options;
    const update = this.updateUniforms;

    gl.useProgram(this.updateProgram);
    gl.uniform1i(update.u_stateTex, 0);
    gl.uniform1f(update.u_originX, options.originX);
    gl.uniform1f(update.u_originY, options.originY);
    gl.uniform1f(update.u_originJitter, options.originJitter);
    gl.uniform1f(update.u_wispCount, options.wispCount);
    gl.uniform1f(update.u_wispSpread, options.wispSpread);
    gl.uniform1f(update.u_upwardSpeed, options.upwardSpeed);
    gl.uniform1f(update.u_upwardVariation, options.upwardVariation);
    gl.uniform1f(
      update.u_minimumUpwardSpeed,
      options.minimumUpwardSpeed,
    );
    gl.uniform1f(update.u_laminarHeight, options.laminarHeight);
    gl.uniform1f(update.u_turbulentHeight, options.turbulentHeight);
    gl.uniform1f(
      update.u_turbulenceEndHeight,
      options.turbulenceEndHeight,
    );
    gl.uniform1f(
      update.u_turbulenceStrength,
      options.turbulenceStrength,
    );
    gl.uniform1f(update.u_curlScale, options.curlScale);
    gl.uniform1f(
      update.u_curlSampleDistance,
      options.curlSampleDistance,
    );
    gl.uniform1f(update.u_noiseTravelSpeed, options.noiseTravelSpeed);
    gl.uniform1f(
      update.u_noiseEvolutionSpeed,
      options.noiseEvolutionSpeed,
    );
    gl.uniform1f(update.u_swayFrequency, options.swayFrequency);
    gl.uniform1f(update.u_swaySpeed, options.swaySpeed);
    gl.uniform1f(update.u_swayAmplitude, options.swayAmplitude);
    gl.uniform1f(update.u_outwardDrift, options.outwardDrift);

    const lifespanValues = new Float32Array(MAX_LIFESPANS);
    const fallbackLifespan = options.dissipationTimes[
      options.dissipationTimes.length - 1
    ];
    lifespanValues.fill(fallbackLifespan);
    lifespanValues.set(options.dissipationTimes);
    gl.uniform1fv(update.u_lifespans, lifespanValues);

    const render = this.renderUniforms;
    gl.useProgram(this.renderProgram);
    gl.uniform1i(render.u_stateTex, 0);
    gl.uniform2f(
      render.u_stateSize,
      options.particleGridWidth,
      options.particleGridHeight,
    );
    gl.uniform1f(render.u_originX, options.originX);
    gl.uniform1f(
      render.u_preserveAspectRatio,
      options.preserveAspectRatio ? 1 : 0,
    );
    gl.uniform1f(render.u_density, options.smokeDensity);
    gl.uniform1f(
      render.u_sharpParticleFraction,
      options.sharpParticleFraction,
    );
    gl.uniform1f(render.u_minBlur, options.minBlur);
    gl.uniform1f(render.u_maxBlur, options.maxBlur);
    gl.uniform1f(render.u_blurStartHeight, options.blurStartHeight);
    gl.uniform1f(render.u_blurEndHeight, options.blurEndHeight);
    gl.uniform1f(render.u_initialPointSize, options.initialPointSize);
    gl.uniform1f(render.u_sharpPointSize, options.sharpPointSize);
    gl.uniform1f(render.u_softPointSize, options.softPointSize);
    gl.uniform1f(render.u_blurSizeBoost, options.blurSizeBoost);
    gl.uniform1f(
      render.u_sizeExpansionPower,
      options.sizeExpansionPower,
    );
    gl.uniform1f(
      render.u_sizeReferenceHeight,
      options.sizeReferenceHeight,
    );
    gl.uniform1f(render.u_maxSizeScale, options.maxSizeScale);
    gl.uniform1f(render.u_fadeInStart, options.fadeInStart);
    gl.uniform1f(render.u_fadeInEnd, options.fadeInEnd);
    gl.uniform1f(render.u_fadeOutAgeStart, options.fadeOutAgeStart);
    gl.uniform1f(render.u_fadeOutAgeEnd, options.fadeOutAgeEnd);
    gl.uniform3fv(
      render.u_hotColor,
      parseColor(options.hotColor, "hotColor"),
    );
    gl.uniform3fv(
      render.u_coolColor,
      parseColor(options.coolColor, "coolColor"),
    );
    gl.uniform1f(render.u_colorShiftAgeMin, options.colorShiftAgeMin);
    gl.uniform1f(render.u_colorShiftAgeMax, options.colorShiftAgeMax);
    gl.uniform1f(
      render.u_colorShiftDuration,
      options.colorShiftDuration,
    );
    gl.uniform1f(render.u_textureDarkness, options.textureDarkness);
    gl.uniform1f(render.u_textureBrightness, options.textureBrightness);
    gl.uniform1f(render.u_sharpProfile, options.sharpProfile);
    gl.uniform1f(render.u_softProfile, options.softProfile);
    gl.uniform1f(render.u_blurredOpacity, options.blurredOpacity);
  }

  _createShader(type, source, label) {
    const gl = this.gl;
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      const log = gl.getShaderInfoLog(shader);
      gl.deleteShader(shader);
      throw new Error(
        `${label} shader compilation failed:\n${log}\n`
        + sourceWithLineNumbers(source),
      );
    }

    return shader;
  }

  _createProgram(vertexSource, fragmentSource, label) {
    const gl = this.gl;
    const vertexShader = this._createShader(
      gl.VERTEX_SHADER,
      vertexSource,
      `${label} vertex`,
    );
    const fragmentShader = this._createShader(
      gl.FRAGMENT_SHADER,
      fragmentSource,
      `${label} fragment`,
    );
    const program = gl.createProgram();
    gl.attachShader(program, vertexShader);
    gl.attachShader(program, fragmentShader);
    gl.linkProgram(program);
    gl.deleteShader(vertexShader);
    gl.deleteShader(fragmentShader);

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      const log = gl.getProgramInfoLog(program);
      gl.deleteProgram(program);
      throw new Error(`${label} program linking failed:\n${log}`);
    }

    return program;
  }

  _stepSimulation(deltaTime) {
    const gl = this.gl;
    gl.useProgram(this.updateProgram);
    gl.bindFramebuffer(
      gl.FRAMEBUFFER,
      this.framebuffers[1 - this.currentRead],
    );
    gl.viewport(
      0,
      0,
      this.options.particleGridWidth,
      this.options.particleGridHeight,
    );
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, this.stateTextures[this.currentRead]);
    gl.uniform1f(this.updateUniforms.u_time, this.globalTime);
    gl.uniform1f(this.updateUniforms.u_dt, deltaTime);
    gl.bindVertexArray(this.updateVao);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    gl.bindVertexArray(null);
    this.currentRead = 1 - this.currentRead;
  }

  _drawToScreen() {
    const gl = this.gl;
    gl.useProgram(this.renderProgram);
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    gl.clearColor(0, 0, 0, 0);
    gl.clear(gl.COLOR_BUFFER_BIT);
    gl.enable(gl.BLEND);
    gl.blendEquation(gl.FUNC_ADD);
    gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, this.stateTextures[this.currentRead]);
    gl.uniform2f(
      this.renderUniforms.u_cssResolution,
      this.cssWidth,
      this.cssHeight,
    );
    gl.uniform1f(
      this.renderUniforms.u_pixelRatio,
      this.currentPixelRatio,
    );
    gl.uniform1f(
      this.renderUniforms.u_pointSizeLimit,
      this.pointSizeLimit,
    );
    gl.bindVertexArray(this.renderVao);
    gl.drawArrays(gl.POINTS, 0, this.particleCount);
    gl.bindVertexArray(null);
    gl.disable(gl.BLEND);
  }

  _prewarm() {
    const steps = Math.ceil(
      this.options.prewarmSeconds / this.options.prewarmStep,
    );
    for (let index = 0; index < steps; index += 1) {
      this.globalTime += this.options.prewarmStep;
      this._stepSimulation(this.options.prewarmStep);
    }
  }

  _render(now) {
    this.frameHandle = null;
    if (!this._shouldAnimate()) {
      return;
    }

    const elapsed = Math.max(0, (now - this.lastFrameTime) / 1000);
    const minimumFrameTime = this.options.maxFps > 0
      ? 1 / this.options.maxFps
      : 0;

    if (elapsed < minimumFrameTime) {
      this._scheduleFrame();
      return;
    }

    this.lastFrameTime = now;
    const deltaTime = Math.min(
      elapsed || 1 / 60,
      this.options.maxDeltaSeconds,
    );
    this.globalTime += deltaTime;
    this._stepSimulation(deltaTime);
    this._drawToScreen();
    this._scheduleFrame();
  }

  _scheduleFrame() {
    if (this.frameHandle === null && this._shouldAnimate()) {
      this.frameHandle = requestAnimationFrame(this._boundRender);
    }
  }

  _cancelFrame() {
    if (this.frameHandle !== null) {
      cancelAnimationFrame(this.frameHandle);
      this.frameHandle = null;
    }
  }

  _shouldAnimate() {
    return (
      !this.destroyed
      && !this.contextLost
      && !this.manualPaused
      && !this.visibilityPaused
      && !this.reducedMotionPaused
    );
  }

  _installEnvironmentListeners() {
    if ("ResizeObserver" in window) {
      this.resizeObserver = new ResizeObserver(this._boundResize);
      this.resizeObserver.observe(this.canvas);
    } else {
      window.addEventListener("resize", this._boundResize);
    }

    document.addEventListener("visibilitychange", this._boundVisibility);
    this.motionQuery = window.matchMedia?.("(prefers-reduced-motion: reduce)");
    this.motionQuery?.addEventListener?.("change", this._boundMotionChange);
    this._handleVisibilityChange();
    this._handleMotionPreference();
  }

  _handleVisibilityChange() {
    this.visibilityPaused = (
      this.options.pauseWhenHidden
      && document.visibilityState === "hidden"
    );
    if (this.visibilityPaused) {
      this._cancelFrame();
    } else {
      this.lastFrameTime = performance.now();
      this._scheduleFrame();
    }
  }

  _handleMotionPreference() {
    this.reducedMotionPaused = Boolean(
      this.options.respectReducedMotion
      && this.motionQuery?.matches,
    );
    if (this.reducedMotionPaused) {
      this._cancelFrame();
      if (this.gl && !this.contextLost) {
        this.renderOnce();
      }
    } else {
      this.lastFrameTime = performance.now();
      this._scheduleFrame();
    }
  }

  _handleContextLost(event) {
    event.preventDefault();
    this.contextLost = true;
    this._cancelFrame();
  }

  _handleContextRestored() {
    this.contextLost = false;
    try {
      this._initializeGpuResources();
      this.resize();
      this.globalTime = 0;
      this._prewarm();
      this.renderOnce();
      this.lastFrameTime = performance.now();
      this._scheduleFrame();
    } catch (error) {
      this._reportError(error);
    }
  }

  _reportError(error) {
    this.onError?.(error);
    this.canvas.dispatchEvent(new CustomEvent("smokeerror", {
      detail: error,
    }));
  }

  _disposeStateResources() {
    if (!this.gl) {
      return;
    }
    for (const framebuffer of this.framebuffers) {
      this.gl.deleteFramebuffer(framebuffer);
    }
    for (const texture of this.stateTextures) {
      this.gl.deleteTexture(texture);
    }
    this.framebuffers = [];
    this.stateTextures = [];
  }

  _disposeGpuResources() {
    if (!this.gl || this.contextLost) {
      return;
    }
    this._disposeStateResources();
    this.gl.deleteBuffer(this.quadBuffer);
    this.gl.deleteVertexArray(this.updateVao);
    this.gl.deleteVertexArray(this.renderVao);
    this.gl.deleteProgram(this.updateProgram);
    this.gl.deleteProgram(this.renderProgram);
  }
}


