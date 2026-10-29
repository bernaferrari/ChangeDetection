/** Run `npm run build && npm run test:ui`. All writes use temporary projects. */
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs"),
  path = require("node:path"),
  os = require("node:os"),
  zlib = require("node:zlib");
const { pathToFileURL } = require("node:url");
const { chromium } = require("playwright");
const { serve } = require("./server.cjs");
const { createWorkspaceStore } = require("./workspace-store.cjs");
function png(width, height) {
  const crc = (buffer) => {
    let c = 0xffffffff;
    for (const byte of buffer) {
      c ^= byte;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    return (c ^ 0xffffffff) >>> 0;
  };
  const chunk = (type, data) => {
    const content = Buffer.concat([Buffer.from(type), data]),
      head = Buffer.alloc(4),
      tail = Buffer.alloc(4);
    head.writeUInt32BE(data.length);
    tail.writeUInt32BE(crc(content));
    return Buffer.concat([head, content, tail]);
  };
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 6;
  const raw = Buffer.alloc(height * (width * 4 + 1), 245);
  for (let y = 0; y < height; y++) {
    raw[y * (width * 4 + 1)] = 0;
    for (let x = 0; x < width; x++) raw[y * (width * 4 + 1) + x * 4 + 4] = 255;
  }
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", header),
    chunk("IDAT", zlib.deflateSync(raw)),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}
async function setup(t) {
  const root = fs.realpathSync(
      fs.mkdtempSync(path.join(os.tmpdir(), "studio-shadcn-")),
    ),
    write = (file, value) => {
      fs.mkdirSync(path.dirname(file), { recursive: true });
      fs.writeFileSync(
        file,
        Buffer.isBuffer(value) || typeof value === "string"
          ? value
          : JSON.stringify(value),
      );
    };
  const app = (folder, name) => {
    const repo = path.join(root, folder),
      l = path.join(repo, "store-listing");
    write(l + "/studio.json", { app: name, locales: ["en-US", "de-DE"] });
    write(l + "/profiles.json", {
      phone: {
        width: 400,
        height: 800,
        captureWidth: 400,
        captureHeight: 800,
        label: "Phone",
      },
      tablet7: { width: 600, height: 800, label: "Tablet" },
    });
    write(l + "/gallery.json", {
      phone: [
        { feature: "hello", raw: "phone/home.png", file: "phone/01-hello.png" },
      ],
      tablet7: [{ feature: "hello" }],
    });
    for (const locale of ["en-US", "de-DE"]) {
      write(`${l}/${locale}/copy.json`, {
        hello: { title: locale === "en-US" ? name : "", body: "Words" },
      });
      write(`${l}/${locale}/raw/phone/home.png`, png(400, 800));
      write(`${l}/${locale}/raw/phone/home.nodes.json`, {
        width: 400,
        height: 800,
        nodes: [
          { path: "0", cls: "Root", b: [0, 0, 400, 800] },
          {
            path: "0.1",
            cls: "Button",
            id: "app:id/action",
            text: "Suggested channel",
            b: locale === "en-US" ? [20, 100, 180, 160] : [40, 180, 220, 260],
          },
          {
            path: "0.1.0",
            cls: "TextView",
            text: "Tap to change",
            b: [50, 115, 150, 145],
          },
        ],
      });
    }
    write(l + "/en-US/phone/01-hello.png", png(400, 800));
    write(l + "/theme.js", "export default {}");
    write(
      l + "/theme.css",
      "body{background:#eee;font-family:Arial}header{top:30px;left:20px;right:20px}h1{font-size:36px}.device{left:60px;top:220px;width:280px;--bezel:6px;--radius:20px}",
    );
    return repo;
  };
  const roots = [app("Alpha", "Alpha"), app("Beta", "Beta")],
    library = path.join(root, "discover/Library");
  write(library + "/screenshots/sample.png", png(80, 160));
  const discoverApp = app("discover/Gamma", "Gamma"),
    store = createWorkspaceStore({
      registryFile: path.join(root, "registry.json"),
    });
  await store.add(roots);
  const projects = await store.list(),
    server = await serve(root, 0, {
      workspace: store,
      editable: true,
      selectFolder: async () => library,
    }),
    browser = await chromium.launch();
  t.after(async () => {
    if (/callout|mobile/.test(t.name))
      await page
        .screenshot({
          path:
            "/tmp/studio-shadcn-" +
            (/callout/.test(t.name) ? "picker" : "mobile") +
            "-test.png",
        })
        .catch(() => {});
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
    fs.rmSync(root, { recursive: true, force: true });
  });
  const page = await browser.newPage({
      viewport: { width: 1400, height: 1000 },
    }),
    base = `http://127.0.0.1:${server.address().port}`,
    errors = [];
  page.setDefaultTimeout(8000);
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", (m) => {
    if (/DialogContent.*requires|Missing.*Description/.test(m.text()))
      errors.push(m.text());
  });
  t.after(() => assert.deepEqual(errors, []));
  await page.goto(base + "/studio/");
  await page.locator(".workspace-card").first().waitFor();
  return {
    page,
    base,
    projects,
    roots,
    root,
    library,
    discoverApp,
    write,
    store,
  };
}
const activeNav = (page) => page.locator(".workspace-project.current");
async function view(page, name) {
  await activeNav(page).getByRole("link", { name, exact: true }).click();
}
async function choice(page, label, option) {
  await page.getByRole("combobox", { name: label, exact: true }).click();
  await page.getByRole("option", { name: option, exact: true }).click();
}
const active = (page) => page.locator(".editor-view:not([hidden])");

