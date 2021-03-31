package com.lion.utility.http.httpmonitor.server;

import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AccessFlag;
import com.lion.utility.tool.code.HashLIB;
import com.lion.utility.tool.common.PackageLIB;
import com.lion.utility.tool.log.LogLIB;
import com.lion.utility.http.httpmonitor.tool.CommonLIB;
import com.lion.utility.http.rpchttp.constant.Constant;

/**
 * 字节码方式修改方法
 * 
 * @author lion
 */
public class BytecodeMethod {
	private BytecodeMethod() {
	}

	/**
	 * 添加监控代码
	 * 
	 * @param classNameSet 需要监控的类名列表
	 * @throws Exception 异常
	 */
	public static void addMonitorCode(Set<String> classNameSet) throws Exception {
		// 用于日志输出
		Set<String> classNameFinalSet = new HashSet<>();
		// 父类名
		Set<String> superClassNameSet = new HashSet<>();

		// 修改指定方法
		for (String className : classNameSet) {
			CtClass mCtc = HttpServerMonitor.classPool.get(className);
			// 过滤接口
			if (mCtc.isInterface()) {
				continue;
			}

			// 获取父类
			CtClass supermCtc = mCtc.getSuperclass();
			if (supermCtc != null) {
				superClassNameSet.add(supermCtc.getName());
			}
			// 过滤父类
			if (superClassNameSet.contains(className)) {
				continue;
			}

			for (CtMethod method : mCtc.getDeclaredMethods()) {
				// 只对public方法进行修改
				if (AccessFlag.isPublic(method.getMethodInfo().getAccessFlags())) {
					try {
						String methodId = BytecodeMethod.getMethodId(method);
						String methodCode = mCtc.getName() + "." + method.getName();
						// 初始化监控统计数据
						HttpServerMonitor.initMonotorStat(methodId, methodCode);

						// 加入监控代码
						method.addLocalVariable("bc_startTime", CtClass.longType);
						method.insertBefore("bc_startTime = System.currentTimeMillis();" + System.getProperty("line.separator"));
						method.insertAfter(com.lion.utility.http.httpmonitor.server.HttpServerMonitor.class.getName() + ".monitor(\"" + methodId + "\", true, bc_startTime);" + System.getProperty("line.separator"), false);
						method.addCatch("{" + com.lion.utility.http.httpmonitor.server.HttpServerMonitor.class.getName() + ".monitor(\"" + methodId + "\", false, 0L); " + System.getProperty("line.separator") + "throw $e;" + System.getProperty("line.separator") + "}", ClassPool.getDefault().get("java.lang.Exception"));
					} catch (Exception e) {
						throw new Exception("addMonitorCode exception, " + method.getLongName(), e);
					}
				}
			}

			classNameFinalSet.add(className);

			// 字节码生成
			Class<?> cls = mCtc.toClass();
			// 对象释放
			mCtc.detach();

			// debug模式，则记录生成的字节码
			if (HttpServerMonitor.isDebug) {
				CommonLIB.saveBytecodeFile(cls.getName(), mCtc.toBytecode());
			}
		}

		LogLIB.info("addMonitorCode complete, total:" + classNameFinalSet.size() + ", " + classNameFinalSet);
	}

	/**
	 * 合并监控包和类
	 * 
	 * @param includeMonitorPackageNameSet 需要监控的包名列表（会自动将包下属所有类及方法加入监控）
	 * @param includeMonitorClassNameSet   需要监控的类名列表（会自动将类下属所有方法加入监控）
	 * @param excludeMonitorPackageNameSet 不需要监控的包名列表（优先级高于include）
	 * @param excludeMonitorClassNameSet   不需要监控的类名列表（优先级高于include）
	 * @return 结果
	 * @throws Exception 异常
	 */
	public static Set<String> merge(
			Set<String> includeMonitorPackageNameSet,
			Set<String> includeMonitorClassNameSet,
			Set<String> excludeMonitorPackageNameSet,
			Set<String> excludeMonitorClassNameSet) throws Exception {
		// 合并include包和类
		Set<String> includeClassNameTempSet = new HashSet<>();
		if (includeMonitorPackageNameSet != null) {
			for (String packageName : includeMonitorPackageNameSet) {
				Set<String> classNameSet = PackageLIB.getClassNameSet(packageName);
				includeClassNameTempSet.addAll(classNameSet);
			}
		}
		if (includeMonitorClassNameSet != null) {
			includeClassNameTempSet.addAll(includeMonitorClassNameSet);
		}

		// 合并exclude包和类
		Set<String> excludeClassNameTempSet = new HashSet<>();
		if (excludeMonitorPackageNameSet != null) {
			for (String packageName : excludeMonitorPackageNameSet) {
				Set<String> classNameSet = PackageLIB.getClassNameSet(packageName);
				excludeClassNameTempSet.addAll(classNameSet);
			}
		}
		if (excludeMonitorClassNameSet != null) {
			excludeClassNameTempSet.addAll(excludeMonitorClassNameSet);
		}

		// 合并include类和exclude类
		Set<String> classNameFinalSet = new HashSet<>();
		for (String includeClassName : includeClassNameTempSet) {
			if (!excludeClassNameTempSet.contains(includeClassName)) {
				classNameFinalSet.add(includeClassName);
			}
		}

		return classNameFinalSet;
	}

	/**
	 * 获取methodkey的key，做哈希
	 * 
	 * @param methodKey 方法key
	 * @return 结果
	 * @throws Exception 异常
	 */
	private static String getMethodId(CtMethod method) throws Exception {
		return HashLIB.encrypt(HashLIB.HASHTYPE_MD5, BytecodeMethod.getMethodKey(method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), method.getReturnType()), Constant.ENCODING);
	}

	/**
	 * 获取唯一标识
	 * 
	 * @param interfaceClassName
	 * @param methodName
	 * @param parmTypes
	 * @param returnType
	 * @return
	 * @throws Exception
	 */
	private static String getMethodKey(String interfaceClassName, String methodName, CtClass[] parmTypes, CtClass returnType) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (CtClass clz : parmTypes) {
			sb.append("," + clz.getName());
		}
		sb.append("-" + returnType.getName());

		String s = "";
		if (sb.length() > 0) {
			s = sb.substring(",".length(), sb.length());
		}

		return interfaceClassName + "." + methodName + "_" + HashLIB.encrypt(HashLIB.HASHTYPE_MD5, s, Constant.ENCODING);
	}
}
