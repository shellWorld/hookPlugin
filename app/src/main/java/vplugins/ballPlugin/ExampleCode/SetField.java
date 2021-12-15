package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//修改字段例子
public class SetField implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {
			final Class clazz = XposedHelpers.findClass("类名路径", loadPackageParam.classLoader);
			XposedHelpers.setStaticIntField(clazz, "字段名", 11);
			XposedHelpers.setStaticObjectField(clazz, "字段名", "字符串");
		}
	}
}