test("genuine shadcn sidebar, menus and navigation preserve the document and loaded cards", async (t) => {
  const { page } = await setup(t);
  await page.evaluate(() => {
    window.shell = document.querySelector('[data-slot="sidebar-inner"]');
    window.probe = true;
  });
  let documents = 0;
  page.on("request", (r) => {
    if (r.isNavigationRequest() && r.frame() === page.mainFrame()) documents++;
  });
  await page
    .locator(".sidebar")
    .getByRole("link", { name: "Alpha", exact: true })
    .click();
  await page.getByRole("tablist", { name: "Project sections" }).waitFor();
  await page.getByRole("link", { name: "Open studio", exact: true }).click();
  await active(page).locator("iframe.preview-ready").waitFor();
  const original = await active(page)
    .locator("iframe")
    .evaluateHandle((n) => n);
  await view(page, "Cards");
  await active(page).locator(".canvas-surface").waitFor();
  assert(
    await active(page)
      .locator("iframe")
      .evaluate((n, old) => n === old, original),
  );
  await view(page, "Copy");
  await page.getByRole("textbox").first().waitFor();
  await view(page, "Cards");
  await active(page).locator(".canvas-surface").waitFor();
  assert(
    await active(page)
      .locator("iframe")
      .evaluate((n, old) => n === old, original),
  );
  await page.goBack();
  await page.getByRole("textbox").first().waitFor();
  await page.goForward();
  await active(page).locator("iframe").waitFor();
  await page.getByRole("button", { name: "Joined", exact: true }).click();
  await page.waitForFunction(
    () =>
      document.querySelector(".editor-view:not([hidden]) .canvas-surface")
        ?.dataset.joined === "false",
  );
  await page.getByRole("slider", { name: "Preview size", exact: true }).focus();
  await page.keyboard.press("ArrowRight");
  assert(
    await active(page)
      .locator("iframe")
      .evaluate((n, old) => n === old, original),
  );
  assert.equal(documents, 0);
  assert(
    await page.evaluate(
      () =>
        window.probe &&
        window.shell === document.querySelector('[data-slot="sidebar-inner"]'),
    ),
  );
  assert((await page.locator('[data-slot="slider"]').count()) > 0);
  await page.locator("[data-slot=sidebar-trigger]").click();
  await page.waitForFunction(
    () =>
      document.querySelector("[data-slot=sidebar]")?.dataset.state ===
      "collapsed",
  );
});

test("copy filtering, custom selects, conflict recovery and keyboard saves retain draft data", async (t) => {
  const { page, base, projects, roots, write } = await setup(t);
  await page.goto(base + projects[0].editorURL + "?view=copy&locale=de-DE");
  const area = page.getByRole("textbox", {
    name: "de-DE hello title",
    exact: true,
  });
  await area.waitFor();
  await page.getByRole("button", { name: "Missing only", exact: true }).click();
  await area.pressSequentially("Draft kept while typing");
  assert.equal(await area.inputValue(), "Draft kept while typing");
  await page
    .getByRole("searchbox", { name: "Search copy", exact: true })
    .fill("not found");
  await page.getByText("No matching copy", { exact: true }).waitFor();
  await page
    .getByRole("button", { name: "Clear filters", exact: true })
    .click();
  assert.equal(await area.inputValue(), "Draft kept while typing");
  const search = page.getByRole("searchbox", {
    name: "Search copy",
    exact: true,
  });
  await search.fill("Draft kept");
  await search.press("Enter");
  assert.equal(
    JSON.parse(fs.readFileSync(roots[0] + "/store-listing/de-DE/copy.json"))
      .hello.title,
    "",
  );
  await choice(page, "Jump to copy card", "01 · Hello");
  assert.equal(await search.inputValue(), "");
  await area.press("Control+Enter");
  await page.getByText("All changes saved", { exact: true }).waitFor();
  assert.equal(
    JSON.parse(fs.readFileSync(roots[0] + "/store-listing/de-DE/copy.json"))
      .hello.title,
    "Draft kept while typing",
  );
  await area.fill("Conflict draft");
  write(roots[0] + "/store-listing/de-DE/copy.json", {
    hello: { title: "External change", body: "Words" },
  });
  await page.reload();
  await page.getByText("The saved copy changed", { exact: true }).waitFor();
  assert(
    await page
      .getByRole("button", { name: "Save de-DE/copy.json", exact: true })
      .isDisabled(),
  );
  await page
    .getByRole("button", { name: "Use saved copy", exact: true })
    .click();
  assert.equal(await area.inputValue(), "External change");
  await choice(page, "Language", "American English · reference");
  await page
    .getByRole("textbox", { name: "en-US hello title", exact: true })
    .waitFor();
  assert.equal(await active(page).locator(".copy-reference").count(), 0);
});

test("a pending save stays scoped to the original project after switching projects", async (t) => {
  const { page, base, projects, roots } = await setup(t);
  await page.goto(base + projects[0].editorURL + "?view=copy");
  await page.getByRole("textbox").first().fill("Saved only in Alpha");
  let release, observed;
  const gate = new Promise((r) => (release = r)),
    put = new Promise((r) => (observed = r));
  await page.route("**/en-US/copy.json", async (route) => {
    if (route.request().method() === "PUT") {
      observed();
      await gate;
    }
    await route.continue();
  });
  await page
    .getByRole("button", { name: "Save en-US/copy.json", exact: true })
    .click();
  await put;
  await page
    .locator(".sidebar")
    .getByRole("link", { name: "Beta", exact: true })
    .click();
  await page.getByRole("heading", { name: "Beta", exact: true }).waitFor();
  release();
  await page.waitForResponse((r) => r.request().method() === "PUT");
  assert.equal(
    JSON.parse(fs.readFileSync(roots[0] + "/store-listing/en-US/copy.json"))
      .hello.title,
    "Saved only in Alpha",
  );
  assert.equal(
    JSON.parse(fs.readFileSync(roots[1] + "/store-listing/en-US/copy.json"))
      .hello.title,
    "Beta",
  );
});

