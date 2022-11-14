package io.shulie.takin.web.biz.service.pressureresource.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Maps;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.web.biz.pojo.request.pressureresource.*;
import io.shulie.takin.web.biz.service.pressureresource.*;
import io.shulie.takin.web.biz.service.pressureresource.common.CheckStatusEnum;
import io.shulie.takin.web.biz.service.pressureresource.common.JoinFlagEnum;
import io.shulie.takin.web.biz.service.pressureresource.common.ModuleEnum;
import io.shulie.takin.web.biz.service.pressureresource.common.SourceTypeEnum;
import io.shulie.takin.web.biz.service.pressureresource.vo.*;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.common.util.ActivityUtil;
import io.shulie.takin.web.data.dao.linkmanage.SceneDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceDetailDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceRelateAppDAO;
import io.shulie.takin.web.data.dao.pressureresource.PressureResourceRelateDsDAO;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceDetailMapper;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceMapper;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceRelateDsMapperV2;
import io.shulie.takin.web.data.mapper.mysql.PressureResourceRelateTableMapperV2;
import io.shulie.takin.web.data.model.mysql.pressureresource.*;
import io.shulie.takin.web.data.param.linkmanage.SceneCreateParam;
import io.shulie.takin.web.data.param.linkmanage.SceneUpdateParam;
import io.shulie.takin.web.data.param.pressureresource.PressureResourceDetailQueryParam;
import io.shulie.takin.web.data.param.pressureresource.PressureResourceDsQueryParam;
import io.shulie.takin.web.data.param.pressureresource.PressureResourceQueryParam;
import io.shulie.takin.web.data.result.linkmange.SceneResult;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingchen
 * @description: TODO
 * @date 2022/8/30 2:53 PM
 */
@Service
public class PressureResourceServiceImpl implements PressureResourceService {
    private static Logger logger = LoggerFactory.getLogger(PressureResourceServiceImpl.class);

    @Resource
    private PressureResourceDAO pressureResourceDAO;

    @Resource
    private PressureResourceMapper pressureResourceMapper;

    @Resource
    private PressureResourceRelateDsMapperV2 pressureResourceRelateDsMapperV2;

    @Resource
    private PressureResourceRelateTableMapperV2 pressureResourceRelateTableMapperV2;

    @Resource
    private PressureResourceDetailDAO pressureResourceDetailDAO;

    @Resource
    private PressureResourceDetailMapper pressureResourceDetailMapper;

    @Resource
    private PressureResourceRelateAppDAO pressureResourceRelateAppDAO;

    @Resource
    private PressureResourceRelateDsDAO pressureResourceRelateDsDAO;

    @Resource
    private PressureResourceCommonService pressureResourceCommonService;

    @Resource
    private SceneDAO sceneDAO;

    @Resource
    private PressureResourceAppService pressureResourceAppService;

    @Resource
    private PressureResourceDsService pressureResourceDsService;

    @Resource
    private PressureResourceMqConsumerService pressureResourceMqConsumerService;

    @Resource
    private PressureResourceRemoteCallService pressureResourceRemoteCallService;

