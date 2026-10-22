package com.bernaferrari.changedetection.browser

/** Scripts shared by every platform's capture so pages are cleaned the same way everywhere. */
object PageCleanup {
    /** Record only a cleanup script's synchronous mutations when capturing the interactive browser. */
    internal fun reversible(script: String): String = """
        (function(){
            var observer=new MutationObserver(function(){});
            observer.observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['style'],attributeOldValue:true,characterData:true,characterDataOldValue:true,childList:true});
            try { return $script; }
            finally {
                var changes=window.__cdCaptureUndo||(window.__cdCaptureUndo=[]);
                observer.takeRecords().forEach(function(change){changes.push(change);});
                observer.disconnect();
            }
        })()
    """.trimIndent()

    /** Restore DOM nodes in place, preserving JavaScript state, event listeners and the login session. */
    internal val RESTORE_CAPTURE = """
        (function(){
            var changes=window.__cdCaptureUndo||[];
            delete window.__cdCaptureUndo;
            for(var i=changes.length-1;i>=0;i--){
                var r=changes[i];
                if(r.type==='attributes'){
                    if(r.oldValue===null)r.target.removeAttribute(r.attributeName);
                    else r.target.setAttribute(r.attributeName,r.oldValue);
                }else if(r.type==='characterData')r.target.nodeValue=r.oldValue;
                else if(r.type==='childList'){
                    r.addedNodes.forEach(function(n){if(n.parentNode===r.target)r.target.removeChild(n);});
                    r.removedNodes.forEach(function(n){r.target.insertBefore(n,r.nextSibling&&r.nextSibling.parentNode===r.target?r.nextSibling:null);});
                }
            }
            return 0;
        })()
    """.trimIndent()

    /** Cookie / consent / LGPD overlays are noise in every capture; hide floating ones that say so. */
    const val HIDE_CONSENT = "(function(){var k=/cookie|consent|gdpr|lgpd|privacidade|privacy|tracking/i;document.querySelectorAll('body *').forEach(function(e){var p=getComputedStyle(e).position;if((p==='fixed'||p==='sticky')&&e.offsetHeight<window.innerHeight*0.9&&k.test((e.id||'')+' '+(typeof e.className==='string'?e.className:'')+' '+(e.innerText||'').slice(0,400)))e.style.setProperty('display','none','important');});return 0;})()"

    /**
     * Ads change on every load and would make every check a "change". They are hidden but keep their space,
     * so the rest of the page does not move. Same idea as a content blocker's cosmetic filters.
     */
    const val HIDE_ADS = "(function(){var q=['iframe[src*=\"doubleclick\"]','iframe[src*=\"googlesyndication\"]','iframe[id^=\"google_ads\"]','ins.adsbygoogle','[id^=\"div-gpt-ad\"]','[id*=\"google_ads\"]','[class*=\"advert\" i]','[id*=\"advert\" i]','[class~=\"ad\"]','[class*=\"-ad-\"]','[class*=\"ad-slot\"]','[class*=\"ad-banner\"]','[class*=\"banner-ad\"]','[data-ad]','[data-ad-slot]','[aria-label=\"Advertisement\" i]','[class*=\"taboola\" i]','[id*=\"taboola\" i]','[class*=\"outbrain\" i]','[class*=\"sponsor\" i]'];var s=document.createElement('style');s.textContent=q.join(',')+'{visibility:hidden !important;opacity:0 !important}';document.head.appendChild(s);document.querySelectorAll('body *').forEach(function(e){if(e.children.length>8)return;var t=(e.innerText||'').trim();if(/^(an[uú]ncio|publicidade|publicidad|advertisement|sponsored|pubblicit[aà]|patrocinado)$/i.test(t)){var p=e.parentElement;if(p&&p!==document.body)p.style.setProperty('visibility','hidden','important');}});return 0;})()"

    /** Floating bars pinned to the bottom of the screen (app-style tab bars, chat bubbles) would be stitched into the middle of the page. */
    const val HIDE_BOTTOM_FLOATING = "(function(){document.querySelectorAll('body *').forEach(function(e){var p=getComputedStyle(e).position;if(p!=='fixed'&&p!=='sticky')return;var r=e.getBoundingClientRect();if(r.top>window.innerHeight*0.5&&r.height<window.innerHeight*0.5)e.style.setProperty('visibility','hidden','important');});return 0;})()"

    /** After the first screen is captured, pinned headers are hidden so they are not repeated down the page. */
    const val HIDE_PINNED = "(function(){document.querySelectorAll('body *').forEach(function(e){var p=getComputedStyle(e).position;if(p==='fixed'||p==='sticky')e.style.setProperty('visibility','hidden','important');});return 0;})()"

    /** The page's own name: Open Graph title first, then the document title. */
    const val TITLE = "(function(){var m=document.querySelector('meta[property=\"og:title\"]');return (m&&m.content)||document.title||'';})()"

