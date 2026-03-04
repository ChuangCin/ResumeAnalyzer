package com.resume.analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class AIService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    public String analyzeResume(String content) {

        String prompt = """
你是一个智能简历分析系统。

请分析简历，并返回 JSON：

{
  "summary": "核心评价",

  "highlights": [
    "亮点1",
    "亮点2",
    "亮点3"
  ],

  "scores": {
    "skill": 0-20,
    "experience": 0-40,
    "structure": 0-15,
    "expression": 0-10,
    "completeness": 0-15
  },

  "totalScore": 0-100,

  "suggestions": {
    "high": [
      {
        "title": "问题标题",
        "content": "详细建议"
      }
    ],
    "medium": [
      {
        "title": "问题标题",
        "content": "详细建议"
      }
    ],
    "low": [
      {
        "title": "问题标题",
        "content": "详细建议"
      }
    ]
  }
}

要求：
- 返回纯JSON
- 不要解释
- 不要markdown
- suggestions必须包含 high medium low

简历：
""" + content;

        return callDeepSeek(prompt);
    }


    public String matchJob(String resumeContent, String jobDescription) {

        String prompt = """
你是一个智能招聘系统。

请根据简历和岗位要求进行匹配评分。

返回 JSON：

{
  "jobName": "岗位名称",
  "matchScore": 0-100,
  "advantages": [
    "匹配优势1",
    "匹配优势2"
  ],
  "disadvantages": [
    "不足1",
    "不足2"
  ],
  "suggestions": [
    "建议1",
    "建议2"
  ]
}

要求：
- 返回纯JSON
- 不要解释
- 不要markdown

岗位要求：
""" + jobDescription + """

简历：
""" + resumeContent;

        return callDeepSeek(prompt);
    }


    private String callDeepSeek(String prompt) {

        String url = "https://api.deepseek.com/v1/chat/completions";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();

        body.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", prompt);

        messages.add(msg);

        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        // 解析 JSON
        Map result = response.getBody();

        List choices = (List) result.get("choices");
        Map choice = (Map) choices.get(0);
        Map message = (Map) choice.get("message");

        return message.get("content").toString();
    }
}