    /**
     * 新增
     *
     * @param input
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(PressureResourceInput input) {
        if (StringUtils.isBlank(input.getName())) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "参数未传递");
        }
        PressureResourceEntity entity = pressureResourceDAO.queryByName(input.getName());
        if (entity != null) {
            // 业务流程名字可重复，这里处理下
            if (input.getType().intValue() == SourceTypeEnum.AUTO.getCode()) {
                input.setName(input.getName() + "_" + DateUtil.formatDateTime(new Date()));
            } else {
                throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_QUERY_ERROR, input.getName() + "已存在");
            }
        }
        // 压测资源配置
        PressureResourceEntity insertEntity = new PressureResourceEntity();
        insertEntity.setName(input.getName());
        // 来源Id,业务流程Id
        insertEntity.setSourceId(input.getSourceId());
        insertEntity.setType(input.getType());
        insertEntity.setUserId(input.getUserId());
        insertEntity.setGmtCreate(new Date());
        insertEntity.setGmtModified(new Date());
        Long resourceId = pressureResourceDAO.add(insertEntity);

        // 获取详情
        List<PressureResourceDetailInput> detailInputs = input.getDetailInputs();
        if (CollectionUtils.isNotEmpty(detailInputs)) {
            List<PressureResourceDetailEntity> insertEntityList = convertEntitys(input.getType(), resourceId, detailInputs);
            pressureResourceDetailDAO.batchInsert(insertEntityList);
        }

        // 手工的新增流程
        if (input.getType().intValue() == SourceTypeEnum.MANUAL.getCode()) {
            // 新增业务流程
            SceneCreateParam sceneCreateParam = new SceneCreateParam();
            sceneCreateParam.setSceneName(input.getName());
            sceneCreateParam.setType(1);
            Long sourceId = sceneDAO.insert(sceneCreateParam);

            // 更新sourceId
            PressureResourceEntity updateResource = new PressureResourceEntity();
            updateResource.setId(resourceId);
            updateResource.setSourceId(sourceId);
            pressureResourceMapper.updateById(updateResource);
        }
        pressureResourceCommonService.processNotify(detailInputs);
    }

    @Override
    public void delete(Long resourceId) {
        if (resourceId == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "参数未传递");
        }
        PressureResourceEntity resourceEntity = pressureResourceMapper.selectById(resourceId);
        if (resourceEntity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_QUERY_ERROR, "数据不存在");
        }
        if (resourceEntity.getType().intValue() == SourceTypeEnum.AUTO.getCode()) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "此链路自动新增,无法删除");
        }
        // 删除主表
        pressureResourceMapper.deleteById(resourceId);
        // 删除详情
        QueryWrapper<PressureResourceDetailEntity> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("resource_id", resourceId);
        pressureResourceDetailMapper.delete(detailWrapper);

        // 删除数据源
        QueryWrapper<PressureResourceRelateDsEntityV2> dsWrapper = new QueryWrapper<>();
        dsWrapper.eq("resource_id", resourceId);
        pressureResourceRelateDsMapperV2.delete(dsWrapper);

        // 删除表
        QueryWrapper<PressureResourceRelateTableEntityV2> tableWrapper = new QueryWrapper<>();
        tableWrapper.eq("resource_id", resourceId);
        pressureResourceRelateTableMapperV2.delete(tableWrapper);

        // 删除流程
        SceneUpdateParam updateParam = new SceneUpdateParam();
        updateParam.setId(resourceEntity.getSourceId());
        updateParam.setIsDeleted(1);
        sceneDAO.update(updateParam);
    }

    /**
     * 转换
     *
     * @param type
     * @param resourceId
     * @param detailInputs
     * @return
     */
    private List<PressureResourceDetailEntity> convertEntitys(int type, Long resourceId, List<PressureResourceDetailInput> detailInputs) {
        if (CollectionUtils.isEmpty(detailInputs)) {
            return Collections.EMPTY_LIST;
        }
        List<PressureResourceDetailEntity> insertEntityList = detailInputs.stream().map(detail -> {
            PressureResourceDetailEntity detailEntity = new PressureResourceDetailEntity();
            BeanUtils.copyProperties(detail, detailEntity);

            detailEntity.setId(null);
            // 来源类型
            detailEntity.setType(type);
            detailEntity.setResourceId(resourceId);
            String linkId = ActivityUtil.createLinkId(detail.getEntranceUrl(), detail.getMethod(),
                    detail.getAppName(), detail.getRpcType(), detail.getExtend());
            detailEntity.setLinkId(linkId);
            detailEntity.setGmtCreate(new Date());
            detailEntity.setGmtModified(new Date());
            detailEntity.setTenantId(WebPluginUtils.traceTenantId());
            detailEntity.setEnvCode(WebPluginUtils.traceEnvCode());
            return detailEntity;
        }).collect(Collectors.toList());
        return insertEntityList;
    }

