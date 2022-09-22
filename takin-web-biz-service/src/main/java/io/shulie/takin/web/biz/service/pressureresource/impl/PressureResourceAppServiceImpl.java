package io.shulie.takin.web.biz.service.pressureresource.impl;

import com.pamirs.takin.entity.domain.vo.ApplicationVo;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryBean;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.model.mysql.SceneManageEntity;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.web.biz.pojo.openapi.response.application.ApplicationListResponse;
import io.shulie.takin.web.biz.pojo.request.pressureresource.PressureResourceAppInput;
import io.shulie.takin.web.biz.pojo.request.pressureresource.PressureResourceAppRequest;
import io.shulie.takin.web.biz.service.ApplicationService;
import io.shulie.takin.web.biz.service.pressureresource.PressureResourceAppService;
import io.shulie.takin.web.biz.service.pressureresource.common.JoinFlagEnum;
import io.shulie.takin.web.biz.service.pressureresource.vo.PressureResourceRelateAppVO;
import io.shulie.takin.web.biz.service.scenemanage.SceneManageService;
import io.shulie.takin.web.common.common.Response;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.data.dao.SceneExcludedApplicationDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceDetailDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceRelateAppDAO;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceMapper;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceRelateAppMapper;
import io.shulie.takin.web.data.model.mysql.SceneExcludedApplicationEntity;
import io.shulie.takin.web.data.model.mysql.pressureresource.PressureResourceDetailEntity;
import io.shulie.takin.web.data.model.mysql.pressureresource.PressureResourceEntity;
import io.shulie.takin.web.data.model.mysql.pressureresource.PressureResourceRelateAppEntity;
import io.shulie.takin.web.data.param.pressureresource.PressureResourceAppQueryParam;
import io.shulie.takin.web.data.param.pressureresource.PressureResourceDetailQueryParam;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingchen
 * @description: TODO
 * @date 2022/8/30 2:53 PM
 */
@Service
public class PressureResourceAppServiceImpl implements PressureResourceAppService {
    private static Logger logger = LoggerFactory.getLogger(PressureResourceAppServiceImpl.class);

    @Resource
    private SceneManageService sceneManageService;

    @Resource
    private PressureResourceRelateAppDAO pressureResourceRelateAppDAO;

    @Resource
    private PressureResourceDetailDAO pressureResourceDetailDAO;

    @Resource
    private PressureResourceRelateAppMapper pressureResourceRelateAppMapper;

    @Resource
    private ApplicationService applicationService;

    @Resource
    private PressureResourceMapper pressureResourceMapper;

    @Resource
    private SceneManageDAO sceneManageDAO;

    @Resource
    private SceneExcludedApplicationDAO sceneExcludedApplicationDAO;

    /**
     * 应用检查列表
     *
     * @param request
     * @return
     */
    @Override
    public PagingList<PressureResourceRelateAppVO> appCheckList(PressureResourceAppRequest request) {
        // 校验Resource
        PressureResourceEntity resourceEntity = pressureResourceMapper.selectById(request.getResourceId());
        if (resourceEntity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "配置未查询到");
        }
        PressureResourceAppQueryParam param = new PressureResourceAppQueryParam();
        BeanUtils.copyProperties(request, param);
        if (StringUtils.isNotBlank(request.getEntry())) {
            // 通过entry获取详情Id
            PressureResourceDetailQueryParam queryParam = new PressureResourceDetailQueryParam();
            queryParam.setLinkId(request.getEntry());
            List<PressureResourceDetailEntity> detailEntityList = pressureResourceDetailDAO.getList(queryParam);
            if (CollectionUtils.isNotEmpty(detailEntityList)) {
                List<Long> detailIds = detailEntityList.stream().map(detail -> detail.getId()).distinct().collect(Collectors.toList());
                param.setDetailIds(detailIds);
            } else {
                // 没有找到详情,直接返回空
                return PagingList.of(Collections.emptyList(), 0);
            }
        }

