package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.request.QueryPageRequest;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
public class PageService {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    CmsConfigRepository cmsConfigRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    CmsSiteRepository cmsSiteRepository;


//进行分页查询
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {
        if (page <= 0) {
            page = 1;
        }
        page = page - 1;//为了适应mongodb的接口将页码减1
        if (size <= 0) {
            size = 10;
        }
        Pageable request = PageRequest.of(page, size);

        Page<CmsPage> all = cmsPageRepository.findAll(request);
        QueryResult<CmsPage> result = new QueryResult<CmsPage>();
        result.setList(all.getContent());//数据列表
        result.setTotal(all.getTotalElements());

        return new QueryResponseResult(CommonCode.SUCCESS, result);

    }

    public QueryResponseResult findList02(int page, int size, QueryPageRequest queryPageRequest) {
        //条件值
        CmsPage cmsPage = new CmsPage();
        //站点ID
        if (StringUtils.isNotEmpty(queryPageRequest.getSiteId())) {
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        //模板ID
        if (StringUtils.isNotEmpty(queryPageRequest.getTemplateId())) {
            cmsPage.setPageTemplate(queryPageRequest.getTemplateId());
        }
        //别名查询
        if (StringUtils.isNotEmpty(queryPageRequest.getPageAliase())) {
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        //对于别名的模糊查询
        ExampleMatcher exampleMatcher = ExampleMatcher.matching();
        exampleMatcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
                //页面别名模糊查询，需要自定义字符串的匹配器实现模糊查询
                // ExampleMatcher.GenericPropertyMatchers.contains() 包含
                // ExampleMatcher.GenericPropertyMatchers.startsWith() 开头匹配
        //创建条件实例
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);

        //页码
        page = page - 1;
        //分页对象
        Pageable pageable = new PageRequest(page, size);

        //分页查询
        Page<CmsPage> all = cmsPageRepository.findAll(example, pageable);

        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());

        return new QueryResponseResult(CommonCode.SUCCESS, cmsPageQueryResult);

    }

