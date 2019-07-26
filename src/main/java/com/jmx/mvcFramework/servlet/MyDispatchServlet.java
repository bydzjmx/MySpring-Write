package com.jmx.mvcFramework.servlet;

import com.jmx.mvcFramework.annotation.MyAutowired;
import com.jmx.mvcFramework.annotation.MyController;
import com.jmx.mvcFramework.annotation.MyRequestMapping;
import com.jmx.mvcFramework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 此类作为启动入口,当该servlet启动时,会调用init()方法,从init方法的参数中,获取主配置文件的路径,从而获取配置文件信息
 * 1. 加载配置文件
 * 2. 从配置文件中获取类的所在, 扫描所有相关的类
 * 3. 初始化所有扫描到的类, 添加到IOC容器中
 * 4. 依赖注入
 * 5. 构造handlerMapping
 * 6. 启动服务器, 自动接收请求并转发到该servlet进行处理
 */
public class MyDispatchServlet extends HttpServlet {
    private static final long serialVersionID = 1L;
    //设置初始参数名
    private static final String LOCATION = "contextConfigLocation";
    //保存所有配置信息
    private Properties p = new Properties();
    //保存所有扫描到的类名
    private List<String> classNames = new ArrayList<>();
    //定义核心IOC容器, 保存所有初始化的bean
    private Map<String,Object> ioc = new HashMap<>();
    //保存所有url和方法的映射关系,可以根据url定位使用的方法
    private Map<String, Method> handlerMapping = new HashMap<>();

    public MyDispatchServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //处理用户的请求
        try {
            dispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Details:\r\n" + Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]","")
                    .replaceAll(",\\s","\r\n"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //处理用户的请求
        try {
            dispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Details:\r\n" + Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]","")
            .replaceAll(",\\s","\r\n"));
        }
    }

    /**
     * 初始化, 加载业务处理
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("My Spring is init111");
        //1. 加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));
        //2. 扫描所有相关的类
        doScanner(p.getProperty("scanPackage"));
        //3. 初始化所有相关类的实例, 保存到IOC容器中
        doInstance();
        System.out.println("My Spring is init22");
        //4. 依赖注入
        doAutowired();
        //5. 构造handlerMapping
        initHandlerMapping();
        //6. 等待请求, 匹配请求的url, 根据关系定位方法, 反射调用执行方法(doGet或者doPost方法)
        System.out.println("My Spring is init");
    }

    /**
     * 根据url调用相应method进行处理
     * @param req
     * @param resp
     */
    private void dispatch(HttpServletRequest req, HttpServletResponse resp) {
        if(this.handlerMapping.isEmpty()){
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        System.out.println("接收到的请求的url是: "+url);
        System.out.println("接收到的请求的contextPath是: "+contextPath);
        url = url.replace(contextPath,"").replaceAll("/+","/");
        System.out.println("修改后的url是:"+url);

        //如果没有查找到匹配的处理器,则返回
        if (!handlerMapping.containsKey(url)) {
            try {
                resp.getWriter().write("404 NOT FOUND");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        //存储请求的参数列表
        Map<String, String[]> paramsMap = req.getParameterMap();
        //根据url,获取controller中对应的method
        Method method = this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //保存方法的参数值
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称,做处理
            Class parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class){
                //是request对象,与传入参数的request做对应
                paramValues[i] = req;
                continue;
            }else if(parameterType == HttpServletResponse.class){
                //参数是response对象
                paramValues[i] = resp;
                continue;
            }else if(parameterType == String.class){
                //如果是String类型的参数,从请求中取出参数的值并赋值给变量进行存储
                for (Map.Entry<String, String[]> param : paramsMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|]]", "")
                            .replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }
        //反射机制调用方法
        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        try {
            method.invoke(this.ioc.get(beanName),paramValues);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将RequestMapping中配置的url和method进行关联,保存相关关系
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            //如果类带有@controller注解,获取requestMapping的值
            if(!clazz.isAnnotationPresent(MyController.class)){
                continue;
            }
            String baseUrl = "";
            //获取controller的url配置(元位置)
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                //获取注解对象
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                //获取@MyRequestMapping的值
                baseUrl = requestMapping.value();
            }

            //获取类的方法的的url配置
            Method[] methods = clazz.getMethods();
            //处理所有方法
            for (Method method : methods) {
                //没有添加RequestMapping注解的方法直接忽略
                if(!method.isAnnotationPresent(MyRequestMapping.class)){
                    continue;
                }
                //对于有requestMapping的注解,映射其url
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = (baseUrl+requestMapping.value().replaceAll("/+","/"));
                //加入到handlerMapping中
                handlerMapping.put(url,method);
            }
        }
        System.out.println("initHandlerMapping完成."+handlerMapping.toString());
    }

    /**
     * 将IOC容器中的类,需要赋值的字段进行赋值
     */
    private void doAutowired() {
        if(ioc.isEmpty()){
            return;
        }
        //将ioc中的类初始化,key为beanName,value为包的全限定名
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到实例对象中的所有字段,getFields()获取包括父类的字段,getDeclaredFields(),获取不包括父类的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            //处理字段,需要赋值的进行赋值
            for (Field field : fields) {
                //非autowired类型字段,不处理
                if(!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                // 如果这个字段是私有字段的话,那么,要强制访问
                field.setAccessible(true);
                try {
                    //属性赋值
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
        //自动注入
        System.out.println("自动注入完成");
    }

    /**
     * 初始化所有的类,并放入IOC容器中.IOC容器默认key为类名首字母小写, 除非自己设置类名.
     * 因此, 需要定义一个针对类名首字母的处理工具方法
     */
    private void doInstance() {
        System.out.println("开始初始化类");
        //1. 从classNames中取出所有的类
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String className : classNames) {
                //反射调用
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    //将首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    //将类名作为key,实例作为value加入ioc容器中
                    ioc.put(beanName,clazz.newInstance());
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    //获取service注解对象
                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    //如果用户设置了名字,则以该名字为beanName
                    if("".equals(beanName.trim())){
                        ioc.put(beanName,clazz.newInstance());
                        continue;
                    }
                    //如果用户没有设置名字,以该接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(), clazz.newInstance());
                    }
                    continue;
                }else {
                    continue;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        System.out.println("初始化类完成,ioc容器为:"+ioc.toString());
    }

    //将类名的首字母进行小写化
    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        //asc码加32,表示转换为小写
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 递归扫描包下的所有class文件
     * 此处packageName为com.jmx.demo
     */
    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            System.out.println(file.toString());
            //如果是文件夹,继续递归扫描
            if (file.isDirectory()) {
                doScanner(packageName+"."+file.getName());
            }else {
                //把扫描到的包放入classNames中,去掉class后缀
                classNames.add(packageName+"."+file.getName().replace(".class","").trim());
            }
        }
        System.out.println("包扫描完成"+classNames.toString());
    }

    /**
     * 加载配置文件
     * @param initParameter
     */
    private void doLoadConfig(String initParameter) {
        InputStream inputStream = null;
        try {
            inputStream = this.getClass().getClassLoader().getResourceAsStream(initParameter);
            //读取配置文件
            p.load(inputStream);
            System.out.println("加载配置文件完成,配置文件为"+p.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关闭流
            if(inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
