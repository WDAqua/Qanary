"use strict";

/*
 * Qanary error page — decorative animated canvas backdrop.
 *
 * A lightweight, self-contained ambient animation drawn behind the error card
 * in the Qanary logo colours (green / blue / teal). It is optional and purely
 * decorative: if it throws, or this file fails to load, the server-rendered
 * error information is unaffected. No build step and no dependencies.
 *
 * It runs on its own and also responds to the arrow keys / A,D and the space
 * bar when the page chrome is not focused.
 */
(function () {
  const canvas = document.getElementById("error-bg");
  if (!canvas || typeof canvas.getContext !== "function") return;
  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  const reduceMotion =
    window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // --- Qanary palette (read from the shared stylesheet, with hard fallbacks) ---
  const css = getComputedStyle(document.documentElement);
  const cssVar = (name, fallback) => (css.getPropertyValue(name).trim() || fallback);
  const SHAPE_COLORS = [
    cssVar("--qg", "#16a87a"),     // Qanary green
    cssVar("--qb", "#1d6f9c"),     // Qanary blue
    cssVar("--accent", "#0f8a7a"), // teal accent
  ];
  const SPACE = "#0b1620";         // backdrop fill
  const MARK = "#ffffff";
  const TRACE = cssVar("--qg", "#16a87a");

  // --- viewport / hi-dpi handling --------------------------------------------
  let W = 0, H = 0, DPR = 1;
  let stars = [];
  function resize() {
    DPR = Math.min(window.devicePixelRatio || 1, 2);
    W = window.innerWidth;
    H = window.innerHeight;
    canvas.width = Math.round(W * DPR);
    canvas.height = Math.round(H * DPR);
    canvas.style.width = W + "px";
    canvas.style.height = H + "px";
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
    stars = Array.from({ length: Math.round((W * H) / 9000) }, () => ({
      x: Math.random() * W,
      y: Math.random() * H,
      r: Math.random() * 1.2 + 0.2,
      a: Math.random() * 0.5 + 0.2,
    }));
    marker.x = Math.min(Math.max(marker.x, 18), W - 18);
    marker.y = H - 34;
  }

  // --- entities --------------------------------------------------------------
  const marker = { x: 0, y: 0, w: 26, h: 30, cooldown: 0 };
  let shapes = [];
  let traces = [];
  let particles = [];   // particle effects
  let banner = null;    // transient banner text { text, t, dur }
  let score = 0;
  let lastMilestone = 0;
  let startTime = 0;
  let manualUntil = 0;  // while performance.now() < this, input drives the marker

  function makeShape(size, x, y) {
    const radius = size * 16;
    const n = 9 + Math.floor(Math.random() * 4);
    const outline = Array.from({ length: n }, () => 0.72 + Math.random() * 0.5);
    return {
      size, // 3 = large, 2 = medium, 1 = small
      x: x != null ? x : Math.random() * W,
      y: y != null ? y : -radius,
      vx: (Math.random() - 0.5) * 40,
      vy: 22 + Math.random() * 34 + (3 - size) * 10,
      r: radius,
      rot: Math.random() * Math.PI * 2,
      vrot: (Math.random() - 0.5) * 1.4,
      outline,
      color: SHAPE_COLORS[Math.floor(Math.random() * SHAPE_COLORS.length)],
    };
  }
  function spawnWave() {
    const n = 4 + Math.floor(Math.random() * 3);
    shapes = [];
    for (let i = 0; i < n; i++) {
      const s = makeShape(3, Math.random() * W, -Math.random() * H);
      if (reduceMotion) s.y = 60 + Math.random() * (H * 0.55); // scatter for the still frame
      shapes.push(s);
    }
  }

  // a burst of fading particles
  function spawnSparkles(x, y, baseColor, count, spread) {
    if (particles.length > 700) return;
    for (let i = 0; i < count; i++) {
      const a = Math.random() * Math.PI * 2;
      const sp = spread * (0.2 + Math.random() * 0.8);
      particles.push({
        x, y,
        vx: Math.cos(a) * sp,
        vy: Math.sin(a) * sp,
        life: 0,
        maxLife: 0.5 + Math.random() * 0.7,
        size: 1.2 + Math.random() * 2.2,
        color: Math.random() < 0.5 ? baseColor : "#ffffff",
      });
    }
  }

  // every 1000 points: a banner plus a spread of particle bursts
  function celebrate(value) {
    banner = { text: value.toLocaleString() + " points!", t: 0, dur: 2.0 };
    for (let i = 0; i < 5; i++) {
      const fx = W * (0.15 + 0.7 * (i / 4));
      const fy = H * (0.28 + Math.random() * 0.25);
      spawnSparkles(fx, fy, SHAPE_COLORS[i % SHAPE_COLORS.length], 40, 320);
    }
  }

  // --- input -----------------------------------------------------------------
  const keys = {};
  const CONTROL = { ArrowLeft: 1, ArrowRight: 1, a: 1, A: 1, d: 1, D: 1, " ": 1, Spacebar: 1 };
  const onInteractiveElement = () =>
    document.activeElement &&
    document.activeElement.closest &&
    document.activeElement.closest("a, button, input, textarea, select");

  window.addEventListener("keydown", (e) => {
    if (!CONTROL[e.key] || onInteractiveElement()) return; // keep keyboard nav intact
    e.preventDefault();
    keys[e.key] = true;
    manualUntil = performance.now() + 5000;
    if (e.key === " " || e.key === "Spacebar") emit();
  });
  window.addEventListener("keyup", (e) => { if (CONTROL[e.key]) keys[e.key] = false; });

  function emit() {
    if (marker.cooldown > 0) return;
    traces.push({ x: marker.x, y: marker.y - marker.h / 2, vy: -460 });
    marker.cooldown = 0.18;
  }

  // --- update ----------------------------------------------------------------
  function nearestShape() {
    let best = null, bestD = Infinity;
    for (const s of shapes) {
      const d = Math.abs(s.x - marker.x) + Math.max(0, marker.y - s.y) * 0.15;
      if (d < bestD) { bestD = d; best = s; }
    }
    return best;
  }

  function update(dt, now) {
    let dir = 0;
    if (now < manualUntil) {
      if (keys.ArrowLeft || keys.a || keys.A) dir -= 1;
      if (keys.ArrowRight || keys.d || keys.D) dir += 1;
    } else {
      // when idle: track the nearest shape and emit when lined up
      const target = nearestShape();
      if (target) {
        const dx = target.x - marker.x;
        if (Math.abs(dx) > 6) dir = dx > 0 ? 1 : -1;
        if (Math.abs(dx) < 24) emit();
      } else {
        emit();
      }
    }
    marker.x = Math.min(Math.max(marker.x + dir * 300 * dt, 18), W - 18);
    marker.y = H - 34;
    if (marker.cooldown > 0) marker.cooldown -= dt;

    for (const t of traces) t.y += t.vy * dt;
    traces = traces.filter((t) => t.y > -10);

    for (const s of shapes) {
      s.x += s.vx * dt;
      s.y += s.vy * dt;
      s.rot += s.vrot * dt;
      if (s.x < -s.r) s.x = W + s.r;
      else if (s.x > W + s.r) s.x = -s.r;
      if (s.y > H + s.r) { s.y = -s.r; s.x = Math.random() * W; } // recycle off the bottom
    }

    // particles drift and fade out
    for (const p of particles) {
      p.life += dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vx *= 0.95;
      p.vy *= 0.95;
    }
    particles = particles.filter((p) => p.life < p.maxLife);
    if (banner) { banner.t += dt; if (banner.t >= banner.dur) banner = null; }

    // a trace meets a shape: remove it, add points, emit particles, split larger ones
    for (let i = shapes.length - 1; i >= 0; i--) {
      const s = shapes[i];
      for (let j = traces.length - 1; j >= 0; j--) {
        const t = traces[j];
        const dx = t.x - s.x, dy = t.y - s.y;
        if (dx * dx + dy * dy <= s.r * s.r) {
          traces.splice(j, 1);
          shapes.splice(i, 1);
          score += (4 - s.size) * 10;
          if (s.size > 1) {
            spawnSparkles(s.x, s.y, s.color, 10, 130);   // smaller burst when it splits
            for (let k = 0; k < 2; k++) {
              const child = makeShape(s.size - 1, s.x, s.y);
              child.vx = (Math.random() - 0.5) * 90;
              child.vy = Math.abs(child.vy) * 0.6 + 20;
              shapes.push(child);
            }
          } else {
            spawnSparkles(s.x, s.y, s.color, 24, 220);    // larger burst when fully removed
          }
          break;
        }
      }
    }

    // banner at each 1000-point milestone
    const milestone = Math.floor(score / 1000);
    if (milestone > lastMilestone) { lastMilestone = milestone; celebrate(milestone * 1000); }

    // keep the field populated
    if (shapes.length === 0) spawnWave();
    else if (shapes.length < 3 && Math.random() < dt * 0.6) shapes.push(makeShape(3));
  }

  // --- render ----------------------------------------------------------------
  function drawMarker() {
    ctx.save();
    ctx.translate(marker.x, marker.y);
    ctx.beginPath();
    ctx.moveTo(0, -marker.h / 2);
    ctx.lineTo(marker.w / 2, marker.h / 2);
    ctx.lineTo(0, marker.h / 2 - 7);
    ctx.lineTo(-marker.w / 2, marker.h / 2);
    ctx.closePath();
    ctx.fillStyle = MARK;
    ctx.shadowColor = SHAPE_COLORS[0];
    ctx.shadowBlur = 12;
    ctx.fill();
    ctx.restore();
  }
  function drawShape(s) {
    ctx.save();
    ctx.translate(s.x, s.y);
    ctx.rotate(s.rot);
    ctx.beginPath();
    for (let i = 0; i < s.outline.length; i++) {
      const ang = (i / s.outline.length) * Math.PI * 2;
      const rad = s.r * s.outline[i];
      const x = Math.cos(ang) * rad, y = Math.sin(ang) * rad;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.globalAlpha = 0.85;
    ctx.fillStyle = s.color;
    ctx.fill();
    ctx.globalAlpha = 1;
    ctx.lineWidth = 1.5;
    ctx.strokeStyle = "rgba(255,255,255,.45)";
    ctx.stroke();
    ctx.restore();
  }
  function drawParticles() {
    ctx.globalCompositeOperation = "lighter";
    for (const p of particles) {
      const k = 1 - p.life / p.maxLife; // 1 -> 0
      ctx.globalAlpha = k;
      ctx.fillStyle = p.color;
      const sz = p.size * (0.5 + k * 0.5);
      ctx.beginPath();
      ctx.arc(p.x, p.y, sz, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalCompositeOperation = "source-over";
    ctx.globalAlpha = 1;
  }
  function drawBanner() {
    if (!banner) return;
    const k = banner.t / banner.dur;                       // 0 -> 1
    const alpha = k < 0.15 ? k / 0.15 : 1 - (k - 0.15) / 0.85; // quick in, slow out
    const scale = 0.8 + k * 0.6;
    ctx.save();
    ctx.globalAlpha = Math.max(0, Math.min(1, alpha));
    ctx.translate(W / 2, H * 0.8);
    ctx.scale(scale, scale);
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.font = "800 34px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";
    const g = ctx.createLinearGradient(-160, 0, 160, 0);
    g.addColorStop(0, SHAPE_COLORS[0]);
    g.addColorStop(1, SHAPE_COLORS[1]);
    ctx.fillStyle = g;
    ctx.shadowColor = SHAPE_COLORS[0];
    ctx.shadowBlur = 22;
    ctx.fillText("★ " + banner.text + " ★", 0, 0);
    ctx.restore();
  }
  function render() {
    ctx.fillStyle = SPACE;
    ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = "#cfe8f1";
    for (const s of stars) { ctx.globalAlpha = s.a; ctx.fillRect(s.x, s.y, s.r, s.r); }
    ctx.globalAlpha = 1;
    for (const s of shapes) drawShape(s);
    ctx.fillStyle = TRACE;
    for (const t of traces) ctx.fillRect(t.x - 1.5, t.y - 6, 3, 9);
    drawMarker();
    drawParticles();
    drawBanner();

    // overlay text: elapsed time + points (right), hint (left)
    const elapsed = Math.max(0, Math.floor((performance.now() - startTime) / 1000));
    ctx.textBaseline = "alphabetic";
    ctx.font = "12px ui-monospace, SFMono-Regular, Menlo, monospace";
    ctx.fillStyle = "rgba(255,255,255,.34)";
    ctx.textAlign = "right";
    ctx.fillText("TIME " + elapsed + "s   ·   SCORE " + score, W - 14, H - 14);
    ctx.textAlign = "left";
    ctx.fillText("← → move · space to shoot", 14, H - 14);
  }

  // --- loop ------------------------------------------------------------------
  let last = 0, raf = 0;
  function frame(t) {
    const now = t || performance.now();
    let dt = (now - last) / 1000;
    last = now;
    if (dt > 0.05) dt = 0.05; // clamp after a tab switch / stall
    update(dt, now);
    render();
    raf = requestAnimationFrame(frame);
  }

  function start() {
    resize();
    marker.x = W / 2;
    spawnWave();
    startTime = performance.now();
    if (reduceMotion) { render(); return; } // static scene, no animation
    last = performance.now();
    raf = requestAnimationFrame(frame);
  }

  window.addEventListener("resize", resize);
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) { cancelAnimationFrame(raf); raf = 0; }
    else if (!reduceMotion && !raf) { last = performance.now(); raf = requestAnimationFrame(frame); }
  });

  start();
})();
