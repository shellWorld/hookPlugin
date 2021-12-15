package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


//替换方法例子

public class ReplaceMethod implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {

			final Class clazz =
					XposedHelpers.findClass("要hook函数的类名路径", loadPackageParam.classLoader);
			XposedHelpers.findAndHookMethod(clazz, "要替换的函数名",
					new XC_MethodReplacement() {
						/* access modifiers changed from: protected */
						public Object replaceHookedMethod(MethodHookParam methodHookParam)
								throws Throwable {
							Log.d("tag", "这是替换之后的输出");
							return null;
						}
					});
		}
	}
}