test("a slow abandoned editor cannot commit over the next project", async (t) => {
  const { page, projects } = await setup(t);
  let release, observed;
  const gate = new Promise((r) => (release = r)),
    started = new Promise((r) => (observed = r));
  await page.route("**/studio.json", async (route) => {
    if (route.request().url().includes(projects[0].id)) {
      observed();
      await gate;
    }
    await route.continue();
  });
  await page
    .locator(".workspace-card")
    .filter({ hasText: "Alpha" })
    .getByRole("link", { name: "Open studio", exact: true })
    .click();
  await started;
  await page
    .locator(".sidebar")
    .getByRole("link", { name: "Beta", exact: true })
    .click();
  await page.getByRole("heading", { name: "Beta", exact: true }).waitFor();
  release();
  await page.waitForTimeout(200);
  assert.equal(await page.locator("#main h1").textContent(), "Beta");
  assert.equal(
    await page
      .locator(".workspace-project.current .project-link-name")
      .textContent(),
    "Beta",
  );
});

test("language tabs and collapsibles mount previews only on demand and reuse them", async (t) => {
  const { page, base, projects } = await setup(t);
  await page.goto(base + projects[0].editorURL + "?view=languages");
  await page.locator(".language-row").first().waitFor();
  assert.equal(await page.locator("iframe").count(), 0);
  await page.getByRole("tab", { name: "Needs copy", exact: true }).click();
  assert.equal(await page.locator(".language-row:visible").count(), 1);
  await page
    .getByRole("searchbox", { name: "Search languages", exact: true })
    .fill("English");
  await page.getByText("No matching languages", { exact: true }).waitFor();
  await page
    .getByRole("button", { name: "Clear filters", exact: true })
    .click();
  const preview = page.getByRole("button", {
    name: "Preview German (Germany)",
    exact: true,
  });
  await preview.click();
  await active(page).locator("iframe.preview-ready").waitFor();
  const original = await active(page)
    .locator("iframe")
    .evaluateHandle((n) => n);
  await preview.click();
  await preview.click();
  assert(
    await active(page)
      .locator("iframe")
      .evaluate((n, old) => n === old, original),
  );
  await page.getByRole("tab", { name: "Exported", exact: true }).click();
  await page.waitForFunction(
    () =>
      document.querySelector(".editor-view:not([hidden]) .canvas-surface")
        ?.dataset.previewSource === "exported",
  );
  await active(page).locator(".missing").waitFor();
  await page.getByRole("tab", { name: "Live", exact: true }).click();
  assert(
    await active(page)
      .locator("iframe")
      .evaluate((n, old) => n === old, original),
  );
  await page
    .getByRole("button", { name: "Continue translation", exact: true })
    .click();
  await page
    .getByRole("textbox", { name: "de-DE hello title", exact: true })
    .waitFor();
});

test("project dialogs use shadcn to discover, connect, rename and remove folders safely", async (t) => {
  const { page, root, library, store } = await setup(t);
  await page
    .getByRole("button", { name: "Add project", exact: true })
    .last()
    .click();
  let dialog = page.getByRole("dialog", {
    name: "Connect your projects",
    exact: true,
  });
  await dialog.getByRole("tab", { name: "Find projects", exact: true }).click();
  await dialog
    .getByRole("textbox", { name: "Search inside folder", exact: true })
    .fill(root + "/discover");
  await dialog
    .getByRole("button", { name: "Find projects", exact: true })
    .click();
  await dialog.getByText("2 projects found", { exact: true }).waitFor();
  await dialog
    .getByRole("button", { name: "Add 2 projects", exact: true })
    .click();
  await page.locator(".workspace-card").filter({ hasText: "Gamma" }).waitFor();
  assert.equal((await store.list()).length, 4);
  await page
    .getByRole("searchbox", { name: "Search projects", exact: true })
    .fill("Library");
  await page.getByRole("link", { name: "View images", exact: true }).click();
  await page.locator(".library-image img").evaluate((img) => img.decode());
  await page
    .getByRole("button", { name: "Manage Library", exact: true })
    .click();
  await page
    .getByRole("menuitem", { name: "Change folder or name", exact: true })
    .click();
  dialog = page.getByRole("dialog", { name: "Project settings", exact: true });
  await dialog
    .getByRole("textbox", { name: "Project name optional", exact: true })
    .fill("My screenshots");
  await dialog
    .getByRole("button", { name: "Save project", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "My screenshots", exact: true })
    .waitFor();
  await page
    .getByRole("button", { name: "Manage My screenshots", exact: true })
    .click();
  await page
    .getByRole("menuitem", { name: "Remove from workspace", exact: true })
    .click();
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: "Remove from workspace", exact: true })
    .click();
  await page.locator(".workspace-card").first().waitFor();
  assert.equal((await store.list()).length, 3);
  assert(fs.existsSync(library + "/screenshots/sample.png"));
});

