package com.resume.analyzer.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块：固定长度 + 重叠，便于 RAG 检索。
 */
public final class ChunkUtil {

    /** 每块目标字符数（中文按字计） */
    public static final int DEFAULT_CHUNK_SIZE = 400;
    /** 块间重叠字符数，避免语义断裂 */
    public static final int DEFAULT_OVERLAP = 50;

    /**
     * 将长文本按固定大小分块，块间带重叠。
     */
    public static List<String> split(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;
        String s = text.trim();
        if (s.length() <= chunkSize) {
            chunks.add(s);
            return chunks;
        }
        int start = 0;
        while (start < s.length()) {
            int end = Math.min(start + chunkSize, s.length());
            chunks.add(s.substring(start, end));
            // 最后一块已到结尾，避免由于 overlap 回退导致重复分块死循环
            if (end >= s.length()) break;
            int back = (overlap > 0 ? Math.min(overlap, Math.max(1, chunkSize / 2)) : 0);
            int nextStart = end - back;
            // 防御：确保指针前进，避免 back 过大导致停滞
            if (nextStart <= start) nextStart = end;
            start = nextStart;
        }
        return chunks;
    }

    public static List<String> split(String text) {
        return split(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }
}
