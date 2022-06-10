package com.yuyue.admin.dal.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PO基类
 *
 * @author bowen
 * @date 2022-03-10
 */
@Data
public class BasePO {

    /**
     * PK
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField("created_date")
    private LocalDateTime createdDate;

    /**
     * 最后修改人
     */
    @TableField("modified_by")
    private String modifiedBy;

    /**
     * 最后修改时间
     */
    @TableField("last_modified_date")
    private LocalDateTime lastModifiedDate;

}
