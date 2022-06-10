package com.yuyue.admin.dal.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * chart
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("case_info")
public class CaseInfoPO {
    @TableField("lgt")
    private double lgt;
    @TableField("lat")
    private double lat;
    @TableField("id")
    private Integer id;
    @TableField("detail_address")
    private String detailAddress;
    @TableField("confidence")
    private int confidence;
    @TableField("name")
    private String name;
    @TableField("phone_num1")
    private String phoneNum1;
    @TableField("phone_num2")
    private String phoneNum2;
    @TableField("id_card")
    private String idCard;
    @TableField("province")
    private String province;
    @TableField("city")
    private String city;
    @TableField("district")
    private String district;
    @TableField("town")
    private String town;
    @TableField("street")
    private String street;
    @TableField("file")
    private String file;
    @TableField("save_time")
    private String saveTime;
    @TableField("save_user")
    private String saveUser;
    @TableField("is_del")
    private String isDel;
    @TableField("birthday")
    private LocalDate birthday;
    @TableField("edu")
    private String edu;
    @TableField("tg")
    private float tg;
    @TableField("sex")
    private String sex;
    @TableField("height")
    private double height;
    @TableField("year")
    private int year;
    @TableField("weight")
    private double weight;
    @TableField("job")
    private String job;
    @TableField("disease")
    private String disease;
    @TableField("health_care")
    private String healthCare;
    @TableField("surface_part")
    private String surfacePart;
    @TableField("surface_area")
    private double surfaceArea;
    @TableField("surface_type")
    private String surfaceType;
    @TableField("metabolic_ulcer")
    private String metabolicUlcer;
    @TableField("dm_time")
    private String dmTime;
    @TableField("dm_used")
    private String dmUsed;
    @TableField("dm_course")
    private String dmCourse;
    @TableField("df_time")
    private String dfTime;
    @TableField("df_cause")
    private String dfCause;
    @TableField("df_part")
    private String dfPart;
    @TableField("operation_part")
    private String operationPart;
    @TableField("amputation_part")
    private String amputationPart;
    @TableField("smoke")
    private String smoke;
    @TableField("drink_years")
    private String drinkYears;
    @TableField("drinking")
    private String drinking;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("before_day")
    private String beforeDay;
    @TableField("cure_way")
    private String cureWay;
    @TableField("pathogenic_bacteria")
    private String pathogenicBacteria;
    @TableField("hosipital")
    private int hosipital;
    @TableField("hosipital_address")
    private String hosipitalAddress;
    @TableField("doctor")
    private String doctor;
    @TableField("department")
    private String department;
    @TableField("in_day")
    private String inDay;
    @TableField("cost")
    private double cost;
    @TableField("course")
    private String course;
    @TableField("cure_outcome")
    private String cureOutcome;
    @TableField("operation_course")
    private String operationCourse;
    @TableField("case_source")
    private String caseSource;
    @TableField("bmi")
    private double bmi;
    @TableField("dc")
    private String dc;
    @TableField("hba1c")
    private double hba1c;
    @TableField("wagner")
    private String wagner;
    @TableField("wbc")
    private double wbc;
    @TableField("crp")
    private String crp;
    @TableField("procalcitonin")
    private double procalcitonin;
    @TableField("fpg")
    private double fpg;
    @TableField("uric_acid")
    private double uricAcid;
    @TableField("triglyceride")
    private double triglyceride;
    @TableField("total_cholesterol")
    private double totalCholesterol;
    @TableField("hdlc")
    private double hdlc;
    @TableField("ldlc")
    private double ldlc;
    @TableField("hemoglobin")
    private double hemoglobin;
    @TableField("alb")
    private double alb;
    @TableField("abi")
    private double abi;
    @TableField("cre")
    private double cre;
    @TableField("platelet")
    private double platelet;
    @TableField("neut")
    private double neut;
}
