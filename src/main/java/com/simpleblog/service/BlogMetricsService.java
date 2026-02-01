package com.simpleblog.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.simpleblog.mapper.ArticlePvDailyMapper;
import com.simpleblog.mapper.BlogMapper;
import com.simpleblog.model.entity.ArticlePvDaily;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
public class BlogMetricsService {
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private final StringRedisTemplate redisTemplate;
    private final ArticlePvDailyMapper pvDailyMapper;
    private final BlogMapper blogMapper;

    public BlogMetricsService(StringRedisTemplate redisTemplate,
                              ArticlePvDailyMapper pvDailyMapper,
                              BlogMapper blogMapper) {
        this.redisTemplate = redisTemplate;
        this.pvDailyMapper = pvDailyMapper;
        this.blogMapper = blogMapper;
    }

    public void trackView(Long blogId) {
        if (blogId == null) {
            return;
        }
        String day = LocalDate.now(DEFAULT_ZONE).format(DAY_FORMAT);
        String totalKey = totalKey(blogId);
        String dailyKey = dailyKey(blogId, day);
        redisTemplate.opsForValue().increment(totalKey);
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, Duration.ofDays(1));

        String daySetKey = daySetKey(day);
        redisTemplate.opsForSet().add(daySetKey, blogId.toString());
        redisTemplate.expire(daySetKey, Duration.ofDays(1));
    }

    public long getTotalViews(Long blogId, long fallback) {
        String value = redisTemplate.opsForValue().get(totalKey(blogId));
        if (value != null) {
            return parseLong(value);
        }
        if (fallback > 0) {
            redisTemplate.opsForValue().set(totalKey(blogId), String.valueOf(fallback));
        }
        return fallback;
    }

    public long getDailyViews(Long blogId, LocalDate day) {
        String value = redisTemplate.opsForValue().get(dailyKey(blogId, day.format(DAY_FORMAT)));
        return parseLong(value);
    }

    @Scheduled(fixedDelayString = "${app.metrics.flush-interval-ms:60000}")
    public void flushDailyViews() {
        String day = LocalDate.now(DEFAULT_ZONE).format(DAY_FORMAT);
        String daySetKey = daySetKey(day);
        Set<String> ids = redisTemplate.opsForSet().members(daySetKey);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LocalDate dayDate = LocalDate.parse(day, DAY_FORMAT);
        for (String idStr : ids) {
            Long blogId = parseLongObject(idStr);
            if (blogId == null) {
                continue;
            }
            String dailyValue = redisTemplate.opsForValue().get(dailyKey(blogId, day));
            if (dailyValue != null) {
                ArticlePvDaily pv = new ArticlePvDaily();
                pv.setBlogId(blogId);
                pv.setDay(dayDate);
                pv.setViews(parseLong(dailyValue));
                pvDailyMapper.upsert(pv);
            }
            String totalValue = redisTemplate.opsForValue().get(totalKey(blogId));
            if (totalValue != null) {
                UpdateWrapper<com.simpleblog.model.entity.Blog> wrapper = new UpdateWrapper<>();
                wrapper.eq("id", blogId).set("views", parseLong(totalValue));
                blogMapper.update(null, wrapper);
            }
        }
    }

    private String totalKey(Long blogId) {
        return "pv:article:" + blogId;
    }

    private String dailyKey(Long blogId, String day) {
        return "pv:article:" + blogId + ":" + day;
    }

    private String daySetKey(String day) {
        return "pv:article:daily:set:" + day;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Long parseLongObject(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
