package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.TagMapper;
import com.simpleblog.model.entity.Tag;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TagService {
    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public List<Tag> listAll() {
        return tagMapper.selectList(new QueryWrapper<>());
    }

    public Tag findById(Long id) {
        return tagMapper.selectById(id);
    }

    public List<Tag> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(ids);
    }

    public Tag create(String name, String slug) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.insert(tag);
        return tag;
    }
}
