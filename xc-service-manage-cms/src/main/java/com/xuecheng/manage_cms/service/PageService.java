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


//??????????????????
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {
        if (page <= 0) {
            page = 1;
        }
        page = page - 1;//????????????mongodb?????????????????????1
        if (size <= 0) {
            size = 10;
        }
        Pageable request = PageRequest.of(page, size);

        Page<CmsPage> all = cmsPageRepository.findAll(request);
        QueryResult<CmsPage> result = new QueryResult<CmsPage>();
        result.setList(all.getContent());//????????????
        result.setTotal(all.getTotalElements());

        return new QueryResponseResult(CommonCode.SUCCESS, result);

    }

    public QueryResponseResult findList02(int page, int size, QueryPageRequest queryPageRequest) {
        //?????????
        CmsPage cmsPage = new CmsPage();
        //??????ID
        if (StringUtils.isNotEmpty(queryPageRequest.getSiteId())) {
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        //??????ID
        if (StringUtils.isNotEmpty(queryPageRequest.getTemplateId())) {
            cmsPage.setPageTemplate(queryPageRequest.getTemplateId());
        }
        //????????????
        if (StringUtils.isNotEmpty(queryPageRequest.getPageAliase())) {
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        //???????????????????????????
        ExampleMatcher exampleMatcher = ExampleMatcher.matching();
        exampleMatcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
                //?????????????????????????????????????????????????????????????????????????????????
                // ExampleMatcher.GenericPropertyMatchers.contains() ??????
                // ExampleMatcher.GenericPropertyMatchers.startsWith() ????????????
        //??????????????????
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);

        //??????
        page = page - 1;
        //????????????
        Pageable pageable = new PageRequest(page, size);

        //????????????
        Page<CmsPage> all = cmsPageRepository.findAll(example, pageable);

        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());

        return new QueryResponseResult(CommonCode.SUCCESS, cmsPageQueryResult);

    }

    //????????????
    public CmsPageResult add(CmsPage cmsPage) {
       // ???Dao?????????  ?????????????????????Id???????????????  ??????????????????
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(
                cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath()
        );
        if (cmsPage1 != null) {
            //????????????
            //??????????????????????????????????????????
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        if (cmsPage1 == null) {
            cmsPage.setPageId(null);//???Dao??????????????????
            cmsPageRepository.save(cmsPage);//??????
            return new CmsPageResult(CommonCode.SUCCESS, cmsPage);
        }
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    //??????id??????
    public CmsPage get(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {//???false???????????????null
            return optional.get();
        }
        return null;
    }

    //????????????
    public CmsPageResult update(String id, CmsPage cmsPage) {
        //?????????????????????????????????????????????
        CmsPage byId = this.get(id);
        if (byId != null) {
            //????????????
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
        //????????????
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    //????????????
    public ResponseResult delete(String id) {
        CmsPage cmsPage = this.get(id);
        if (cmsPage != null) {
            //????????????
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    //??????id????????????????????????
    public CmsConfig getConfigById(String id) {
        Optional<CmsConfig> optional = cmsConfigRepository.findById(id);
        if (optional.isPresent()) {
            CmsConfig cmsConfig = optional.get();
            return cmsConfig;
        }
        return null;
    }


//-------------------------------------------------------------------------------------------------
    //?????????????????????
    /*
     *?????????????????????
     * ??????????????????????????????DataUrl
     * ???????????????????????????DataUrl?????????????????????
     * ??????????????????????????????????????????
     *?????????????????????
     * */



    //???????????????
    public String getPageHtml(String pageId){
        //????????????????????????
        Map modelByPageId = this.getModelByPageId(pageId);
        if (modelByPageId==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //??????????????????
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

    //?????????????????????   ??????
    public Map getModelByPageId(String pageId) {
        //?????????id???????????????????????????
        CmsPage cmsPage = this.get(pageId);
        //?????????????????????????????????????????????
        if (cmsPage == null) {
            //???????????????
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //??????dataurl???
        String dataUrl = cmsPage.getDataUrl();
        if (dataUrl == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //????????????
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        //??????dataurl???????????????????????????map??????
        Map body = forEntity.getBody();
        return body;
    }

    //??????????????????
    public String getTemplateByPageId(String pageId) {
        //??????????????????
        CmsPage cmsPage = this.get(pageId);
        if (cmsPage == null) {
            //???????????????
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }

        //????????????
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            //??????????????????
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if (optional.isPresent()) {
            CmsTemplate cmsTemplate = optional.get();
            //??????????????????id
            String templateIdFileId = cmsTemplate.getTemplateFileId();
            //??????????????????
            GridFSFile gridFSFile =
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateIdFileId)));
            //?????????????????????,??????templateIdFileId ??????files????????????
            GridFSDownloadStream gridFSDownloadStream =
                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //??????GridFsResource
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

    //?????????????????????
    public String generateHtml(String template, Map model) {
        try {
            //??????????????????
            Configuration configuration = new Configuration(Configuration.getVersion());
            //???????????????
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", template);
            //????????????????????????
            configuration.setTemplateLoader(stringTemplateLoader);
            //????????????
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

//????????????
    public ResponseResult postPage(String pageId){
        //?????????????????????
        String pageHtml = this.getPageHtml(pageId);
        //?????????????????????????????????GridFs???
        CmsPage cmsPage = saveHtml(pageId, pageHtml);
        //???MQ????????????
        sendPostPage(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //????????????????????????
    private void sendPostPage(String pageId){
        CmsPage cmsPage = this.get(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        HashMap<String, String> msgMap = new HashMap<>();
        msgMap.put("pageId",pageId);
        //????????????
        String msg = JSON.toJSONString(msgMap);
        //????????????id?????? routingKey
        String siteId = cmsPage.getSiteId();
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,siteId,msg);
    }


    //????????????????????????
    private CmsPage saveHtml(String pageId, String HtmlContent) {
        //????????????
        CmsPage cmsPage = this.get(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        ObjectId objectId=null;
        try {
            //???HtmlContent?????????????????????
            InputStream inputStream = IOUtils.toInputStream(HtmlContent, "utf-8");
            //???html?????????????????????GridFs
            objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //???html??????id?????????cmsPage???
        cmsPage.setHtmlFileId(objectId.toHexString());
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }

    //???????????????????????????????????????????????????
    public CmsPageResult save(CmsPage cmsPage) {
        CmsPage ONE = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(
                cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (ONE!=null){
            return this.update(ONE.getPageId(),cmsPage);
        }else {
            return  this.add(cmsPage);
        }
    }


    //????????????
    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        //???????????????
        CmsPageResult cmsPageResult = this.save(cmsPage);
        if (!cmsPageResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //????????????id
        CmsPage cmsPage1 = cmsPageResult.getCmsPage();
        String pageId = cmsPage1.getPageId();
        //???????????????
        ResponseResult responseResult = this.postPage(pageId);
        if (!responseResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //????????????url
        //??????url=????????????+??????webpath+??????webpath+????????????
        String siteId = cmsPage1.getSiteId();
        CmsSite cmsSite = findCmsSiteById(siteId);
        //???????????????
        String siteDomain = cmsSite.getSiteDomain();
        //??????web??????
        String siteWebPath = cmsSite.getSiteWebPath();
        //??????webpath
        String pageWebPath = cmsPage1.getPageWebPath();
        //??????web??????
        String pageName = cmsPage1.getPageName();
        //??????web????????????
        String pageUrl=siteDomain+siteWebPath+pageWebPath+pageName;
        return new CmsPostPageResult(CommonCode.SUCCESS,pageUrl);

    }
    //??????id??????????????????
    public CmsSite findCmsSiteById(String siteBy){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteBy);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }
}