    /**
     * 修改
     *
     * @param input
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(PressureResourceInput input) {
        if (input.getId() == null) {
            throw new TakinWebException(TakinWebExceptionEnum.ERROR_COMMON, "参数Id未指定");
        }
        // 判断是否存在
        PressureResourceEntity entity = pressureResourceMapper.selectById(input.getId());
        if (entity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_QUERY_ERROR, "未查询到指定数据");
        }
        PressureResourceQueryParam param = new PressureResourceQueryParam();
        param.setName(input.getName());
        PressureResourceEntity nameEntity = pressureResourceDAO.queryByName(input.getName());
        if (nameEntity != null && !nameEntity.getId().equals(input.getId())) {
            if (input.getType().intValue() == SourceTypeEnum.MANUAL.getCode()) {
                throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, input.getName() + "已存在");
            } else {
                input.setName(input.getName() + DateUtil.formatDateTime(new Date()));
            }
        }
        PressureResourceEntity updateResourceEntity = new PressureResourceEntity();
        updateResourceEntity.setId(input.getId());
        updateResourceEntity.setName(input.getName());
        // 系统自动扫描的,不要更新时间,排序有影响
        if (input.getType().equals(SourceTypeEnum.AUTO.getCode())) {
            updateResourceEntity.setGmtModified(new Date());
        }
        updateResourceEntity.setUserId(input.getUserId());
        pressureResourceMapper.updateById(updateResourceEntity);

        SceneResult sceneResult = sceneDAO.getSceneDetail(entity.getSourceId());
        if (sceneResult != null && !sceneResult.getSceneName().equals(input.getName())) {
            SceneUpdateParam updateParam = new SceneUpdateParam();
            updateParam.setId(sceneResult.getId());
            updateParam.setSceneName(input.getName());
            updateParam.setUpdateTime(new Date());
            sceneDAO.update(updateParam);
        }
        // 修改详情
        PressureResourceDetailQueryParam detailParam = new PressureResourceDetailQueryParam();
        detailParam.setResourceId(input.getId());
        List<PressureResourceDetailEntity> oldList = pressureResourceDetailDAO.getList(detailParam);
        List<PressureResourceDetailEntity> newList = convertEntitys(input.getType(), input.getId(), input.getDetailInputs());

        Map<String, List<PressureResourceDetailEntity>> newMap = newList.stream().collect(Collectors.groupingBy(ele -> fetchKey(ele)));
        Map<String, List<PressureResourceDetailEntity>> oldMap = oldList.stream().collect(Collectors.groupingBy(ele -> fetchKey(ele)));
        //判断需要新增的,不在oldMap里面的
        List<PressureResourceDetailEntity> insertEntitys = Lists.newArrayList();
        List<PressureResourceDetailEntity> updateEntitys = Lists.newArrayList();
        for (Map.Entry<String, List<PressureResourceDetailEntity>> entry : newMap.entrySet()) {
            String tmpKey = entry.getKey();
            if (!oldMap.containsKey(tmpKey)) {
                // 相同URL和请求方式只有一个
                insertEntitys.add(entry.getValue().get(0));
            } else {
                // 判断下名字是否被修改
                PressureResourceDetailEntity old = oldMap.get(tmpKey).get(0);
                PressureResourceDetailEntity neww = newMap.get(tmpKey).get(0);
                if (old != null && StringUtils.isNotBlank(old.getEntranceName()) && !old.getEntranceName().equals(neww.getEntranceName())) {
                    updateEntitys.add(neww);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(insertEntitys)) {
            pressureResourceDetailDAO.batchInsert(insertEntitys);
        }
        if (CollectionUtils.isNotEmpty(updateEntitys)) {
            // 修改名称
            updateEntitys.stream().forEach(tmp -> {
                pressureResourceDetailDAO.updateEntranceName(tmp);
            });
        }
        // 自动梳理出来的不做删除操作
        if (input.getType().intValue() != SourceTypeEnum.AUTO.getCode()) {
            // 删除的,不在newMap里面的,
            List<Long> deleteIds = Lists.newArrayList();
            for (Map.Entry<String, List<PressureResourceDetailEntity>> entry : oldMap.entrySet()) {
                String tmpKey = entry.getKey();
                if (!newMap.containsKey(tmpKey)) {
                    Long id = entry.getValue().get(0).getId();
                    if (id != null) {
                        deleteIds.add(id);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(deleteIds)) {
                pressureResourceDetailMapper.deleteBatchIds(deleteIds);
            }
        }

        // 把最新的链路去处理链路拓扑
        PressureResourceDetailQueryParam newDetailParam = new PressureResourceDetailQueryParam();
        detailParam.setResourceId(input.getId());
        List<PressureResourceDetailEntity> newDetailList = pressureResourceDetailDAO.getList(newDetailParam);
        List<PressureResourceDetailInput> detailInputs = newDetailList.stream().map(tmp -> {
            PressureResourceDetailInput in = new PressureResourceDetailInput();
            BeanUtils.copyProperties(tmp, in);
            return in;
        }).collect(Collectors.toList());
        pressureResourceCommonService.processNotify(detailInputs);
    }

    private String fetchKey(PressureResourceDetailEntity ele) {
        return String.format("%s|%s|%s|%s|%s", ele.getEntranceUrl(), ele.getMethod(), ele.getAppName(), ele.getRpcType(), ele.getExtend());
    }

    /**
     * 列表查询
     *
     * @param request
     * @return
     */
    @Override
    public PagingList<PressureResourceVO> list(PressureResourceQueryRequest request) {
        PressureResourceQueryParam param = new PressureResourceQueryParam();
        BeanUtils.copyProperties(request, param);

        PagingList<PressureResourceEntity> pageList = pressureResourceDAO.pageList(param);
        if (pageList.isEmpty()) {
            return PagingList.of(Collections.emptyList(), pageList.getTotal());
        }
        //转换下
        List<PressureResourceEntity> source = pageList.getList();
        List<Long> configIds = source.stream().map(configDto -> configDto.getId()).collect(Collectors.toList());
        PressureResourceDetailQueryParam queryParam = new PressureResourceDetailQueryParam();
        queryParam.setResourceIds(configIds);
        List<PressureResourceDetailEntity> detailEntities = pressureResourceDetailDAO.getList(queryParam);
        Map<String, List<PressureResourceDetailEntity>> detailMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(detailEntities)) {
            detailMap = detailEntities.stream().collect(Collectors.groupingBy(entity -> String.valueOf(entity.getResourceId())));
        }
        Map<String, List<PressureResourceDetailEntity>> finalDetailMap = detailMap;
        List<PressureResourceVO> returnList = source.stream().map(configDto -> {
            PressureResourceVO vo = new PressureResourceVO();
            BeanUtils.copyProperties(configDto, vo);
            vo.setId(String.valueOf(configDto.getId()));
            // 设置详情条数
            vo.setDetailCount(finalDetailMap.getOrDefault(String.valueOf(configDto.getId()), Collections.EMPTY_LIST).size());
            // 未开始
            vo.setStatus(processStatus(configDto.getId()));
            // 处理下状态
            return vo;
        }).collect(Collectors.toList());

