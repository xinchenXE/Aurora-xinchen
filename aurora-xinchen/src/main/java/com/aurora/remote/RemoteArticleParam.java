package com.aurora.remote;

import com.aurora.model.vo.ConditionVO;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 *  远程获取文章数据-请求参数
 * <p>
 *
 * @author hsh 18038120@qq.com
 * @date 2024/1/17
 */
@Data
public class RemoteArticleParam extends ConditionVO {
   
   // 目标源
   @NotBlank(message = "目标源不能为空")
   private String targetSource;
   
   private RemoteArticleSource source;
   
}
