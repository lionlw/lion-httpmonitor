package com.lion.utility.http.httpmonitor.client;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.util.AntPathMatcher;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import com.lion.utility.tool.common.Tool;
import com.lion.utility.tool.thread.CustomThreadFactory;
import com.lion.utility.tool.log.LogLIB;
import com.lion.utility.http.httpmonitor.entity.HttpStat;
import com.lion.utility.http.net.NetLIB;

/**
 * http client请求监控
 * 
 * 此类整个应用只实例化一个，然后应用到各处
 * 
 * @author lion
 *
 */
public class HttpClientMonitor {
	/**
	 * 应用名
	 */
	protected String appName;
	/**
	 * 监控地址
	 */
	protected String monitorAddress;
	/**
	 * debug标记
	 */
	protected boolean isDebug;

	/**
	 * url模式匹配map（key：url映射模式（采用springMVC的url映射规则），value：url标识）
	 */
	protected Map<String, String> urlPatternMap = new HashMap<>();
	/**
	 * 直接url接匹配map（key：url相对地址，value：url标识）
	 */
	protected Map<String, String> urlDirectMap = new HashMap<>();

	/**
	 * ip
	 */
	protected String ip;

	/**
	 * 统计1
	 */
	protected ConcurrentHashMap<String, HttpStat> httpStatMap1 = new ConcurrentHashMap<>();
	/**
	 * 统计2
	 */
	protected ConcurrentHashMap<String, HttpStat> httpStatMap2 = new ConcurrentHashMap<>();
	/**
	 * 是否使用统计1
	 */
	protected AtomicBoolean useHttpStatMap1 = new AtomicBoolean(true);

	/**
	 * url缓存（key：请求url的相对路径，value：接口标识）
	 */
	protected LoadingCache<String, String> cache;

	/**
	 * url匹配器
	 */
	protected AntPathMatcher antPathMatcher = new AntPathMatcher();

	/**
	 * 构造方法
	 * 
	 * @param appName        应用名
	 * @param monitorAddress 监控地址
	 * @param urlMatcherMap  url匹配map（key：url映射模式（采用springMVC的url映射规则），value：url标识）
	 * @param isDebug        debug标记
	 */
	public HttpClientMonitor(String appName, String monitorAddress, Map<String, String> urlMatcherMap, boolean isDebug) {
		if (isDebug) {
			LogLIB.info("tmpdir: " + Tool.getTmpDir());
		}

		this.appName = appName;
		this.monitorAddress = monitorAddress;
		this.isDebug = isDebug;

		try {
			this.ip = NetLIB.getIp();
		} catch (Exception e) {
			LogLIB.error("getIp Exception", e);
		}

		// map初始化
		for (Entry<String, String> entry : urlMatcherMap.entrySet()) {
			if (this.antPathMatcher.isPattern(entry.getKey())) {
				this.urlPatternMap.put(entry.getKey(), entry.getValue());
			} else {
				this.urlDirectMap.put(entry.getKey(), entry.getValue());
			}
		}

		// 缓存初始化
		this.cache = Caffeine.newBuilder()
				.maximumSize(10L * 10000L)
				.refreshAfterWrite(12, TimeUnit.HOURS)
				.build(new CacheLoader<String, String>() {
					@Override
					public String load(String path) throws Exception {
						// 直接匹配查找
						String relativeUrl = urlDirectMap.get(path);
						if (relativeUrl != null) {
							// 加入url缓存
							return relativeUrl;
						}

						// 模式匹配查找
						for (Entry<String, String> entry : urlPatternMap.entrySet()) {
							if (antPathMatcher.match(entry.getKey(), path)) {
								return entry.getValue();
							}
						}

						// 没找到，则也缓存下，避免后续查找
						return "";
					}
				});

		// 计时器类
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, new CustomThreadFactory("HttpMonitor", "HttpClientMonitorReportTimerTask"));
		ses.scheduleAtFixedRate(new HttpClientMonitorReportTimerTask(this), 60L, 60L, TimeUnit.SECONDS);

		LogLIB.info("HttpClientMonitor init complete");
	}

	/**
	 * 监控
	 * 
	 * @param requestUrl 请求地址
	 * @param succeed    是否成功
	 * @param startTime  方法请求起始时间（单位：ms）
	 */
	public void monitor(String requestUrl, boolean succeed, long startTime) {
		try {
			long elapse = 0L;
			if (succeed) {
				elapse = System.currentTimeMillis() - startTime;
			}

			String urlkey = this.getUrlkey(requestUrl);
			if (!Tool.checkHaveValue(urlkey)) {
				if (this.isDebug) {
					LogLIB.info("url match failed, so can't monitor, " + requestUrl);
				}
				return;
			}

			if (this.isDebug) {
				LogLIB.info("url match succeed, so monitor, " + requestUrl + " " + urlkey);
			}

			HttpStat httpStat = null;
			if (this.useHttpStatMap1.get()) {
				httpStat = this.httpStatMap1.computeIfAbsent(urlkey,
						k -> this.getHttpStat(urlkey));
			} else {
				httpStat = this.httpStatMap2.computeIfAbsent(urlkey,
						k -> this.getHttpStat(urlkey));
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
	 * 获取url key
	 * 
	 * @param requestUrl 请求地址
	 * @return
	 * @throws Exception
	 */
	private String getUrlkey(String requestUrl) throws Exception {
		URL url = new URL(requestUrl);
		String domain = url.getHost() + ":" + url.getPort();
		String path = url.getPath();

		String relativeUrl = this.cache.get(path);
		if (!Tool.checkHaveValue(relativeUrl)) {
			// 即便返回，但值为空，也表示不需要监控
			return null;
		}

		return domain + "|||" + relativeUrl;
	}

	/**
	 * 获取httpStat类
	 * 
	 * @param urlkey urlkey
	 * @return 结果
	 */
	private HttpStat getHttpStat(String urlkey) {
		String[] array = urlkey.split("\\|\\|\\|");
		return new HttpStat(array[0], array[1]);
	}
}
