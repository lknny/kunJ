

package kunj.com.analyze;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import kunj.com.annotation.Access;
import kunj.com.annotation.Inject;
import kunj.com.annotation.Service;

public class ClassAnalyzer {
	private static final String CLASS_DIRECTORY_PATH = "bin";
	private static final String BASE_CLASS_FILE_PATH = System.getProperty("user.dir") + File.separator
			+ CLASS_DIRECTORY_PATH;
	private static final String POINT = ".";
	private static Map<String, Object> bean ;
	private static AtomicBoolean isInit=new AtomicBoolean(false);
	

	public static void inject() {
		List<File> files = new ArrayList<>();
		Set<Class<?>> classes = getClasses(getAllClassFiles(BASE_CLASS_FILE_PATH, files));
		Map<String, Object> injectObj = null;
		try {
			injectObj = getInjectObjects(classes);
			objInject(injectObj);
			bean=injectObj;
			startAccess(injectObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		isInit.set(true);
	}

	private static void startAccess(Map<String, Object> injectObj) throws Exception {
		for (Map.Entry<String, Object> element : injectObj.entrySet()) {
			Method[] methods = element.getValue().getClass().getMethods();
			for (Method method : methods) {
				if (method.getAnnotation(Access.class) != null) {
					method.invoke(element.getValue());
				}
			}
		}
	}

	private static void objInject(Map<String, Object> injectObj) throws Exception {
		for (Map.Entry<String, Object> element : injectObj.entrySet()) {
			Field[] fields = element.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if (field.getAnnotation(Inject.class) != null) {
					field.setAccessible(true);
					field.set(element.getValue(), injectObj.get(field.getType().getName()));
				}
			}
		}
	}

	private static List<File> getAllClassFiles(String path, List<File> classFiles) {
		File rootFile = new File(path);
		File[] files = rootFile.listFiles();
		if (null == files || 0 == files.length) {
			return classFiles;
		}
		getClassFiles(files, classFiles);
		return classFiles;

	}

	private static void getClassFiles(File[] files, List<File> classFiles) {
		for (File file : files) {
			if (file.isDirectory()) {
				getAllClassFiles(file.getAbsolutePath(), classFiles);
			}
			if (file.getName().endsWith(".class")) {
				classFiles.add(file);
			}
		}
	}

	private static Set<Class<?>> getClasses(List<File> classFiles) {
		Set<Class<?>> classes = new HashSet<>();
		for (File file : classFiles) {
			try {
				String className = file.getPath()
						.substring(file.getPath().indexOf(CLASS_DIRECTORY_PATH) + 4, file.getPath().indexOf(".class"))
						.replace(File.separator, POINT).replace(".class", "");
				classes.add(Class.forName(className));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return classes;
	}

	private static Map<String, Object> getInjectObjects(Set<Class<?>> classes) throws Exception {
		Map<String, Object> objects = new HashMap<>();
		for (Class<?> clz : classes) {
			if (clz.getAnnotation(Service.class) == null) {
				continue;
			}
			if (!hasDefaultConstructor(clz)) {
				continue;
			}
			generateInjectObj(clz, objects);
		}
		return objects;
	}

	private static void generateInjectObj(Class<?> clz, Map<String, Object> objects) throws Exception {
		Object object = clz.newInstance();
		// 接口作为key，存入map
		if (clz.getInterfaces().length > 0) {
			for (Class<?> c : clz.getInterfaces()) {
				objects.put(c.getName(), object);
			}
		}
		// 类名作为key，存入map
		objects.put(clz.getName(), object);

	}

	private static boolean hasDefaultConstructor(Class<?> clz) {
		Constructor<?>[] constructors = clz.getDeclaredConstructors();
		for (Constructor<?> constructor : constructors) {
			if (constructor.getParameterCount() == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 重新注入 @throws Exception @throws
	 */
	public static void reInject(Class<?> clz, Object... objects) throws Exception {
		if (!isInit.get()) {
			inject();
		}
		for (Object object : objects) {
			Class<?>[] inf = object.getClass().getInterfaces();
			for (Entry<String, Object> element : bean.entrySet()) {
				// 待注入的类正好是此对象，直接替换，默认object中无注入，或有注入并已经注入
				if (isEquals(inf, element.getValue().getClass().getInterfaces())) {
					bean.put(element.getKey(), object);
					continue;
				}
				// iocbean中有对象含有此对象，则注入
				Field[] fields = element.getValue().getClass().getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					Field f = fields[i];
					// iocbean中对象含有此成员变量，并且成员变量和入参object同接口类型，则更改成员变量
					if (f.getAnnotation(Inject.class) != null && isEquals(inf, new Class<?>[] { f.getType() })) {
						f.setAccessible(true);
						f.set(element.getValue(), object);
					}
				}
			}

		}
		reStart();
	}

	private static void reStart() {
		try {
			startAccess(bean);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean isEquals(Class<?>[] clz1, Class<?>[] clz2) {
		for (Class<?> class1 : clz1) {
			for (Class<?> class2 : clz2) {
				if (class1.getName().equals(class2.getName())) {
					return true;
				}
			}
		}
		return false;
	}
}