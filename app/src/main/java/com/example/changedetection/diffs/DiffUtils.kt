/*-
 * #%L
 * java-diff-utils
 * %%
 * Copyright (C) 2009 - 2017 java-diff-utils
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */
package com.example.changedetection.diffs

import com.example.changedetection.diffs.algorithm.DiffAlgorithm
import com.example.changedetection.diffs.algorithm.DiffException
import com.example.changedetection.diffs.algorithm.myers.MyersDiff
import com.example.changedetection.diffs.patch.Patch
import com.example.changedetection.diffs.patch.PatchFailedException
import java.util.*

/**
 * Implements the difference and patching engine
 *
 * @author [Dmitry Naumenko](dm.naumenko@gmail.com)
 * @version 0.4.1
 */
object DiffUtils {

    /**
     * Computes the difference between the original and revised list of elements with default diff algorithm
     *
     * @param original The original text. Must not be `null`.
     * @param revised The revised text. Must not be `null`.
     * @return The patch describing the difference between the original and revised sequences. Never `null`.
     */
    @Throws(DiffException::class)
    fun <T> diff(original: List<T>, revised: List<T>): Patch<T> {
        return DiffUtils.diff(original, revised, MyersDiff())
    }

    /**
     * Computes the difference between the original and revised text.
     */
    @Throws(DiffException::class)
    fun diff(originalText: String, revisedText: String): Patch<String> {
        return DiffUtils.diff(
            Arrays.asList(*originalText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()),
            Arrays.asList(*revisedText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        )
    }

    /**
     * Computes the difference between the original and revised list of elements with default diff algorithm
     *
     * @param original The original text. Must not be `null`.
     * @param revised The revised text. Must not be `null`.
     *
     * @param equalizer the equalizer object to replace the default compare algorithm (Object.equals). If `null`
     * the default equalizer of the default algorithm is used..
     * @return The patch describing the difference between the original and revised sequences. Never `null`.
     */
    @Throws(DiffException::class)
    fun <T> diff(
        original: List<T>, revised: List<T>,
        equalizer: ((T, T) -> Boolean)?
    ): Patch<T> {
        return if (equalizer != null) {
            DiffUtils.diff(
                original, revised,
                MyersDiff(equalizer)
            )
        } else DiffUtils.diff(original, revised, MyersDiff())
    }

    /**
     * Computes the difference between the original and revised list of elements with default diff algorithm
     *
     * @param original The original text. Must not be `null`.
     * @param revised The revised text. Must not be `null`.
     * @param algorithm The diff algorithm. Must not be `null`.
     * @return The patch describing the difference between the original and revised sequences. Never `null`.
     */
    @Throws(DiffException::class)
    fun <T> diff(
        original: List<T>, revised: List<T>,
        algorithm: DiffAlgorithm<T>
    ): Patch<T> {
        Objects.requireNonNull(original, "original must not be null")
        Objects.requireNonNull(revised, "revised must not be null")
        Objects.requireNonNull(algorithm, "algorithm must not be null")

        return Patch.generate(original, revised, algorithm.diff(original, revised))
    }

    /**
     * Computes the difference between the given texts inline. This one uses the "trick" to make out of texts lists of
     * characters, like DiffRowGenerator does and merges those changes at the end together again.
     *
     * @param original
     * @param revised
     * @return
     */
    @Throws(DiffException::class)
    fun diffInline(original: String, revised: String): Patch<String> {
        val origList = ArrayList<String>()
        val revList = ArrayList<String>()
        for (character in original.toCharArray()) {
            origList.add(character.toString())
        }
        for (character in revised.toCharArray()) {
            revList.add(character.toString())
        }
        val patch = DiffUtils.diff(origList, revList)
        for (delta in patch.getDeltas()) {
            delta.original.lines = compressLines(delta.original.lines, "")
            delta.revised.lines = compressLines(delta.revised.lines, "")
        }
        return patch
    }

    private fun compressLines(lines: List<String>, delimiter: String): List<String> {
        return if (lines.isEmpty()) {
            emptyList()
        } else {
            listOf(lines.joinToString(delimiter))
        }
    }

    /**
     * Patch the original text with given patch
     *
     * @param original the original text
     * @param patch the given patch
     * @return the revised text
     * @throws PatchFailedException if can't apply patch
     */
    @Throws(PatchFailedException::class)
    fun <T> patch(original: List<T>, patch: Patch<T>): List<T> {
        return patch.applyTo(original)
    }

    /**
     * Unpatch the revised text for a given patch
     *
     * @param revised the revised text
     * @param patch the given patch
     * @return the original text
     */
    fun <T> unpatch(revised: List<T>, patch: Patch<T>): List<T> {
        return patch.restore(revised)
    }
}
