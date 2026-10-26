const {test} = require('node:test');
const assert = require('node:assert/strict');
const load = () => import('./gallery.js');
const tree = nodes => ({width: 500, height: 800, nodes});
const reference = tree([{path: '0.1', id: 'app:id/action', cls: 'Button', b: [20, 40, 200, 100]}]);
const box = {x: 20, y: 40, w: 180, h: 60, target: '0.1', targetClass: 'Button', targetMode: 'element'};

test('element mode snaps to exact localized bounds, rather than saved pixel offsets', async () => {
  const {followCallout} = await load();
  const localized = tree([{path: '0.1', cls: 'Button', b: [30, 80, 260, 170]}]);
  assert.deepEqual(followCallout({...box, x: 24, w: 120}, reference, localized), {x: 30, y: 80, w: 230, h: 90, path: '0.1'});
});

test('a unique resource ID survives an accessibility path change', async () => {
  const {followCallout} = await load();
  const localized = tree([{path: '0.8', id: 'app:id/action', cls: 'Button', b: [50, 100, 300, 200]}]);
  assert.equal(followCallout({...box, targetId: 'app:id/action'}, reference, localized).path, '0.8');
});

test('missing and ambiguous resource IDs cannot fall through to a recycled path', async () => {
  const {followCallout} = await load();
  const pinned = {...box, targetId: 'app:id/action'};
  assert.equal(followCallout(pinned, reference, tree([{path: '0.1', id: 'different', cls: 'Button', b: [20, 40, 200, 100]}])), null);
  assert.equal(followCallout(pinned, reference, tree([reference.nodes[0], {...reference.nodes[0], path: '0.2'}])), null);
});

test('missing reference targets, incompatible classes and invalid bounds are rejected', async () => {
  const {followCallout} = await load();
  assert.equal(followCallout({...box, target: 'missing'}, reference, reference), null);
  assert.equal(followCallout(box, reference, tree([{path: '0.1', cls: 'TextView', b: [20, 40, 200, 100]}])), null);
  assert.equal(followCallout(box, reference, tree([{path: '0.1', cls: 'Button', b: [20, 40, 520, 100]}])), null);
});

test('an explicit crop anchor uses the same reference node even when a child overlaps better', async () => {
  const {followCallout} = await load();
  const ref = tree([...reference.nodes, {path: '0.1.0', cls: 'TextView', b: [40, 50, 100, 90]}]);
  const localized = tree([{path: '0.1', cls: 'Button', b: [50, 60, 300, 160]}]);
  assert.deepEqual(followCallout({x: 40, y: 50, w: 60, h: 40, target: '0.1'}, ref, localized), {x: 70, y: 70, w: 130, h: 80, path: '0.1'});
});

test('legacy hand-drawn callouts still follow by overlap', async () => {
  const {followCallout} = await load();
  assert.deepEqual(followCallout({x: 20, y: 40, w: 180, h: 60}, reference, tree([{path: '0.1', cls: 'Button', b: [30, 80, 260, 170]}])), {x: 30, y: 80, w: 230, h: 90, path: '0.1'});
});


test('element mode requires an explicit target rather than guessing by overlap', async () => {
  const {followCallout} = await load();
  const {target, ...unpinned} = box;
  assert.equal(followCallout(unpinned, reference, reference), null);
});
