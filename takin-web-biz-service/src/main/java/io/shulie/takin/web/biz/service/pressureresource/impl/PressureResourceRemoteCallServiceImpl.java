package io.shulie.takin.web.biz.service.pressureresource.impl;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.web.amdb.api.TraceClient;
import io.shulie.takin.web.amdb.bean.query.trace.TraceInfoQueryDTO;
import io.shulie.takin.web.amdb.bean.result.trace.EntryTraceInfoDTO;
import io.shulie.takin.web.biz.pojo.request.pressureresource.*;
import io.shulie.takin.web.biz.service.pressureresource.PressureResourceRemoteCallService;
import io.shulie.takin.web.biz.service.pressureresource.common.CheckStatusEnum;
import io.shulie.takin.web.biz.service.pressureresource.common.PassEnum;
import io.shulie.takin.web.biz.service.pressureresource.common.RemoteCallUtil;
import io.shulie.takin.web.biz.service.pressureresource.common.dy.DynamicCompilerUtil;
import io.shulie.takin.web.biz.service.pressureresource.vo.PressureResourceRelateRemoteCallVO;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.data.dao.application.AppRemoteCallDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceDetailDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceRelateRemoteCallDAO;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceRelateRemoteCallMapper;
import io.shulie.takin.web.data.model.mysql.AppRemoteCallEntity;
import io.shulie.takin.web.data.model.mysql.pressureresource.PressureResourceRelateRemoteCallEntity;
import io.shulie.takin.web.data.param.pressureresource.PressureResourceRemoteCallQueryParam;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingchen
 * @description: TODO
 * @date 2022/9/16 12:06 PM
 */
@Service
public class PressureResourceRemoteCallServiceImpl implements PressureResourceRemoteCallService {
    private static Logger logger = LoggerFactory.getLogger(PressureResourceRemoteCallServiceImpl.class);

    @Resource
    private PressureResourceRelateRemoteCallDAO pressureResourceRelateRemoteCallDAO;

    @Resource
    private PressureResourceRelateRemoteCallMapper pressureResourceRelateRemoteCallMapper;

    @Resource
    private PressureResourceDetailDAO pressureResourceDetailDAO;

    @Resource
    private TraceClient traceClient;

    @Resource
    private AppRemoteCallDAO appRemoteCallDAO;

    /**
     * @param request
     * @return
     */
    @Override
    public PagingList<PressureResourceRelateRemoteCallVO> pageList(PressureResourceRelateRemoteCallRequest request) {
        PressureResourceRemoteCallQueryParam param = new PressureResourceRemoteCallQueryParam();
        BeanUtils.copyProperties(request, param);
        PagingList<PressureResourceRelateRemoteCallEntity> pageList = pressureResourceRelateRemoteCallDAO.pageList(param);

        if (pageList.isEmpty()) {
            return PagingList.of(Collections.emptyList(), pageList.getTotal());
        }
        //转换下
        List<PressureResourceRelateRemoteCallEntity> source = pageList.getList();
        List<PressureResourceRelateRemoteCallVO> returnList = source.stream().map(configDto -> {
            PressureResourceRelateRemoteCallVO vo = new PressureResourceRelateRemoteCallVO();
            BeanUtils.copyProperties(configDto, vo);
            vo.setId(String.valueOf(configDto.getId()));
            // 调用依赖
            if (StringUtils.isNotBlank(vo.getAppName()) && StringUtils.isNotBlank(vo.getServerAppName())) {
                vo.setInvoke(vo.getAppName() + "调用" + vo.getServerAppName());
            }
            return vo;
        }).collect(Collectors.toList());
        return PagingList.of(returnList, pageList.getTotal());
    }

    /**
     * 更新mock
     *
     * @param mockInput
     */
    @Override
    public void update(PressureResourceMockInput mockInput) {
        Long id = mockInput.getId();
        if (id == null) {
            throw new TakinWebException(TakinWebExceptionEnum.ERROR_COMMON, "参数Id未指定");
        }
        PressureResourceRelateRemoteCallEntity update = new PressureResourceRelateRemoteCallEntity();
        update.setId(id);
        MockInfo mockInfo = mockInput.getMockInfo();
        if (mockInfo != null) {
            update.setMockReturnValue(JSON.toJSONString(mockInfo));
        }
        if (mockInput.getPass() != null) {
            update.setPass(mockInput.getPass());
        }
        update.setGmtModified(new Date());
        update.setType(RemoteCallUtil.getType(update));
        // 假如是放行,默认不检测，检测状态直接置位成功
        if (update.getPass() != null && update.getPass() == PassEnum.PASS_YES.getCode()) {
            update.setStatus(CheckStatusEnum.CHECK_FIN.getCode());
        }

        // mockReturnValue更新到app_remote_call表中
        PressureResourceRelateRemoteCallEntity entity = pressureResourceRelateRemoteCallMapper.selectById(id);
        if(entity.getRelateAppRemoteCallId() != null){
            AppRemoteCallEntity ety = new AppRemoteCallEntity();
            ety.setId(entity.getRelateAppRemoteCallId());
            ety.setType(update.getType());
            ety.setMockReturnValue(update.getMockReturnValue());
            ety.setGmtModified(update.getGmtModified());
            appRemoteCallDAO.updateById(ety);
            // 移除数据
            update.setType(null);
            update.setMockReturnValue(null);
        }

        pressureResourceRelateRemoteCallMapper.updateById(update);
    }

