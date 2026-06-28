/* Qanary project page — vanilla JS, works from file:// and GitHub Pages. */
(function () {
  "use strict";

  /* ---- mobile nav -------------------------------------------------- */
  var burger = document.querySelector(".nav__burger");
  var links = document.querySelector(".nav__links");
  if (burger && links) {
    burger.addEventListener("click", function () { links.classList.toggle("open"); });
    links.addEventListener("click", function (e) {
      if (e.target.tagName === "A") links.classList.remove("open");
    });
  }

  /* ---- lightbox gallery ------------------------------------------- */
  var shots = Array.prototype.slice.call(document.querySelectorAll(".shot"));
  var lb = document.getElementById("lightbox");
  if (lb && shots.length) {
    var lbImg = lb.querySelector(".lb__img");
    var lbCap = lb.querySelector(".lb__cap");
    var lbCount = lb.querySelector(".lb__count");
    var current = 0;

    function show(i) {
      current = (i + shots.length) % shots.length;
      var fig = shots[current];
      var img = fig.querySelector("img");
      var b = fig.querySelector("figcaption b");
      var s = fig.querySelector("figcaption span");
      // Reuse the exact resource the thumbnail already resolved (currentSrc is an
      // absolute URL once loaded) so the enlarged view works identically whether
      // the page is served over http(s) or opened directly via file://.
      lbImg.src = img.currentSrc || img.src || img.getAttribute("data-full");
      lbImg.alt = img.alt;
      if (lbCount) lbCount.innerHTML = "<b>" + (current + 1) + "</b> / " + shots.length;
      lbCap.innerHTML = "<b>" + (b ? b.textContent : "") + "</b>" +
                        (s ? " &nbsp;—&nbsp; " + s.textContent : "");
    }
    function open(i) { show(i); lb.classList.add("open"); document.body.style.overflow = "hidden"; }
    function close() { lb.classList.remove("open"); document.body.style.overflow = ""; }

    shots.forEach(function (fig, i) {
      fig.addEventListener("click", function () { open(i); });
      fig.setAttribute("tabindex", "0");
      fig.addEventListener("keydown", function (e) {
        if (e.key === "Enter" || e.key === " ") { e.preventDefault(); open(i); }
      });
    });

    lb.addEventListener("click", function (e) {
      if (e.target === lb || e.target.classList.contains("lb__x")) close();
      else if (e.target.classList.contains("lb__next")) show(current + 1);
      else if (e.target.classList.contains("lb__prev")) show(current - 1);
    });
    document.addEventListener("keydown", function (e) {
      if (!lb.classList.contains("open")) return;
      if (e.key === "Escape") close();
      else if (e.key === "ArrowRight") show(current + 1);
      else if (e.key === "ArrowLeft") show(current - 1);
    });
  }

  /* ---- reveal on scroll (fail-safe: never leave content hidden) --- */
  var reveals = Array.prototype.slice.call(document.querySelectorAll(".reveal"));
  function revealAll() { reveals.forEach(function (el) { el.classList.add("in"); }); }
  if ("IntersectionObserver" in window && reveals.length) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add("in"); io.unobserve(en.target); }
      });
    }, { threshold: 0, rootMargin: "0px 0px -8% 0px" });
    reveals.forEach(function (el) {
      // Reveal anything already at/above the viewport (e.g. when the page is
      // loaded directly at a deep #anchor) so no section can stay invisible.
      if (el.getBoundingClientRect().top < window.innerHeight * 1.1) el.classList.add("in");
      else io.observe(el);
    });
    // Belt-and-braces: once everything has loaded, reveal any stragglers.
    window.addEventListener("load", function () { setTimeout(revealAll, 800); });
  } else {
    revealAll();
  }

  /* ---- footer year ------------------------------------------------- */
  var y = document.getElementById("year");
  if (y) y.textContent = new Date().getFullYear();
})();
