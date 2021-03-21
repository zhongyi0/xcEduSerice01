package com.xuecheng.auth;


import com.xuecheng.framework.client.XcServiceList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class Test03 {
    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    RestTemplate restTemplate;

    @Test
    public void testClient(){
        //采取客户端负载均衡，从euraka中获取认证服务的ip和端口号(因为spring security在认证服务中)
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        //此地址就是http://ip:port
        URI uri = serviceInstance.getUri();
        //令牌申请地址为：http://localhost:40400/auth/oauth/token
        String authUrl=uri+"/auth/oauth/token";

        //定义Header
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        //"Basic WGNXZWJBcHA6WGNXZWJBcHA="
        String httpbasic = httpbasic("XcWebApp", "XcWebApp");

        headers.add("Authorization", httpbasic);

        //2.包括了 grant_type、username、password、
        LinkedMultiValueMap<String, String> boby = new LinkedMultiValueMap<>();
        boby.add("grant_type","password");
        boby.add("username","itcast");
        boby.add("password","123");

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
        System.out.println(body1);
    }

    //获取httpbasic的串
    private String httpbasic(String clientId,String clientSecret){
        //将客户端的Id和客户端密码拼接，按“客户端id：客户端密码”
        String string = clientId + ":" + clientSecret;
        //进行base64位进行编码
        byte[] encode = Base64Utils.encode(string.getBytes());
        return "Basic "+new String(encode);
    }

}
