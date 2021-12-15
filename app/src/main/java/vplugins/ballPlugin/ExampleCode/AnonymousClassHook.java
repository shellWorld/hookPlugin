package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AnonymousClassHook implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {

			final Class clazz =
					XposedHelpers.findClass("要hook函数的类名路径", loadPackageParam.classLoader);
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"要hook函数的类名路径$1", //匿名类用数字(查阅smali)表示, 例: new Demo()无变量赋值操作
					loadPackageParam.classLoader),
					"内部类的函数名",
					String.class, new XC_MethodHook() {
						/* access modifiers changed from: protected */
						public void beforeHookedMethod(MethodHookParam param) throws Throwable {

						}
					});
		}
	}
}