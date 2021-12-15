package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//hook无参构造函数

public class ConstructorHook implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {
			final Class clazz = XposedHelpers.findClass("类名路径", loadPackageParam.classLoader);
			XposedHelpers.findAndHookConstructor(clazz, new XC_MethodHook() {
				/* access modifiers changed from: protected */
				public void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Log.d("tag", "这是无参构造函数前");
				}

				/* access modifiers changed from: protected */
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					Log.d("tag", "这是无参构造函数后");
				}
			});
		}
	}
}
