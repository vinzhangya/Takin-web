package io.shulie.takin.web.biz.pojo.output.report;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("压测报告比对-柱状图")
public class ReportCompareColumnarOut implements Serializable {

    private BigDecimal tps;

    private BigDecimal rt;

    private BigDecimal successRate;

    private Long reportId;

    public BigDecimal getTps() {
        return tps != null ? tps.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getRt() {
        return rt != null ? rt.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getSuccessRate() {
        return successRate != null ? successRate.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }
}
