package com.lion.utility.http.httpmonitor.server;

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
 * rpc监控上报类
 * 
 * @author lion
 *
 */
class HttpServerMonitorReportTimerTask extends TimerTask {
	@Override
	public void run() {
		ConcurrentHashMap<String, HttpStat> httpStatMap = null;

		try {
			// 切换统计缓存
			if (HttpServerMonitor.useHttpStatMap1.get()) {
				HttpServerMonitor.useHttpStatMap1.set(false);
				httpStatMap = HttpServerMonitor.httpStatMap1;
			} else {
				HttpServerMonitor.useHttpStatMap1.set(true);
				httpStatMap = HttpServerMonitor.httpStatMap2;
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

			// 统计
			Map<String, String> postDataMap = new HashMap<>();
			postDataMap.put("appName", HttpServerMonitor.appName);
			postDataMap.put("ipport", HttpServerMonitor.ip);
			postDataMap.put("httpStats", JsonLIB.toJson(httpStats));

			NetResult netResult = NetLIB.getFileContentPost(
					HttpServerMonitor.monitorAddress + "/outi/reportMonitorHttpServer.do",
					null,
					postDataMap,
					null,
					"utf8",
					3000,
					10000,
					false,
					200,
					null);

			if (HttpServerMonitor.isDebug) {
				LogLIB.rpc(netResult.toString());
			}
		} catch (Exception e) {
			LogLIB.error("HttpServerMonitorReportTimerTask exception", e);
		} finally {
			// 初始化统计类
			for (Entry<String, HttpStat> entry : httpStatMap.entrySet()) {
				entry.getValue().getTotal().reset();
				entry.getValue().getFailed().reset();
				entry.getValue().getElapse().reset();
			}
		}
	}
}
