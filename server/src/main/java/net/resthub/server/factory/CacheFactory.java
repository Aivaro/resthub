package net.resthub.server.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.resthub.server.query.Query;
import net.resthub.server.handler.Handler;
import net.sf.ehcache.config.CacheConfiguration;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * CacheManager
 * @author valdo
 */
@Log4j
@Singleton
public class CacheFactory implements AutoCloseable {
    
    @Inject
    private Scheduler scheduler;
    
    @Inject
    private InjectorJobFactory ijf;
    
    private final CacheManager manager = CacheManager.newInstance();
    private final CacheConfiguration defaultConfig = this.manager.getConfiguration().getCacheConfigurations().get("data");

    public CacheFactory() {
        log.info(String.format("Cache manager is %s: %s", manager.getStatus(), manager.getActiveConfigurationText()));
    }
    
    public void add(Query query) {
        String name = query.getQid().getId();
        if (!this.manager.cacheExists(name) && query.isCacheable()) {
            
            CacheConfiguration config = defaultConfig.clone();
            config.setName(name);
            config.setEternal(query.isEternal());
            config.setTimeToLiveSeconds(query.getCacheTime());
            
            Cache c = new Cache(config);
            this.manager.addCache(c);
            
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cache %s created: eternal = %s, cacheTime = %d", 
                        c.getName(), query.isEternal(), query.getCacheTime()));
            }
            
        }
    }
    
    public Cache get(Query query) {
        String name = query.getQid().getId();
        if (this.manager.cacheExists(name)) {
            return this.manager.getCache(name);
        }
        return null;
    }
    
    public void remove(Query query) {
        String name = query.getQid().getId();
        if (this.manager.cacheExists(name)) {
            
            this.manager.removeCache(name);
            
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cache %s removed", name));
            }
            
        }
    }
    
    public void logStats() {
        for (String name: manager.getCacheNames()) {
            StatisticsGateway s = manager.getCache(name).getStatistics();
            log.debug(String.format("Cache %s: hit/miss = %d/%d (%f), heap = %d (%d bytes), disk = %d (%d bytes)", 
                    name,
                    s.cacheHitCount(),
                    s.cacheMissCount(),
                    s.cacheHitRatio(),
                    s.getLocalHeapSize(),
                    s.getLocalHeapSizeInBytes(),
                    s.getLocalDiskSize(),
                    s.getLocalDiskSizeInBytes()));
        }
    }
    
    @Override
    public void close() throws Exception {
        manager.shutdown();
    }
    
    private final Map<Integer, CacheJobData> cacheJobDataMap = new ConcurrentHashMap<>();
    
    public void createCacheJob(Handler<?,?> handler, long expTime) {
        try {         
            InjectorJobFactory.startCacheJob(scheduler, ijf, handler);
            cacheJobDataMap.put(handler.getId(), new CacheJobData(handler, expTime));          
        } catch (SchedulerException ex) {
            log.error("Error while scheduling cache job", ex);
        }
    }
    
    public CacheJobData getCacheJobData(Integer id) {
        return cacheJobDataMap.remove(id);
    }
    
    @RequiredArgsConstructor
    @Getter
    public static class CacheJobData {
        
        private final Handler<?,?> handler;
        private final long expTime;
    }
    
}
