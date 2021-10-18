package io.shulie.takin.web.app.conf;

import javax.sql.DataSource;

import com.dangdang.ddframe.job.event.JobEventConfiguration;
import io.shulie.takin.job.ElasticJobProperties;
import io.shulie.takin.job.ElasticRegCenterConfig;
import io.shulie.takin.job.config.ElasticJobConfig;
import io.shulie.takin.job.config.zk.ZkClientConfig;
import io.shulie.takin.job.factory.SpringJobSchedulerFactory;
import io.shulie.takin.job.parser.JobConfParser;
import io.shulie.takin.web.biz.utils.ConfigServerHelper;
import io.shulie.takin.web.common.enums.config.ConfigServerKeyEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 无涯
 * @date 2021/6/14 12:59 上午
 */
@Configuration
@EnableConfigurationProperties(ElasticJobProperties.class)
public class ElasticJobAutoConfig {

    @Value("${env: prod}")
    private String env;

    @Bean
    @ConditionalOnMissingBean(JobEventConfiguration.class)
    public SpringJobSchedulerFactory springJobSchedulerFactory(ElasticJobProperties elasticJobProperties,DataSource dataSource) {
        ElasticJobConfig elasticJobConfig = new ElasticJobConfig();
        ZkClientConfig zkClientConfig = new ZkClientConfig();
        zkClientConfig.setZkServers(ConfigServerHelper.getValueByKey(ConfigServerKeyEnum.TAKIN_CONFIG_ZOOKEEPER_ADDRESS));
        zkClientConfig.setNamespace("takin-web-job-" + env);
        elasticJobConfig.setZkClientConfig(zkClientConfig);
        elasticJobConfig.setDataSource(dataSource);
        ElasticRegCenterConfig elasticRegCenterConfig = new ElasticRegCenterConfig(elasticJobConfig);
        return new SpringJobSchedulerFactory(elasticJobProperties, elasticRegCenterConfig.regCenter(),elasticRegCenterConfig.jobEventConfiguration());
    }

    @Bean
    public JobConfParser jobConfParser(SpringJobSchedulerFactory springJobSchedulerFactory){
        return new JobConfParser(springJobSchedulerFactory);
    }
}
