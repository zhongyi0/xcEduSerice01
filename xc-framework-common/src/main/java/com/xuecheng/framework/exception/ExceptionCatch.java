package com.xuecheng.framework.exception;

import com.google.common.collect.ImmutableMap;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.model.response.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

//捕获异常
@ControllerAdvice//控制器增强
public class ExceptionCatch {
    //打印日志信息
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatch.class);

    //使用EXCEPTIONS存放异常类和错误代码的映射，ImmutableMap的特点一旦创建不可改变，并且线程安全
    private static ImmutableMap<Class<? extends Throwable>,ResultCode>EXCEPTIONS;
    //使用builder来构建一个异常类型和错误代码的异常
    protected static ImmutableMap.Builder<Class<? extends Throwable>,ResultCode> builder=
            ImmutableMap.builder();
    //捕获CustomException异常

    @ExceptionHandler(CustomException.class)//增强异常处理
    @ResponseBody//转成Json格式，不然直接404
    public ResponseResult customException(CustomException customException){
        //记录日志
        LOGGER.error("catch exception:{}", customException.getMessage());
        ResultCode resultCode = customException.getResultCode();
        ResponseResult responseResult = new ResponseResult(resultCode);
        return responseResult;
    }
    @ExceptionHandler(Exception.class)//增强异常处理
    @ResponseBody//转成Json格式，不然直接404
    public ResponseResult exception(Exception e){
        //记录日志
        LOGGER.error("catch exception:{}", e.getMessage());
        if (EXCEPTIONS==null){
            EXCEPTIONS=builder.build();//EXCEPTIONS构建成功
        }
        //从EXCEPTIONS中找异常类型对应的错误代码，如果找到了将错误代码响应给用户，如果找不到就响应给用户9999异常
        ResultCode resultCode = EXCEPTIONS.get(e.getClass());
        if (resultCode!=null) {
            return new ResponseResult(resultCode);
        }else {
            return new ResponseResult(CommonCode.SERVER_ERROR);
        }
    }
    static {
        builder.put(HttpMessageNotReadableException.class, CommonCode.INVALIDPARAM);
    }

}