test("shadcn callout dialog saves exact element metadata, follows localized bounds and supports manual crops", async (t) => {
  const { page, base, projects, roots } = await setup(t);
  await page.goto(base + projects[0].editorURL + "?view=cards");
  await page.getByRole("button", { name: "Pick callout", exact: true }).click();
  let dialog = page.getByRole("dialog", {
    name: "Pick a callout",
    exact: true,
  });
  await dialog.getByRole("tab", { name: "Element", exact: true }).waitFor();
  await page.waitForFunction(
    () =>
      document.querySelector("[role=tab][data-state=active]") &&
      [...document.querySelectorAll("[role=tab]")].some(
        (n) => n.textContent === "Element" && n.dataset.state === "active",
      ),
  );
  await dialog
    .getByRole("combobox", { name: "Find accessibility element", exact: true })
    .fill("Suggested");
  await dialog.getByRole("option").first().click();
  await dialog
    .getByRole("button", { name: "Save as this card's callout", exact: true })
    .click();
  await dialog.waitFor({ state: "hidden" });
  let callout = JSON.parse(
    fs.readFileSync(roots[0] + "/store-listing/gallery.json"),
  ).phone[0].callouts[0];
  assert.deepEqual(
    [callout.x, callout.y, callout.w, callout.h],
    [20, 100, 160, 60],
  );
  assert.equal(callout.targetId, "app:id/action");
  assert.equal(callout.targetMode, "element");
  await choice(page, "Language", "German (Germany)");
  await page.waitForFunction(() =>
    document
      .querySelector(".editor-view:not([hidden]) iframe")
      ?.src.includes("locale=de-DE"),
  );
  await active(page).locator("iframe.preview-ready").waitFor();
  const report = await active(page)
    .locator("iframe")
    .evaluate((f) =>
      JSON.parse(f.contentDocument.documentElement.dataset.report),
    );
  assert.equal(report.fixedCallouts.length, 0);
  assert.equal(report.movedCallouts.length, 1);
  await page.getByRole("button", { name: "Pick callout", exact: true }).click();
  dialog = page.getByRole("dialog", { name: "Pick a callout", exact: true });
  await dialog.locator("img").evaluate((img) => img.decode());
  await dialog.getByRole("tab", { name: "Draw box", exact: true }).click();
  const rect = await dialog.locator("img").boundingBox();
  await page.mouse.move(rect.x + rect.width * 0.1, rect.y + rect.height * 0.3);
  await page.mouse.down();
  await page.mouse.move(rect.x + rect.width * 0.5, rect.y + rect.height * 0.4, {
    steps: 6,
  });
  await page.mouse.up();
  await dialog
    .getByRole("button", { name: "Save as this card's callout", exact: true })
    .click();
  await dialog.waitFor({ state: "hidden" });
  callout = JSON.parse(
    fs.readFileSync(roots[0] + "/store-listing/gallery.json"),
  ).phone[0].callouts[0];
  assert(callout.w > 0 && callout.h > 0);
  assert.equal(callout.targetMode, undefined);
});

test("mobile Sidebar Sheet, keyboard search, saved preview and dark mode work without overflow", async (t) => {
  const { page, base, projects } = await setup(t);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(base + projects[0].editorURL + "?view=copy");
  await page.getByRole("textbox").first().waitFor();
  await page.locator("[data-slot=sidebar-trigger]").click();
  const sheet = page.getByRole("dialog", { name: "Sidebar", exact: true });
  await sheet.getByRole("link", { name: "Alpha", exact: true }).click();
  await sheet.waitFor({ state: "hidden" });
  await page.getByRole("heading", { name: "Alpha", exact: true }).waitFor();
  await page.locator("[data-slot=sidebar-trigger]").click();
  await page
    .getByRole("dialog", { name: "Sidebar", exact: true })
    .getByRole("button", {
      name: "Local workspace 2 connected projects",
      exact: true,
    })
    .click();
  await page.getByRole("menuitemradio", { name: "Dark", exact: true }).click();
  assert(
    await page.locator("html").evaluate((n) => n.classList.contains("dark")),
  );
  await page.getByRole("menu").waitFor({ state: "hidden" });
  await page
    .getByRole("button", { name: "Close project sidebar", exact: true })
    .click();
  await page
    .getByRole("dialog", { name: "Sidebar", exact: true })
    .waitFor({ state: "hidden" });
  await page.locator("[data-slot=sidebar-trigger]").click();
  await page
    .getByRole("dialog", { name: "Sidebar", exact: true })
    .getByRole("link", { name: "Copy", exact: true })
    .click();
  await page.getByRole("textbox").first().waitFor();
  await page.getByRole("button", { name: "Preview", exact: true }).click();
  const preview = page.getByRole("dialog", {
    name: "Saved preview",
    exact: true,
  });
  await preview.locator("iframe.preview-ready").waitFor();
  await page.keyboard.press("Escape");
  await preview.waitFor({ state: "hidden" });
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth,
    ),
    0,
  );
  await page.keyboard.press("Control+k");
  await page
    .getByRole("combobox")
    .filter({ has: page.locator("input") })
    .count();
  await page.getByPlaceholder("Search projects and actions…").fill("Beta");
  await page.getByRole("option", { name: /Beta/ }).click();
  await page.getByRole("heading", { name: "Beta", exact: true }).waitFor();
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth,
    ),
    0,
  );
});

test("the compiled UI opens a shadcn launch screen directly from disk", async (t) => {
  const browser = await chromium.launch();
  t.after(() => browser.close());
  const page = await browser.newPage();
  await page.goto(pathToFileURL(path.join(__dirname, "index.html")).href);
  await page
    .getByText("Open your screenshot workspace", { exact: true })
    .waitFor();
  assert((await page.locator('[data-slot="card"]').count()) > 0);
  assert(
    await page
      .getByRole("button", { name: "Copy command", exact: true })
      .isVisible(),
  );
});

