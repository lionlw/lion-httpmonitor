package com.lion.utility.http.httpmonitor.entity;

import java.util.concurrent.atomic.LongAdder;

/**
 * http server监控类
 * 
 * @author lion
 *
 */
public class HttpStat {
	/**
	 * 服务名
	 */
	private String serviceName;
	/**
	 * 方法标识（className+methodName）
	 */
	private String methodCode = "";
	/**
	 * 请求总数
	 */
	private LongAdder total = new LongAdder();
	/**
	 * 请求失败总数
	 */
	private LongAdder failed = new LongAdder();
	/**
	 * 请求总耗时（单位：ms）
	 */
	private LongAdder elapse = new LongAdder();

	/**
	 * 构造方法
	 * 
	 * @param serviceName
	 *            服务名
	 * @param methodCode
	 *            方法标识
	 */
	public HttpStat(String serviceName, String methodCode) {
		this.serviceName = serviceName;
		this.methodCode = methodCode;
	}

	/**
	 * 设置服务名
	 * 
	 * @param serviceName
	 *            服务名
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * 获取服务名
	 * 
	 * @return 服务名
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * 设置方法标识（className+methodName）
	 * 
	 * @param methodCode
	 *            方法标识（className+methodName）
	 */
	public void setMethodCode(String methodCode) {
		this.methodCode = methodCode;
	}

	/**
	 * 获取方法标识（className+methodName）
	 * 
	 * @return 方法标识（className+methodName）
	 */
	public String getMethodCode() {
		return methodCode;
	}

	/**
	 * 设置请求总数
	 * 
	 * @param total
	 *            请求总数
	 */
	public void setTotal(LongAdder total) {
		this.total = total;
	}

	/**
	 * 获取请求总数
	 * 
	 * @return 请求总数
	 */
	public LongAdder getTotal() {
		return total;
	}

	/**
	 * 设置请求失败总数
	 * 
	 * @param failed
	 *            请求失败总数
	 */
	public void setFailed(LongAdder failed) {
		this.failed = failed;
	}

	/**
	 * 获取请求失败总数
	 * 
	 * @return 请求失败总数
	 */
	public LongAdder getFailed() {
		return failed;
	}

	/**
	 * 设置请求总耗时（单位：ms）
	 * 
	 * @param elapse
	 *            请求总耗时（单位：ms）
	 */
	public void setElapse(LongAdder elapse) {
		this.elapse = elapse;
	}

	/**
	 * 获取请求总耗时（单位：ms）
	 * 
	 * @return 请求总耗时（单位：ms）
	 */
	public LongAdder getElapse() {
		return elapse;
	}

}
