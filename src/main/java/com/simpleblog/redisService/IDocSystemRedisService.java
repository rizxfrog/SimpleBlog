package com.simpleblog.redisService;

import com.simpleblog.common.constants.RedisKeys;
import com.simpleblog.common.utils.RedisKeyBuilder;

import java.util.concurrent.TimeUnit;

public interface IDocSystemRedisService {
    /**
     * 清除文档树缓存
     */
    void invalidateDocTreeCacheBySpaceId(Long spaceId);

    int nextSortKey(Long spaceId, Long parentId);
}
