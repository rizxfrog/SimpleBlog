package com.simpleblog.service;

import com.simpleblog.model.entity.Category;

import java.util.List;

public interface CategoryService {
    /**
     * 查询所有分类
     * @return 分类列表
     */
    List<Category> listAll();

    /**
     * 根据ID查找分类
     * @param id 分类ID
     * @return 分类对象
     */
    Category findById(Long id);

    /**
     * 创建分类
     * @param name 分类名称
     * @param slug 分类别名
     * @return 创建的分类
     */
    Category create(String name, String slug);
}