function addViewerGallery({ roots, write }, count = 3) {
  const listing = roots[0] + "/store-listing",
    gallery = JSON.parse(fs.readFileSync(listing + "/gallery.json"));
  for (let i = 1; i < count; i++) {
    const feature = `detail-${i + 1}`;
    gallery.phone.push({ feature, file: `phone/0${i + 1}-${feature}.png` });
    write(`${listing}/en-US/phone/0${i + 1}-${feature}.png`, png(400, 800));
  }
  write(listing + "/gallery.json", gallery);
  for (const locale of ["en-US", "de-DE"]) {
    const file = `${listing}/${locale}/copy.json`,
      copy = JSON.parse(fs.readFileSync(file));
    for (let i = 1; i < count; i++)
      copy[`detail-${i + 1}`] = {
        title: locale === "en-US" ? `Detail ${i + 1}` : "",
        body: "",
      };
    write(file, copy);
  }
}
const viewer = (page) => page.locator(".image-viewer");
async function waitViewer(page, type) {
  await viewer(page).waitFor();
  if (type)
    assert.equal(await viewer(page).getAttribute("data-media-type"), type);
  await viewer(page).locator(".image-viewer-stage[aria-busy=false]").waitFor();
}
async function viewerIndex(page, index) {
  await page.waitForFunction(
    (index) =>
      document.querySelector(".image-viewer")?.dataset.imageIndex ===
      String(index),
    index,
  );
}

test("overview images open a shadcn gallery with wrapping navigation, zoom, panning and restored focus", async (t) => {
  const f = await setup(t),
    { page } = f;
  addViewerGallery(f);
  await page.reload();
  await page
    .getByRole("button", { name: "Enlarge Alpha screenshot 1", exact: true })
    .waitFor();
  let documents = 0;
  page.on("request", (r) => {
    if (r.isNavigationRequest() && r.frame() === page.mainFrame()) documents++;
  });
  const trigger = page.getByRole("button", {
    name: "Enlarge Alpha screenshot 1",
    exact: true,
  });
  await trigger.click();
  await waitViewer(page, "image");
  assert.equal(await page.context().pages().length, 1);
  assert.equal(await viewer(page).getAttribute("data-image-index"), "0");
  await page.keyboard.press("ArrowRight");
  await viewerIndex(page, 1);
  await waitViewer(page);
  await viewer(page)
    .getByRole("button", { name: /Show screenshot 3:/ })
    .click();
  await viewerIndex(page, 2);
  await waitViewer(page);
  await page.keyboard.press("ArrowRight");
  await viewerIndex(page, 0);
  await waitViewer(page);
  await viewer(page)
    .getByRole("button", { name: /Show screenshot 1:/ })
    .click();
  assert.equal(
    await viewer(page).locator(".image-viewer-stage").getAttribute("aria-busy"),
    "false",
    "Re-selecting the current image must not leave a loader stuck",
  );
  const before = await viewer(page)
    .getByRole("status", { name: "Image zoom", exact: true })
    .textContent();
  await viewer(page)
    .getByRole("button", { name: "Actual image size", exact: true })
    .click();
  await page.waitForFunction(
    () =>
      document.querySelector('.image-viewer [aria-label="Image zoom"]')
        .textContent === "100%",
  );
  assert.notEqual(before, "100%");
  const stage = viewer(page).locator(".image-viewer-stage"),
    bounds = await stage.boundingBox(),
    top = await stage.evaluate((n) => n.scrollTop);
  await page.mouse.move(
    bounds.x + bounds.width / 2,
    bounds.y + bounds.height / 2,
  );
  await page.mouse.down();
  await page.mouse.move(
    bounds.x + bounds.width / 2,
    bounds.y + bounds.height / 2 + 65,
    { steps: 5 },
  );
  await page.mouse.up();
  assert.notEqual(await stage.evaluate((n) => n.scrollTop), top);
  await viewer(page)
    .getByRole("button", { name: "Fit image", exact: true })
    .click();
  assert.equal(
    await viewer(page)
      .getByRole("status", { name: "Image zoom", exact: true })
      .textContent(),
    before,
  );
  assert(
    await viewer(page)
      .getByRole("link", { name: "Download image", exact: true })
      .isVisible(),
  );
  await page.keyboard.press("Escape");
  await viewer(page).waitFor({ state: "hidden" });
  await page.waitForFunction(
    () =>
      document.activeElement?.getAttribute("aria-label") ===
      "Enlarge Alpha screenshot 1",
  );
  assert.equal(documents, 0);
});

