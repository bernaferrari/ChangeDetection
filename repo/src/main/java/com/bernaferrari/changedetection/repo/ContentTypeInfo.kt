package com.bernaferrari.changedetection.repo

/**
 * This will be used when multiple contentTypes were detected on the local database for the same
 * website, so it will return the respective contentType and how many are present as a way of
 * letting the user decide to open or remove them.
 *
 * @param contentType the contentType
 * @param count the number of items with the same contentType as defined above
 */
data class ContentTypeInfo(val contentType: String, val count: Int)