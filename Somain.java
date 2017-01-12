package org.linuxsogood.reference.chp1.pdf;

/**
 * Created by honway on 2017/1/12.
 */
public class Somain {

    private String soId;
    private String customerCode;
    private String name;
    private String createTime;
    private String account;
    private String soTotal;
    private String prePayFee;
    private String payType;

    public Somain(String soId, String customerCode, String name, String createTime, String account, String soTotal, String prePayFee, String payType) {
        this.soId = soId;
        this.customerCode = customerCode;
        this.name = name;
        this.createTime = createTime;
        this.account = account;
        this.soTotal = soTotal;
        this.prePayFee = prePayFee;
        this.payType = payType;
    }

    public String getSoId() {
        return soId;
    }

    public void setSoId(String soId) {
        this.soId = soId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSoTotal() {
        return soTotal;
    }

    public void setSoTotal(String soTotal) {
        this.soTotal = soTotal;
    }

    public String getPrePayFee() {
        return prePayFee;
    }

    public void setPrePayFee(String prePayFee) {
        this.prePayFee = prePayFee;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }
}
