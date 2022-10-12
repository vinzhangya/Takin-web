package io.shulie.takin.web.data.model.mysql.pressureresource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.shulie.takin.web.data.model.mysql.base.TenantBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * @author xingchen
 * @description: TODO
 * @date 2022/8/30 10:24 AM
 */
@Data
@TableName(value = "t_pressure_resource_relate_table")
@ToString(callSuper = true)
public class PressureResourceRelateTableEntity extends TenantBaseEntity {
    @TableId(value = "ID", type = IdType.AUTO)
    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("资源配置Id")
    @TableField(value = "`resource_id`")
    private Long resourceId;

    @ApiModelProperty("数据源唯一键")
    @TableField(value = "`ds_key`")
    private String dsKey;

    @ApiModelProperty("状态(0-未检测 1-检测失败 2-检测成功)")
    @TableField(value = "`status`")
    private Integer status;

    @ApiModelProperty("业务表")
    @TableField(value = "`business_table`")
    private String businessTable;

    @ApiModelProperty("影子表")
    @TableField(value = "`shadow_table`")
    private String shadowTable;

    @ApiModelProperty("是否加入(0-加入 1-未加入)")
    @TableField(value = "`join_flag`")
    private Integer joinFlag;

    @ApiModelProperty("类型(0-手工 1-自动)")
    @TableField(value = "`type`")
    private Integer type;

    @ApiModelProperty("扩展信息")
    @TableField(value = "`ext_info`")
    private String extInfo;

    @ApiModelProperty("备注")
    @TableField(value = "`remark`")
    private String remark;

    @TableField(value = "`gmt_create`")
    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @TableField(value = "`gmt_modified`")
    @ApiModelProperty("更新时间")
    private Date gmtModified;
}
