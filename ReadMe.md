# httpmonitor

## 特点
实现http监控封装，包括http client监控，http server监控。粒度为方法级，监控参数为请求总数，请求失败总数，请求总耗时。此项目不含监控数据中心。

内部采用了内存块切换的方式，防止对应用性能产生影响。即有2个内存块分别用于统计与上报。定时进行切换。

### 服务端项目
针对发布http服务的项目，提供http接口的运行情况监控。将接口的执行情况上报到监控数据中心。
采用字节码方式对方法进行拦截重写，对项目无侵入。
统计每分钟每个方法的请求数，失败数，请求耗时
PS：此处虽用于controller方法，但实际可用于任意方法拦截。

#### 集成方式
监控代码初始化必须在所处理类加载前进行处理。
```
// 启用http server监控
		HttpServerMonitor.init(
				"testHttpServerMonitor",
				"testHttpServerMonitor",
				"http://test.admin.tvxio.com:8080/RPCManage/",
				Tool.newHashSet("com.lion.utility.ext"),
				null,
				null,
				null,
				true);
```
includeMonitorPackageNames 需要监控的包名列表（会自动将包下属所有类及方法加入监控）。
includeMonitorClassNames 需要监控的类名列表（会自动将类下属所有方法加入监控）。此处类名必须为文本方式写入，不能采用类似 NotifyController.class.getName()的方式，后者会导致类被载入，从而使得字节码处理失败。
excludeMonitorPackageNames 不需要监控的包名列表（优先级高）。
excludeMonitorClassNames 不需要监控的类名列表（优先级高）。

### 客户端项目
针对调用http服务的项目，提供所调用http接口的运行情况监控。将接口的执行情况上报到监控数据中心（与rpc为同一个监控数据中心）。
由于http client调用，只有一个url，因此为了将url与监控接口方法进行匹配，需要定义url的匹配策略。此处采用的是AntPathMatcher。
此处是针对lion-rpchttp进行的封装，若使用的是其他http调用方式（如：httpclient，okhttp等），需要对其进行封装。

#### 集成方式
建议放在项目初始化处。
```
            // 启用http client监控
            Map<String, String> urlMatcherMap = new HashMap<>();
            urlMatcherMap.put("/api/item/**", "/api/item/");
            urlMatcherMap.put("/api/subitem/**", "/api/subitem/");
            urlMatcherMap.put("/api/tv/relate/**", "/api/tv/relate/");
            urlMatcherMap.put("/v*_*/*/*/api/tv/section/**", "/api/tv/section/");
            urlMatcherMap.put("/v*_*/*/*/api/tv/banners/**", "/api/tv/banners/");
            urlMatcherMap.put("/v*_*/*/*/api/tv/banner/**", "/api/tv/banner/");

            HttpMonitor httpMonitor = new HttpMonitor(Constant.APP_NAME, Config.RPCMonitorAddress, urlMatcherMap, false);
            NetLIB.httpMonitorPolicy = new HttpMonitorPolicy(true, httpMonitor);
```
urlMatcherMap为定义匹配模式，符合匹配模式的会进行监控。key：模式，value：接口标识
用于将url中参数过滤匹配到唯一接口标识。
