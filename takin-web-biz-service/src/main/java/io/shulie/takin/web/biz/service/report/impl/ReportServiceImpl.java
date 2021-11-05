package io.shulie.takin.web.biz.service.report.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.pamirs.takin.common.constant.VerifyResultStatusEnum;
import com.pamirs.takin.common.exception.ApiException;
import com.pamirs.takin.entity.domain.dto.report.LeakVerifyResult;
import com.pamirs.takin.entity.domain.dto.report.ReportDTO;
import com.pamirs.takin.entity.domain.vo.report.ReportIdVO;
import com.pamirs.takin.entity.domain.vo.report.ReportQueryParam;
import com.pamirs.takin.entity.domain.vo.report.ReportTrendQueryParam;
import io.shulie.takin.cloud.entrypoint.report.CloudReportApi;
import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.cloud.sdk.model.common.BusinessActivitySummaryBean;
import io.shulie.takin.cloud.sdk.model.request.common.CloudCommonInfoWrapperReq;
import io.shulie.takin.cloud.sdk.model.request.report.ReportDetailByIdReq;
import io.shulie.takin.cloud.sdk.model.request.report.ReportDetailBySceneIdReq;
import io.shulie.takin.cloud.sdk.model.request.report.ReportQueryReq;
import io.shulie.takin.cloud.sdk.model.request.report.TrendRequest;
import io.shulie.takin.cloud.sdk.model.request.report.WarnQueryReq;
import io.shulie.takin.cloud.sdk.model.response.report.ActivityResponse;
import io.shulie.takin.cloud.sdk.model.response.report.MetricesResponse;
import io.shulie.takin.cloud.sdk.model.response.report.ReportDetailResp;
import io.shulie.takin.cloud.sdk.model.response.report.ReportResp;
import io.shulie.takin.cloud.sdk.model.response.report.TrendResponse;
import io.shulie.takin.cloud.sdk.model.response.scenemanage.WarnDetailResponse;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.biz.pojo.output.report.ReportDetailOutput;
import io.shulie.takin.web.biz.pojo.output.report.ReportDetailTempOutput;
import io.shulie.takin.web.biz.pojo.request.leakverify.LeakVerifyTaskReportQueryRequest;
import io.shulie.takin.web.biz.pojo.response.leakverify.LeakVerifyTaskResultResponse;
import io.shulie.takin.web.biz.service.VerifyTaskReportService;
import io.shulie.takin.web.biz.service.report.ReportService;
import io.shulie.takin.web.common.constant.RemoteConstant;
import io.shulie.takin.web.common.domain.ErrorInfo;
import io.shulie.takin.web.common.domain.WebRequest;
import io.shulie.takin.web.common.domain.WebResponse;
import io.shulie.takin.web.common.enums.activity.BusinessTypeEnum;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.data.dao.activity.ActivityDAO;
import io.shulie.takin.web.data.param.activity.ActivityQueryParam;
import io.shulie.takin.web.data.result.activity.ActivityListResult;
import io.shulie.takin.web.data.result.activity.ActivityResult;
import io.shulie.takin.web.diff.api.report.ReportApi;
import io.shulie.takin.web.ext.entity.UserExt;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * @author qianshui
 * @date 2020/5/12 下午3:33
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportApi reportApi;
    @Autowired
    private CloudReportApi cloudReportApi;

    @Autowired
    private VerifyTaskReportService verifyTaskReportService;

    @Autowired
    private ActivityDAO activityDAO;

    @Override
    public List<ReportDTO> listReport(ReportQueryParam param) {
        // 前端查询条件 传用户
        if (StringUtils.isNotBlank(param.getUserName())) {
            List<UserExt> userList = WebPluginUtils.selectByName(param.getUserName());
            if (CollectionUtils.isNotEmpty(userList)) {
                List<Long> userIds = userList.stream().map(UserExt::getId).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(userIds)) {
                    //param.setUserIdStr(null);
                } else {
                    //param.setUserIdStr(StringUtils.join(userIds, ","));
                }
            } else {
                return Lists.newArrayList();
            }
        }
        List<ReportResp> reportResponseList = cloudReportApi.listReport(new ReportQueryReq() {{
            setSceneName(param.getSceneName());
            setStartTime(param.getStartTime());
            setEndTime(param.getEndTime());
        }}).getData();
        List<Long> userIds = reportResponseList.stream().filter(data -> null != data.getUserId())
            .map(data -> Long.valueOf(data.getUserId().toString()))
            .collect(Collectors.toList());
        //用户信息Map key:userId  value:user对象
        Map<Long, UserExt> userMap = WebPluginUtils.getUserMapByIds(userIds);
        return reportResponseList.stream().map(t -> {
            Long userId = t.getUserId() == null ? null : Long.valueOf(t.getUserId().toString());
            //负责人名称
            String userName = Optional.ofNullable(userMap.get(userId))
                .map(UserExt::getName)
                .orElse("");
            ReportDTO result = BeanUtil.copyProperties(t, ReportDTO.class);
            result.setUserName(userName);
            result.setUserId(userId);
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    public ReportDetailOutput getReportByReportId(Long reportId) {
        ReportDetailByIdReq req = new ReportDetailByIdReq();
        req.setReportId(reportId);
        ReportDetailResp detailResponse = cloudReportApi.detail(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
        ReportDetailOutput output = new ReportDetailOutput();
        BeanUtils.copyProperties(detailResponse, output);
        assembleVerifyResult(output);
        // 虚拟业务活动处理
        //dealVirtualBusiness(output);
        //补充报告执行人
        fillExecuteMan(output);
        return output;

    }

    private void fillExecuteMan(ReportDetailOutput output) {
        if (output == null || output.getUserId() == null) {
            return;
        }
        WebPluginUtils.fillUserData(output);
    }

    /**
     * todo 不做需要修改
     */
    private void dealVirtualBusiness(ReportDetailOutput output) {
        List<Long> ids = output.getBusinessActivity().stream().map(BusinessActivitySummaryBean::getBusinessActivityId).collect(Collectors.toList());
        ActivityQueryParam param = new ActivityQueryParam();
        param.setActivityIds(ids);
        List<ActivityListResult> result = activityDAO.getActivityList(param);
        Map<Long, List<ActivityListResult>> map = result.stream().collect(Collectors.groupingBy(ActivityListResult::getActivityId));
        List<BusinessActivitySummaryBean> beans = output.getBusinessActivity();
        for (BusinessActivitySummaryBean bean : output.getBusinessActivity()) {
            List<ActivityListResult> activityResults = map.get(bean.getBusinessActivityId());
            if (activityResults != null && activityResults.size() > 0) {
                ActivityListResult listResult = activityResults.get(0);
                if (listResult.getBusinessType().equals(BusinessTypeEnum.VIRTUAL_BUSINESS.getType())) {
                    ActivityResult activityResult = activityDAO.getActivityById(listResult.getBindBusinessId());
                    BusinessActivitySummaryBean activitySummaryBean = new BusinessActivitySummaryBean();
                    activitySummaryBean.setBusinessActivityId(activityResult.getActivityId());
                    activitySummaryBean.setBusinessActivityName(activityResult.getActivityName());
                    beans.add(activitySummaryBean);
                }
            }
        }
    }

    private void assembleVerifyResult(ReportDetailOutput output) {
        //组装验证任务结果
        LeakVerifyTaskReportQueryRequest queryRequest = new LeakVerifyTaskReportQueryRequest();
        queryRequest.setReportId(output.getId());
        LeakVerifyTaskResultResponse verifyTaskResultResponse = verifyTaskReportService.getVerifyTaskReport(
            queryRequest);
        if (Objects.isNull(verifyTaskResultResponse)) {
            return;
        }
        LeakVerifyResult leakVerifyResult = new LeakVerifyResult();
        leakVerifyResult.setCode(verifyTaskResultResponse.getStatus());
        leakVerifyResult.setLabel(VerifyResultStatusEnum.getLabelByCode(verifyTaskResultResponse.getStatus()));
        output.setLeakVerifyResult(leakVerifyResult);
    }

    @Override
    public TrendResponse queryReportTrend(TrendRequest param) {
        return cloudReportApi.trend(param);
    }

    @Override
    public ReportDetailTempOutput tempReportDetail(Long sceneId) {
        ReportDetailBySceneIdReq req = new ReportDetailBySceneIdReq();
        req.setSceneId(sceneId);
        ResponseResult<ReportDetailResp> result = reportApi.tempReportDetail(req);
        ReportDetailResp resp = result.getData();
        ReportDetailTempOutput output = new ReportDetailTempOutput();
        BeanUtils.copyProperties(resp, output);

        List<Long> allowStartStopUserIdList = WebPluginUtils.getStartStopAllowUserIdList();
        if (CollectionUtils.isNotEmpty(allowStartStopUserIdList)) {
            Long userId = output.getUserId();
            if (userId == null || !allowStartStopUserIdList.contains(userId)) {
                output.setCanStartStop(Boolean.FALSE);
            } else {
                output.setCanStartStop(Boolean.TRUE);
            }
        } else {
            output.setCanStartStop(Boolean.TRUE);
        }
        fillExecuteMan(output);
        return output;

    }

    @Override
    public TrendResponse queryTempReportTrend(TrendRequest param) {
        return cloudReportApi.tempTrend(param);
    }

    @Override
    public List<WarnDetailResponse> listWarn(WarnQueryReq req) {
        return cloudReportApi.listWarn(req).getData();

    }

    @Override
    public List<ActivityResponse> queryReportActivityByReportId(Long reportId) {
        return cloudReportApi.activityByReportId(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
    }

    @Override
    public List<ActivityResponse> queryReportActivityBySceneId(Long sceneId) {
        return cloudReportApi.activityBySceneId(new ReportDetailBySceneIdReq() {{
            setSceneId(sceneId);
        }});
    }

    @Override
    public List<BusinessActivitySummaryBean> querySummaryList(Long reportId) {
        return cloudReportApi.summary(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
    }

    @Override
    public List<MetricesResponse> queryMetrics(Long reportId, Long sceneId, Long tenantId) {
        return cloudReportApi.metrics(new TrendRequest() {{
            setTenantId(tenantId);
            setReportId(reportId);
            setSceneId(sceneId);
        }});
    }

    @Override
    public Map<String, Object> queryReportCount(Long reportId) {
        return cloudReportApi.warnCount(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
    }

    @Override
    public Long queryRunningReport() {
        return cloudReportApi.listRunning(new ContextExt() {{

        }});
    }

    @Override
    public List<Long> queryListRunningReport() {
        CloudCommonInfoWrapperReq req = new CloudCommonInfoWrapperReq();
        ResponseResult<List<Long>> result = reportApi.queryListRunningReport(req);
        if (result == null || !result.getSuccess()) {
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_REPORT_THIRD_PARTY_ERROR,
                Optional.ofNullable(result).map(ResponseResult::getError).map(JsonHelper::bean2Json).orElse("cloud查询异常"));
        }
        return result.getData();
    }

    @Override
    public Boolean lockReport(Long reportId) {
        return cloudReportApi.lock(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
    }

    @Override
    public Boolean unLockReport(Long reportId) {
        return cloudReportApi.unlock(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
    }

    @Override
    public Boolean finishReport(Long reportId) {
        return cloudReportApi.finish(new ReportDetailByIdReq() {{
            setReportId(reportId);
        }});
    }

}