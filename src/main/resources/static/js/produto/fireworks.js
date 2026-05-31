(function () {
  "use strict";

  if (!document.getElementById("trails-canvas")) return;

  const IS_MOBILE = window.innerWidth <= 640;
  const GRAVITY = 0.9;
  let simSpeed = 1;
  let stageW, stageH;
  let quality = 2;
  let isLowQuality = false;
  let isNormalQuality = true;
  let isHighQuality = false;

  const COLOR = {
    Red: "#ff0043",
    Green: "#14fc56",
    Blue: "#1e7fff",
    Purple: "#e60aff",
    Gold: "#ffbf36",
    White: "#ffffff",
  };
  const INVISIBLE = "_INVISIBLE_";
  const PI_2 = Math.PI * 2;
  const PI_HALF = Math.PI * 0.5;

  const COLOR_CODES = Object.values(COLOR);
  const COLOR_CODES_W_INVIS = [...COLOR_CODES, INVISIBLE];
  const COLOR_TUPLES = {};
  COLOR_CODES.forEach((hex) => {
    COLOR_TUPLES[hex] = {
      r: parseInt(hex.substr(1, 2), 16),
      g: parseInt(hex.substr(3, 2), 16),
      b: parseInt(hex.substr(5, 2), 16),
    };
  });

  const MyMath = {
    random: (min, max) => min + Math.random() * (max - min),
    randomChoice: (arr) => arr[Math.floor(Math.random() * arr.length)],
    clamp: (v, min, max) => Math.min(Math.max(v, min), max),
    pointDist: (x1, y1, x2, y2) => Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2),
    pointAngle: (x1, y1, x2, y2) => Math.atan2(x2 - x1, y2 - y1),
  };

  // Canvas setup
  const trailsCanvas = document.getElementById("trails-canvas");
  const mainCanvas = document.getElementById("main-canvas");
  const trailsCtx = trailsCanvas.getContext("2d");
  const mainCtx = mainCanvas.getContext("2d");
  const dpr = window.devicePixelRatio || 1;

  function resizeCanvases() {
    stageW = window.innerWidth;
    stageH = window.innerHeight;
    [trailsCanvas, mainCanvas].forEach((c) => {
      c.width = stageW * dpr;
      c.height = stageH * dpr;
      c.style.width = stageW + "px";
      c.style.height = stageH + "px";
    });
  }
  resizeCanvases();
  window.addEventListener("resize", resizeCanvases);

  function randomColor(opts) {
    let color = COLOR_CODES[Math.random() * COLOR_CODES.length | 0];
    if (opts && opts.notColor) {
      while (color === opts.notColor) {
        color = COLOR_CODES[Math.random() * COLOR_CODES.length | 0];
      }
    }
    return color;
  }
  function whiteOrGold() {
    return Math.random() < 0.5 ? COLOR.Gold : COLOR.White;
  }
  function makePistilColor(c) {
    return (c === COLOR.White || c === COLOR.Gold)
      ? randomColor({ notColor: c })
      : whiteOrGold();
  }

  function createParticleCollection() {
    const c = {};
    COLOR_CODES_W_INVIS.forEach((col) => c[col] = []);
    return c;
  }

  const Star = {
    drawWidth: 3,
    airDrag: 0.98,
    airDragHeavy: 0.992,
    active: createParticleCollection(),
    _pool: [],
    _new() {
      return {};
    },
    add(x, y, color, angle, speed, life, sox, soy) {
      const i = this._pool.pop() || this._new();
      i.visible = true;
      i.heavy = false;
      i.x = x;
      i.y = y;
      i.prevX = x;
      i.prevY = y;
      i.color = color;
      i.speedX = Math.sin(angle) * speed + (sox || 0);
      i.speedY = Math.cos(angle) * speed + (soy || 0);
      i.life = life;
      i.fullLife = life;
      i.spinAngle = Math.random() * PI_2;
      i.spinSpeed = 0.8;
      i.spinRadius = 0;
      i.sparkFreq = 0;
      i.sparkSpeed = 1;
      i.sparkTimer = 0;
      i.sparkColor = color;
      i.sparkLife = 750;
      i.sparkLifeVariation = 0.25;
      i.strobe = false;
      i.updateFrame = 0;
      i.onDeath = null;
      i.secondColor = null;
      i.transitionTime = 0;
      i.colorChanged = false;
      this.active[color].push(i);
      return i;
    },
    returnInstance(i) {
      i.onDeath && i.onDeath(i);
      i.onDeath = null;
      i.secondColor = null;
      i.transitionTime = 0;
      i.colorChanged = false;
      this._pool.push(i);
    },
  };

  const Spark = {
    drawWidth: 1,
    airDrag: 0.9,
    active: createParticleCollection(),
    _pool: [],
    _new() {
      return {};
    },
    add(x, y, color, angle, speed, life) {
      const i = this._pool.pop() || this._new();
      i.x = x;
      i.y = y;
      i.prevX = x;
      i.prevY = y;
      i.color = color;
      i.speedX = Math.sin(angle) * speed;
      i.speedY = Math.cos(angle) * speed;
      i.life = life;
      this.active[color].push(i);
      return i;
    },
    returnInstance(i) {
      this._pool.push(i);
    },
  };

  const BurstFlash = {
    active: [],
    _pool: [],
    _new() {
      return {};
    },
    add(x, y, radius) {
      const i = this._pool.pop() || this._new();
      i.x = x;
      i.y = y;
      i.radius = radius;
      this.active.push(i);
      return i;
    },
    returnInstance(i) {
      this._pool.push(i);
    },
  };

  function createBurst(count, factory, startAngle = 0, arcLength = PI_2) {
    const R = 0.5 * Math.sqrt(count / Math.PI);
    const C = 2 * R * Math.PI;
    const C_HALF = C / 2;
    for (let i = 0; i <= C_HALF; i++) {
      const ringAngle = i / C_HALF * PI_HALF;
      const ringSize = Math.cos(ringAngle);
      const partsPerFullRing = C * ringSize;
      const partsPerArc = partsPerFullRing * (arcLength / PI_2);
      const angleInc = PI_2 / partsPerFullRing;
      const angleOffset = Math.random() * angleInc + startAngle;
      const maxRandom = angleInc * 0.33;
      for (let j = 0; j < partsPerArc; j++) {
        factory(
          angleInc * j + angleOffset + Math.random() * maxRandom,
          ringSize,
        );
      }
    }
  }

  function createParticleArc(start, arcLen, count, rand, factory) {
    const d = arcLen / count;
    const end = start + arcLen - (d * 0.5);
    if (end > start) {
      for (let a = start; a < end; a += d) {
        factory(a + Math.random() * d * rand);
      }
    } else {for (let a = start; a > end; a += d) {
        factory(a + Math.random() * d * rand);
      }}
  }

  // Shell burst
  function burst(x, y, opts) {
    const speed = opts.spreadSize / 96;
    const color = opts.color || randomColor();
    const starLife = opts.starLife || 1000;
    const starLifeVar = opts.starLifeVariation || 0.125;
    const starCount = opts.starCount ||
      Math.max(6, Math.pow(opts.spreadSize / 54, 2));

    let sparkFreq = 0, sparkSpeed = 0, sparkLife = 0, sparkLifeVar = 0.25;
    if (opts.glitter === "light") {
      sparkFreq = 400;
      sparkSpeed = 0.3;
      sparkLife = 300;
      sparkLifeVar = 2;
    } else if (opts.glitter === "heavy") {
      sparkFreq = 80;
      sparkSpeed = 0.8;
      sparkLife = 1400;
      sparkLifeVar = 2;
    }
    sparkFreq = sparkFreq / quality;

    const starFactory = (angle, speedMult) => {
      const star = Star.add(
        x,
        y,
        color,
        angle,
        speedMult * speed,
        starLife + Math.random() * starLife * starLifeVar,
        0,
        -opts.spreadSize / 1800,
      );
      if (opts.pistilColor) {
        star.transitionTime = starLife * (Math.random() * 0.05 + 0.32);
        star.secondColor = opts.pistilColor;
      }
      if (sparkFreq) {
        star.sparkFreq = sparkFreq;
        star.sparkSpeed = sparkSpeed;
        star.sparkLife = sparkLife;
        star.sparkLifeVariation = sparkLifeVar;
        star.sparkColor = opts.glitterColor || color;
        star.sparkTimer = Math.random() * star.sparkFreq;
      }
    };
    createBurst(starCount, starFactory);

    if (opts.pistilColor) {
      burst(x, y, {
        spreadSize: opts.spreadSize * 0.5,
        starLife: starLife * 0.6,
        color: opts.pistilColor,
        starCount: Math.max(4, starCount * 0.4),
      });
    }
    BurstFlash.add(x, y, opts.spreadSize / 4);
  }

  // Lança um foguete e explode no alto
  function launchShell(xFrac, heightFrac) {
    const hpad = 60;
    const launchX = xFrac * (stageW - hpad * 2) + hpad;
    const launchY = stageH;
    const minHeight = stageH - stageH * 0.45;
    const vpad = 50;
    const burstY = minHeight - (heightFrac * (minHeight - vpad));
    const launchDistance = launchY - burstY;
    const launchVelocity = Math.pow(launchDistance * 0.04, 0.64);

    const colors = [
      COLOR.Red,
      COLOR.Green,
      COLOR.Blue,
      COLOR.Purple,
      COLOR.Gold,
      COLOR.White,
    ];
    const color = colors[Math.floor(Math.random() * colors.length)];
    const size = IS_MOBILE ? 2 : 3;
    const spreadSize = 300 + size * 100;

    // Cometa de subida
    const comet = Star.add(
      launchX,
      launchY,
      COLOR.White,
      Math.PI,
      launchVelocity,
      launchVelocity * 400,
    );
    comet.heavy = true;
    comet.spinRadius = MyMath.random(0.32, 0.85);
    comet.sparkFreq = 32 / quality;
    comet.sparkLife = 320;
    comet.sparkLifeVariation = 3;
    comet.secondColor = INVISIBLE;
    comet.transitionTime = Math.pow(Math.random(), 1.5) * 700 + 500;
    comet.onDeath = () => {
      burst(launchX, burstY, {
        spreadSize,
        starLife: 900 + size * 200,
        color,
        pistilColor: makePistilColor(color),
        glitter: Math.random() < 0.5 ? "light" : "heavy",
        glitterColor: whiteOrGold(),
      });
    };
  }

  // Lança vários fogos de uma vez (finale!)
  function lancaFinale() {
    const positions = [0.15, 0.3, 0.5, 0.7, 0.85];
    positions.forEach((x, i) => {
      setTimeout(() => {
        launchShell(x, 0.5 + Math.random() * 0.35);
      }, i * 180);
    });
    // Mais 2 no centro com delay
    setTimeout(() => launchShell(0.4, 0.7 + Math.random() * 0.2), 600);
    setTimeout(() => launchShell(0.6, 0.7 + Math.random() * 0.2), 800);
  }

  // Loop de animação
  let paused = false;
  let currentFrame = 0;
  let lastTime = 0;

  function update(ts) {
    if (paused) return;
    requestAnimationFrame(update);

    const frameTime = Math.min(ts - lastTime, 67);
    lastTime = ts;
    const lag = frameTime / 16.666;
    const timeStep = frameTime * simSpeed;
    const speed = simSpeed * lag;
    currentFrame++;

    const starDrag = 1 - (1 - Star.airDrag) * speed;
    const starDragHeavy = 1 - (1 - Star.airDragHeavy) * speed;
    const sparkDrag = 1 - (1 - Spark.airDrag) * speed;
    const gAcc = timeStep / 1000 * GRAVITY;

    COLOR_CODES_W_INVIS.forEach((color) => {
      const stars = Star.active[color];
      for (let i = stars.length - 1; i >= 0; i--) {
        const star = stars[i];
        if (star.updateFrame === currentFrame) continue;
        star.updateFrame = currentFrame;
        star.life -= timeStep;
        if (star.life <= 0) {
          stars.splice(i, 1);
          Star.returnInstance(star);
        } else {
          const burnRate = Math.pow(star.life / star.fullLife, 0.5);
          const burnRateInverse = 1 - burnRate;
          star.prevX = star.x;
          star.prevY = star.y;
          star.x += star.speedX * speed;
          star.y += star.speedY * speed;
          if (!star.heavy) {
            star.speedX *= starDrag;
            star.speedY *= starDrag;
          } else {
            star.speedX *= starDragHeavy;
            star.speedY *= starDragHeavy;
          }
          star.speedY += gAcc;
          if (star.spinRadius) {
            star.spinAngle += star.spinSpeed * speed;
            star.x += Math.sin(star.spinAngle) * star.spinRadius * speed;
            star.y += Math.cos(star.spinAngle) * star.spinRadius * speed;
          }
          if (star.sparkFreq) {
            star.sparkTimer -= timeStep;
            while (star.sparkTimer < 0) {
              star.sparkTimer += star.sparkFreq * 0.75 +
                star.sparkFreq * burnRateInverse * 4;
              Spark.add(
                star.x,
                star.y,
                star.sparkColor,
                Math.random() * PI_2,
                Math.random() * star.sparkSpeed * burnRate,
                star.sparkLife * 0.8 +
                  Math.random() * star.sparkLifeVariation * star.sparkLife,
              );
            }
          }
          if (star.life < star.transitionTime) {
            if (star.secondColor && !star.colorChanged) {
              star.colorChanged = true;
              star.color = star.secondColor;
              stars.splice(i, 1);
              Star.active[star.secondColor].push(star);
              if (star.secondColor === INVISIBLE) star.sparkFreq = 0;
            }
            if (star.strobe) {
              star.visible = Math.floor(star.life / star.strobeFreq) % 3 === 0;
            }
          }
        }
      }
      const sparks = Spark.active[color];
      for (let i = sparks.length - 1; i >= 0; i--) {
        const spark = sparks[i];
        spark.life -= timeStep;
        if (spark.life <= 0) {
          sparks.splice(i, 1);
          Spark.returnInstance(spark);
        } else {
          spark.prevX = spark.x;
          spark.prevY = spark.y;
          spark.x += spark.speedX * speed;
          spark.y += spark.speedY * speed;
          spark.speedX *= sparkDrag;
          spark.speedY *= sparkDrag;
          spark.speedY += gAcc;
        }
      }
    });

    render(speed);
  }

  function render(speed) {
    trailsCtx.scale(dpr, dpr);
    mainCtx.scale(dpr, dpr);

    trailsCtx.globalCompositeOperation = "source-over";
    trailsCtx.fillStyle = "rgba(0,0,0,0.18)";
    trailsCtx.fillRect(0, 0, stageW, stageH);
    mainCtx.clearRect(0, 0, stageW, stageH);

    while (BurstFlash.active.length) {
      const bf = BurstFlash.active.pop();
      const g = trailsCtx.createRadialGradient(
        bf.x,
        bf.y,
        0,
        bf.x,
        bf.y,
        bf.radius,
      );
      g.addColorStop(0.024, "rgba(255,255,255,1)");
      g.addColorStop(0.125, "rgba(255,160,20,0.2)");
      g.addColorStop(0.32, "rgba(255,140,20,0.11)");
      g.addColorStop(1, "rgba(255,120,20,0)");
      trailsCtx.fillStyle = g;
      trailsCtx.fillRect(
        bf.x - bf.radius,
        bf.y - bf.radius,
        bf.radius * 2,
        bf.radius * 2,
      );
      BurstFlash.returnInstance(bf);
    }

    trailsCtx.globalCompositeOperation = "lighten";
    trailsCtx.lineWidth = Star.drawWidth;
    trailsCtx.lineCap = "round";
    mainCtx.strokeStyle = "#fff";
    mainCtx.lineWidth = 1;
    mainCtx.beginPath();
    COLOR_CODES.forEach((color) => {
      const stars = Star.active[color];
      trailsCtx.strokeStyle = color;
      trailsCtx.beginPath();
      stars.forEach((star) => {
        if (star.visible) {
          trailsCtx.moveTo(star.x, star.y);
          trailsCtx.lineTo(star.prevX, star.prevY);
          mainCtx.moveTo(star.x, star.y);
          mainCtx.lineTo(
            star.x - star.speedX * 1.6,
            star.y - star.speedY * 1.6,
          );
        }
      });
      trailsCtx.stroke();
    });
    mainCtx.stroke();
    trailsCtx.lineWidth = Spark.drawWidth;
    trailsCtx.lineCap = "butt";
    COLOR_CODES.forEach((color) => {
      const sparks = Spark.active[color];
      trailsCtx.strokeStyle = color;
      trailsCtx.beginPath();
      sparks.forEach((spark) => {
        trailsCtx.moveTo(spark.x, spark.y);
        trailsCtx.lineTo(spark.prevX, spark.prevY);
      });
      trailsCtx.stroke();
    });

    trailsCtx.setTransform(1, 0, 0, 1, 0, 0);
    mainCtx.setTransform(1, 0, 0, 1, 0, 0);
  }

  // === FADE IN/OUT da sobreposição escura ===
  const overlay = document.createElement("div");
  overlay.style.cssText =
    "position:fixed;top:0;left:0;width:100%;height:100%;background:#000;z-index:2;pointer-events:none;opacity:0.15;";
  document.body.appendChild(overlay);

  // Inicia escuro, lança fogos, depois clareia
  requestAnimationFrame((ts) => {
    lastTime = ts;
    requestAnimationFrame(update);
  });

  lancaFinale();

  // Após 5s começa a clarear muito devagar
  setTimeout(() => {
    overlay.style.transition = "opacity 7s ease";
    overlay.style.opacity = "0";
  }, 5000);

  // Após 12s remove overlay e para fogos com fade suave
  setTimeout(() => {
    paused = true;
    overlay.remove();
    const wrap = document.getElementById("fireworks-wrap");
    if (wrap) {
      wrap.style.transition = "opacity 4s ease";
      wrap.style.opacity = "0";
      setTimeout(() => {
        if (wrap) wrap.style.display = "none";
      }, 4000);
    }
  }, 12000);
})();
