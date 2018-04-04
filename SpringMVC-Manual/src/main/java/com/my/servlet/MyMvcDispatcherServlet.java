package com.my.servlet;

import com.my.annotation.MyController;
import com.my.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class MyMvcDispatcherServlet extends HttpServlet {
    private Map<String, Class> scannedClasses = new HashMap<String, Class>();//存放被扫描到的类

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("URL: " + req.getRequestURL());

        methodPattern();
        resp.getWriter().write("welcome to access my web server!");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("-----------------init [MyMvcDispatcherServlet]------------");
        System.out.println(config.getInitParameter("scan-package"));
        String packageName = config.getInitParameter("scan-package");
        scanClasses(packageName);
        autoWired();
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
                    System.out.println("扫描到类：" + className);
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
            scannedClasses.put(getFirstLowerString(clazz.getSimpleName()), clazz);
        }

        Annotation controllerB = clazz.getAnnotation(MyService.class);
        if (controllerB != null) {
            String value = ((MyService) controllerB).value();
            if (value != null && !"".equals(value.trim())) {
                scannedClasses.put(value, clazz);
            } else {
                Class[] classInterfaces = clazz.getInterfaces();
                if(classInterfaces == null || classInterfaces.length < 1) {
                    scannedClasses.put(getFirstLowerString(clazz.getSimpleName()), clazz);
                }else {
                    scannedClasses.put(getFirstLowerString(classInterfaces[0].getSimpleName()), clazz);
                }
            }
        }

    }

    private void autoWired() {

    }

    private void methodPattern() {

    }

    private String getFirstLowerString(String str) {
        //byte[] chars = str.getBytes();
        char[] chars = str.toCharArray();
        if (chars[0] < 97) {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }
}
