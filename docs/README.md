# Qanary project page (GitHub Pages)

A static landing page for the Qanary framework with four parts:

1. **The Qanary methodology** — the idea and how the vocabulary-driven approach works.
2. **How a Qanary component works** — the microservice / SPARQL annotation model.
3. **Screenshot gallery** — the pipeline's web user interface (click to enlarge).
4. **Publications & references** — each entry links to its own sub-page at
   <https://wse-research.org/publications>.

It is plain HTML/CSS/JS (no build step, no Jekyll) so it works both as GitHub
Pages and when opened locally.

## Check it locally

Either just open the file:

```bash
xdg-open docs/index.html        # Linux   (or: open docs/index.html on macOS)
```

…or serve it (recommended — identical to how GitHub Pages serves it):

```bash
cd docs
python3 -m http.server 8000
# then visit http://localhost:8000/
```

## Publish via GitHub Pages

Repo **Settings → Pages → Build and deployment**:
- **Source:** *Deploy from a branch*
- **Branch:** `master` and folder **`/docs`** → *Save*
- **Custom domain:** `qanary.wse-research.org`

The site is served at **https://qanary.wse-research.org/**. The custom domain is
pinned by the `CNAME` file in this folder; a DNS `CNAME` record for
`qanary.wse-research.org` must point to `wdaqua.github.io`. Giving this repo its
own custom domain also overrides the WDAqua org-level domain, so the site is not
served under `wdaqua.eu`. The `.nojekyll` file disables Jekyll processing so all
assets are served as-is.

## Files

```
docs/
├── index.html              # the page
├── assets/css/style.css    # styling
├── assets/js/main.js       # lightbox, mobile nav, scroll reveal
├── assets/img/             # logo, favicon
│   └── screenshots/        # UI screenshots (from frontend-e2e/ and doc/)
├── .nojekyll
└── README.md
```

Screenshots are copied from `frontend-e2e/screenshots/` and `doc/`. To refresh
them after regenerating the E2E captures, re-copy those PNGs into
`docs/assets/img/screenshots/`.
