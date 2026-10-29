const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { createWorkspaceStore } = require("./workspace-store.cjs");
const { serve } = require("./server.cjs");
function fixture(t) {
  const root = fs.realpathSync(
    fs.mkdtempSync(path.join(os.tmpdir(), "studio-workspace-")),
  );
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const write = (file, value) => {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(
      file,
      typeof value === "object" ? JSON.stringify(value) : value,
    );
  };
  const app = (name) => {
    const repo = path.join(root, name),
      listing = path.join(repo, "store-listing");
    write(path.join(listing, "studio.json"), {
      app: name,
      defaultLocale: "en-US",
      locales: ["en-US", "de-DE"],
    });
    write(path.join(listing, "profiles.json"), {
      phone: { width: 400, height: 800 },
      tablet7: { width: 800, height: 1200 },
    });
    write(path.join(listing, "gallery.json"), {
      phone: [{ feature: "home", raw: "phone/home.png" }],
      tablet7: { inherits: "phone", cards: [{}] },
    });
    write(path.join(listing, "en-US/copy.json"), {
      home: { title: "Hello", body: "World" },
    });
    write(path.join(listing, "de-DE/copy.json"), {
      home: { title: "Hallo", body: "" },
    });
    write(path.join(listing, "en-US/raw/phone/home.png"), "capture");
    write(path.join(listing, "en-US/raw/phone/home.nodes.json"), { nodes: [] });
    write(path.join(listing, "en-US/phone/01-home.jpg"), "export");
    write(path.join(listing, "en-US/tablet7/01-home.jpg"), "export");
    return repo;
  };
  return {
    root,
    write,
    app,
    store: createWorkspaceStore({
      registryFile: path.join(root, ".workspace/projects.json"),
    }),
  };
}
test("projects persist, deduplicate canonical folders, and removal preserves their files", async (t) => {
  const f = fixture(t),
    repo = f.app("First");
  const [project] = await f.store.add([path.join(repo, "store-listing")]);
  fs.symlinkSync(repo, path.join(f.root, "alias"));
  assert.equal(
    (await f.store.add([repo, path.join(f.root, "alias")]))[0].id,
    project.id,
  );
  assert.equal((await f.store.list()).length, 1);
  const reopened = createWorkspaceStore({ registryFile: f.store.registryFile });
  assert.equal(reopened.get(project.id).root, repo);
  await reopened.update(project.id, { root: repo, name: "My listing" });
  assert.equal((await reopened.list())[0].name, "My listing");
  await reopened.remove(project.id);
  assert.equal(
    fs.existsSync(path.join(repo, "store-listing/gallery.json")),
    true,
  );
  assert.equal((await reopened.list()).length, 0);
});
test("coverage resolves inherited galleries and counts only actual locale files", async (t) => {
  const f = fixture(t);
  await f.store.add([f.app("Coverage")]);
  const [project] = await f.store.list();
  assert.equal(project.cardCount, 2);
  assert.equal(project.completeLocales, 1);
  assert.deepEqual(
    project.locales.map((l) => [
      l.translated,
      l.captures,
      l.layouts,
      l.exports,
      l.next,
    ]),
    [
      [2, 1, 1, 2, "review"],
      [1, 0, 0, 0, "copy"],
    ],
  );
  assert.equal(project.totals.expectedCaptures, 2); // One unique capture in each language.
  assert.equal(project.totals.expectedExports, 4);
  assert.equal(project.previews.length, 2);
});
test("folder discovery skips build trees and reports connected projects and image libraries", async (t) => {
  const f = fixture(t),
    app = f.app("Found");
  await f.store.add([app]);
  f.app("node_modules/Hidden");
  f.write(path.join(f.root, "Library/screenshots/one.png"), "png");
  f.write(path.join(f.root, "Library/screenshots/two.webp"), "webp");
  const result = await f.store.discover(f.root);
  assert.equal(result.projects.length, 2);
  assert(result.projects.find((p) => p.name === "Found").registeredId);
  const [library] = await f.store.add([path.join(f.root, "Library")]);
  assert.equal(f.store.library(library.id).files.length, 2);
  assert.equal(
    (await f.store.list()).find((p) => p.id === library.id).kind,
    "library",
  );
  fs.rmSync(app, { recursive: true });
  assert.equal(
    (await f.store.list()).find((p) => p.root === app).kind,
    "unavailable",
  );
});
test("independent registry writers preserve both additions and reject duplicate reconnects", async (t) => {
  const f = fixture(t),
    other = createWorkspaceStore({ registryFile: f.store.registryFile });
  const a = f.app("One"),
    b = f.app("Two");
  await Promise.all([f.store.add([a]), other.add([b])]);
  const projects = await f.store.list();
  assert.equal(projects.length, 2);
  await assert.rejects(
    f.store.update(projects[0].id, { root: projects[1].root }),
    (e) => e.status === 409,
  );
  await assert.rejects(
    f.store.add(["/path/that/does/not/exist"]),
    /could not be opened/,
  );
});
test("mounted editors isolate writes, block path escapes, and keep shared UI read only", async (t) => {
  const f = fixture(t),
    a = f.app("A"),
    b = f.app("B");
  const [project] = await f.store.add([a, b]);
  const server = await serve(f.root, 0, {
    workspace: f.store,
    editable: true,
    selectFolder: async () => a,
  });
  t.after(() => new Promise((resolve) => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`,
    prefix = `/p/${project.id}`;
  const get = async (url) => fetch(base + url),
    put = async (url, value, headers = {}) =>
      fetch(base + url, {
        method: "PUT",
        headers,
        body: JSON.stringify(value),
      });
  assert.equal((await get("/studio/")).status, 200);
  assert.equal(
    (await get(prefix + "/store-listing/studio/assets/studio.js")).status,
    200,
  );
  assert.equal(
    (await put(prefix + "/store-listing/studio/assets/studio.js", {})).status,
    403,
  );
  const value = { home: { title: "Saved in A" } };
  assert.equal(
    (await put(prefix + "/store-listing/de-DE/copy.json", value)).status,
    204,
  );
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(a, "store-listing/de-DE/copy.json"))),
    value,
  );
  assert.equal(
    JSON.parse(fs.readFileSync(path.join(b, "store-listing/de-DE/copy.json")))
      .home.title,
    "Hallo",
  );
  assert.equal((await put(prefix + "/README.md", {})).status, 403);
  assert.equal(
    (
      await put(
        prefix + "/store-listing/de-DE/copy.json",
        {},
        { Origin: "https://elsewhere.test" },
      )
    ).status,
    403,
  );
  assert.equal(
    await new Promise((resolve) =>
      require("node:http").get(
        base + "/api/workspace",
        { headers: { Host: "elsewhere.test" } },
        (res) => {
          res.resume();
          resolve(res.statusCode);
        },
      ),
    ),
    403,
  );
  const outside = path.join(f.root, "secret");
  f.write(outside, "hidden");
  fs.symlinkSync(outside, path.join(a, "leak.txt"));
  assert.equal((await get(prefix + "/leak.txt")).status, 403);
  fs.mkdirSync(path.join(a, "linked-index"));
  fs.symlinkSync(outside, path.join(a, "linked-index/index.html"));
  assert.equal((await get(prefix + "/linked-index/")).status, 403);
  const picked = await fetch(base + "/api/workspace/pick-folder", {
    method: "POST",
  });
  assert.equal((await picked.json()).path, a);
  const listing = await (await get("/api/workspace")).json();
  assert.equal(listing.projects.length, 2);
  const standalone = await serve(a, 0);
  t.after(() => new Promise((resolve) => standalone.close(resolve)));
  assert.equal(
    (await fetch(`http://127.0.0.1:${standalone.address().port}/api/workspace`))
      .status,
    404,
  );
});
test("drafts survive project switches, isolate locales, and detect changed disk copy", async () => {
  const { createDraftStore } = await import("./editor-drafts.js"),
    values = new Map();
  const storage = {
    getItem: (k) => values.get(k),
    setItem: (k, v) => values.set(k, v),
    removeItem: (k) => values.delete(k),
  };
  const a = createDraftStore("/p/a/", storage),
    b = createDraftStore("/p/b/", storage),
    disk = { title: "Original" };
  const draft = a.restore("en-US", disk);
  draft.data = { title: "Draft" };
  a.persist("en-US", draft);
  assert.equal(a.restore("en-US", disk).data.title, "Draft");
  assert.equal(b.restore("en-US", disk).data.title, "Original");
  assert.equal(a.restore("de-DE", disk).data.title, "Original");
  const changed = a.restore("en-US", { title: "Changed on disk" });
  assert.equal(changed.conflict, true);
  a.persist("en-US", changed);
  assert.equal(a.restore("en-US", { title: "Changed on disk" }).conflict, true);
  changed.conflict = false;
  changed.saved = JSON.stringify(changed.data);
  a.persist("en-US", changed);
  assert.equal(values.size, 0);
});

test('copy writes reject stale bases without changing disk', async t => {
  const f = fixture(t), root = f.app('Conflict');
  const [project] = await f.store.add([root]);
  const server = await serve(f.root, 0, {workspace: f.store, editable: true});
  t.after(() => new Promise(resolve => server.close(resolve)));
  const file = path.join(root, 'store-listing/de-DE/copy.json');
  const original = JSON.stringify(JSON.parse(fs.readFileSync(file)));
  const external = {home: {title: 'External change'}};
  fs.writeFileSync(file, JSON.stringify(external));
  const response = await fetch(`http://127.0.0.1:${server.address().port}/p/${project.id}/store-listing/de-DE/copy.json`, {
    method: 'PUT', headers: {'X-Studio-Base': require('node:crypto').createHash('sha256').update(original).digest('hex')}, body: JSON.stringify({home: {title: 'Stale draft'}})
  });
  assert.equal(response.status, 409);
  assert.deepEqual(JSON.parse(fs.readFileSync(file)), external);
  const fresh = await fetch(`http://127.0.0.1:${server.address().port}/p/${project.id}/store-listing/de-DE/copy.json`, {
    method: 'PUT', headers: {'X-Studio-Base': require('node:crypto').createHash('sha256').update(JSON.stringify(external)).digest('hex')}, body: JSON.stringify({home: {title: 'Resolved'}})
  });
  assert.equal(fresh.status, 204);
});

test('coverage excludes reference-only galleries and includes separate callout captures', async t => {
  const f = fixture(t), root = f.app('Coverage'), listing = path.join(root, 'store-listing');
  f.write(path.join(listing, 'profiles.json'), {phone:{width:400,height:800},tablet7:{width:800,height:1200,defaultLocaleOnly:true}});
  f.write(path.join(listing, 'gallery.json'), {phone:[{feature:'home',raw:'phone/home.png',callouts:[{raw:'phone/notice.png'}]}],tablet7:{inherits:'phone',cards:[{}]}});
  await f.store.add([root]);
  const [project] = await f.store.list(), german = project.locales.find(l => l.locale === 'de-DE');
  assert.equal(german.expectedExports, 1);
  assert.equal(german.expectedCaptures, 2);
});
