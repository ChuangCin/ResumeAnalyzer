package com.resume.analyzer.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfUtil {

    public static String parsePdf(String filePath) {

        try {
            PDDocument document = PDDocument.load(new File(filePath));
            PDFTextStripper stripper = new PDFTextStripper();

            String text = stripper.getText(document);

            document.close();

            return sanitizeText(text);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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