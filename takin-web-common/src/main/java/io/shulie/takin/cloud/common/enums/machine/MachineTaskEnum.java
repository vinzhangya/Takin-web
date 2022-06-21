package io.shulie.takin.cloud.common.enums.machine;

/**
 * @author fanxx
 * @date 2020/5/13 上午10:49
 */
public enum MachineTaskEnum {
    /**
     * 机器任务类型：开通任务、销毁任务
     */
    OPEN(1, "开通任务"),
    DESTORY(2, "销毁任务");
    private Integer code;
    private String status;

    MachineTaskEnum(Integer code, String status) {
        this.code = code;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
