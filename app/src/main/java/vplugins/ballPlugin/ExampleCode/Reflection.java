package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//反射例子

public class Reflection implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {

			final Class clazz = XposedHelpers.findClass("要hook函数的类名路径", loadPackageParam.classLoader);
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"要hook函数的类名路径",
					loadPackageParam.classLoader),
					"要hook的函数名",
					String.class, new XC_MethodHook() {
						/* access modifiers changed from: protected */
						public void beforeHookedMethod(MethodHookParam param) throws Throwable {

							Field field = clazz.getDeclaredField("fieldName");
							Object obj = clazz.newInstance();
							field.setAccessible(true);
							String str = (String) field.get(obj);
							Log.d("这是反射前获取的字段", str);
							field.set(obj, "newInfo");
							String str2 = (String) field.get(obj);
							Log.d("这是反射后设置的字段", str2);
							Method method = clazz.getDeclaredMethod("methodNmae");
							method.setAccessible(true);
							method.invoke(obj);
						}
					});

			Log.d("tag", clazz.getName());
			//获取所有的方法名
			for (Method method : clazz.getDeclaredMethods()) {
				Log.d("tag", method.toString());
			}

			//获取所有的字段名
			Field[] fd = clazz.getDeclaredFields();
			for (Field field : fd) {
				Log.d("tag", field.toString());
			}
			Log.d("tag", "====================================");

			//获取所有内部类名再迭代获取类的方法名与字段名
			Class[] cls = clazz.getDeclaredClasses();
			for (int i = 0; i < fd.length; i++) {
				Log.d("tag", cls[i].getName());
				for (Method method2 : cls[i].getDeclaredMethods()) {
					Log.d("tag", method2.toString());
				}
				for (Field field2 : cls[i].getDeclaredFields()) {
					Log.d("tag", field2.toString());
				}
			}
		}
	}
}
