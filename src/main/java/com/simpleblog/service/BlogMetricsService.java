package com.simpleblog.service;

import java.time.LocalDate;

public interface BlogMetricsService {
    /**
     * 记录博客浏览量
     * @param blogId 博客ID
     */
    void trackView(Long blogId);

    /**
     * 获取博客总浏览量
     * @param blogId 博客ID
     * @param fallback 回退值(数据库中的值)
     * @return 总浏览量
     */
    long getTotalViews(Long blogId, long fallback);

    /**
     * 获取博客每日浏览量
     * @param blogId 博客ID
     * @param day 日期
     * @return 浏览量
     */
    long getDailyViews(Long blogId, LocalDate day);

    /**
     * 刷新每日浏览量到数据库
     */
    void flushDailyViews();
}
