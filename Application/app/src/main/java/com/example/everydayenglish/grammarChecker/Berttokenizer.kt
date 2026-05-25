package com.example.everydayenglish.grammarChecker

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class BertTokenizer(context: Context, vocabFile: String = "vocab.txt") {

    private val vocab: Map<String, Int>
    private val unkId: Int
    private val clsId: Int
    private val sepId: Int
    private val padId: Int = 0

    init {
        val map = mutableMapOf<String, Int>()
        context.assets.open(vocabFile).use { stream ->
            // ✅ 修复①：先 readLines() 再 forEachIndexed，类型可正常推断
            BufferedReader(InputStreamReader(stream)).readLines()
                .forEachIndexed { index, line ->
                    map[line.trim()] = index
                }
        }
        vocab = map
        unkId = map["[UNK]"] ?: 100
        clsId = map["[CLS]"] ?: 101
        sepId = map["[SEP]"] ?: 102
    }

    // ✅ 修复②：data class 含 IntArray，手动 override equals / hashCode
    data class Encoding(
        val inputIds: IntArray,
        val attentionMask: IntArray,
        val tokenTypeIds: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Encoding) return false
            return inputIds.contentEquals(other.inputIds) &&
                    attentionMask.contentEquals(other.attentionMask) &&
                    tokenTypeIds.contentEquals(other.tokenTypeIds)
        }

        override fun hashCode(): Int {
            var result = inputIds.contentHashCode()
            result = 31 * result + attentionMask.contentHashCode()
            result = 31 * result + tokenTypeIds.contentHashCode()
            return result
        }
    }

    fun encode(text: String, maxLength: Int = 64): Encoding {
        val tokens = wordPieceTokenize(text.lowercase().trim())
        val truncated = tokens.take(maxLength - 2)

        // ✅ 修复③：IntArray(n) 默认填 0，去掉多余的初始化 lambda
        val ids   = IntArray(maxLength)
        val mask  = IntArray(maxLength)
        val types = IntArray(maxLength)

        ids[0]  = clsId
        mask[0] = 1

        truncated.forEachIndexed { i, token ->
            ids[i + 1]  = vocab[token] ?: unkId
            mask[i + 1] = 1
        }

        val sepIndex = truncated.size + 1
        if (sepIndex < maxLength) {
            ids[sepIndex]  = sepId
            mask[sepIndex] = 1
        }

        return Encoding(ids, mask, types)
    }

    private fun wordPieceTokenize(text: String): List<String> {
        val result = mutableListOf<String>()
        // ✅ 修复④：去掉 \p{Punct} 外层多余的 []
        val words = text.split(Regex("\\s+|(?=\\p{Punct})|(?<=\\p{Punct})"))
            .filter { it.isNotBlank() }

        for (word in words) {
            if (word in vocab) {
                result.add(word)
                continue
            }
            var start = 0
            var isBad = false
            val subTokens = mutableListOf<String>()
            while (start < word.length) {
                var end = word.length
                var curSubStr: String? = null
                while (start < end) {
                    val sub = if (start == 0) word.substring(start, end)
                    else "##${word.substring(start, end)}"
                    if (sub in vocab) { curSubStr = sub; break }
                    end--
                }
                if (curSubStr == null) { isBad = true; break }
                subTokens.add(curSubStr)
                start = end
            }
            if (isBad) result.add("[UNK]") else result.addAll(subTokens)
        }
        return result
    }
}