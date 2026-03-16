package com.simpleblog.redisService.impl;

import com.simpleblog.common.constants.RedisKeys;
import com.simpleblog.common.utils.RedisKeyBuilder;
import com.simpleblog.common.utils.RedisKeyQuickBuilder;
import com.simpleblog.common.utils.RedisUtils;
import com.simpleblog.mapper.DocNodeMapper;
import com.simpleblog.redisService.IDocSystemRedisService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class DocSystemRedisService implements IDocSystemRedisService {
    private final RedisUtils redisUtils;
    private final DocNodeMapper docNodeMapper;


    @Override
    public void invalidateDocTreeCacheBySpaceId(Long spaceId) {
        if (spaceId == null) {
            return;
        }
        Set<String> keys = redisUtils.keys(RedisKeyQuickBuilder.docTreePattern(spaceId));
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisUtils.delete(keys);
    }

    @Override
    public int nextSortKey(Long spaceId, Long parentId) {
        String key = RedisKeyBuilder.key(RedisKeys.DOC_NODE_NEXT_SORTKEY, spaceId + "_" + parentId);
        String value = redisUtils.get(key);
        if (value == null) {
            Integer next = docNodeMapper.nextSortKey(spaceId, parentId);    // 不会有返回null的情况
            redisUtils.setEx(key, next.toString(), 7, TimeUnit.DAYS);
            return next;
        }
        int v = Integer.parseInt(value), next = v + 1;
        redisUtils.setEx(key, Integer.toString(next), 7, TimeUnit.DAYS);
        return v;
    }
}
