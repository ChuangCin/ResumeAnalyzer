package com.resume.analyzer.config;

import com.resume.analyzer.entity.Job;
import com.resume.analyzer.repository.JobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动时若岗位数量不足则补充示例岗位，便于分页演示。
 */
@Component
@Order(1)
public class JobDataSeeder implements ApplicationRunner {

    private static final int MIN_JOBS_FOR_PAGINATION = 30;

    private final JobRepository jobRepository;

    public JobDataSeeder(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        long count = jobRepository.count();
        if (count >= MIN_JOBS_FOR_PAGINATION) return;
        int toAdd = (int) (MIN_JOBS_FOR_PAGINATION - count);
        List<Job> samples = buildSampleJobs(toAdd);
        jobRepository.saveAll(samples);
    }

    private static List<Job> buildSampleJobs(int n) {
        String[] jobNames = {
            "Java 后端开发工程师", "前端开发工程师", "算法工程师", "产品经理", "测试工程师",
            "运维工程师", "数据工程师", "iOS 开发工程师", "Android 开发工程师", "全栈工程师",
            "Python 开发工程师", "Go 开发工程师", "C++ 开发工程师", "大数据开发", "安全工程师",
            "UI 设计师", "运营专员", "人力资源专员", "财务分析师", "市场专员"
        };
        String[] categories = { "技术", "产品", "设计", "运营", "市场", "职能" };
        String[] companies = {
            "字节跳动", "阿里巴巴", "腾讯", "美团", "京东", "网易", "百度", "小米",
            "华为", "滴滴", "拼多多", "快手", "蚂蚁集团", "携程", "哔哩哔哩", "小红书",
            "得物", "商汤科技", "旷视科技", "地平线"
        };
        String[] cities = { "北京", "上海", "深圳", "杭州", "广州", "成都", "南京", "武汉", "西安", "苏州" };
        String[] educations = { "本科", "硕士", "本科及以上", "硕士及以上", "大专及以上" };
        String[] experiences = { "1-3年", "3-5年", "应届毕业生", "5-10年", "不限" };
        int[][] salaryRanges = {
            { 15, 30 }, { 20, 40 }, { 25, 50 }, { 30, 60 }, { 12, 25 }, { 18, 35 }, { 10, 20 }
        };

        return java.util.stream.IntStream.range(0, n)
            .mapToObj(i -> {
                Job job = new Job();
                job.setJobName(jobNames[i % jobNames.length] + "（" + (i + 1) + "）");
                job.setJobCategory(categories[i % categories.length]);
                job.setCompany(companies[i % companies.length]);
                job.setCity(cities[i % cities.length]);
                job.setEducation(educations[i % educations.length]);
                job.setExperience(experiences[i % experiences.length]);
                int[] sal = salaryRanges[i % salaryRanges.length];
                job.setSalaryMin(sal[0] * 1000);
                job.setSalaryMax(sal[1] * 1000);
                job.setSkills("Java/Spring/MySQL/Redis");
                job.setDescription("岗位描述：负责相关业务开发与维护。");
                job.setRequirement("任职要求：具备相应学历与经验。");
                job.setCreateTime(LocalDateTime.now().minusDays(n - i));
                return job;
            })
            .toList();
    }
}
