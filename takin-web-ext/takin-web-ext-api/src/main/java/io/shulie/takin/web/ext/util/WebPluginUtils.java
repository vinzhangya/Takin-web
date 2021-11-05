package io.shulie.takin.web.ext.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.plugin.framework.core.PluginManager;
import io.shulie.takin.web.ext.api.auth.WebDataAuthExtApi;
import io.shulie.takin.web.ext.api.auth.WebUserAuthExtApi;
import io.shulie.takin.web.ext.api.e2e.InspectionExtApi;
import io.shulie.takin.web.ext.api.tenant.WebTenantExtApi;
import io.shulie.takin.web.ext.api.user.WebUserExtApi;
import io.shulie.takin.web.ext.entity.AuthQueryParamCommonExt;
import io.shulie.takin.web.ext.entity.AuthQueryResponseCommonExt;
import io.shulie.takin.web.ext.entity.UserCommonExt;
import io.shulie.takin.web.ext.entity.UserExt;
import io.shulie.takin.web.ext.entity.tenant.SwitchTenantExt;
import io.shulie.takin.web.ext.entity.tenant.TenantCommonExt;
import io.shulie.takin.web.ext.entity.tenant.TenantConfigExt;
import io.shulie.takin.web.ext.entity.tenant.TenantInfoExt;
import io.shulie.takin.web.ext.entity.tenant.TenantInfoExt.TenantEnv;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author by: hezhongqi
 * @date 2021/8/4 14:42
 */
public class WebPluginUtils {

    /**
     * 默认 userAppkey 解决zk PATH 问题
     */
    public static String DEFAULT_TENANT_APP_KEY = "default";
    public static Long DEFAULT_TENANT_ID = 1L;
    public static String DEFAULT_ENV_CODE = "test";
    public static String DEFAULT_TENANT_CODE = "default";

    /**
     * 默认系统相关属性
     */
    public static String SYS_DEFAULT_ENV_CODE = "system";
    public static Long SYS_DEFAULT_TENANT_ID = -1L;



    public static Long DEFAULT_USER_ID = -1L;

    private static WebUserExtApi userApi;
    private static WebDataAuthExtApi dataAuthApi;
    private static WebUserAuthExtApi userAuthExtApi;
    public static InspectionExtApi inspectionExtApi;
    private static WebTenantExtApi tenantExtApi;

    static PluginManager pluginManager;

    @Autowired
    public void setPluginManager(PluginManager pluginManager) {
        WebPluginUtils.pluginManager = pluginManager;
        userApi = pluginManager.getExtension(WebUserExtApi.class);
        dataAuthApi = pluginManager.getExtension(WebDataAuthExtApi.class);
        userAuthExtApi = pluginManager.getExtension(WebUserAuthExtApi.class);
        inspectionExtApi = pluginManager.getExtension(InspectionExtApi.class);
        tenantExtApi = pluginManager.getExtension(WebTenantExtApi.class);
    }

    @AllArgsConstructor
    @Getter
    public enum EnvCodeEnum{
        TEST("test","测试环境",""),
        PROD("prod","生产环境","当前环境为生产环境，请谨慎操作");

        private String envCode;
        private String envName;
        private String desc;
    }

    //********************************用户插件模块**********************************//

    /**
     * 根据userId 获取 用户信息
     *
     * @param userId
     * @return
     */
    public static UserExt getUserExtByUserId(Long userId) {
        if (Objects.nonNull(userApi)) {
            return userApi.getUserExtByUserId(userId);
        }
        return null;
    }

    /**
     * 补充 插入 更新 用户数据
     *
     * @param userCommonExt -
     */
    public static void fillUserData(UserCommonExt userCommonExt) {
        if (Objects.nonNull(userApi)) {
            userApi.fillUserData(userCommonExt);
        }
    }

    //********************************用户插件模块**********************************//


    /**
     * 补充查询 权限 用户数据
     *
     * @param queryParamCommonExt -
     */
    public static void fillQueryParam(AuthQueryParamCommonExt queryParamCommonExt) {
        if (Objects.nonNull(userApi)) {
            userApi.fillQueryParam(queryParamCommonExt);
        }
    }

    /**
     * 补充 查询结果后的用户数据
     *
     * @param queryResponseCommonExt -
     */
    public static void fillQueryResponse(AuthQueryResponseCommonExt queryResponseCommonExt) {
        if (Objects.nonNull(userApi)) {
            userApi.fillQueryResponse(queryResponseCommonExt);
        }
    }

