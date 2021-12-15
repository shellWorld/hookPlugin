package vplugins.ballPlugin;

import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

class Tracer {

	private static void hookMethod(final Class clazz, final Method method) {
		XposedBridge.hookMethod(method, new XC_MethodHook() {
			final String methodNam = method.getName();
			final String classNam = clazz.getName();
			final String logstr =
					"Class: " + classNam + ",Method: " + methodNam;

			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				final StringBuilder sb = new StringBuilder(" :[");
				for (Object o : param.args) {
					String typnam = "";
					if (o != null) {
						typnam = o.getClass().getName();
						sb.append(typnam).append("@@");
					}
				}
				sb.append("]");
				Log.e("ClassTracer", logstr + sb.toString());
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
			}
		});

	}

	public static void traceClass(XC_LoadPackage.LoadPackageParam lpparam,
								  String traceClass) {
		String[] classes =  vplugins.ballPlugin.DexHelper.getClassesInDex(lpparam.classLoader);
		for (String className : classes) {
			if (className.startsWith(traceClass)) {
				try {
					final Class clazz = lpparam.classLoader.loadClass(className);
					for (final Method method : clazz.getDeclaredMethods()) {
						//过滤掉接口,抽象,Native方法
						if (Modifier.isNative(method.getModifiers()) ||
								Modifier.isAbstract(method.getModifiers())
								|| Modifier.isInterface(method.getModifiers())) {
							continue;
						}
						XposedBridge.log("MethodHook bao: " + traceClass + " " + method.getName());
						hookMethod(clazz, method);
					}
				} catch (ClassNotFoundException e) {
					XposedBridge.log(e.toString());
				}
			}
		}
	}
}