    //添加页面
    public CmsPageResult add(CmsPage cmsPage) {
       // 从Dao中查询  页面名字、站点Id、页面路径  是否有这个值
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(
                cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath()
        );
        if (cmsPage1 != null) {
            //抛出异常
            //异常内容就是，页面已经存在了
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        if (cmsPage1 == null) {
            cmsPage.setPageId(null);//让Dao自动生成主键
            cmsPageRepository.save(cmsPage);//存储
            return new CmsPageResult(CommonCode.SUCCESS, cmsPage);
        }
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    //通过id查询
    public CmsPage get(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {//为false的话，返回null
            return optional.get();
        }
        return null;
    }

    //修改页面
    public CmsPageResult update(String id, CmsPage cmsPage) {
        //修改页面得先查询是否有这个页面
        CmsPage byId = this.get(id);
        if (byId != null) {
            //更新数据
            byId.setSiteId(cmsPage.getSiteId());
            byId.setPageName(cmsPage.getPageName());
            byId.setPageAliase(cmsPage.getPageAliase());
            byId.setPageWebPath(cmsPage.getPageWebPath());
            byId.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            byId.setPageName(cmsPage.getPageName());
            byId.setDataUrl(cmsPage.getDataUrl());
            CmsPage save = cmsPageRepository.save(byId);
            if (save != null) {
                CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, save);
                return cmsPageResult;
            }
        }
        //失败返回
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    //删除页面
    public ResponseResult delete(String id) {
        CmsPage cmsPage = this.get(id);
        if (cmsPage != null) {
            //删除页面
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    //根据id查询配置管理信息
    public CmsConfig getConfigById(String id) {
        Optional<CmsConfig> optional = cmsConfigRepository.findById(id);
        if (optional.isPresent()) {
            CmsConfig cmsConfig = optional.get();
            return cmsConfig;
        }
        return null;
    }


//-------------------------------------------------------------------------------------------------
    //页面静态化方法
    /*
     *页面静态化方法
     * 静态化程序获取页面的DataUrl
     * 静态化程序远程请求DataUrl来获取数据模型
     * 静态化程序获取页面的模板信息
     *执行页面静态化
     * */



    //页面静态化
    public String getPageHtml(String pageId){
        //获取页面模型数据
        Map modelByPageId = this.getModelByPageId(pageId);
        if (modelByPageId==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //获取页面模板
        String templateContent = this.getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(templateContent)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        String html = this.generateHtml(templateContent, modelByPageId);
        if (StringUtils.isEmpty(html)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }

    //获取页面模型的   数据
    public Map getModelByPageId(String pageId) {
        //先通过id查询这个页面的数据
        CmsPage cmsPage = this.get(pageId);
        //对查到的页面数据来判断是否有值
        if (cmsPage == null) {
            //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //取出dataurl值
        String dataUrl = cmsPage.getDataUrl();
        if (dataUrl == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //远程调用
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        //通过dataurl来获取所有的值，以map形式
        Map body = forEntity.getBody();
        return body;
    }

    //获取页面模型
    public String getTemplateByPageId(String pageId) {
        //获取页面信息
        CmsPage cmsPage = this.get(pageId);
        if (cmsPage == null) {
            //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }

        //页面模板
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            //模板页面为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if (optional.isPresent()) {
            CmsTemplate cmsTemplate = optional.get();
            //获取模板文件id
            String templateIdFileId = cmsTemplate.getTemplateFileId();
            //取出模板对象
            GridFSFile gridFSFile =
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateIdFileId)));
            //打开下载流对象,通过templateIdFileId 找到files表的数据
            GridFSDownloadStream gridFSDownloadStream =
                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //创建GridFsResource
            GridFsResource gridFsResource
                    = new GridFsResource(gridFSFile, gridFSDownloadStream);
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
                System.out.println(content);
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //执行页面静态化
    public String generateHtml(String template, Map model) {
        try {
            //生成配置文件
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", template);
            //配置值模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        }
        return null;
    }


//-----------------------------------------------------------------------------------------

//页面发布
    public ResponseResult postPage(String pageId){
        //执行页面静态化
        String pageHtml = this.getPageHtml(pageId);
        //将页面静态化文件存储到GridFs中
        CmsPage cmsPage = saveHtml(pageId, pageHtml);
        //向MQ发送消息
        sendPostPage(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //发送页面发送消息
    private void sendPostPage(String pageId){
        CmsPage cmsPage = this.get(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        HashMap<String, String> msgMap = new HashMap<>();
        msgMap.put("pageId",pageId);
        //消息内容
        String msg = JSON.toJSONString(msgMap);
        //获取站点id作为 routingKey
        String siteId = cmsPage.getSiteId();
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,siteId,msg);
    }


    //保存静态页面内容
    private CmsPage saveHtml(String pageId, String HtmlContent) {
        //查询页面
        CmsPage cmsPage = this.get(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        ObjectId objectId=null;
        try {
            //将HtmlContent内容转成输入流
            InputStream inputStream = IOUtils.toInputStream(HtmlContent, "utf-8");
            //将html文件内容保存到GridFs
            objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将html文件id更新到cmsPage中
        cmsPage.setHtmlFileId(objectId.toHexString());
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }

    //添加页面，如果已经存在了就进行更新
    public CmsPageResult save(CmsPage cmsPage) {
        CmsPage ONE = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(
                cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (ONE!=null){
            return this.update(ONE.getPageId(),cmsPage);
        }else {
            return  this.add(cmsPage);
        }
    }


    //一键发布
    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        //先进行存储
        CmsPageResult cmsPageResult = this.save(cmsPage);
        if (!cmsPageResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //得到页面id
        CmsPage cmsPage1 = cmsPageResult.getCmsPage();
        String pageId = cmsPage1.getPageId();
        //页面静态化
        ResponseResult responseResult = this.postPage(pageId);
        if (!responseResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //得到页面url
        //页面url=站点域名+站点webpath+页面webpath+页面名称
        String siteId = cmsPage1.getSiteId();
        CmsSite cmsSite = findCmsSiteById(siteId);
        //站点的域名
        String siteDomain = cmsSite.getSiteDomain();
        //站点web路径
        String siteWebPath = cmsSite.getSiteWebPath();
        //页面webpath
        String pageWebPath = cmsPage1.getPageWebPath();
        //页面web路径
        String pageName = cmsPage1.getPageName();
        //页面web访问地址
        String pageUrl=siteDomain+siteWebPath+pageWebPath+pageName;
        return new CmsPostPageResult(CommonCode.SUCCESS,pageUrl);

    }
    //根据id查询站点信息
    public CmsSite findCmsSiteById(String siteBy){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteBy);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }
}