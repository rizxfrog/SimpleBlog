package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.simpleblog.mapper.CategoryMapper;
import com.simpleblog.model.entity.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> listAll() {
        return categoryMapper.selectList(new QueryWrapper<>());
    }

    public Category findById(Long id) {
        return categoryMapper.selectById(id);
    }

    public Category create(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        categoryMapper.insert(category);
        return category;
    }
}
