package com.aurora.remote;

import lombok.Data;

/**
 * <p>
 *  Todo 远程接口分页参数
 * <p>
 *
 * @author hsh 18038120@qq.com
 * @date 2024/1/17
 */
@Data
public class RemoteArticlePage {
    
    // 分页
    private Long current;
    private Long size;
    private Integer count;
    
    private RemoteArticlePage lastPageOption;
}
