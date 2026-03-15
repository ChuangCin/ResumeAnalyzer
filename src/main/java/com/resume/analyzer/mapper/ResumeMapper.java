package com.resume.analyzer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.analyzer.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