test("live and exported gallery cards use the same viewer without reloading their original previews", async (t) => {
  const f = await setup(t),
    { page, base, projects } = f;
  addViewerGallery(f);
  await page.goto(base + projects[0].editorURL + "?view=cards");
  await active(page).locator("iframe.preview-ready").first().waitFor();
  const original = await active(page)
    .locator("iframe")
    .first()
    .evaluateHandle((node) => node);
  await active(page)
    .getByRole("button", { name: "Enlarge Hello screenshot", exact: true })
    .click();
  await waitViewer(page, "live");
  assert(
    (await viewer(page).locator("iframe").getAttribute("src")).includes(
      "slide=0",
    ),
  );
  await viewer(page)
    .getByRole("button", { name: "Next screenshot", exact: true })
    .click();
  await viewerIndex(page, 1);
  await waitViewer(page);
  assert(
    (await viewer(page).locator("iframe").getAttribute("src")).includes(
      "slide=1",
    ),
  );
  await viewer(page)
    .getByRole("button", { name: "Close image viewer", exact: true })
    .click();
  await viewer(page).waitFor({ state: "hidden" });
  assert(
    await active(page)
      .locator("iframe")
      .first()
      .evaluate((node, old) => node === old, original),
  );
  await page.getByRole("tab", { name: "Exported", exact: true }).click();
  await page.waitForFunction(
    () =>
      document.querySelector(".editor-view:not([hidden]) .canvas-surface")
        ?.dataset.previewSource === "exported",
  );
  await active(page)
    .getByRole("button", { name: "Enlarge Hello screenshot", exact: true })
    .click();
  await waitViewer(page, "image");
  await page.keyboard.press("Escape");
  await viewer(page).waitFor({ state: "hidden" });
  await page.getByRole("tab", { name: "Live", exact: true }).click();
  await choice(page, "Language", "German (Germany)");
  await page.waitForFunction(() =>
    document
      .querySelector(".editor-view:not([hidden]) iframe")
      ?.src.includes("locale=de-DE"),
  );
  await active(page)
    .getByRole("button", { name: "Enlarge Hello screenshot", exact: true })
    .click();
  await waitViewer(page, "live");
  assert(
    (await viewer(page).locator("iframe").getAttribute("src")).includes(
      "locale=de-DE",
    ),
  );
  await viewer(page).getByText("Untranslated copy", { exact: true }).waitFor();
});

test("library image dialogs recover from missing files and support touch swipe on a phone", async (t) => {
  const f = await setup(t),
    { page, store, library, write, base } = f;
  write(library + "/screenshots/second.png", png(160, 80));
  write(library + "/screenshots/broken.png", "invalid image");
  const [project] = await store.add([library]);
  await page.setViewportSize({ width: 320, height: 844 });
  await page.goto(base + `/studio/?project=${project.id}`);
  await page
    .getByRole("button", {
      name: "Enlarge screenshots/broken.png",
      exact: true,
    })
    .click();
  await viewer(page).getByRole("alert").waitFor();
  write(library + "/screenshots/broken.png", png(80, 160));
  await viewer(page)
    .getByRole("button", { name: "Try again", exact: true })
    .click();
  await waitViewer(page, "image");
  const before = Number(await viewer(page).getAttribute("data-image-index"));
  const stage = await viewer(page).locator(".image-viewer-stage").boundingBox(),
    cdp = await page.context().newCDPSession(page);
  await cdp.send("Emulation.setTouchEmulationEnabled", { enabled: true });
  const x = stage.x + stage.width * 0.75,
    y = stage.y + stage.height * 0.5;
  await cdp.send("Input.dispatchTouchEvent", {
    type: "touchStart",
    touchPoints: [{ x, y }],
  });
  await cdp.send("Input.dispatchTouchEvent", {
    type: "touchMove",
    touchPoints: [{ x: x - stage.width * 0.5, y }],
  });
  await cdp.send("Input.dispatchTouchEvent", {
    type: "touchEnd",
    touchPoints: [],
  });
  await viewerIndex(page, (before + 1) % 3);
  await waitViewer(page);
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth,
    ),
    0,
  );
  const dialogBox = await viewer(page).boundingBox();
  assert(dialogBox.x >= 0 && dialogBox.x + dialogBox.width <= 320);
  await viewer(page)
    .getByRole("button", { name: "Close image viewer", exact: true })
    .click();
  await viewer(page).waitFor({ state: "hidden" });
});

test("the copy preview shares the gallery dialog and never saves unsaved words automatically", async (t) => {
  const f = await setup(t),
    { page, base, projects, roots } = f;
  addViewerGallery(f);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(base + projects[0].editorURL + "?view=copy");
  const area = page.getByRole("textbox", {
    name: "en-US hello title",
    exact: true,
  });
  await area.fill("Draft stays in the editor");
  await page.getByRole("button", { name: "Preview", exact: true }).click();
  await waitViewer(page, "live");
  await viewer(page)
    .getByText(
      "This preview uses the saved copy. Save your changes to include the latest words.",
      { exact: true },
    )
    .waitFor();
  await viewer(page)
    .getByRole("button", { name: "Next screenshot", exact: true })
    .click();
  await viewerIndex(page, 1);
  await waitViewer(page);
  await page.keyboard.press("Escape");
  await viewer(page).waitFor({ state: "hidden" });
  assert.equal(await area.inputValue(), "Draft stays in the editor");
  assert.equal(
    JSON.parse(fs.readFileSync(roots[0] + "/store-listing/en-US/copy.json"))
      .hello.title,
    "Alpha",
  );
  await page.waitForFunction(
    () => document.activeElement?.textContent.trim() === "Preview",
  );
  assert(
    await page
      .getByRole("button", { name: "Preview", exact: true })
      .evaluate((node) => node === document.activeElement),
  );
});

