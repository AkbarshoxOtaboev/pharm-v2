package uz.uwon.pharm.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * JWT blacklist. Redis bo'lsa undan, aks holda in-memory.
 */
@Component
public class TokenBlacklist {

    private static final String PREFIX = "blacklist:";

    private final ConcurrentHashMap<String, Long> memoryStore = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public void add(String token, long expirationMillis) {
        if (expirationMillis <= 0) {
            return;
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(PREFIX + token, "1", expirationMillis, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception ignored) {
                // fall through to memory
            }
        }
        memoryStore.put(token, System.currentTimeMillis() + expirationMillis);
    }

    public boolean isBlacklisted(String token) {
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
            } catch (Exception ignored) {
                // fall through
            }
        }
        Long expiry = memoryStore.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            memoryStore.remove(token);
            return false;
        }
        return true;
    }
}