    /**
     * The page's best icon as an absolute URL: apple-touch-icon (a sharp 180px PNG) first, then the largest
     * raster `<link rel=icon>`, then /favicon.ico. SVG is skipped since not every platform decodes it.
     */
    const val ICON = "(function(){var best='',score=-1;document.querySelectorAll('link[rel][href]').forEach(function(l){var rel=l.rel.toLowerCase();if(rel.indexOf('icon')<0||rel.indexOf('mask')>=0)return;var h=l.href||'';if(/\\.svg(\\?|$)/i.test(h)||(l.type||'').indexOf('svg')>=0)return;var sz=parseInt((l.getAttribute('sizes')||'').split('x')[0])||(rel.indexOf('apple')>=0?180:16);var s=(rel.indexOf('apple')>=0?1000:0)+Math.min(sz,512);if(s>score){score=s;best=h;}});return best||(location.origin+'/favicon.ico');})()"

    /**
     * "pending,resources": images still loading, and how many network resources the page
     * has requested so far. A page is settled when nothing is pending and the resource count stops growing.
     */
    // A complete image with naturalWidth == 0 has failed; waiting cannot make it load.
    val LOAD_STATE = loadState()

    /** Only resources that can appear in this capture should hold up its preview. */
    internal fun loadState(top: Double? = null, bottom: Double? = null): String {
        val outside = if (top != null && bottom != null) {
            require(top.isFinite() && bottom.isFinite() && bottom > top)
            "if(r.bottom+window.scrollY<=$top||r.top+window.scrollY>=$bottom)return;"
        } else {
            ""
        }
        return "(function(){var p=0;document.querySelectorAll('img').forEach(function(i){var r=i.getBoundingClientRect();if(r.width<2||r.height<2)return;$outside if(!i.complete)p++;});var n=(performance.getEntriesByType&&performance.getEntriesByType('resource').length)||0;return p+','+n;})()"
    }

    /**
     * Relative timestamps ("Há 42 minutos", "2 hours ago", "hace 5 min") change on every check without the page
     * changing. They are replaced with a clock mark of similar width before capture, so they never show up as changes.
     */
    const val MASK_RELATIVE_TIMES = "(function(){var re=/\\b(?:(?:h[áa]|hace)\\s+(?:cerca\\s+de\\s+)?\\d+\\s*(?:segundos?|minutos?|mins?|horas?|h|dias?|días?|d)|\\d+\\s*(?:seconds?|secs?|minutes?|mins?|min|hours?|hrs?|h|days?|d|m)\\s+ago|\\d+\\s*(?:secondi|minuti|min|ore|ora|giorni|giorno)\\s+fa|just now|agora mesmo|ahora mismo)\\b/gi;var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT,null);var n,c=0;while((n=w.nextNode())){var t=n.nodeValue;if(t&&re.test(t)){re.lastIndex=0;n.nodeValue=t.replace(re,'\\u{1F552}');c++;}re.lastIndex=0;}return c;})()"

    /**
     * Ignored parts stay in the layout (nothing shifts) but are not painted. Opacity as well as visibility:
     * an ad inside an ignored part can set itself `visibility: visible`, but cannot undo its parent's opacity.
     */
    fun hide(selectors: String) = "(function(){var s=document.createElement('style');s.textContent=${js("$selectors{visibility:hidden !important;opacity:0 !important}")};document.head.appendChild(s);return 0;})()"

    /** "left,top,right,bottom" of the first match, in document CSS pixels, or "" when missing. */
    fun rectOf(selector: String) = "(function(){var e=document.querySelector(${js(selector)});if(!e)return '';var r=e.getBoundingClientRect();return [r.left+window.scrollX,r.top+window.scrollY,r.right+window.scrollX,r.bottom+window.scrollY].join(',');})()"

    fun js(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    /**
     * Waits until the page stops loading: no visible image still loading and no new network requests for about a
     * second. Gives up after [maxMs], so a page that never settles (live video, tickers) is still captured.
     */
    suspend fun awaitSettled(evaluate: suspend (String) -> String?, maxMs: Long = 8_000, pollMs: Long = 500, loadStateScript: String = LOAD_STATE) {
        var lastResources = -1
        var quietPolls = 0
        var waited = 0L
        while (waited < maxMs) {
            val parts = evaluate(loadStateScript)?.trim('"')?.split(',')?.mapNotNull { it.trim().toIntOrNull() }
            val pending = parts?.getOrNull(0) ?: 0
            val resources = parts?.getOrNull(1) ?: 0
            quietPolls = if (pending == 0 && resources == lastResources) quietPolls + 1 else 0
            if (quietPolls >= 2) return
            lastResources = resources
            kotlinx.coroutines.delay(pollMs)
            waited += pollMs
        }
    }
}
