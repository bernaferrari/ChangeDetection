# Chinese font subsets

The bundled `shared/src/commonMain/composeResources/font/locale_zh_{regular,semibold,bold}.ttf` files derive from Noto Sans SC version 2.004, distributed by [Google Fonts](https://github.com/google/fonts/tree/a85815a42757630ce188fdad368c2dfc444d4773/ofl/notosanssc).

Source: `ofl/notosanssc/NotoSansSC[wght].ttf` at commit `a85815a42757630ce188fdad368c2dfc444d4773`.
Source SHA-256: `a3041811a78c361b1de50f953c805e0244951c21c5bd412f7232ef0d899af0da`.

The SIL Open Font License is retained in `shared/src/commonMain/composeResources/files/licenses/notosanssc-OFL.txt`, byte-identical to that source's `OFL.txt`. The subsets retain the existing ChangeDetection family/PostScript names and upstream attribution.

The October 2026 repair preserves all 634 previously supported Unicode code points and adds 照 (U+7167), 风 (U+98CE), 力 (U+529B), 鲜 (U+9C9C), 艳 (U+8273), and 柔 (U+67D4), covering the current Chinese resource strings in every bundled weight.

To reproduce with fontTools: subset the source variable font to the union of the bundled fonts' `getBestCmap()` keys and all non-whitespace characters in `composeResources/values-zh/*.xml` text; retain all layout features, name IDs, and name languages. Instantiate the `wght` axis at 400, 600, and 700 for regular, semibold, and bold respectively, and preserve each bundled font's name table. Verify that no previous code point is removed and every Chinese resource character is displayable by all three weights. Future translation changes require repeating this coverage check.
