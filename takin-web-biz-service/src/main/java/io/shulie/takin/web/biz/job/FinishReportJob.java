package io.shulie.takin.web.biz.job;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import io.shulie.takin.job.annotation.ElasticSchedulerJob;
import io.shulie.takin.web.biz.common.AbstractSceneTask;
import io.shulie.takin.web.biz.service.report.ReportTaskService;
import io.shulie.takin.web.common.pojo.dto.SceneTaskDto;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author 无涯
 * @date 2021/6/15 6:08 下午
 */
@Component
@ElasticSchedulerJob(jobName = "finishReportJob",
    // 分片序列号和参数用等号分隔 不需要参数可以不加
    //shardingItemParameters = "0=0,1=1,2=2",
    isSharding = true,
    cron = "*/10 * * * * ?",
    description = "压测报告状态，汇总报告")
@Slf4j
public class FinishReportJob extends AbstractSceneTask implements SimpleJob {
    @Autowired
    private ReportTaskService reportTaskService;

    @Autowired
    @Qualifier("reportFinishThreadPool")
    private ThreadPoolExecutor reportThreadPool;

    private static Map<Long, AtomicInteger> runningTasks = new ConcurrentHashMap<>();
    private static AtomicInteger EMPTY = new AtomicInteger();

    @Override
    public void execute(ShardingContext shardingContext) {
        long start = System.currentTimeMillis();
        final Boolean openVersion = WebPluginUtils.isOpenVersion();
        //任务开始
        while (true){
            List<SceneTaskDto> taskDtoList = getTaskFromRedis();
            if (taskDtoList == null) { break; }
            if(openVersion) {
                for (SceneTaskDto taskDto : taskDtoList) {
                    Long reportId = taskDto.getReportId();
                    // 私有化 + 开源 根据 报告id进行分片
                    // 开始数据层分片
                    if (reportId % shardingContext.getShardingTotalCount() == shardingContext.getShardingItem()) {
                        Object task = runningTasks.putIfAbsent(reportId, EMPTY);
                        if (task == null) {
                            reportThreadPool.execute(() -> {
                                try {
                                    reportTaskService.finishReport(reportId,taskDto);
                                } catch (Throwable e) {
                                    log.error("execute FinishReportJob occured error. reportId={}", reportId, e);
                                } finally {
                                    runningTasks.remove(reportId);
                                }
                            });
                        }
                    }
                }
                cleanUnAvailableTasks(taskDtoList);
            }else {
                //每个租户可以使用的最大线程数
                int allowedTenantThreadMax = this.getAllowedTenantThreadMax();
                //筛选出租户的任务
                final Map<Long, List<SceneTaskDto>> listMap = taskDtoList.stream().filter(t->{
                    //分片：web1= 0^0、1^1  和 web2= 0^1、1^0
                    long x =  t.getTenantId() % shardingContext.getShardingTotalCount();
                    long y =  t.getReportId() % shardingContext.getShardingTotalCount();
                    return (x ^ y) == shardingContext.getShardingItem();
                }).collect(Collectors.groupingBy(SceneTaskDto::getTenantId));
                if (CollectionUtils.isEmpty(listMap)){
                    return;
                }
                for (Entry<Long, List<SceneTaskDto>> listEntry : listMap.entrySet()) {
                    final List<SceneTaskDto> tenantTasks = listEntry.getValue();
                    if (CollectionUtils.isEmpty(tenantTasks)) {
                        continue;
                    }
                    long tenantId = listEntry.getKey();
                    /**
                     * 取最值。当前租户的任务数和允许的最大线程数
                     */
                    AtomicInteger allowRunningThreads = new AtomicInteger(
                        Math.min(allowedTenantThreadMax, tenantTasks.size()));

                    /**
                     * 已经运行的任务数
                     */
                    AtomicInteger oldRunningThreads = runningTasks.putIfAbsent(tenantId, allowRunningThreads);
                    if (oldRunningThreads != null) {
                        /**
                         * 剩下允许执行的任务数
                         * allow running threads calculated by capacity
                         */
                        int permitsThreads = Math.min(allowedTenantThreadMax - oldRunningThreads.get(),
                            allowRunningThreads.get());
                        // add new threads to capacity
                        oldRunningThreads.addAndGet(permitsThreads);
                        // adjust allow current running threads
                        allowRunningThreads.set(permitsThreads);
                    }

                    for (int i = 0; i < allowRunningThreads.get(); i++) {
                        final SceneTaskDto task = tenantTasks.get(i);
                        runTaskInTenantIfNecessary(task, task.getReportId());
                    }
                    cleanUnAvailableTasks(tenantTasks);
                }
            }
        }
        log.debug("finishReport 执行时间:{}", System.currentTimeMillis() - start);
    }

    @Override
    protected void runTaskInTenantIfNecessary( SceneTaskDto tenantTask, Long reportId) {
        //将任务放入线程池
        reportThreadPool.execute(() -> {
            try {
                WebPluginUtils.setTraceTenantContext(tenantTask);
                reportTaskService.finishReport(reportId, tenantTask);
            } catch (Throwable e) {
                log.error("execute FinishReportJob occured error. reportId={}", reportId, e);
            } finally {
                AtomicInteger currentRunningThreads = runningTasks.get(tenantTask.getTenantId());
                if (currentRunningThreads != null) {
                    currentRunningThreads.decrementAndGet();
                }
            }
        });
    }
}