test("a long gallery stays fitted and keeps controls visible on mobile and after resize", async (t) => {
  const fixture = await setup(t),
    { page, base, projects } = fixture;
  addViewerGallery(fixture, 8);
  await page.goto(base + projects[0].editorURL + "?view=cards");
  await active(page)
    .getByRole("button", { name: "Enlarge Hello screenshot", exact: true })
    .click();
  await waitViewer(page, "live");
  for (const width of [390, 320, 900]) {
    await page.setViewportSize({ width, height: 844 });
    await page.waitForFunction(() => {
      const dialog = document.querySelector(".image-viewer"),
        button = dialog?.querySelector('[aria-label="Close image viewer"]'),
        stage = dialog?.querySelector(".image-viewer-stage");
      if (!dialog || !button || !stage) return false;
      const b = button.getBoundingClientRect(),
        r = dialog.getBoundingClientRect(),
        s = stage.getBoundingClientRect();
      return (
        r.x >= 0 &&
        r.right <= innerWidth &&
        r.y >= 0 &&
        r.bottom <= innerHeight &&
        b.x >= 0 &&
        b.right <= innerWidth &&
        b.y >= 0 &&
        b.bottom <= innerHeight &&
        s.width <= innerWidth
      );
    });
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth - innerWidth,
      ),
      0,
    );
  }
  await page.keyboard.press("End");
  await viewerIndex(page, 7);
  await waitViewer(page);
  const selected = viewer(page).getByRole("button", {
      name: /Show screenshot 8:/,
    }),
    strip = viewer(page).locator('[data-slot="scroll-area-viewport"]');
  await page.waitForFunction(() => {
    const selected = document.querySelector(
        '.image-viewer [aria-current="true"]',
      ),
      strip = document.querySelector(
        '.image-viewer [data-slot="scroll-area-viewport"]',
      );
    if (!selected || !strip) return false;
    const a = selected.getBoundingClientRect(),
      b = strip.getBoundingClientRect();
    return a.x >= b.x - 1 && a.right <= b.right + 1;
  });
});

