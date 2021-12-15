package vplugins.ballPlugin.ExampleCode;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MultiDexHook implements IXposedHookLoadPackage {
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Log.d("MultiDexHook", "hooking Start...");
		if (lpparam.packageName.equals("包名")) {
			XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
				/* access modifiers changed from: protected */
				public void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					try {
						XposedHelpers.findAndHookMethod(((Context) param.args[0]).getClassLoader().loadClass("类名路径"), "方法名", String.class, String[][].class, Map.class, ArrayList.class, new XC_MethodHook() {
							/* access modifiers changed from: protected */
							public void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
								String arg0 = (String) param.args[0];
								String arg1 = (String) param.args[1];
								Log.d("tag", arg0);
								Log.d("tag", "测试分dexHook");
							}
						});
					} catch (Exception e) {
						Log.e("tag", "类没找到", e);
					}
				}
			});
		}
	}
}
