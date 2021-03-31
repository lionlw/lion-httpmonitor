package com.lion.utility.http.httpmonitor.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.lion.utility.tool.file.JsonLIB;
import com.lion.utility.tool.log.LogLIB;
import com.lion.utility.http.httpmonitor.entity.HttpStat;
import com.lion.utility.http.net.NetLIB;
import com.lion.utility.http.net.entity.NetResult;

/**
 * http监控上报类
 * 
 * @author lion
 *
 */
class HttpClientMonitorReportTimerTask extends TimerTask {
	private HttpClientMonitor httpClientMonitor;

	public HttpClientMonitorReportTimerTask(HttpClientMonitor httpClientMonitor) {
		this.httpClientMonitor = httpClientMonitor;
	}

	@Override
	public void run() {
		ConcurrentHashMap<String, HttpStat> httpStatMap = null;

		try {
			//切换统计缓存
			if (this.httpClientMonitor.useHttpStatMap1.get()) {
				this.httpClientMonitor.useHttpStatMap1.set(false);
				httpStatMap = this.httpClientMonitor.httpStatMap1;
			} else {
				this.httpClientMonitor.useHttpStatMap1.set(true);
				httpStatMap = this.httpClientMonitor.httpStatMap2;
			}

			// 过滤请求数为0的统计
			List<HttpStat> httpStats = new ArrayList<>();
			for (Entry<String, HttpStat> entry : httpStatMap.entrySet()) {
				HttpStat httpStat = entry.getValue();
				if (httpStat.getTotal().longValue() > 0) {
					httpStats.add(httpStat);
				}
			}

			if (httpStats.size() == 0) {
				return;
			}

			//统计
			Map<String, String> postDataMap = new HashMap<>();
			postDataMap.put("appName", this.httpClientMonitor.appName);
			postDataMap.put("ipport", this.httpClientMonitor.ip);
			postDataMap.put("httpStats", JsonLIB.toJson(httpStats));

			NetResult netResult = NetLIB.getFileContentPost(
					this.httpClientMonitor.monitorAddress + "/outi/reportMonitorHttpClientV2.do",
					null,
					postDataMap,
					null,
					"utf8",
					3000,
					10000,
					false,
					200,
					null);

			if (this.httpClientMonitor.isDebug) {
				LogLIB.info(netResult.toString());
			}
		} catch (Exception e) {
			LogLIB.error("HttpClientMonitorReportTimerTask exception", e);
		} finally {
			//初始化统计类
			for (Entry<String, HttpStat> entry : httpStatMap.entrySet()) {
				entry.getValue().getTotal().reset();
				entry.getValue().getFailed().reset();
				entry.getValue().getElapse().reset();
			}
		}
	}
}
