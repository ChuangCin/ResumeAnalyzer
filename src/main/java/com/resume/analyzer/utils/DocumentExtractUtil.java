package com.resume.analyzer.utils;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档正文提取：PDF、DOCX、TXT、MD。
 * 调用方负责关闭 InputStream。
 */
public final class DocumentExtractUtil {

    private static final int MAX_TEXT_LENGTH = 2_000_000;

    /**
     * 按格式从流中提取纯文本。
     *
     * @param format 小写扩展名：pdf, docx, doc, md, markdown, txt
     */
    public static String extractText(String format, InputStream inputStream) {
        if (format == null || inputStream == null) return "";
        String ext = format.toLowerCase();
        return switch (ext) {
            case "pdf" -> PdfUtil.parsePdf(inputStream);
            case "docx", "doc" -> extractDocx(inputStream);
            case "txt", "md", "markdown" -> extractPlain(inputStream);
            default -> extractPlain(inputStream);
        };
    }

    private static String extractDocx(InputStream inputStream) {
        try {
            try (XWPFDocument doc = new XWPFDocument(inputStream)) {
                List<String> paragraphs = doc.getParagraphs().stream()
                    .map(p -> p.getText())
                    .collect(Collectors.toList());
                String full = String.join("\n", paragraphs);
                return sanitize(full);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String extractPlain(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length > MAX_TEXT_LENGTH) {
                byte[] truncated = new byte[MAX_TEXT_LENGTH];
                System.arraycopy(bytes, 0, truncated, 0, MAX_TEXT_LENGTH);
                bytes = truncated;
            }
            return sanitize(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }
}
