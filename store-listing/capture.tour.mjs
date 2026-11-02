// ChangeDetection's store screens on the capture emulator. The app captures itself: StoreCaptureTest (an
// instrumentation test, built by `./gradlew :shared:assembleAndroidDeviceTest`) drives it on sample pages of
// natural-looking but fictional sites and writes full-display PNGs with their layout maps; this pulls them into
// <locale>/raw/<profile>/<theme>/. With the network off nothing can reach a real site.
// `npx screenshot-studio recapture` runs this for every language and device size, and exports.
// Alone: npx screenshot-studio tour [--profile phone] [--locale de-DE].
// Options (studio.json "capture" runs): theme ("light" or "dark", default both), palette (BLUE…), style (VIBRANT…).
import fs from 'node:fs';
import path from 'node:path';

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.bernaferrari.changedetection.library.test',
  install: '../shared/build/outputs/apk/androidTest/shared-androidTest.apk',
  uninstall: true,  // the test APK is a launchable app: never leave it in another app's app list
  tapMap: false,
  async run(d) {
    for (const theme of ['light', 'dark']) {
      if (d.options.theme && d.options.theme !== theme) continue;
      await d.night(theme === 'dark');
      const extra = (key, value) => value ? ['-e', key, String(value)] : [];
      const result = d.shell('am', 'instrument', '-w', '-e', 'class', 'com.bernaferrari.changedetection.store.StoreCaptureTest',
        '-e', 'store_locale', d.locale, '-e', 'store_profile', d.profile, '-e', 'store_theme', theme,
        ...extra('store_palette', d.options.palette), ...extra('store_style', d.options.style),
        `${d.app}/androidx.test.runner.AndroidJUnitRunner`);
      if (!result.includes('OK (1 test)')) throw new Error(`StoreCaptureTest failed (${d.locale}, ${d.profile}, ${theme}):\n${result}`);
      const target = path.join(d.raw, d.profile, theme);
      fs.mkdirSync(target, {recursive: true});
      for (const old of fs.readdirSync(target)) if (old.endsWith('.png')) fs.rmSync(path.join(target, old));
      d.adb('pull', `/sdcard/Android/data/${d.app}/files/store-capture/${d.profile}/${theme}/.`, target);
      const images = fs.readdirSync(target).filter(name => name.endsWith('.png')).sort();
      for (const name of images) {
        const png = fs.readFileSync(path.join(target, name));
        const width = png.readUInt32BE(16), height = png.readUInt32BE(20);
        if (width !== d.width || height !== d.height) throw new Error(`${name} is ${width}×${height}, not ${d.width}×${d.height}`);
        const layout = JSON.parse(fs.readFileSync(path.join(target, name.replace(/\.png$/, '.nodes.json')), 'utf8'));
        if (layout.width !== width || layout.height !== height || !layout.nodes?.length) throw new Error(`${name}: its layout map does not match`);
      }
      console.log(d.profile, theme, images.join(' '));
    }
  },
};
