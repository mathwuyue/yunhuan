package com.yuyue.admin.web.vo;



/**
 * @Author bowen;
 * @Date 2022/05/31 10:05
 */

public enum SearchEnum {
    /**
     * 枚举
     * */
    MALE(1,"男"),
    FEMALE(2,"女"),
    HY(3,"换药"),
    SSHY(4,"手术,换药"),
    SS(5,"手术"),
    NOTHING(6,"未接受治疗")
    ;
    private  int  code;

    private String  value;
    SearchEnum() {
    }

    SearchEnum(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
