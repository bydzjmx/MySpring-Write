package com.jmx.demo.action;

import com.jmx.demo.service.DemoService;
import com.jmx.mvcFramework.annotation.MyAutowired;
import com.jmx.mvcFramework.annotation.MyController;
import com.jmx.mvcFramework.annotation.MyRequestMapping;
import com.jmx.mvcFramework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 使用自定义的注解进行配置
 */
@MyController
@MyRequestMapping("/demo")
public class DemoAction {
    @MyAutowired
    private DemoService demoService;

    /**
     * 查询
     * @param request
     * @param response
     * @param name
     */
    @MyRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @MyRequestParam("name") String name){
        String result = demoService.query(name);
        //将查询结果输出回前端
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @MyRequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response,
                      @MyRequestParam("a") String a,@MyRequestParam("b")String b){
        try {
            response.getWriter().write(a+"+"+b+"="+(a+b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @MyRequestMapping("/remove")
    public void remove(HttpServletRequest request, HttpServletResponse response,
                      @MyRequestParam("id") String id){
        try {
            response.getWriter().write("remove the item of id:"+id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
