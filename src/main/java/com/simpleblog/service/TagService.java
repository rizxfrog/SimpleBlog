package com.simpleblog.service;

import com.simpleblog.model.entity.Tag;

import java.util.List;

public interface TagService {
    /**
     * 查询所有标签
     * @return 标签列表
     */
    List<Tag> listAll();

    /**
     * 根据ID查找标签
     * @param id 标签ID
     * @return 标签对象
     */
    Tag findById(Long id);

    /**
     * 根据ID列表批量查询标签
     * @param ids 标签ID列表
     * @return 标签列表
     */
    List<Tag> listByIds(List<Long> ids);

    /**
     * 创建标签
     * @param name 标签名称
     * @param slug 标签别名
     * @return 创建的标签
     */
    Tag create(String name, String slug);
}
