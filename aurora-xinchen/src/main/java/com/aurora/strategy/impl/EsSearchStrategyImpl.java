package com.aurora.strategy.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.aurora.model.dto.ArticleSearchDTO;
import com.aurora.strategy.SearchStrategy;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.aurora.constant.CommonConstant.*;
import static com.aurora.enums.ArticleStatusEnum.PUBLIC;

/**
 * ES 搜索策略（Spring Data Elasticsearch 5 / ES Java Client 8 版本 API）
 * ElasticsearchRestTemplate 与 NativeSearchQueryBuilder 已在 Spring Data ES 5 移除，
 * 改用 ElasticsearchOperations + NativeQuery + 新版高亮 API。
 */
@Log4j2
@Service("esSearchStrategyImpl")
public class EsSearchStrategyImpl implements SearchStrategy {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<ArticleSearchDTO> searchArticle(String keywords) {
        if (StringUtils.isBlank(keywords)) {
            return new ArrayList<>();
        }
        return search(buildQuery(keywords));
    }

    private NativeQuery buildQuery(String keywords) {
        // bool: must(bool(should 匹配标题/正文), term isDelete=0, term status=公开)
        Query query = Query.of(q -> q.bool(b -> b
                .must(m -> m.bool(sb -> sb
                        .should(s -> s.match(mt -> mt.field("articleTitle").query(keywords)))
                        .should(s -> s.match(mt -> mt.field("articleContent").query(keywords)))))
                .must(m -> m.term(t -> t.field("isDelete").value(FieldValue.of((long) FALSE))))
                .must(m -> m.term(t -> t.field("status").value(FieldValue.of(PUBLIC.getStatus().longValue()))))));

        HighlightFieldParameters titleFieldParameters = new HighlightFieldParameters.HighlightFieldParametersBuilder()
                .withPreTags(PRE_TAG)
                .withPostTags(POST_TAG)
                .build();
        HighlightFieldParameters contentFieldParameters = new HighlightFieldParameters.HighlightFieldParametersBuilder()
                .withPreTags(PRE_TAG)
                .withPostTags(POST_TAG)
                .withFragmentSize(50)
                .build();
        Highlight highlight = new Highlight(
                new HighlightParameters.HighlightParametersBuilder().build(),
                List.of(
                        new HighlightField("articleTitle", titleFieldParameters),
                        new HighlightField("articleContent", contentFieldParameters)));

        return NativeQuery.builder()
                .withQuery(query)
                .withHighlightQuery(new HighlightQuery(highlight, ArticleSearchDTO.class))
                .build();
    }

    private List<ArticleSearchDTO> search(NativeQuery nativeQuery) {
        try {
            SearchHits<ArticleSearchDTO> search = elasticsearchOperations.search(nativeQuery, ArticleSearchDTO.class);
            return search.getSearchHits().stream().map(hit -> {
                ArticleSearchDTO article = hit.getContent();
                List<String> titleHighLightList = hit.getHighlightFields().get("articleTitle");
                if (CollectionUtils.isNotEmpty(titleHighLightList)) {
                    article.setArticleTitle(titleHighLightList.get(0));
                }
                List<String> contentHighLightList = hit.getHighlightFields().get("articleContent");
                if (CollectionUtils.isNotEmpty(contentHighLightList)) {
                    article.setArticleContent(contentHighLightList.get(contentHighLightList.size() - 1));
                }
                return article;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new ArrayList<>();
    }

}
