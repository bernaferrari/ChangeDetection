package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.error.NetworkError
import com.bernaferrari.changedetection.extensions.canonicalUrlKey
import kotlinx.serialization.json.*

/** No credentials or form values are read. A login page deliberately opened by the user is allowed. */
internal fun validateBrowserSession(requestedUrl: String, actualUrl: String, hasVisiblePasswordForm: Boolean) {
    if (hasVisiblePasswordForm && requestedUrl.canonicalUrlKey(true) != actualUrl.canonicalUrlKey(true)) {
        throw NetworkError(401, "Sign in again in the app browser before checking this page")
    }
}

internal fun validateBrowserSessionState(requestedUrl: String, value: String) {
    val raw = runCatching { Json.decodeFromString<String>(value) }.getOrElse { value }
    val state = Json.parseToJsonElement(raw).jsonArray
    require(state.size == 2) { "Could not read browser session state" }
    validateBrowserSession(requestedUrl, state[0].jsonPrimitive.content, state[1].jsonPrimitive.boolean)
}

internal object RenderedCaptureScripts {
    const val SESSION = """JSON.stringify([window.location.href,Array.from(document.querySelectorAll('input[type=password]')).some(function(e){var r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'})])"""
    const val HTML = "document.documentElement.outerHTML"
    const val MAX_HTML_CHARS = 12_000_000
    const val RESET_RESOURCES = "delete window.__cdCaptureFonts;0"

    /** Only captured images and fonts used by captured text gate readiness. Stylesheets can affect any area. */
    fun resources(selector: String, maxPageCssHeight: Double): String {
        require(maxPageCssHeight.isFinite() && maxPageCssHeight > 0)
        return """(function(){
            var root=${if (selector.isBlank()) "document.body" else "document.querySelector(${PageCleanup.js(selector)})"};
            if(!root)return '';
            var box=root.getBoundingClientRect(),selected=${selector.isNotBlank()},
                top=selected?box.top+scrollY:0,bottom=selected?box.bottom+scrollY:Math.min(Math.max(document.body.scrollHeight,document.documentElement.scrollHeight),$maxPageCssHeight),
                left=selected?box.left+scrollX:0,right=selected?box.right+scrollX:document.documentElement.clientWidth;
            function visible(e,r){
                var s=getComputedStyle(e);
                if(r.width<1||r.height<1||r.bottom+scrollY<=top||r.top+scrollY>=bottom||r.right+scrollX<=left||r.left+scrollX>=right||s.visibility==='hidden'||s.visibility==='collapse')return false;
                for(var p=e;p;p=p.parentElement){var a=getComputedStyle(p);if(a.display==='none'||parseFloat(a.opacity)===0||a.contentVisibility==='hidden')return false;}
                return true;
            }
            var images=Array.from(root.querySelectorAll('img'));if(root.tagName==='IMG')images.push(root);
            images=images.filter(function(i){return visible(i,i.getBoundingClientRect())});
            var fonts=0,badFont=false,walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),n,count=0,
                fontCache=window.__cdCaptureFonts||(window.__cdCaptureFonts=new Map()),
                failedFaces=document.fonts?Array.from(document.fonts).filter(function(f){return f.status==='error'}):[];
            function fontState(font,text){
                var key=font+'\n'+text,entry=fontCache.get(key);
                if(!entry){
                    entry={status:'loading'};fontCache.set(key,entry);
                    try{document.fonts.load(font,text).then(function(){entry.status='loaded'},function(){entry.status='error'})}catch(e){entry.status='error';}
                }
                return entry.status;
            }
            function requestedFaceFailed(style,text){
                // WebKit can fulfill load(900) with a healthy 400 fallback after the declared 900 face fails.
                // Match the requested face, not every face sharing its family (which would reject unused weights).
                var family=(style.fontFamily.match(/"[^"]*"|'[^']*'|[^,]+/)||[''])[0].trim().replace(/^['"]|['"]$/g,'').toLowerCase();
                function weight(v){return v==='normal'?400:v==='bold'?700:parseFloat(v);}
                return failedFaces.some(function(f){
                    if(f.family.replace(/^['"]|['"]$/g,'').toLowerCase()!==family||f.style.split(' ')[0]!==style.fontStyle.split(' ')[0])return false;
                    var bounds=f.weight.split(/\s+/).map(weight),w=weight(style.fontWeight);
                    if(w<bounds[0]||w>bounds[bounds.length-1])return false;
                    return f.unicodeRange.split(',').some(function(range){
                        var b=range.trim().replace(/^U\+/i,'').split('-'),lo=parseInt(b[0].replace(/\?/g,'0'),16),hi=parseInt((b[1]||b[0]).replace(/\?/g,'F'),16);
                        return Array.from(text).some(function(c){var n=c.codePointAt(0);return n>=lo&&n<=hi;});
                    });
                });
            }
            while(document.fonts&&(n=walker.nextNode())&&count++<5000){
                var e=n.parentElement;if(!e||!n.nodeValue.trim())continue;
                var range=document.createRange();range.selectNodeContents(n);if(!visible(e,range.getBoundingClientRect()))continue;
                var s=getComputedStyle(e),font=s.font||(s.fontStyle+' '+s.fontWeight+' '+s.fontSize+' '+s.fontFamily);
                var sample=n.nodeValue.slice(0,128),status=fontState(font,sample);if(status==='loading')fonts++;else if(status==='error')badFont=true;
                if(requestedFaceFailed(s,sample))badFont=true;
            }
            return [images.filter(function(i){return !i.complete}).length,images.filter(function(i){return i.complete&&!!i.currentSrc&&i.naturalWidth===0}).length,badFont?-1:fonts,
                Array.from(document.querySelectorAll('link[rel~=stylesheet]')).filter(function(l){return !l.disabled&&!l.sheet}).length,right-left,bottom-top].join(',');
        })()
        """.trimIndent()
    }
    fun region(selector: String): String = """(function(){try{var a=Array.from(document.querySelectorAll(${PageCleanup.js(selector)}));return [a.length,a.some(function(e){return !!e.innerText&&!!e.innerText.trim()})?1:0].join(',')}catch(e){return '-1'}})()"""
}
