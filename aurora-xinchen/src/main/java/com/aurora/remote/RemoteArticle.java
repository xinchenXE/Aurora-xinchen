package com.aurora.remote;

import com.aurora.model.dto.ArticleCardDTO;
import lombok.Data;

/**
 * <p>
 *  获取远程文章数据-装饰器模式: 文章数据实体类
 * <p>
 *
 * @author hsh 18038120@qq.com
 * @date 2024/1/17
 */
@Data
public class RemoteArticle extends ArticleCardDTO {
   
   // 来源
   private String source;
   private String sourceName;
   
   public void packageUp(RemoteArticleSource source) {
      this.source = source.getSource();
      this.sourceName = source.getSourceName();
      // 强制文章类型为 转载
      this.setType(2);
   }
}
