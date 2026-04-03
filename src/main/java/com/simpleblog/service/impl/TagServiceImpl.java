package com.simpleblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.TagMapper;
import com.simpleblog.model.entity.Tag;
import com.simpleblog.service.TagService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {
    private final TagMapper tagMapper;

    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public List<Tag> listAll() {
        return tagMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public Tag findById(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    public List<Tag> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(ids);
    }

    @Override
    public Tag create(String name, String slug) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.insert(tag);
        return tag;
    }
}
