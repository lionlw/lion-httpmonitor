package com.lion.utility.http.httpmonitor.entity;

import com.lion.utility.http.httpmonitor.client.HttpClientMonitor;

/**
 * http client监控策略
 * 
 * @author lion
 */
public class HttpClientMonitorPolicy {
	/**
	 * 是否执行监控
	 */
	private Boolean isHandler;

	/**
	 * 监控类
	 */
	private HttpClientMonitor httpClientMonitor;

	/**
	 * 构造方法
	 * 
	 * @param isHandler         是否执行监控
	 * @param httpClientMonitor 监控类
	 */
	public HttpClientMonitorPolicy(boolean isHandler, HttpClientMonitor httpClientMonitor) {
		this.isHandler = isHandler;
		this.httpClientMonitor = httpClientMonitor;
	}

	/**
	 * 设置是否执行监控
	 * 
	 * @param isHandler 是否执行监控
	 */
	public void setIsHandler(Boolean isHandler) {
		this.isHandler = isHandler;
	}

	/**
	 * 获取是否执行监控
	 * 
	 * @return 是否执行监控
	 */
	public Boolean getIsHandler() {
		return isHandler;
	}

	/**
	 * 设置监控类
	 * 
	 * @param httpClientMonitor 监控类
	 */
	public void setHttpClientMonitor(HttpClientMonitor httpClientMonitor) {
		this.httpClientMonitor = httpClientMonitor;
	}

	/**
	 * 获取监控类
	 * 
	 * @return 监控类
	 */
	public HttpClientMonitor getHttpClientMonitor() {
		return httpClientMonitor;
	}

}