test("project filters use connected shadcn tab panels and fit narrow screens", async (t) => {
  const { page, store, library } = await setup(t);
  await store.add([library]);
  await page.reload();
  const tabs = page.getByRole("tablist", { name: "Project filters" });
  const all = tabs.getByRole("tab", { name: "All projects", exact: true });
  const inProgress = tabs.getByRole("tab", {
    name: "In progress",
    exact: true,
  });
  const libraries = tabs.getByRole("tab", {
    name: "Libraries & setup",
    exact: true,
  });
  await all.waitFor();
  await page.waitForFunction(
    () => document.querySelectorAll(".workspace-card").length === 3,
  );
  await all.focus();
  await page.keyboard.press("ArrowRight");
  await page.waitForFunction(() =>
    document
      .querySelector('[data-slot="tabs-trigger"][data-state="active"]')
      ?.textContent.includes("In progress"),
  );
  assert.equal(await inProgress.getAttribute("aria-selected"), "true");
  assert.equal(await page.locator(".workspace-card").count(), 2);
  await libraries.click();
  const panel = page.getByRole("tabpanel");
  assert.equal(
    await panel.getAttribute("id"),
    await libraries.getAttribute("aria-controls"),
  );
  assert.equal(
    await panel.getAttribute("aria-labelledby"),
    await libraries.getAttribute("id"),
  );
  assert.equal(await panel.locator(".workspace-card").count(), 1);
  await page
    .getByRole("searchbox", { name: "Search projects" })
    .fill("does-not-exist");
  await page.getByText("No matching projects", { exact: true }).waitFor();
  await page
    .getByRole("button", { name: "Clear filters", exact: true })
    .click();
  assert.equal(await all.getAttribute("aria-selected"), "true");
  assert.equal(await panel.locator(".workspace-card").count(), 3);
  for (const width of [320, 390, 798, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.waitForFunction(
      () => document.documentElement.scrollWidth <= innerWidth,
    );
    const geometry = await tabs.evaluate((el) => ({
      width: el.clientWidth,
      scrollWidth: el.scrollWidth,
      height: el.clientHeight,
      scrollHeight: el.scrollHeight,
      clipped: [...el.children].some((child) => {
        const r = child.getBoundingClientRect(),
          box = el.getBoundingClientRect();
        return (
          r.left < box.left ||
          r.right > box.right + 1 ||
          r.bottom > box.bottom + 1
        );
      }),
    }));
    assert(
      geometry.scrollWidth <= geometry.width + 1,
      `horizontal tab overflow at ${width}`,
    );
    assert(
      geometry.scrollHeight <= geometry.height + 1,
      `vertical tab overflow at ${width}`,
    );
    assert(!geometry.clipped, `clipped tab at ${width}`);
    await page.screenshot({
      path: `/tmp/studio-refined-${width}.png`,
      fullPage: width === 1440,
    });
  }
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page
    .locator(".sidebar")
    .getByRole("link", { name: "Alpha", exact: true })
    .click();
  await page.getByRole("tablist", { name: "Project sections" }).waitFor();
  await page.getByRole("link", { name: "Open studio", exact: true }).click();
  await active(page).locator("iframe.preview-ready").waitFor();
  await view(page, "Languages");
  const languageTabs = page.getByRole("tablist", { name: "Language filters" });
  await languageTabs
    .getByRole("tab", { name: "Needs copy", exact: true })
    .click();
  assert.equal(await active(page).locator(".language-row:visible").count(), 1);
  for (const width of [320, 390]) {
    await page.setViewportSize({ width, height: 900 });
    const overflow = await languageTabs.evaluate((el) => ({
      x: el.scrollWidth - el.clientWidth,
      y: el.scrollHeight - el.clientHeight,
    }));
    assert(
      overflow.x <= 1 && overflow.y <= 1,
      `language tabs overflow at ${width}: ${JSON.stringify(overflow)}`,
    );
  }
});

test("project sections keep history, expose language actions on phones, and link to the right gallery", async (t) => {
  const { page, projects, base } = await setup(t);
  await page.goto(base + projects[0].overviewURL);
  const sections = page.getByRole("tablist", { name: "Project sections" });
  await sections.waitFor();
  await page.evaluate(() => {
    window.projectDocument = document;
  });
  const galleries = sections.getByRole("tab", { name: /Galleries/ });
  const languages = sections.getByRole("tab", { name: /Languages/ });
  await galleries.click();
  await page
    .getByRole("heading", { name: "Choose a gallery", exact: true })
    .waitFor();
  assert.equal(new URL(page.url()).searchParams.get("section"), "galleries");
  const panel = page.getByRole("tabpanel");
  const links = panel.getByRole("link", { name: "Open gallery", exact: true });
  assert.equal(await links.count(), 2);
  assert((await links.nth(1).getAttribute("href")).includes("kind=tablet7"));
  await languages.click();
  await page
    .getByRole("searchbox", { name: "Search project languages" })
    .fill("German");
  assert.equal(await panel.getByRole("row").count(), 2);
  await choice(page, "Language asset status", "Assets complete");
  await page.getByText("No matching languages", { exact: true }).waitFor();
  await page
    .getByRole("button", { name: "Clear filters", exact: true })
    .click();
  assert.equal(await panel.getByRole("row").count(), 3);
  await page.goBack();
  await page
    .getByRole("heading", { name: "Choose a gallery", exact: true })
    .waitFor();
  await page.goForward();
  await page
    .getByRole("heading", { name: "Language coverage", exact: true })
    .waitFor();
  assert(await page.evaluate(() => document === window.projectDocument));
  await page.reload();
  await page
    .getByRole("heading", { name: "Language coverage", exact: true })
    .waitFor();
  assert.equal(await languages.getAttribute("aria-selected"), "true");
  for (const width of [320, 390, 798]) {
    await page.setViewportSize({ width, height: 900 });
    await page.waitForFunction(
      () => document.documentElement.scrollWidth <= innerWidth,
    );
    const cards = panel.getByRole("article");
    assert.equal(await cards.count(), 2);
    assert(await cards.first().isVisible());
    const action = cards.getByRole("link", { name: "Edit copy", exact: true });
    const within = await action.evaluate((el) => {
      const r = el.getBoundingClientRect();
      return r.left >= 0 && r.right <= innerWidth;
    });
    assert(within, `copy action clipped at ${width}`);
    const fits = await sections.evaluate(
      (el) =>
        el.scrollWidth <= el.clientWidth + 1 &&
        el.scrollHeight <= el.clientHeight + 1,
    );
    assert(fits, `project tabs clipped at ${width}`);
  }
  await panel
    .getByRole("article", { name: "German (Germany) asset set", exact: true })
    .getByRole("link", { name: "Edit copy", exact: true })
    .click();
  await page
    .getByRole("textbox", { name: "de-DE hello title", exact: true })
    .waitFor();
  assert.equal(new URL(page.url()).searchParams.get("locale"), "de-DE");
  const reference = page
    .locator(".copy-field:visible")
    .first()
    .locator(".copy-reference");
  const area = page.getByRole("textbox", {
    name: "de-DE hello title",
    exact: true,
  });
  const a = await reference.boundingBox(),
    b = await area.boundingBox();
  assert(
    b.y >= a.y + a.height,
    "reference should stack above the translation on a phone",
  );
});

test("the narrow gallery select switches devices and reuses the loaded gallery", async (t) => {
  const { page, projects, base } = await setup(t);
  await page.setViewportSize({ width: 798, height: 1000 });
  await page.goto(base + projects[0].editorURL + "?view=cards");
  await active(page).locator("iframe.preview-ready").waitFor();
  const original = await active(page)
    .locator("iframe")
    .evaluateHandle((el) => el);
  assert.equal(
    await page.getByRole("tablist", { name: "Device galleries" }).isVisible(),
    false,
  );
  await choice(page, "Device gallery", "Tablet · 1 cards");
  await page.waitForFunction(
    () => new URL(location.href).searchParams.get("kind") === "tablet7",
  );
  await active(page).locator("iframe.preview-ready").waitFor();
  await choice(page, "Device gallery", "Phone · 1 cards");
  await page.waitForFunction(
    () =>
      new URL(location.href).searchParams.get("kind") === "phone" &&
      new URL(
        document.querySelector(".editor-view:not([hidden]) iframe")?.src,
      ).searchParams.get("kind") === "phone",
  );
  assert(
    await active(page)
      .locator("iframe")
      .evaluate((el, old) => el === old, original),
  );
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page
    .getByRole("tablist", { name: "Device galleries" })
    .waitFor({ state: "visible" });
  assert(
    await page.getByRole("tablist", { name: "Device galleries" }).isVisible(),
  );
  assert.equal(
    await page
      .getByRole("combobox", { name: "Device gallery", exact: true })
      .isVisible(),
    false,
  );
});

test('saving a callout preserves other callouts', async t => {
  const {page, base, projects, roots, write} = await setup(t);
  const file = roots[0] + '/store-listing/gallery.json';
  const gallery = JSON.parse(fs.readFileSync(file));
  const sibling = {x:30, y:300, w:100, h:60};
  gallery.phone[0].callouts = [{x:20,y:100,w:160,h:60}, sibling];
  write(file, gallery);
  await page.goto(base + projects[0].editorURL + '?view=cards');
  await page.getByRole('button', {name:'Pick callout',exact:true}).click();
  const dialog = page.getByRole('dialog', {name:'Pick a callout',exact:true});
  await dialog.getByRole('combobox', {name:'Find accessibility element',exact:true}).fill('Suggested');
  await dialog.getByRole('option').first().click();
  await dialog.getByRole('button', {name:"Save as this card's callout",exact:true}).click();
  await dialog.waitFor({state:'hidden'});
  assert.deepEqual(JSON.parse(fs.readFileSync(file)).phone[0].callouts[1], sibling);
});
