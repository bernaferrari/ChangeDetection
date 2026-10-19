# README screenshots

These are captures of the real shared Compose app UI rendered on the Desktop test host at 1080 × 2340 pixels, density 2.625, light theme. They use local example pages and an isolated database. They are not Android device screenshots, live website captures, or generated UI mockups.

Regenerate the source captures:

```sh
./gradlew :shared:desktopTest --tests '*DesignReviewTest.readmeTour'
```

The test writes `build/desktop-review/v6/readme-{home,comparison,text,history,turbo}.png`. Copy these to this directory, naming `readme-text.png` as `text-diff.png`. The fixture and navigation sequence live in `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/review/DesignReviewTest.kt`.

`overview.png` places the complete home and comparison captures side by side, uniformly scaled to half size, on a plain background. No app content is retouched. To rebuild it with FFmpeg:

```sh
ffmpeg -y -i home.png -i comparison.png -filter_complex \
  '[0:v]scale=540:1170,pad=564:1218:12:24:color=0xf5f4fb[a];[1:v]scale=540:1170,pad=564:1218:12:24:color=0xf5f4fb[b];[a][b]hstack[out]' \
  -map '[out]' -frames:v 1 -compression_level 9 overview.png
```

Captured 19 October 2026 from the current development source. Published store builds may differ. Example addresses use the reserved `.example` domain; the tour does not contact them or start a real monitoring session.
