package io.shulie.takin.web.biz.nacos.event;

import io.shulie.takin.web.ext.entity.tenant.TenantCommonExt;
import io.shulie.takin.web.ext.util.WebPluginUtils;

public class DynamicConfigRefreshEvent {

    private String appName;

    private TenantCommonExt commonExt = WebPluginUtils.traceTenantCommonExt();

    public DynamicConfigRefreshEvent(String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }

    public TenantCommonExt getCommonExt() {
        return commonExt;
    }
}
