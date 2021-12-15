package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//自定义参数例子

public class CustomizeArgs implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {

			final Class clazz =
					XposedHelpers.findClass("要hook函数的类名路径", loadPackageParam.classLoader);
			Class cls = Class.forName("自定义类名路径",
					true,
					loadPackageParam.classLoader);

			XposedHelpers.findAndHookMethod(

					clazz,
					"functionName", //要hook的函数名
					"类名路径", //自定义的类名 或者 cls
					String.class, //字符串参数

					new XC_MethodHook() {
						/* access modifiers changed from: protected */
						public void beforeHookedMethod(MethodHookParam param) throws Throwable {
							Log.d("tag", "这是自定义类参数的Hook Class.forName");
						}
					});
		}
	}
}