        return PagingList.of(returnList, pageList.getTotal());
    }

    private int processStatus(Long id) {
        Map<String, Integer> processMap = null;
        try {
            processMap = progress(id);
        } catch (Throwable e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        // 应用状态是完成,如果其他未开始或已完成,状态就为已完成
        Integer appStatus = processMap.get(ModuleEnum.APP.getCode());
        Integer size = processMap.entrySet().stream()
                .filter(entry -> {
                    if (!entry.getKey().equals(ModuleEnum.APP.getCode())) {
                        if (entry.getValue() == FinishStatusEnum.NO.getCode() ||
                                entry.getValue() == FinishStatusEnum.FINSH.getCode()) {
                            return true;
                        }
                    }
                    return false;
                }).map(entry -> entry.getValue()).collect(Collectors.toList()).size();

        if (appStatus == FinishStatusEnum.FINSH.getCode() && size == processMap.size() - 1) {
            return FinishStatusEnum.FINSH.getCode();
        }
        List<Integer> status = processMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
        Integer normal = status.stream().filter(var -> var == FinishStatusEnum.FINSH.getCode()).collect(Collectors.toList()).size();
        Integer ing = status.stream().filter(var -> var == FinishStatusEnum.START_ING.getCode()).collect(Collectors.toList()).size();
        if (ing > 0) {
            return FinishStatusEnum.START_ING.getCode();  // 进行中
        }
        if (normal == status.size()) {
            return FinishStatusEnum.FINSH.getCode();//已完成
        }
        return FinishStatusEnum.NO.getCode(); // 未开始
    }

    /**
     * 详情
     *
     * @param request
     * @return
     */
    @Override
    public PressureResourceInfoVO detail(PressureResourceQueryRequest request) {
        PressureResourceInfoVO infoVO = new PressureResourceInfoVO();

        PressureResourceEntity resourceEntity = pressureResourceMapper.selectById(request.getId());
        if (resourceEntity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_QUERY_ERROR, "数据不存在");
        }
        infoVO.setId(String.valueOf(resourceEntity.getId()));
        infoVO.setName(resourceEntity.getName());
        // 判断是否有链路信息
        PressureResourceDetailQueryParam param = new PressureResourceDetailQueryParam();
        param.setResourceId(request.getId());
        List<PressureResourceDetailEntity> detailList = pressureResourceDetailDAO.getList(param);
        if (CollectionUtils.isEmpty(detailList)) {
            return infoVO;
        }
        List<PressureResourceDetailVO> detailVOList = detailList.stream().map(detail -> {
            PressureResourceDetailVO tmpDetailVo = new PressureResourceDetailVO();
            BeanUtils.copyProperties(detail, tmpDetailVo);
            tmpDetailVo.setValue(String.valueOf(detail.getId()));
            tmpDetailVo.setId(String.valueOf(detail.getId()));
            tmpDetailVo.setResourceId(String.valueOf(detail.getResourceId()));
            return tmpDetailVo;
        }).collect(Collectors.toList());
        infoVO.setDetailInputs(detailVOList);
        return infoVO;
    }

    /**
     * 修改隔离方式
     *
     * @param isolateInput
     */
    @Override
    public void updateIsolate(PressureResourceIsolateInput isolateInput) {
        if (isolateInput.getId() == null) {
            throw new TakinWebException(TakinWebExceptionEnum.ERROR_COMMON, "参数Id未指定");
        }
        // 判断是否存在
        PressureResourceEntity entity = pressureResourceMapper.selectById(isolateInput.getId());
        if (entity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.INTERFACE_PERFORMANCE_QUERY_ERROR, "未查询到指定数据");
        }
        // 修改主表隔离方式
        PressureResourceEntity updateEntity = new PressureResourceEntity();
        updateEntity.setId(entity.getId());
        updateEntity.setIsolateType(isolateInput.getIsolateType());
        updateEntity.setGmtModified(new Date());
        pressureResourceMapper.updateById(updateEntity);
    }

    /**
     * 处理进度
     *
     * @param id
     * @return
     */
    @Override
    public Map<String, Integer> progress(Long id) {
        Map<String, Integer> statusMap = Maps.newHashMap();
        statusMap.put(ModuleEnum.APP.getCode(), FinishStatusEnum.NO.getCode());
        statusMap.put(ModuleEnum.DS.getCode(), FinishStatusEnum.NO.getCode());
        statusMap.put(ModuleEnum.REMOTECALL.getCode(), FinishStatusEnum.NO.getCode());
        statusMap.put(ModuleEnum.MQ.getCode(), FinishStatusEnum.NO.getCode());

        // 查看应用状态
        PressureResourceAppRequest appRequest = new PressureResourceAppRequest();
        appRequest.setResourceId(id);
        PagingList<PressureResourceRelateAppVO> pageList = pressureResourceAppService.appCheckList(appRequest);
        if (!pageList.isEmpty()) {
            List<PressureResourceRelateAppVO> appVOList = pageList.getList();
            // 判断状态是否都是正常的,不需要检查的,status会设置为null
            int normal = appVOList.stream().filter(app -> app.getStatus() == null || app.getStatus() == 0).collect(Collectors.toList()).size();
            if (normal == appVOList.size()) {
                statusMap.put(ModuleEnum.APP.getCode(), FinishStatusEnum.FINSH.getCode());
            }
            // 存在正常的,进行中
            if (appVOList.size() - normal > 0) {
                statusMap.put(ModuleEnum.APP.getCode(), FinishStatusEnum.START_ING.getCode());
            }
        }

        // 影子资源检查
        PressureResourceDsQueryParam dsQueryParam = new PressureResourceDsQueryParam();
        dsQueryParam.setResourceId(id);
        List<RelateDsEntity> dsEntityList = pressureResourceRelateDsDAO.queryByParam_v2(dsQueryParam);
        if (CollectionUtils.isNotEmpty(dsEntityList)) {
            // 判断状态是否都是正常的
            int normal = dsEntityList.stream().filter(ds -> ds.getStatus() == CheckStatusEnum.CHECK_FIN.getCode()).collect(Collectors.toList()).size();
            if (normal == dsEntityList.size()) {
                statusMap.put(ModuleEnum.DS.getCode(), FinishStatusEnum.FINSH.getCode());
            }
            // 存在正常的,进行中
            if (dsEntityList.size() - normal > 0) {
                statusMap.put(ModuleEnum.DS.getCode(), FinishStatusEnum.START_ING.getCode());
            }
        }

        // 远程调用
        PressureResourceRelateRemoteCallRequest callRequest = new PressureResourceRelateRemoteCallRequest();
        callRequest.setResourceId(id);
        callRequest.setPageSize(2000);
        callRequest.setConvert(true);
        PagingList<PressureResourceRelateRemoteCallVO> callPagingList = pressureResourceRemoteCallService.pageList(callRequest);
        if (!callPagingList.isEmpty()) {
            List<PressureResourceRelateRemoteCallVO> callVOList = callPagingList.getList();
            // 判断状态是否都是正常的
            int normal = callVOList.stream().filter(app -> app.getStatus() == CheckStatusEnum.CHECK_FIN.getCode()).collect(Collectors.toList()).size();
            if (normal == callVOList.size()) {
                statusMap.put(ModuleEnum.REMOTECALL.getCode(), FinishStatusEnum.FINSH.getCode());
            }
            // 存在正常的,进行中
            if (callVOList.size() - normal > 0) {
                statusMap.put(ModuleEnum.REMOTECALL.getCode(), FinishStatusEnum.START_ING.getCode());
            }
        }

        // 影子MQ检查
        PressureResourceMqConsumerQueryRequest mqRequest = new PressureResourceMqConsumerQueryRequest();
        mqRequest.setResourceId(id);
        mqRequest.setPageSize(2000);
        PagingList<PressureResourceMqComsumerVO> mqPageList = pressureResourceMqConsumerService.list(mqRequest);
        if (!mqPageList.isEmpty()) {
            List<PressureResourceMqComsumerVO> appVOList = mqPageList.getList();
            // 判断状态是否都是正常的
            int normal = appVOList.stream().filter(app -> app.getStatus() == CheckStatusEnum.CHECK_FIN.getCode()).collect(Collectors.toList()).size();
            if (normal == appVOList.size()) {
                statusMap.put(ModuleEnum.MQ.getCode(), FinishStatusEnum.FINSH.getCode());
            }
            // 存在正常的,进行中
            if (appVOList.size() - normal > 0) {
                statusMap.put(ModuleEnum.MQ.getCode(), FinishStatusEnum.START_ING.getCode());
            }
        }
        return statusMap;
    }

    /**
     * 处理页面汇总数据
     *
     * @return
     */
    @Override
    public PressureResourceExtInfo appInfo(Long id) {
        PressureResourceExtInfo extInfo = new PressureResourceExtInfo();
        extInfo.setTotalSize(0);
        extInfo.setExceptionSize(0);
        extInfo.setNormalSize(0);

        PressureResourceEntity entity = pressureResourceMapper.selectById(id);
        if (entity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "配置不存在");
        }
        PressureResourceAppRequest appRequest = new PressureResourceAppRequest();
        appRequest.setResourceId(id);
        appRequest.setPageSize(500);
        PagingList<PressureResourceRelateAppVO> pageList = pressureResourceAppService.appCheckList(appRequest);
        if (!pageList.isEmpty()) {
            List<PressureResourceRelateAppVO> appVOList = pageList.getList();
            if (CollectionUtils.isNotEmpty(appVOList)) {
                // 总的应用数
                extInfo.setTotalSize(appVOList.size());
                // 正常的应用数
                Long normalSize = appVOList.stream().filter(app -> app.getStatus() == 0 || app.getJoinPressure() == JoinFlagEnum.NO.getCode()).count();
                extInfo.setNormalSize(normalSize.intValue());
                extInfo.setExceptionSize(appVOList.size() - normalSize.intValue());
            }
        }
        // 检测时间都是一批的
        extInfo.setCheckTime(entity.getCheckTime());
        extInfo.setUserName(WebPluginUtils.getUserName(entity.getUserId(), WebPluginUtils.getUserMapByIds(Arrays.asList(entity.getUserId()))));
        return extInfo;
    }

    /**
     * 汇总信息-数据源
     *
     * @param id
     * @return
     */
    @Override
    public PressureResourceExtInfo dsInfo(Long id) {
        PressureResourceExtInfo extInfo = new PressureResourceExtInfo();
        extInfo.setTotalSize(0);
        extInfo.setExceptionSize(0);
        extInfo.setNormalSize(0);

        PressureResourceEntity entity = pressureResourceMapper.selectById(id);
        if (entity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "配置不存在");
        }
        PressureResourceRelateDsRequest dsRequest = new PressureResourceRelateDsRequest();
        dsRequest.setResourceId(id);
        dsRequest.setPageSize(2000);
        PagingList<PressureResourceRelateDsVO> pageList = pressureResourceDsService.listByDs(dsRequest);
        if (!pageList.isEmpty()) {
            List<PressureResourceRelateDsVO> resourceRelateDsVOS = pageList.getList();
            if (CollectionUtils.isNotEmpty(resourceRelateDsVOS)) {
                extInfo.setTotalSize(resourceRelateDsVOS.size());
                Long normalSize = resourceRelateDsVOS.stream().filter(ds -> ds.getStatus() == 2).count();
                extInfo.setNormalSize(normalSize.intValue());
                extInfo.setExceptionSize(extInfo.getTotalSize() - extInfo.getNormalSize());
            }
        }
        // 检测时间都是一批的
        extInfo.setCheckTime(entity.getCheckTime());
        extInfo.setUserName(WebPluginUtils.getUserName(entity.getUserId(), WebPluginUtils.getUserMapByIds(Arrays.asList(entity.getUserId()))));
        return extInfo;
    }

    /**
     * 汇总信息-影子消费者
     *
     * @param id
     * @return
     */
    @Override
    public PressureResourceExtInfo mqInfo(Long id) {
        PressureResourceExtInfo extInfo = new PressureResourceExtInfo();
        extInfo.setTotalSize(0);
        extInfo.setExceptionSize(0);
        extInfo.setNormalSize(0);

        PressureResourceEntity entity = pressureResourceMapper.selectById(id);
        if (entity == null) {
            throw new TakinWebException(TakinWebExceptionEnum.PRESSURE_RESOURCE_OP_ERROR, "配置不存在");
        }
        PressureResourceMqConsumerQueryRequest mqRequest = new PressureResourceMqConsumerQueryRequest();
        mqRequest.setResourceId(id);
        mqRequest.setPageSize(2000);
        PagingList<PressureResourceMqComsumerVO> pageList = pressureResourceMqConsumerService.list(mqRequest);
        if (!pageList.isEmpty()) {
            List<PressureResourceMqComsumerVO> resourceRelateDsVOS = pageList.getList();
            if (CollectionUtils.isNotEmpty(resourceRelateDsVOS)) {
                extInfo.setTotalSize(resourceRelateDsVOS.size());
                Long normalSize = resourceRelateDsVOS.stream().filter(ds -> ds.getStatus() == 2).count();
                extInfo.setNormalSize(normalSize.intValue());
                extInfo.setExceptionSize(extInfo.getTotalSize() - extInfo.getNormalSize());
            }
        }
        // 检测时间都是一批的
        extInfo.setCheckTime(entity.getCheckTime());
        extInfo.setUserName(WebPluginUtils.getUserName(entity.getUserId(), WebPluginUtils.getUserMapByIds(Arrays.asList(entity.getUserId()))));
        return extInfo;
    }
}
