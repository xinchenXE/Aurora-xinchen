package com.aurora.remote;

import com.aurora.remote.adapter.RemoteArticleHandler;
import lombok.Data;

/**
 * <p>
 *  目标源配置类
 * <p>
 *
 * @author hsh 18038120@qq.com
 * @date 2024/1/17
 */
@Data
public class RemoteArticleSource {
    
    // 目标源redis key
    private String source;
    // 目标源名称：
    private String sourceName;
    // 完整的接口地址: www.xxx.xx/api/articles/all
    private String apiUrl;
    
    // Todo 远程接口分页参数
    private RemoteArticlePage page;
    
    // 目标接口实现类
    private String handlerName;
    // 适配器
    private RemoteArticleHandler handler;
}
