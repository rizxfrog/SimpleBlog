package com.simpleblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.CategoryMapper;
import com.simpleblog.model.entity.Category;
import com.simpleblog.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public Category findById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public Category create(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        categoryMapper.insert(category);
        return category;
    }
}
