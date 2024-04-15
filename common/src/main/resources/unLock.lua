---
--- Generated by Luanalysis
--- Created by zzzi.
--- DateTime: 2024/4/12 22:35
---
local key = KEYS[1]; --锁的key
local threadId = ARGV[1] --加上了uuid的线程唯一id

--加锁的就是当前线程
if (redis.call('get', key) == threadId) then
    -- 删除锁并返回1作为成功标志
    redis.call('del', key);
    return 1;
end ;
-- 在这里说明加锁的不是当前线程，解锁失败
return 0;
