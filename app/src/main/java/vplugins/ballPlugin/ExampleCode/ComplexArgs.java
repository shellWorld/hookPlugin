package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//复杂参数例子

public class ComplexArgs implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {
			final Class clazz = XposedHelpers.findClass("类名路径", loadPackageParam.classLoader);
			XposedHelpers.findAndHookMethod(
					clazz,
					"functionName", //函数名
					"java.lang.String", //字符串
					"[[Ljava.lang.String;", //二维字符串数组
					Map.class,
					Class.forName("java.util.ArrayList"),
					new XC_MethodHook() {
						/* access modifiers changed from: protected */
						public void beforeHookedMethod(MethodHookParam param) throws Throwable {
							Log.d("tag", "functionName is hooked before");
						}

						/* access modifiers changed from: protected */
						public void afterHookedMethod(MethodHookParam param) throws Throwable {
							Log.d("tag", "functionName is hooked after");
						}
					});
		}
	}
}
