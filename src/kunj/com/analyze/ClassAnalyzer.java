package kunj.com.analyze;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import kunj.com.annotation.Access;
import kunj.com.annotation.Inject;
import kunj.com.annotation.Service;

public class ClassAnalyzer {
    public static final String BASE_CLASS_PACKAGE_NAME = "kunj.com.app";
    public static final String BASE_CLASS_PACKAGE_FILE_PATH = "bin/kunj/com/app/";
        
        
        public  static Map<String, Object> inject() {
            Set<Class<?>> result = new HashSet<>();
            findAndAddClassesInPackageByFile(BASE_CLASS_PACKAGE_NAME, BASE_CLASS_PACKAGE_FILE_PATH, true, result);
            Map<String, Object> resultMap = filterClass(result);
            injectChildren(resultMap);
            return resultMap;
        }

        private static void injectChildren(Map<String, Object> resultMap) {
            for (Map.Entry<String, Object> it : resultMap.entrySet()) {
                try {
                    // 如果要是接口的话略过
                    if (Class.forName(it.getKey()).isInterface()) {
                        continue;
                    }
                    Field[] fields = it.getValue().getClass().getDeclaredFields();
                    for (Field its : fields) {
                        if (its.getAnnotation(Inject.class) != null) {
                            its.setAccessible(true);
                            its.set(it.getValue(), resultMap.get(its.getType().getName()));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            for (Map.Entry<String, Object> it : resultMap.entrySet()) {
                try {
                    if (Class.forName(it.getKey()).isInterface()) {
                        continue;
                    }
                    Method[] methods=it.getValue().getClass().getMethods();
                    for (Method method : methods) {
                        if (method.getAnnotation(Access.class)!=null) {
                            method.invoke(it.getValue());
                        }
                    }
}
                 catch (ClassNotFoundException | SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        }
        private static void findAndAddClassesInPackageByFile(String packageName, String packagePath,
                final boolean recursive, Set<Class<?>> classes) {
            File dir = new File(packagePath);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }
            File[] dirfiles = dir
                    .listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class")));
            for (File file : dirfiles) {
                dealDirFile(packageName, recursive, classes, file);
            }
        }

        private static void dealDirFile(String packageName, final boolean recursive, Set<Class<?>> classes, File file) {
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        private static Map<String, Object> filterClass(Set<Class<?>> set) {
            Map<String, Object> result = new HashMap<String, Object>();
            set.forEach((clazz) -> {
                Service s = clazz.getAnnotation(Service.class);
                if (s != null) {
                    try {
                        Constructor<?> constructor[] = clazz.getDeclaredConstructors();
                        boolean isCanNew = false;
                        for (Constructor<?> c : constructor) {
                            // 有默认构造器
                            if (c.getParameterCount() == 0) {
                                isCanNew = true;
                                break;
                            }
                        }

                        if (isCanNew) {
                            Object obj = clazz.newInstance();
                            result.put(clazz.getName(), obj);
                            Class<?>[] classes = clazz.getInterfaces();
                            for (int i = 0; i < classes.length; i++) {
                                Class<?> cl = classes[i];
                                result.put(cl.getName(), obj);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
            return result;
        }
}