    /**
     * 获取服务平均响应时间,id为远程调用服务Id
     *
     * @param id
     * @return
     */
    @Override
    public MockDetailVO mockDetail(Long id) {
        MockDetailVO mockDetailVO = new MockDetailVO();
        mockDetailVO.setRequest(Collections.emptyList());
        mockDetailVO.setResponseTime("0");
        // 远程调用服务Id
        PressureResourceRelateRemoteCallEntity call = pressureResourceRelateRemoteCallMapper.selectById(id);
        if (call == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_QUERY_ERROR, "数据未找到");
        }
        if(call.getRelateAppRemoteCallId() != null){
            AppRemoteCallEntity callEntity = appRemoteCallDAO.getById(call.getRelateAppRemoteCallId());
            populateProperties(call, callEntity);
        }
        TraceInfoQueryDTO traceInfoQueryDTO = new TraceInfoQueryDTO();
        traceInfoQueryDTO.setQueryType(1);
        traceInfoQueryDTO.setAppName(call.getAppName());
        if (call.getInterfaceName().contains("#")) {
            traceInfoQueryDTO.setServiceName(call.getInterfaceName().split("#")[0]);
            traceInfoQueryDTO.setQueryMethodName(call.getInterfaceName().split("#")[1]);
        } else {
            traceInfoQueryDTO.setServiceName(call.getInterfaceName());
        }
        traceInfoQueryDTO.setLogType("2");
        traceInfoQueryDTO.setSortField("startDate");
        traceInfoQueryDTO.setSortType("desc");
        traceInfoQueryDTO.setPageNum(0);
        traceInfoQueryDTO.setPageSize(20);
        PagingList<EntryTraceInfoDTO> pageList = null;
        try {
            pageList = traceClient.listEntryTraceInfo(traceInfoQueryDTO);
            if (pageList == null || pageList.isEmpty()) {
                return mockDetailVO;
            }
        } catch (Throwable e) {
            return mockDetailVO;
        }
        Double avg = pageList.getList().stream().mapToLong(EntryTraceInfoDTO::getCost).average().orElse(0D);
        mockDetailVO.setResponseTime(String.valueOf(Math.floor(avg)));
        List<String> requests = pageList.getList().stream().map(mock -> {
            String request = mock.getRequest();
            if (request.startsWith("{{") && request.endsWith("}}")) {
                request = request.substring(1);
                request = request.substring(0, request.length() - 1);
            }
            return request;
        }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(requests)) {
            mockDetailVO.setRequest(ListUtil.sub(requests, 1, 2));
        }
        return mockDetailVO;
    }

    /**
     * 校验
     *
     * @param mockInfo
     * @return
     */
    @Override
    public PressureResourceCheckVO check(MockInfo mockInfo) {
        PressureResourceCheckVO checkVO = new PressureResourceCheckVO();
        checkVO.setSuccess(true);

        if (StringUtils.isBlank(mockInfo.getMockValue())) {
            checkVO.setRemark("mock数据为空!");
            checkVO.setSuccess(false);
            return checkVO;
        }
        if (mockInfo.getType().equals("0")) {
            try {
                JSON.parseObject(mockInfo.getMockValue());
            } catch (Throwable e) {
                checkVO.setSuccess(false);
                checkVO.setRemark(e.getMessage());
            }
        } else {
            try {
                String remark = DynamicCompilerUtil.check(mockInfo.getMockValue());
                if (StringUtils.isNotBlank(remark)) {
                    checkVO.setRemark(remark);
                    checkVO.setSuccess(false);
                }
            } catch (Throwable e) {
                checkVO.setRemark(e.getMessage());
                checkVO.setSuccess(false);
            }
        }
        return checkVO;
    }

    private void populateProperties(PressureResourceRelateRemoteCallEntity entity, AppRemoteCallEntity appRemoteCall) {
        if(appRemoteCall == null){
            return;
        }
        entity.setInterfaceName(appRemoteCall.getInterfaceName());
        entity.setInterfaceType(appRemoteCall.getInterfaceType());
        entity.setRemark(appRemoteCall.getRemark());
        entity.setType(appRemoteCall.getType());
        entity.setMockReturnValue(appRemoteCall.getMockReturnValue());
        entity.setUserId(appRemoteCall.getUserId());
        entity.setIsSynchronize(appRemoteCall.getIsSynchronize() == null ? 0 : appRemoteCall.getIsSynchronize() ? 1 : 0);
    }
}