    /**
     * 查询用户数据
     *
     * @param userIds -
     * @return -
     */
    public static Map<Long, UserExt> getUserMapByIds(List<Long> userIds) {
        if (CollectionUtils.isNotEmpty(userIds) && Objects.nonNull(userApi)) {
            return userApi.getUserMapByIds(userIds);
        }
        return Maps.newHashMap();
    }

    /**
     * 查询所有用户数据
     *
     * @return -
     */
    public static List<UserExt> selectAllUser() {
        if (Objects.nonNull(userApi)) {
            return userApi.selectAllUser();
        }
        return Lists.newArrayList();
    }

    ///**
    // * 根据userAppKey查询用户
    // *
    // * @param userAppKey userAppKey
    // * @return UserExt对象
    // */
    //public static UserExt getUserByAppKey(String userAppKey) {
    //    if (StringUtils.isBlank(userAppKey)) {
    //        return null;
    //    }
    //    //for (UserExt ext : selectAllUser()) {
    //    //    //if (userAppKey.equals(ext.getKey())) {
    //    //    //    return ext;
    //    //    //}
    //    //}
    //    return null;
    //}



    /**
     * 补充用户名称
     *
     * @param userId  用户主键
     * @param userMap 用户信息
     * @return 用户名称
     */
    public static String getUserName(Long userId, Map<Long, UserExt> userMap) {
        if (!userMap.isEmpty() && Objects.nonNull(userId) && Objects.nonNull(
            userMap.get(userId))) {
            return userMap.get(userId).getName();
        }
        return "";
    }


