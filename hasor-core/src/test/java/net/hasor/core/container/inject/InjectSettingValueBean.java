package net.hasor.core.container.inject;
import net.hasor.core.InjectSettings;
import net.hasor.core.SingletonMode;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
//
// 8 种基本类型和其包装类型，以及4种时间类型和常用的枚举、字符串
//
public class InjectSettingValueBean {
    //
    @InjectSettings("byteValue")
    private byte               byteValue;
    @InjectSettings("byteValue")
    private Byte               byteValue2;
    //
    @InjectSettings("shortValue")
    private short              shortValue;
    @InjectSettings("shortValue")
    private Short              shortValue2;
    //
    @InjectSettings("intValue")
    private int                intValue;
    @InjectSettings("intValue")
    private Integer            intValue2;
    //
    @InjectSettings("longValue")
    private long               longValue;
    @InjectSettings("longValue")
    private Long               longValue2;
    //
    @InjectSettings("floatValue")
    private float              floatValue;
    @InjectSettings("floatValue")
    private Float              floatValue2;
    //
    @InjectSettings("doubleValue")
    private double             doubleValue;
    @InjectSettings("doubleValue")
    private Double             doubleValue2;
    //
    @InjectSettings("booleanValue")
    private boolean            booleanValue;
    @InjectSettings("booleanValue")
    private Boolean            booleanValue2;
    //
    @InjectSettings("charValue")
    private char               charValue;
    @InjectSettings("charValue")
    private Character          charValue2;
    //
    @InjectSettings("dateValue")
    private java.util.Date     dateValue1;
    @InjectSettings("dateValue")
    private java.sql.Date      dateValue2;
    @InjectSettings("dateValue")
    private java.sql.Time      dateValue3;
    @InjectSettings("dateValue")
    private java.sql.Timestamp dateValue4;
    //
    @InjectSettings("stringValue")
    private String             stringValue;
    @InjectSettings("enumValue")
    private SingletonMode      enumValue;
    //
    //
    //
    public byte getByteValue() {
        return byteValue;
    }
    public Byte getByteValue2() {
        return byteValue2;
    }
    public short getShortValue() {
        return shortValue;
    }
    public Short getShortValue2() {
        return shortValue2;
    }
    public int getIntValue() {
        return intValue;
    }
    public Integer getIntValue2() {
        return intValue2;
    }
    public long getLongValue() {
        return longValue;
    }
    public Long getLongValue2() {
        return longValue2;
    }
    public float getFloatValue() {
        return floatValue;
    }
    public Float getFloatValue2() {
        return floatValue2;
    }
    public double getDoubleValue() {
        return doubleValue;
    }
    public Double getDoubleValue2() {
        return doubleValue2;
    }
    public boolean isBooleanValue() {
        return booleanValue;
    }
    public Boolean getBooleanValue2() {
        return booleanValue2;
    }
    public char getCharValue() {
        return charValue;
    }
    public Character getCharValue2() {
        return charValue2;
    }
    public Date getDateValue1() {
        return dateValue1;
    }
    public java.sql.Date getDateValue2() {
        return dateValue2;
    }
    public Time getDateValue3() {
        return dateValue3;
    }
    public Timestamp getDateValue4() {
        return dateValue4;
    }
    public String getStringValue() {
        return stringValue;
    }
    public SingletonMode getEnumValue() {
        return enumValue;
    }
}