        PagingList<PressureResourceRelateAppEntity> pageList = pressureResourceRelateAppDAO.pageList(param);
        if (pageList.isEmpty()) {
            return PagingList.of(Collections.emptyList(), pageList.getTotal());
        }
        //转换下
        List<PressureResourceRelateAppEntity> source = pageList.getList();
        List<PressureResourceRelateAppVO> returnList = source.stream().map(configDto -> {
            PressureResourceRelateAppVO vo = new PressureResourceRelateAppVO();
            BeanUtils.copyProperties(configDto, vo);
            // 设置主表隔离方式
            vo.setIsolateType(resourceEntity.getIsolateType());
            // 默认是0
            vo.setAgentNodeNum(0);
            // 默认不正常
            vo.setStatus(1);
            vo.setId(String.valueOf(configDto.getId()));
            // 获取应用信息
            List<ApplicationListResponse> list = applicationService.getApplicationList(vo.getAppName());
            if (CollectionUtils.isNotEmpty(list)) {
                Response<ApplicationVo> voResponse = applicationService.getApplicationInfo(String.valueOf(list.get(0).getApplicationId()));
                if (voResponse.getSuccess()) {
                    ApplicationVo applicationVo = voResponse.getData();
                    // 默认等于探针在线节点数
                    vo.setAgentNodeNum(applicationVo.getOnlineNodeNum());
                    vo.setStatus(applicationVo.getAccessStatus().equals(0) ? 0 : 1);
                }
            }
            return vo;
        }).collect(Collectors.toList());
        return PagingList.of(returnList, pageList.getTotal());
    }

    /**
     * 修改检查应用信息
     *
     * @param input
     */
    @Override
    public void update(PressureResourceAppInput input) {
        // 校验数据是否存在
        Long relationAppId = input.getId();
        if (relationAppId == null) {
            throw new TakinWebException(TakinWebExceptionEnum.ERROR_COMMON, "参数Id未指定");
        }
        PressureResourceRelateAppEntity entity = pressureResourceRelateAppMapper.selectById(relationAppId);
        if (entity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_QUERY_ERROR, "未查询到指定数据");
        }

        PressureResourceRelateAppEntity updateEntity = new PressureResourceRelateAppEntity();
        updateEntity.setId(input.getId());
        if (input.getJoinPressure() != null) {
            updateEntity.setJoinPressure(input.getJoinPressure());
            this.processIgnoreApp(updateEntity.getJoinPressure(), entity.getResourceId(), entity.getAppName());
        }
        if (input.getNodeNum() != null) {
            updateEntity.setNodeNum(input.getNodeNum());
        }
        updateEntity.setGmtModified(new Date());
        pressureResourceRelateAppMapper.updateById(updateEntity);
    }

    @Override
    public void processIgnoreApp(Integer joinFlag, Long resourceId, String appName) {
        // 通过ResouceId读取流程
        PressureResourceEntity resourceEntity = pressureResourceMapper.selectById(resourceId);
        Long flowId = resourceEntity.getSourceId();
        if (flowId == null) {
            return;
        }
        // 通过流程查询绑定的场景
        SceneManageQueryBean sceneManageQueryBean = new SceneManageQueryBean();
        sceneManageQueryBean.setBusinessFlowId(flowId);
        List<SceneManageEntity> sceneManageEntityList = sceneManageDAO.queryScene(sceneManageQueryBean);
        // 当不需要加入压测范围的时候,修改压测场景的忽略应用
        if (joinFlag.intValue() == JoinFlagEnum.NO.getCode()) {
            if (CollectionUtils.isNotEmpty(sceneManageEntityList)) {
                sceneManageEntityList.stream().forEach(sceneManageEntity -> {
                    // 忽略检测的应用
                    Long appId = applicationService.queryApplicationIdByAppName(appName);
                    SceneExcludedApplicationEntity tmp = sceneExcludedApplicationDAO.query(sceneManageEntity.getId(), appId);
                    if (tmp == null) {
                        // 不存在则新增
                        sceneManageService.createSceneExcludedApplication(sceneManageEntity.getId(), Arrays.asList(appId));
                    }
                });
            }
        }
        // 假如是加入压测范围的话，判断之前是否有设置到忽略应用列表
        if (joinFlag.intValue() == JoinFlagEnum.YES.getCode()) {
            if (CollectionUtils.isNotEmpty(sceneManageEntityList)) {
                sceneManageEntityList.stream().forEach(sceneManageEntity -> {
                    Long appId = applicationService.queryApplicationIdByAppName(appName);
                    SceneExcludedApplicationEntity tmp = sceneExcludedApplicationDAO.query(sceneManageEntity.getId(), appId);
                    if (tmp != null) {
                        // 存在则删除
                        sceneExcludedApplicationDAO.removeBySceneIdAndAppId(sceneManageEntity.getId(), appId);
                    }
                });
            }
        }
    }
}
