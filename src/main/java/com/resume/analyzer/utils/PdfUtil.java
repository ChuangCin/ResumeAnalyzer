package com.resume.analyzer.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.InputStream;

public class PdfUtil {

    public static String parsePdf(String filePath) {
        try {
            return parsePdf(PDDocument.load(new File(filePath)));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /** 从输入流解析 PDF（调用方负责关闭流；方法内会关闭 PDDocument） */
    public static String parsePdf(InputStream inputStream) {
        try {
            PDDocument document = PDDocument.load(inputStream);
            try {
                return parsePdf(document);
            } finally {
                document.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String parsePdf(PDDocument document) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        return sanitizeText(stripper.getText(document));
    }

    private static String sanitizeText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text
                // Remove private-use and special non-characters often used for PDF bullets/icons.
                .replaceAll("[\\uE000-\\uF8FF]", "")
                .replaceAll("[\\uFFF0-\\uFFFF]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}