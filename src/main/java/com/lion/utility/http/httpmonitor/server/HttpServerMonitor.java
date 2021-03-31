package com.lion.utility.http.httpmonitor.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.server.HttpConnection;
import org.springframework.web.context.request.RequestContextHolder;

import javassist.ClassClassPath;
import javassist.ClassPool;
import com.lion.utility.tool.common.Tool;
import com.lion.utility.tool.thread.CustomThreadFactory;
import com.lion.utility.tool.log.LogLIB;
import com.lion.utility.http.httpmonitor.entity.HttpStat;
import com.lion.utility.http.net.NetLIB;

/**
 * http server请求监控
 * 
 * 此类整个应用初始化一次，然后应用到各处
 * 
 * @author lion
 *
 */
public class HttpServerMonitor {
	private HttpServerMonitor() {
	}

	/**
	 * 应用名
	 */
	protected static String appName;
	/**
	 * 服务名
	 */
	protected static String serviceName;
	/**
	 * 监控地址
	 */
	protected static String monitorAddress;
	/**
	 * debug标记
	 */
	protected static boolean isDebug;

	/**
	 * ip
	 */
	protected static String ip;

	/**
	 * 统计1
	 */
	protected static ConcurrentHashMap<String, HttpStat> httpStatMap1 = new ConcurrentHashMap<>();
	/**
	 * 统计2
	 */
	protected static ConcurrentHashMap<String, HttpStat> httpStatMap2 = new ConcurrentHashMap<>();
	/**
	 * 是否使用统计1
	 */
	protected static AtomicBoolean useHttpStatMap1 = new AtomicBoolean(true);

	/**
	 * 字节码类容器，必须全局唯一，否则无法实现在字节码中调用字节码生成的类
	 */
	protected static ClassPool classPool;

	/**
	 * 初始化
	 * 
	 * @param appName                      应用名
	 * @param serviceName                  服务名
	 * @param monitorAddress               监控地址
	 * @param includeMonitorPackageNameSet 需要监控的包名列表（会自动将包下属所有类及方法加入监控）
	 * @param includeMonitorClassNameSet   需要监控的类名列表（会自动将类下属所有方法加入监控）
	 * @param excludeMonitorPackageNameSet 不需要监控的包名列表（优先级高于include）
	 * @param excludeMonitorClassNameSet   不需要监控的类名列表（优先级高于include）
	 * @param isDebug                      debug标记
	 * @throws Exception 异常
	 */
	public static void init(String appName, String serviceName, String monitorAddress,
			Set<String> includeMonitorPackageNameSet,
			Set<String> includeMonitorClassNameSet,
			Set<String> excludeMonitorPackageNameSet,
			Set<String> excludeMonitorClassNameSet,
			boolean isDebug) throws Exception {
		// PS：monitorPackageNames必须为文本方式写入，不能采用类似 NotifyController.class.getName()的方式，
		// 后者会导致类被载入，从而使得字节码处理失败

		if (isDebug) {
			LogLIB.info("tmpdir: " + Tool.getTmpDir());
		}

		HttpServerMonitor.appName = appName;
		HttpServerMonitor.serviceName = serviceName;
		HttpServerMonitor.monitorAddress = monitorAddress;
		HttpServerMonitor.isDebug = isDebug;

		try {
			HttpServerMonitor.ip = NetLIB.getIp();
		} catch (Exception e) {
			LogLIB.error("getIp Exception", e);
		}

		HttpServerMonitor.classPool = new ClassPool(true);
		// 解决tomcat classloader问题
		HttpServerMonitor.classPool.insertClassPath(new ClassClassPath(HttpServerMonitor.class));

		// 合并
		Set<String> classNameMergeSet = BytecodeMethod.merge(
				includeMonitorPackageNameSet,
				includeMonitorClassNameSet,
				excludeMonitorPackageNameSet,
				excludeMonitorClassNameSet);
		// 生成字节码
		BytecodeMethod.addMonitorCode(classNameMergeSet);

		// 计时器类
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, new CustomThreadFactory("HttpMonitor", "HttpServerMonitorReportTimerTask"));
		ses.scheduleAtFixedRate(new HttpServerMonitorReportTimerTask(), 60L, 60L, TimeUnit.SECONDS);

		LogLIB.info("HttpServerMonitor init complete");
	}

	/**
	 * 监控
	 * 
	 * @param methodId  方法标识
	 * @param succeed   是否成功
	 * @param startTime 方法请求起始时间（单位：ms）
	 */
	public static void monitor(String methodId, boolean succeed, long startTime) {
		try {
			long elapse = 0L;
			if (succeed) {
				elapse = System.currentTimeMillis() - startTime;
			}

			// httpserver监控的目标可以是web容器中的方法，由于rpc框架也会复用该方法，因此需要做过滤，避免重复统计
			// 此处对当前请求上下文做判断，过滤掉rpc框架的请求，避免重复统计（支持springMVC及jetty2种web容器的判断）
			if (RequestContextHolder.getRequestAttributes() == null &&
					HttpConnection.getCurrentConnection() == null) {
				if (HttpServerMonitor.isDebug) {
					LogLIB.info("request is rpc, so can't monitor, " + HttpServerMonitor.httpStatMap1.get(methodId).getMethodCode());
				}
				return;
			}

			HttpStat httpStat = null;
			if (HttpServerMonitor.useHttpStatMap1.get()) {
				httpStat = HttpServerMonitor.httpStatMap1.get(methodId);
			} else {
				httpStat = HttpServerMonitor.httpStatMap2.get(methodId);
			}

			if (httpStat != null) {
				httpStat.getTotal().increment();
				if (!succeed) {
					httpStat.getFailed().increment();
				}
				httpStat.getElapse().add(elapse);
			}
		} catch (Exception e) {
			LogLIB.error("monitor exception", e);
		}
	}

	/**
	 * 初始化监控数据
	 * 
	 * @param methodId   方法标识（应用容器里唯一）
	 * @param methodCode 简易方法标识
	 */
	protected static void initMonotorStat(String methodId, String methodCode) {
		HttpServerMonitor.httpStatMap1.put(methodId, new HttpStat(HttpServerMonitor.serviceName, methodCode));
		HttpServerMonitor.httpStatMap2.put(methodId, new HttpStat(HttpServerMonitor.serviceName, methodCode));
	}
}
