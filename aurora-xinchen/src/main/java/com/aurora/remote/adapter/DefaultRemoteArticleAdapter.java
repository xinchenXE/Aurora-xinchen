package com.aurora.remote.adapter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.aurora.model.dto.PageResultDTO;
import com.aurora.model.vo.ResultVO;
import com.aurora.remote.RemoteArticle;
import com.aurora.remote.RemoteArticleParam;
import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  获取远程文章数据-适配器模式: 目标接口默认实现类
 *     如果远程返回的数据结构与现有的类(RemoteArticle/ArticleCardDTO)结构不同，可以
 *     新增一个RemoteArticleAdapter实现来处理， 例如 GysyRemoteArticleAdapter，并修改目标源的实现类(handlerName)
 * <p>
 *
 * @author hsh 18038120@qq.com
 * @date 2024/1/17
 */
public class DefaultRemoteArticleAdapter implements RemoteArticleHandler {
    
    @Override
    public PageResultDTO<RemoteArticle> getRemoteData(RemoteArticleParam param) {
        String url = param.getSource().getApiUrl();
        // http参数
        HashMap<String, Object> paramMap = new HashMap<>();
        // 分页参数
        if (ObjectUtil.isNotNull(param.getSource().getPage())) {
            paramMap.put("current", param.getSource().getPage().getCurrent());
            paramMap.put("size", param.getSource().getPage().getSize());
        }
        
        String result = HttpUtil.get(url, paramMap);
        if (StringUtils.isNotEmpty(result)) {
            ResultVO vo = JSONObject.parseObject(result, ResultVO.class);
            System.out.println(vo);
            
            // 是否成功
            if (vo.getFlag()) {
                Map<String, Object> map = JSON.parseObject(vo.getData().toString(), new TypeReference<Map<String, Object>>(){});
                System.out.println(map.get("records"));
                
                List<RemoteArticle> list = JSONArray.parseArray(map.get("records").toString(), RemoteArticle.class);
                for (RemoteArticle artice : list) {
                    // todo 声明文章来源(目标源)，接口地址，分页参数等
                    artice.packageUp(param.getSource());
                }
                Integer count = Integer.parseInt(map.get("count").toString());
                return new PageResultDTO<>(list, count);
            }
            // todo 请求失败处理
        }
        return null;
    }
}