    /**
     * 是否带用户模块
     *
     * @return true/false
     */
    public static Boolean checkUserPlugin() {
        if (userApi != null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
        //UserExt user = pluginUtils.queryUserByKey(pluginUtils.getTenantUserKey());
        //if (null == user) {
        //    return Response.fail("未查询到用户相关信息");
        //}

        //UserExt user = null;
        ////TakinRestContext.getUser();
        //if (Objects.isNull(user) || user.getUserType() != 0) {
        //    log.error("用户为空或用户类型非管理员，开启验证任务失败");
        //    return;
        //}
    }

    public List<UserExt> selectDistinctUserAppKey() {
        //userMapper.selectDistinctUserAppKey();
        return Lists.newArrayList();
    }

    public static UserExt queryUserFromCache(String userAppKey) {
        if (userApi != null) {
            return userApi.queryUserFromCache(userAppKey);
        }
        return null;
    }

    //public void fillMiddlewareUserData(AppMiddlewareQuery query) {
    //    //UserExt user = null;
    //    ////TakinRestContext.getUser();
    //    //if (1 == user.getRole()) {
    //    //    query.setUserId(user.getId());
    //    //}
    //}

    public static Class<?> getClassByName(String className) {
        try {
            // 先扫描用户插件
            return Class.forName(className, true, pluginManager.getExtension(WebUserExtApi.class).getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 根据用户名 模糊查询
     *
     * @param userName -
     * @return -
     */
    public static List<UserExt> selectByName(String userName) {
        if (Objects.nonNull(userApi)) {
            return userApi.selectByName(userName);
        }
        return Lists.newArrayList();
    }
    //********************************用户权限模块**********************************//
    /**
     * 权限相关数据
     *
     * @return -
     */
    public static List<Long> getQueryAllowUserIdList() {
        if (Objects.nonNull(dataAuthApi)) {
            return dataAuthApi.getQueryAllowUserIdList();
        }
        return Lists.newArrayList();
    }

    public static List<Long> getUpdateAllowUserIdList() {
        if (Objects.nonNull(dataAuthApi)) {
            return dataAuthApi.getUpdateAllowUserIdList();
        }
        return Lists.newArrayList();
    }

    public static List<Long> getDeleteAllowUserIdList() {
        if (Objects.nonNull(dataAuthApi)) {
            return dataAuthApi.getDeleteAllowUserIdList();
        }
        return Lists.newArrayList();
    }

    public static List<Long> getDownloadAllowUserIdList() {
        if (Objects.nonNull(dataAuthApi)) {
            return dataAuthApi.getDownloadAllowUserIdList();
        }
        return Lists.newArrayList();
    }

    public static List<Long> getStartStopAllowUserIdList() {
        if (Objects.nonNull(dataAuthApi)) {
            return dataAuthApi.getStartStopAllowUserIdList();
        }
        return Lists.newArrayList();
    }

    public static List<Long> getEnableDisableAllowUserIdList() {
        if (Objects.nonNull(dataAuthApi)) {
            return dataAuthApi.getEnableDisableAllowUserIdList();
        }
        return Lists.newArrayList();
    }

    public static boolean validateSuperAdmin() {
        if (Objects.nonNull(dataAuthApi)) {
            return userAuthExtApi.validateSuperAdmin();
        }
        return false;
    }
    //********************************用户权限模块**********************************//


    //********************************租户插件模块**********************************//

    /**
     * 是否有租户插件
     *
     * @return true/false
     */
    public static Boolean checkTenantPlugin() {
        if (tenantExtApi != null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    /**
     * 前端 根据租户code 获取租户信息 目前给插件user-module使用
     * @param tenantAppKey
     * @param tenantCode
     * @return
     */
    public static TenantInfoExt getTenantInfo(String tenantAppKey,String tenantCode) {
        if (tenantExtApi != null) {
            return tenantExtApi.getTenantInfo(tenantAppKey,tenantCode);
        }
        if(userApi != null) {
            // 只是带用户插件
            List<TenantInfoExt> tenantInfoList = userApi.getTenantInfoList();
            if (CollectionUtils.isEmpty(tenantInfoList)) {
                return null;
            }
            TenantInfoExt tenantInfoExt = tenantInfoList.get(0);
            if (StringUtils.isNotBlank(tenantAppKey) && tenantAppKey.equals(tenantInfoExt.getTenantAppKey())) {
                return tenantInfoExt;
            }
            if(StringUtils.isNotBlank(tenantCode) && tenantCode.equals(tenantInfoExt.getTenantCode())) {
                return tenantInfoExt;
            }
            return null;
        }
        return null;
    }

    /**
     * 返回默认的环境 目前给插件user-module使用
     * @param userAppKey
     * @param tenantCode
     * @return
     */
    public static String getDefaultEnvCode(String userAppKey,String tenantCode) {
        if (tenantExtApi != null) {
            return tenantExtApi.getDefaultEnvCode(userAppKey,tenantCode);
        }
        return DEFAULT_ENV_CODE;
    }

    /**
     * 获取默认用户id
     * @param userAppKey
     * @param tenantCode
     * @return
     */
    public static Long getDefaultUserId(String userAppKey,String tenantCode) {
        if (tenantExtApi != null) {
            return tenantExtApi.getDefaultUserId(userAppKey,tenantCode);
        }
        return DEFAULT_USER_ID;
    }



    /**
     * 前端 根据租户code 判断租户,默认存在 目前给插件user-module使用
     * @param tenantAppKey
     * @param tenantCode
     * @return
     */
    public static Boolean isExistTenant(String tenantAppKey,String tenantCode) {
        if (tenantExtApi != null) {
            return tenantExtApi.isExistTenant(tenantAppKey,tenantCode);
        }
        if(userApi != null) {
            // 只是带用户插件
            if(StringUtils.isNotBlank(tenantCode)) {
                return tenantCode.equals(DEFAULT_TENANT_APP_KEY);
            }else {
                List<TenantInfoExt> tenantInfoList = userApi.getTenantInfoList();
                if (CollectionUtils.isEmpty(tenantInfoList)) {
                    return Boolean.FALSE;
                }
                TenantInfoExt tenantInfoExt = tenantInfoList.get(0);
                if (tenantAppKey.equals(tenantInfoExt.getTenantAppKey())) {
                    return Boolean.TRUE;
                }
            }

        }
        return Boolean.FALSE;
    }


    /**
     * 根据租户id查询当前租户 key
     *
     * @param tenantId -
     * @return -
     */

    public static TenantCommonExt fillTenantCommonExt(Long tenantId,String envCode) {
        if (Objects.nonNull(tenantExtApi) && tenantId != null) {
            // saas
            TenantInfoExt tenantInfo = tenantExtApi.getTenantInfo(tenantId);
            if (tenantInfo != null) {
                TenantCommonExt ext = new TenantCommonExt();
                ext.setTenantId(tenantId);
                ext.setEnvCode(envCode);
                ext.setTenantAppKey(tenantInfo.getTenantAppKey());
                return ext;
            }
        }
        if(Objects.nonNull(userApi)) {
            // 企业版
            TenantInfoExt infoExt = userApi.getTenantInfoList().get(0);
            TenantCommonExt ext = new TenantCommonExt();
            ext.setEnvCode(envCode);
            ext.setTenantAppKey(infoExt.getTenantAppKey());
            ext.setTenantId(infoExt.getTenantId());
            ext.setTenantCode(infoExt.getTenantCode());
            return ext;
        }
        // 开源版
        TenantCommonExt ext = new TenantCommonExt();
        ext.setEnvCode(DEFAULT_ENV_CODE);
        ext.setTenantAppKey(DEFAULT_TENANT_APP_KEY);
        ext.setTenantId(DEFAULT_TENANT_ID);
        ext.setTenantCode(DEFAULT_TENANT_CODE);
        return ext;
    }

    /**
     * 获取所有租户信息
     *
     * @return -
     */
    public static List<TenantInfoExt> getTenantInfoList() {
        if (Objects.nonNull(tenantExtApi)) {
            return tenantExtApi.getTenantInfoList();
        }
        // 默认一个租户
        if(Objects.nonNull(userApi)) {
            // 企业版
            return userApi.getTenantInfoList();
        }
        // 开源版本
        return getDefaultTenantInfoList();
    }

    /**
     * 根据租户code 查询租户数据
     * @param tenantCode
     * @return
     */
    public static List<TenantInfoExt> getTenantInfoListByTenantCode(String tenantCode) {
        if (Objects.nonNull(tenantExtApi)) {
            return tenantExtApi.getTenantInfoListByTenantCode(tenantCode);
        }
        // 默认一个租户
        if(Objects.nonNull(userApi)) {
            // 企业版
            return userApi.getTenantInfoList();
        }
        // 开源版本
        return getDefaultTenantInfoList();
    }

    /**
     * 切换租户
     * @param ext
     * @return  TenantInfoExt
     */
    public static TenantInfoExt switchTenant(SwitchTenantExt ext) {
        if(Objects.nonNull(userApi)) {
           return userApi.switchTenant(ext);
        }
        return getDefaultTenantInfoList().get(0);
    }

    /**
     * 切换环境
     * @param ext
     */
    public static void switchEnv(SwitchTenantExt ext) {
        if(Objects.nonNull(userApi)) {
            userApi.switchEnv(ext);
        }
    }

    /**
     * 切换
     * @param tenantAppKey
     * @param envCode
     * @return
     */
    public static List<TenantConfigExt> getTenantConfig(String tenantAppKey, String envCode) {

        if (Objects.nonNull(tenantExtApi)) {
            return tenantExtApi.getTenantConfig(tenantAppKey,envCode);
        }
        // 默认一个租户
        if(Objects.nonNull(userApi)) {
            // 企业版
            return userApi.getTenantConfig(envCode);
        }
        // 开源版本
        return Lists.newArrayList();
    }

    /**
     * 租户参数传递
     * 转 TenantCommonExt
     * @param source -
     * @param target -
     */
    public static void transferTenantParam(TenantCommonExt source, TenantCommonExt target) {
        target.setTenantAppKey(source.getTenantAppKey());
        target.setEnvCode(source.getEnvCode());
        target.setTenantId(source.getTenantId());
    }

    /**
     * 租户参数传递 cloud 简单传递
     * 转 ContextExt
     * @param source -
     * @param target -
     */
    public static void transferTenantParam(TenantCommonExt source, ContextExt target) {
        target.setEnvCode(source.getEnvCode());
        target.setTenantId(source.getTenantId());
    }

    //********************************租户插件模块**********************************//


    public static Map<String, String> getSystemInfo() {
        if (Objects.nonNull(userApi)) {
            return userApi.getSystemInfo();
        }
        HashMap<String, String> dataMap = new LinkedHashMap<>();
        dataMap.put("租户ID", DEFAULT_TENANT_ID + "");
        dataMap.put("租户user-app-key", DEFAULT_TENANT_APP_KEY);
        dataMap.put("用户ID", DEFAULT_USER_ID + "");
        dataMap.put("用户user-app-key", DEFAULT_TENANT_APP_KEY);
        return dataMap;
    }

    /**
     * 补充cloud 溯源数据
     *
     * @param traceContextExt 溯源数据对象
     */
    public static void fillCloudUserData(ContextExt traceContextExt) {
        if (Objects.nonNull(userApi)) {
            userApi.fillCloudUserData(traceContextExt);
        }
    }

    //********************************http线程上下文模块**********************************//

    /**
     * 设置租户信息
     *
     * @param tenantId 租户 id
     * @param tenantAppKey appKey
     * @param envCode 环境
     */
    public static void setTraceTenantContext(Long tenantId, String tenantAppKey, String envCode,String tenantCode) {
        if (Objects.nonNull(userApi)) {
            TenantCommonExt tenantCommonExt = new TenantCommonExt();
            tenantCommonExt.setTenantId(tenantId);
            tenantCommonExt.setTenantAppKey(tenantAppKey);
            tenantCommonExt.setEnvCode(envCode);
            tenantCommonExt.setTenantCode(tenantCode);
            userApi.setTraceTenantContext(tenantCommonExt);
        }
    }

    /**
     * 设置租户信息
     * @param commonExt
     */
    public static void setTraceTenantContext(TenantCommonExt commonExt) {
        if (Objects.nonNull(userApi)) {
            userApi.setTraceTenantContext(commonExt);
        }
    }

    /**
     * 移除租户信息
     */
    public static void removeTraceContext() {
        if (Objects.nonNull(userApi)) {
            userApi.removeTraceContext();
        }
    }

    /**
     * 获取登录账号
     *
     * @return 登录的用户信息
     */
    public static UserExt traceUser() {
        if (Objects.nonNull(userApi)) {
            return userApi.traceUser();
        }
        return null;
    }

    /**
     * 返回租户id
     * 租户依赖于用户
     * @return 租户主键
     */
    public static Long traceTenantId() {
        return traceTenantId(false);
    }

    /**
     * 返回租户id
     * 租户依赖于用户
     * @param defaultSystem 是否使用系统的默认值 true=使用系统默认值(-1,system...) false=使用一般默认值(prod,test,1....)
     * @return 租户主键
     */
    public static Long traceTenantId(boolean defaultSystem) {
        if (userApi != null) {
            return userApi.traceTenantId();
        }
        return defaultSystem ? SYS_DEFAULT_TENANT_ID : DEFAULT_TENANT_ID;
    }

    /**
     * 返回 tenantAppKey
     *
     * @return tenantAppKey
     */
    public static String traceTenantAppKey() {
        if (userApi != null) {
            return userApi.traceTenantAppKey();
        }
        // 返回一个默认
        return DEFAULT_TENANT_APP_KEY;
    }
    /**
     * 返回环境
     *
     * @return 环境代码
     */
    public static String traceEnvCode() {
        return traceEnvCode(false);
    }

    /**
     * @param defaultSystem 是否使用系统的默认值 true=使用系统默认值(-1,system...) false=使用一般默认值(prod,test,1....)
     * @return
     */
    public static String traceEnvCode(boolean defaultSystem) {
        if (userApi != null) {
            return userApi.traceEnvCode();
        }
        return defaultSystem ? SYS_DEFAULT_ENV_CODE : DEFAULT_ENV_CODE;
    }

    /**
     * 返回用户id
     *
     * @return 用户主键
     */
    public static Long traceUserId() {
        if (userApi != null) {
            if (userApi.traceUser() != null) {
                return userApi.traceUser().getId();
            }
        }
        return DEFAULT_USER_ID;
    }

    /**
     *
     * 返回租户code
     * @return
     */
    public static String traceTenantCode() {
        if (userApi != null) {
            return userApi.traceTenantCode();
        }
        return DEFAULT_TENANT_CODE;
    }

    /**
     * 组装 http 租户参数
     * @return
     */
    public static TenantCommonExt traceTenantCommonExt() {
        TenantCommonExt ext = new TenantCommonExt();
        ext.setTenantId(traceTenantId());
        ext.setEnvCode(traceEnvCode());
        ext.setTenantAppKey(traceTenantAppKey());
        ext.setTenantCode(traceTenantCode());
        return ext;
    }
    //********************************http线程上下文模块**********************************//


    /**
     * 获取默认租户
     * @return
     */
    private static List<TenantInfoExt> getDefaultTenantInfoList() {
        List<TenantInfoExt> exts = Lists.newArrayList();
        TenantInfoExt ext = new TenantInfoExt();
        ext.setTenantId(DEFAULT_TENANT_ID);
        ext.setTenantAppKey(DEFAULT_TENANT_APP_KEY);
        ext.setTenantName(DEFAULT_TENANT_APP_KEY);
        ext.setTenantNick(DEFAULT_TENANT_APP_KEY);
        ext.setTenantCode(DEFAULT_TENANT_APP_KEY);
        ext.setEnvs(getDefaultTenantEnvList());
        exts.add(ext);
        return exts;
    }

    /**
     * 获取默认租户 环境
     * @return
     */
    private static List<TenantEnv> getDefaultTenantEnvList() {
        List<TenantEnv> envs = Lists.newArrayList();
        TenantInfoExt.TenantEnv env = new TenantEnv();
        env.setEnvCode(DEFAULT_ENV_CODE);
        env.setEnvName("测试环境");
        env.setDesc("无插件");
        env.setIsDefault(Boolean.TRUE);
        envs.add(env);
        return envs;
    }
}
