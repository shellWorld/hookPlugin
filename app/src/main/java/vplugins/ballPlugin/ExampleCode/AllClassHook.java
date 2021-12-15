package vplugins.ballPlugin.ExampleCode;

import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AllClassHook implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

		Log.d("tag", "Hooking......");
		if (loadPackageParam.packageName.equals("包名")) {

			XposedHelpers.findAndHookMethod(ClassLoader.class,
					"loadClass",
					String.class, new XC_MethodHook() {
						/* access modifiers changed from: protected */
						public void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
							Class clazz = (Class) param.getResult();
							String clazzName = clazz.getName();
							Log.d("tag", "LoadClass: " + clazzName);
							if (clazzName.contains("com.xxx")) {
								Method[] mds = clazz.getDeclaredMethods();
								for (int i = 0; i < mds.length; i++) {
									final Method md = mds[i];
									int mod = mds[i].getModifiers();
									if (!Modifier.isAbstract(mod) && !Modifier.isNative(mod) && !Modifier.isInterface(mod)) {
										XposedBridge.hookMethod(mds[i], new XC_MethodHook() {
											public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
												if (md.getName().contains("complexParameterFunc")) {
													for (Object obj : param.args) {
														Log.d("tag", obj.getClass().getName());
													}
												}
											}
										});
									}
								}
							}
						}

					});
		}
	}
}
