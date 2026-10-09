/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

/** Scripts shared by every platform's capture so pages are cleaned the same way everywhere. */
object PageCleanup {
    /** Cookie / consent / LGPD overlays are noise in every capture; hide floating ones that say so. */
    const val HIDE_CONSENT = "(function(){var k=/cookie|consent|gdpr|lgpd|privacidade|privacy|tracking/i;document.querySelectorAll('body *').forEach(function(e){var p=getComputedStyle(e).position;if((p==='fixed'||p==='sticky')&&e.offsetHeight<window.innerHeight*0.9&&k.test((e.id||'')+' '+(typeof e.className==='string'?e.className:'')+' '+(e.innerText||'').slice(0,400)))e.style.setProperty('display','none','important');});return 0;})()"

    /**
     * Ads change on every load and would make every check a "change". They are hidden but keep their space,
     * so the rest of the page does not move. Same idea as a content blocker's cosmetic filters.
     */
    const val HIDE_ADS = "(function(){var q=['iframe[src*=\"doubleclick\"]','iframe[src*=\"googlesyndication\"]','iframe[id^=\"google_ads\"]','ins.adsbygoogle','[id^=\"div-gpt-ad\"]','[id*=\"google_ads\"]','[class*=\"advert\" i]','[id*=\"advert\" i]','[class~=\"ad\"]','[class*=\"-ad-\"]','[class*=\"ad-slot\"]','[class*=\"ad-banner\"]','[class*=\"banner-ad\"]','[data-ad]','[data-ad-slot]','[aria-label=\"Advertisement\" i]','[class*=\"taboola\" i]','[id*=\"taboola\" i]','[class*=\"outbrain\" i]','[class*=\"sponsor\" i]'];var s=document.createElement('style');s.textContent=q.join(',')+'{visibility:hidden !important}';document.head.appendChild(s);document.querySelectorAll('body *').forEach(function(e){if(e.children.length>8)return;var t=(e.innerText||'').trim();if(/^(an[uú]ncio|publicidade|publicidad|advertisement|sponsored|pubblicit[aà]|patrocinado)$/i.test(t)){var p=e.parentElement;if(p&&p!==document.body)p.style.setProperty('visibility','hidden','important');}});return 0;})()"

    /** Floating bars pinned to the bottom of the screen (app-style tab bars, chat bubbles) would be stitched into the middle of the page. */
    const val HIDE_BOTTOM_FLOATING = "(function(){document.querySelectorAll('body *').forEach(function(e){var p=getComputedStyle(e).position;if(p!=='fixed'&&p!=='sticky')return;var r=e.getBoundingClientRect();if(r.top>window.innerHeight*0.5&&r.height<window.innerHeight*0.5)e.style.setProperty('visibility','hidden','important');});return 0;})()"

    /** After the first screen is captured, pinned headers are hidden so they are not repeated down the page. */
    const val HIDE_PINNED = "(function(){document.querySelectorAll('body *').forEach(function(e){var p=getComputedStyle(e).position;if(p==='fixed'||p==='sticky')e.style.setProperty('visibility','hidden','important');});return 0;})()"

    /** The page's own name: Open Graph title first, then the document title. */
    const val TITLE = "(function(){var m=document.querySelector('meta[property=\"og:title\"]');return (m&&m.content)||document.title||'';})()"

    /** Ignored parts stay in the layout (nothing shifts) but are not painted. */
    fun hide(selectors: String) = "(function(){var s=document.createElement('style');s.textContent=${js("$selectors{visibility:hidden !important}")};document.head.appendChild(s);return 0;})()"

    /** "left,top,right,bottom,documentHeight,documentWidth" of the first match, in CSS pixels, or "" when missing. */
    fun rectOf(selector: String) = "(function(){var e=document.querySelector(${js(selector)});if(!e)return '';var r=e.getBoundingClientRect();return [r.left,r.top+window.scrollY,r.right,r.bottom+window.scrollY,Math.max(document.body.scrollHeight,document.documentElement.scrollHeight),document.documentElement.clientWidth].join(',');})()"

    fun js(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""
}
