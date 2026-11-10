package com.aurora.remote;

import cn.hutool.core.util.ObjectUtil;
import com.aurora.exception.BizException;
import com.aurora.model.dto.*;
import com.aurora.model.vo.*;
import com.aurora.remote.adapter.RemoteArticleHandler;
import com.aurora.service.RedisService;
import lombok.extern.log4j.Log4j2;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.simpleframework.xml.core.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Tag(name = "文章模块")
@RestController
public class RemoteArticleController {
    
    private final static String REMOTE_SOURCE_PREFIX = "REMOTE_ARTICLE_SOURCE:";
    
    @Autowired
    private RedisService redisService;
    
    @PostConstruct
    public void initRemoteSource() {
        // gysy.ltd
        RemoteArticleSource gysy_ltd = new RemoteArticleSource();
        gysy_ltd.setSource("gysy.ltd");
        gysy_ltd.setSourceName("公益事业.ltd");
        gysy_ltd.setApiUrl("https://gysy.ltd/api/articles/all");
        gysy_ltd.setHandlerName("com.aurora.remote.adapter.DefaultRemoteArticleAdapter"); // 目标实现类
        
        redisService.set(REMOTE_SOURCE_PREFIX + gysy_ltd.getSource(), gysy_ltd);
        
        // orangejun.cn
        RemoteArticleSource orangejun = new RemoteArticleSource();
        orangejun.setSource("orangejun.cn");
        orangejun.setSourceName("橘子君");
        orangejun.setApiUrl("https://orangejun.cn/api/articles/all");
        orangejun.setHandlerName("com.aurora.remote.adapter.DefaultRemoteArticleAdapter");
        redisService.set(REMOTE_SOURCE_PREFIX + orangejun.getSource(), orangejun);
        
        // blog.jonk.top
        RemoteArticleSource blog_jonk_top = new RemoteArticleSource();
        blog_jonk_top.setSource("jonk.top");
        blog_jonk_top.setSourceName("一个懒猫");
        blog_jonk_top.setApiUrl("https://blog.jonk.top/api/articles/all");
        blog_jonk_top.setHandlerName("com.aurora.remote.adapter.DefaultRemoteArticleAdapter");
        redisService.set(REMOTE_SOURCE_PREFIX + blog_jonk_top.getSource(), blog_jonk_top);
    }
   
    private Map<String, RemoteArticleSource> sourceMap = new HashMap<>();
    
    /**
     * 读缓存的远程数据源，带自愈（队列项 009 步骤 2）。
     * <p>
     * 反序列化白名单收敛（{@code RedisConfig}）后，若缓存里存的是白名单不接受的值，
     * {@code redisService.get} 会抛异常。这里容错：删掉脏键并返回 {@code null}，
     * 由调用方走既有"找不到数据源"分支——**升级后首个请求不会 500**。
     * 与 {@code AiPersonaServiceImpl.getCachedPersona} 的自愈模式同构。
     */
    private RemoteArticleSource readCachedSource(String targetSource) {
        String key = REMOTE_SOURCE_PREFIX + targetSource;
        try {
            return (RemoteArticleSource) redisService.get(key);
        } catch (Exception e) {
            // 旧格式/非白名单类型：删键，等下一次 initRemoteSource 或写回重建
            redisService.del(key);
            return null;
        }
    }

    @Operation(summary = "获取所有文章-带分页")
    @GetMapping("/remote/article/all")
    public ResultVO<PageResultDTO<RemoteArticle>> listArticles(@Validate RemoteArticleParam param) {
        RemoteArticleSource remoteSource = sourceMap.get(REMOTE_SOURCE_PREFIX + param.getTargetSource());
        RemoteArticleHandler handler = null;
        
        try {
            // todo 从数据库或redis中获得 目标源配置类
            if (ObjectUtil.isNull(remoteSource)) {
                // 首次使用。readCachedSource 带自愈：脏值/非白名单类型会删键并返回 null
                remoteSource = readCachedSource(param.getTargetSource());

                if (ObjectUtil.isNull(remoteSource)) {
                    // 必须在上面的自愈返回 null 之后立刻判空：原先此处先做 remoteSource.toString()
                    // 拼接调试日志，遇到自愈返回 null 会抛 NPE，使自愈失效（2026-09-30 实测）
                    throw new BizException("找不到数据源");
                }
                log.debug("目标源: {}", remoteSource);
                
                // 适配器类
                Class<?> clazz = null;
                clazz = Class.forName(remoteSource.getHandlerName());
                handler = (RemoteArticleHandler) clazz.newInstance();
                
                remoteSource.setHandler(handler);
                remoteSource.setPage(new RemoteArticlePage());
                sourceMap.put(param.getTargetSource(), remoteSource); // todo
                //param.setSource(remoteSource);
            } else {
                handler = remoteSource.getHandler();
            }
            // todo 分页参数处理
            remoteSource.getPage().setCurrent(param.getCurrent());
            remoteSource.getPage().setSize(param.getSize());
            
            param.setSource(remoteSource);
            // todo 断言
            assert handler != null;
            return ResultVO.ok(handler.getRemoteData(param));
        } catch (BizException e) {
            // 业务异常直接上抛：否则会被下面的 catch(Exception) 包成 RuntimeException → 落成 50000，
            // "找不到数据源"这类可读业务码永远透不出去（2026-09-30 实测发现）
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
