package com.bernaferrari.changedetection.detection
import org.junit.Test
import org.junit.Assert.*
class BrowserSelectorCompatibilityTest {
 @Test fun nonBrowserTextPseudosAreRejectedBeforeSaving(){for(s in listOf("p:contains(Stock)","div:eq(1)","p:containsOwn(Stock)")){val e=runCatching{TextRegion(s).validate()}.exceptionOrNull();assertTrue("Non-browser selector accepted: $s",e is TextRegionException)}}
 @Test fun quotedAttributeValuesAreNotMistakenForPseudos(){TextRegion("[data-label=':contains(Stock)']").validate()}
}
