package com.my.servlet;

import com.my.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class MyMvcDispatcherServlet extends HttpServlet {
    private Map<String, Object> scannedClasses = new HashMap<String, Object>();//存放被扫描到的类
    private Map<String, MatchMethod> methodMap = new HashMap<String, MatchMethod>();//存放被匹配到的方法

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("URL: " + req.getRequestURL());
        String requestUrl = req.getRequestURI().replace(req.getContextPath(), "");
        System.out.println("requestUrl: " + requestUrl);
        MatchMethod matchMethod = methodMap.get(requestUrl);
        if (matchMethod == null) {
            System.out.println("not match request url: " + requestUrl);
            resp.setStatus(404);
            resp.getWriter().write("404");
            return;
        }

        try {

            Object[] objects = null;
            if (matchMethod.params.size() > 0) {
                objects = new Object[matchMethod.params.size()];
                int i = 0;

                for (Map.Entry<String, Class> param : matchMethod.params.entrySet()) {
                    String paramValue = req.getParameter(param.getKey());
                    if (paramValue == null) {
                        resp.getWriter().write(param.getKey() + "不能为空");
                        return;
                    }
                    objects[i++] = paramValue;
                }
            }
            matchMethod.method.invoke(matchMethod.controller, objects);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        resp.getWriter().write("access success!");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("-----------------init [MyMvcDispatcherServlet]------------");
        System.out.println(config.getInitParameter("scan-package"));
        String packageName = config.getInitParameter("scan-package");
        scanClasses(packageName);
        autoWired();
        methodPattern();
        System.out.println("-----------------init [MyMvcDispatcherServlet] success------------");

    }

    private void scanClasses(String packageName) {
        URL ur = this.getClass().getClassLoader().getResource(packageName.replace(".", "/"));
        File file = new File(ur.getFile());
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file1.isDirectory()) {
                scanClasses(packageName.replace(".", "/") + "/" + file1.getName());
            } else {
                Class clazz = null;
                String className = file1.getName().replace(".class", "");
                try {
                    clazz = Class.forName(packageName.replace("/", ".") + "." + className);
                    registerBean(clazz);
                } catch (ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                    System.out.println("加载类：没找到类" + className);
                }

            }
        }

    }

    private void registerBean(Class clazz) {

        Annotation controllerA = clazz.getAnnotation(MyController.class);
        if (controllerA != null) {
            try {
                Object o = clazz.newInstance();
                scannedClasses.put(getFirstLowerString(clazz.getSimpleName()), o);
                System.out.println("成功注册：" + clazz.getSimpleName());

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        Annotation serviceB = clazz.getAnnotation(MyService.class);
        if (serviceB != null) {
            try {
                Object o = clazz.newInstance();
                String value = ((MyService) serviceB).value();
                String autoWiredBeanName = null;
                if (value != null && !"".equals(value.trim())) {
                    autoWiredBeanName = value;
                } else {
                    Class[] classInterfaces = clazz.getInterfaces();
                    if (classInterfaces == null || classInterfaces.length < 1) {
                        autoWiredBeanName = getFirstLowerString(clazz.getSimpleName());
                    } else {
                        autoWiredBeanName = getFirstLowerString(classInterfaces[0].getSimpleName());
                    }
                }
                scannedClasses.put(autoWiredBeanName, o);
                System.out.println("成功注册：" + clazz.getSimpleName());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }

    private void autoWired() {
        for (Map.Entry<String, Object> map : scannedClasses.entrySet()) {
            Field[] fields = map.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                Annotation myAutoWired = field.getAnnotation(MyAutoWired.class);
                if (myAutoWired != null) {
                    String value = ((MyAutoWired) myAutoWired).value();
                    String autoWiredBeanName = null;
                    if (value != null && !"".equals(value.trim())) {
                        autoWiredBeanName = value;
                    } else {
                        autoWiredBeanName = getFirstLowerString(field.getType().getSimpleName());
                    }
                    Object autoWiredBean = scannedClasses.get(autoWiredBeanName);
                    if (autoWiredBean == null) {
                        System.out.println("not found autowired bean：" + autoWiredBeanName);
                        continue;
                    } else {
                        field.setAccessible(true);
                        try {
                            field.set(map.getValue(), autoWiredBean);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void methodPattern() {
        for (Map.Entry<String, Object> map : scannedClasses.entrySet()) {
            if (map.getValue().getClass().getAnnotation(MyController.class) == null ||
                    map.getValue().getClass().getAnnotation(MyRequestMapping.class) == null) {
                continue;
            }

            Annotation firstUri = map.getValue().getClass().getAnnotation(MyRequestMapping.class);
            String firstUriValue = ((MyRequestMapping) firstUri).value();
            if (firstUriValue == null || "".equals(firstUriValue.trim())) {
                continue;
            }

            for (Method method : map.getValue().getClass().getMethods()) {
                Annotation secondUri = method.getAnnotation(MyRequestMapping.class);
                if (secondUri == null) {
                    continue;
                }
                String secondUriValue = ((MyRequestMapping) secondUri).value();
                if (secondUriValue == null || "".equals(secondUriValue.trim())) {
                    continue;
                }

                MatchMethod matchMethod = new MatchMethod(map.getValue(), method);
                Map<String, Class> params = new LinkedHashMap<String, Class>();
                matchMethod.params = params;
                //获取到参数
                Parameter[] parameters = method.getParameters();
                for (Parameter parameter : parameters) {
                    MyRequestParam requestParamAnnotation = parameter.getAnnotation(MyRequestParam.class);
                    if (requestParamAnnotation == null) {
                        continue;
                    }

                    params.put(requestParamAnnotation.value(), parameter.getType());
                    System.out.println(firstUriValue + secondUriValue + "成功匹配到注解参数：" + requestParamAnnotation.value());
                }

                methodMap.put(firstUriValue + secondUriValue, matchMethod);
            }

        }


    }

    private String getFirstLowerString(String str) {
        char[] chars = str.toCharArray();
        if (chars[0] < 97) {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    private class MatchMethod {
        protected Object controller;
        protected Method method;
        protected Map<String, Class> params;//存放参数名和参数值的集合

        public MatchMethod(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }

}
