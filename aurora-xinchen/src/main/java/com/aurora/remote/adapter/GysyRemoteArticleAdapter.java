package com.aurora.remote.adapter;

import com.aurora.model.dto.PageResultDTO;
import com.aurora.remote.RemoteArticle;
import com.aurora.remote.RemoteArticleParam;

/**
 * <p>
 *  获取远程文章数据-适配器模式: 目标接口实现类
 *  -示例
 * <p>
 *
 * @author hsh 18038120@qq.com
 * @date 2024/1/17
 */
public class GysyRemoteArticleAdapter implements RemoteArticleHandler {
    
    @Override
    public PageResultDTO<RemoteArticle> getRemoteData(RemoteArticleParam param) {
       
        return null;
    }
}
