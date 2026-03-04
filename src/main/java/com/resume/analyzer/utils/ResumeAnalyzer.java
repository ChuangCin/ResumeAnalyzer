package com.resume.analyzer.utils;

import java.util.ArrayList;
import java.util.List;

public class ResumeAnalyzer {

    public static String extractSkills(String text) {

        List<String> skills = new ArrayList<>();

        if (text.contains("Java")) skills.add("Java");
        if (text.contains("Spring")) skills.add("Spring");
        if (text.contains("MySQL")) skills.add("MySQL");
        if (text.contains("Vue")) skills.add("Vue");
        if (text.contains("Python")) skills.add("Python");

        return String.join(",", skills);
    }

    public static String extractEducation(String text) {

        if (text.contains("本科")) return "本科";
        if (text.contains("硕士")) return "硕士";
        if (text.contains("博士")) return "博士";

        return "未知";
    }

    public static String extractExperience(String text) {

        if (text.contains("3年")) return "3年经验";
        if (text.contains("5年")) return "5年经验";
        if (text.contains("1年")) return "1年经验";

        return "无经验";
    }
}