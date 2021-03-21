package com.xuecheng.auth.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.exception.ExceptionCast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;



    //认证方法
    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        //申请令牌
        AuthToken authToken = applyToken(username, password, clientId, clientSecret);
        if (authToken ==null){
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }
        //用户身份令牌
        String access_token = authToken.getAccess_token();
        //存储到redis中的内容
        String jsonString = JSON.toJSONString(authToken);
        //将令牌存储到redis
        boolean result = this.saveToken(access_token, jsonString, tokenValiditySeconds);
        if (!result) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_TOKEN_SAVEFAIL);
        }
        return authToken;
    }
    /**
     *
     * @param access_token 用户身份令牌
     * @param content  内容就是AuthToken对象的内容
     * @param ttl 过期时间
     * @return
     */
    //存储到令牌到redis
    private boolean saveToken(String access_token,String content,long ttl){
        String key = "user_token:" + access_token;
        stringRedisTemplate.boundValueOps(key).set(content,ttl, TimeUnit.SECONDS);
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire>0;
    }
    //删除redis中令牌,token
    public boolean deleteToken(String token){
        String key = "user_token:" + token;
        Boolean delete = stringRedisTemplate.delete(key);
        //可能redis中令牌已经过期了，自动没有了
//        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return true;
    }
    //认证方法
    private AuthToken applyToken(String username, String password, String clientId, String clientSecret) {
        //采取客户端负载均衡，从euraka中获取认证服务的ip和端口号(因为spring security在认证服务中)
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        //此地址就是http://ip:port
        URI uri = serviceInstance.getUri();
        //令牌申请地址为：http://localhost:40400/auth/oauth/token
        String authUrl=uri+"/auth/oauth/token";

        //定义Header
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        //"Basic WGNXZWJBcHA6WGNXZWJBcHA="
        String httpbasic = httpbasic(clientId, clientSecret);

        headers.add("Authorization", httpbasic);

        //2.包括了 grant_type、username、password、
        LinkedMultiValueMap<String, String> boby = new LinkedMultiValueMap<>();
        boby.add("grant_type","password");
        boby.add("username",username);
        boby.add("password",password);

        HttpEntity<MultiValueMap<String, String>> multiValueMapHttpEntity =
                new HttpEntity<>(boby, headers);
        //指定restTemplate当遇到400或者401响应时候也不要抛出异常，正常返回值
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response) throws IOException{
                if (response.getRawStatusCode()!=400&&response.getRawStatusCode()!=401){
                    super.handleError(response);
                }
            }
        });


        //远程调用申请令牌
        ResponseEntity<Map> exchange = restTemplate.exchange(authUrl, HttpMethod.POST, multiValueMapHttpEntity, Map.class);

        Map body1 = exchange.getBody();


        if (StringUtils.isEmpty(body1)){
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }
        System.out.println(body1);

        if (body1 == null || body1.get("access_token")==null
                || body1.get("refresh_token")==null|| body1.get("jti")== null){
                //jti是jwt令牌的唯一标识作为用户身份令牌
            String error_description = (String) body1.get("error_description");

            if (org.apache.commons.lang.StringUtils.isNotEmpty(error_description)){
                if (error_description.equals("坏的凭证")){
                    ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
                }else if (error_description.indexOf("UserDetailsService returned null")>=0){
                    ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
                }
            }
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }
        AuthToken authToken = new AuthToken();
        authToken.setAccess_token((String) body1.get("jti"));//用户身份令牌
        authToken.setRefresh_token((String) body1.get("refresh_token"));//刷新令牌
        authToken.setJwt_token((String) body1.get("access_token"));//jwt令牌
        return authToken;
    }

    //获取httpbasic的串
    private String httpbasic(String clientId,String clientSecret){
        //将客户端的Id和客户端密码拼接，按“客户端id：客户端密码”
        String string = clientId + ":" + clientSecret;
        //进行base64位进行编码
        byte[] encode = Base64Utils.encode(string.getBytes());
        return "Basic "+new String(encode);
    }

    //从redis中查询令牌
    public AuthToken getUserToken(String access_token) {
        String userToken="user_token:"+access_token;
        String userTokenString = stringRedisTemplate.opsForValue().get(userToken);
        if (userToken!=null){
            AuthToken authToken=null;
            try {
                authToken=JSON.parseObject(userTokenString,AuthToken.class);

            } catch (Exception e) {
                e.printStackTrace();

            }
            return authToken;
        }
        return null;
    }
}
