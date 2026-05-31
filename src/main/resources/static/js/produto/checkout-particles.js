(function () {
  function init() {
    const canvas = document.getElementById("ck-particles");
    const btn = document.getElementById("btnConfirmar");
    if (!canvas || !btn) return;

    const ctx = canvas.getContext("2d", { alpha: true });
    let particles = [];
    let isHovered = false;
    let frameCount = 0;
    let dpr = window.devicePixelRatio || 1;
    let btnPos = { x: 0, y: 0, w: 0, h: 0 };

    const rand = (a, b) => a + Math.random() * (b - a);

    const SHAPES = ["circle", "square", "diamond", "heart"];

    const drawShape = (ctx, shape, cx, cy, r) => {
      ctx.beginPath();
      if (shape === "circle") {
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
      } else if (shape === "square") {
        ctx.rect(cx - r, cy - r, r * 2, r * 2);
      } else if (shape === "diamond") {
        ctx.moveTo(cx, cy - r);
        ctx.lineTo(cx + r, cy);
        ctx.lineTo(cx, cy + r);
        ctx.lineTo(cx - r, cy);
        ctx.closePath();
      } else if (shape === "heart") {
        ctx.moveTo(cx, cy + r * 0.5);
        ctx.bezierCurveTo(
          cx + r,
          cy - r * 0.3,
          cx + r * 1.4,
          cy - r,
          cx,
          cy - r * 1.3,
        );
        ctx.bezierCurveTo(
          cx - r * 1.4,
          cy - r,
          cx - r,
          cy - r * 0.3,
          cx,
          cy + r * 0.5,
        );
        ctx.closePath();
      }
      ctx.fill();
    };

    const syncLayout = () => {
      dpr = window.devicePixelRatio || 1;
      const rect = btn.getBoundingClientRect();
      const wrap = canvas.parentElement.getBoundingClientRect();
      canvas.width = wrap.width * dpr;
      canvas.height = wrap.height * dpr;
      canvas.style.width = wrap.width + "px";
      canvas.style.height = wrap.height + "px";
      ctx.scale(dpr, dpr);
      btnPos = {
        x: rect.left - wrap.left + rect.width / 2,
        y: rect.top - wrap.top + rect.height / 2,
        w: rect.width,
        h: rect.height,
      };
    };

    const spawnParticle = (x, y, isBurst) => {
      const hue = rand(0, 360);
      const angle = rand(0, Math.PI * 2);
      const speed = isBurst ? rand(1.5, 4.5) : rand(0.3, 0.8);
      const size = isBurst ? rand(3, 8) : rand(2, 5);
      return {
        x,
        y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed - (isBurst ? rand(0.5, 2) : 0),
        gravity: isBurst ? 0.06 : 0.01,
        life: isBurst ? rand(0.8, 1.0) : rand(0.4, 0.8),
        fade: isBurst ? rand(0.012, 0.022) : rand(0.008, 0.015),
        size,
        hue,
        shape: SHAPES[Math.floor(Math.random() * SHAPES.length)],
        isBurst,
      };
    };

    const burst = (x, y) => {
      for (let i = 0; i < 80; i++) {
        particles.push(spawnParticle(x, y, true));
      }
    };

    const spawnAmbient = () => {
      const angle = rand(0, Math.PI * 2);
      const r = btnPos.w * 0.5 * rand(0.85, 1.05);
      particles.push(spawnParticle(
        btnPos.x + Math.cos(angle) * r,
        btnPos.y + Math.sin(angle) * r * 0.4,
        false,
      ));
    };

    const tick = () => {
      const w = canvas.width / dpr;
      const h = canvas.height / dpr;
      ctx.clearRect(0, 0, w, h);

      const burstActive = particles.some((p) => p.isBurst && p.life > 0);

      if (isHovered && !burstActive && ++frameCount % 3 === 0) {
        spawnAmbient();
      }

      let i = 0;
      while (i < particles.length) {
        const p = particles[i];
        p.x += p.vx;
        p.y += p.vy;
        p.vy += p.gravity;
        p.life -= p.fade;

        if (p.life <= 0) {
          particles[i] = particles[particles.length - 1];
          particles.pop();
          continue;
        }

        ctx.globalAlpha = Math.max(0, p.life);
        ctx.fillStyle = `oklch(80% 0.30 ${p.hue}deg)`;
        ctx.shadowBlur = 6;
        ctx.shadowColor = ctx.fillStyle;
        drawShape(ctx, p.shape, p.x, p.y, p.size);
        ctx.fill();
        i++;
      }

      ctx.globalAlpha = 1;
      ctx.shadowBlur = 0;
      requestAnimationFrame(tick);
    };

    btn.addEventListener("mouseenter", () => (isHovered = true));
    btn.addEventListener("mouseleave", () => (isHovered = false));
    btn.addEventListener("mousedown", (e) => {
      if (!btn.disabled) {
        const wrap = canvas.parentElement.getBoundingClientRect();
        burst(e.clientX - wrap.left, e.clientY - wrap.top);
      }
    });

    window.addEventListener("resize", () => {
      syncLayout();
    });

    syncLayout();
    requestAnimationFrame(tick);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
