package com.xuecheng.search.Service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsCourseService {
    @Value("${xuecheng.elasticsearch.course.index}")
    private String index;

    @Value("${xuecheng.elasticsearch.course.type}")
    private String doc;

    @Value("${xuecheng.elasticsearch.course.source_field}")
    private String source_field;

    @Value("${xuecheng.elasticsearch.media.index}")
    private String media_index;

    @Value("${xuecheng.elasticsearch.media.type}")
    private String media_type;

    @Value("${xuecheng.elasticsearch.media.source_field}")
    private String media_source_field;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        //设置索引
        SearchRequest searchRequest = new SearchRequest(index);
        //设置类型
        searchRequest.types(doc);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //source源字段过滤
        String[] split = source_field.split(",");
        searchSourceBuilder.fetchSource(split, new String[]{});


        //设置布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //关键字
        if (StringUtils.isNotEmpty(courseSearchParam.getKeyword())) {
            //匹配关键字
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(),
                    "name", "teachplan", "description");
            //设置匹配占比
            multiMatchQueryBuilder.minimumShouldMatch("70%");
            //设置其中一个字段的Boost 权重
            multiMatchQueryBuilder.field("name", 10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }
        //根据分类   过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            //根据一级分类
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", courseSearchParam.getMt()));
        }
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            //根据二级分类
            boolQueryBuilder.filter(QueryBuilders.termQuery("st", courseSearchParam.getSt()));
        }
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            //根据难度等级
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }


        //布尔查询
        searchSourceBuilder.query(boolQueryBuilder);
        //  请求搜索
        searchRequest.source(searchSourceBuilder);

        //用于得到的源文档中的信息，塞进list集合中
        List<CoursePub> list = new ArrayList<>();
        QueryResult<CoursePub> queryResult = new QueryResult<>();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            //结果处理
            SearchHits hits = searchResponse.getHits();
            //得到记录总数
            long totalHits = hits.getTotalHits();
            queryResult.setTotal(totalHits);
            SearchHit[] hits1 = hits.getHits();
            //设置数据列表,把从源文档中取出来的值设置进去

            for (SearchHit hit : hits1) {
                CoursePub coursePub = new CoursePub();
                //取出源文档
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //取出名称
                String name = (String) sourceAsMap.get("name");
                coursePub.setName(name);
                //图片
                String pic = (String) sourceAsMap.get("pic");
                coursePub.setPic(pic);
                //价格
                Double price = null;
                if (sourceAsMap.get("price") != null) {
                    price = (Double) sourceAsMap.get("price");
                }
                coursePub.setPrice(price);

                Double price_old = null;
                if (sourceAsMap.get("price") != null) {
                    price_old = (Double) sourceAsMap.get("price");
                }
                coursePub.setPrice_old(price_old);

                //把coursePub得到的信息全部塞进去
                list.add(coursePub);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        queryResult.setList(list);

        QueryResponseResult<CoursePub> coursePubQueryResponseResult = new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
        return coursePubQueryResponseResult;
    }

    public Map<String, CoursePub> getall(String id) {
        //设置索引库
        SearchRequest searchRequest = new SearchRequest(index);
        //设置类型
        searchRequest.types(doc);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //查询条件，根据课程id查询
        searchSourceBuilder.query(QueryBuilders.termQuery("id", id));
        //不用进行sourse原字段过滤，查询所有字段

        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = null;
        try {
            //执行搜索
            searchResponse = restHighLevelClient.search(searchRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //搜索的结果
        SearchHits hits = searchResponse.getHits();
        SearchHit[] hitsHits = hits.getHits();
        HashMap<String, CoursePub> map = new HashMap<>();
        for (SearchHit hit : hitsHits) {
            String courseId = hit.getId();
            //源信息
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String id1 = (String) sourceAsMap.get("id");
            String name = (String) sourceAsMap.get("name");
            String grade = (String) sourceAsMap.get("grade");
            String charge = (String) sourceAsMap.get("charge");
            String pic = (String) sourceAsMap.get("pic");
            String description = (String) sourceAsMap.get("description");
            String teachplan = (String) sourceAsMap.get("teachplan");
            CoursePub coursePub = new CoursePub();
            coursePub.setId(id1);
            coursePub.setName(name);
            coursePub.setPic(pic);
            coursePub.setGrade(grade);
            coursePub.setTeachplan(teachplan);
            coursePub.setDescription(description);
            coursePub.setCharge(charge);
            map.put(courseId, coursePub);
        }


        return map;

    }

    //根据课程计划查询媒资信息
    public QueryResponseResult<TeachplanMediaPub> getmedia(String[] teachplanIds) {
        //设置索引
        SearchRequest searchRequest = new SearchRequest(media_index);
        //设置类型
        searchRequest.types(media_type);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //查询条件，根据课程计划id查询（可传入多个id）
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id", teachplanIds));
        //source源字段过滤
        String[] source_fields = media_source_field.split(",");
        searchSourceBuilder.fetchSource(source_fields, new String[]{});

        searchRequest.source(searchSourceBuilder);


        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            //获取搜索结果
            SearchHits hits = searchResponse.getHits();
            SearchHit[] hits1 = hits.getHits();
            long total= hits.totalHits;

            //数据列表
            List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();

            for (SearchHit hit : hits1) {
                TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
                //得到源数据
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //取出课程计划媒资信息
                String courseid = (String) sourceAsMap.get("courseid");
                String media_id = (String) sourceAsMap.get("media_id");
                String media_url = (String) sourceAsMap.get("media_url");
                String teachplan_id = (String) sourceAsMap.get("teachplan_id");
                String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");
                //塞进去
                teachplanMediaPub.setCourseId(courseid);
                teachplanMediaPub.setMediaUrl(media_url);
                teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
                teachplanMediaPub.setMediaId(media_id);
                teachplanMediaPub.setTeachplanId(teachplan_id);

                //将数据加入列表
                teachplanMediaPubList.add(teachplanMediaPub);
            }
            //构建返回课程媒资信息对象
            QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();
            queryResult.setList(teachplanMediaPubList);
            queryResult.setTotal(total);
            QueryResponseResult<TeachplanMediaPub> queryResponseResult = new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);

            return queryResponseResult;
        } catch (IOException e) {
            e.printStackTrace();
        }
       return new QueryResponseResult<>(CommonCode.FAIL,null);
    }
}
