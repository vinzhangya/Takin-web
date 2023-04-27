package io.shulie.takin.web.biz.pojo.output.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("压测报告RT比对")
public class ReportCompareRtOutput implements Serializable {
    @ApiModelProperty(value = "报告id")
    private Long reportId;
    @ApiModelProperty(value = "压测时长")
    private String pressureTestTime;
    @ApiModelProperty(value = "平均Rt")
    private BigDecimal avgRt;
    @ApiModelProperty(value = "最大Rt")
    private BigDecimal maxRt;
    @ApiModelProperty(value = "最小Rt")
    private BigDecimal minRt;
    @ApiModelProperty(value = "50分位Rt")
    private BigDecimal rt50;
    @ApiModelProperty(value = "75分位Rt")
    private BigDecimal rt75;
    @ApiModelProperty(value = "90分位Rt")
    private BigDecimal rt90;
    @ApiModelProperty(value = "95分位Rt")
    private BigDecimal rt95;
    @ApiModelProperty(value = "99分位Rt")
    private BigDecimal rt99;

    @ApiModelProperty(value = "压测开始时间")
    private String startTime;

    @ApiModelProperty(value = "压测结束时间")
    private String endTime;

    public BigDecimal getAvgRt() {
        return avgRt != null ? avgRt.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getMaxRt() {
        return maxRt != null ? maxRt.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getMinRt() {
        return minRt != null ? minRt.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getRt50() {
        return rt50 != null ? rt50.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getRt75() {
        return rt75 != null ? rt75.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getRt90() {
        return rt90 != null ? rt90.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getRt95() {
        return rt95 != null ? rt95.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }

    public BigDecimal getRt99() {
        return rt99 != null ? rt99.setScale(2, BigDecimal.ROUND_HALF_UP) : null;
    }
}
