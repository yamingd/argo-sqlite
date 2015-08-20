package com.argo.sqlite.app.model;

import com.argo.sqlite.annotations.Column;
import com.argo.sqlite.annotations.Table;

import java.util.Date;

/**
 * Created by user on 8/20/15.
 */
@Table(value = "ts_person")
public class TSPerson {

    @Column(pk = true)
    private int id;

    @Column
    private String name;

    @Column
    private Date birthday;

    @Column
    private byte[] secretData;

    @Column
    private boolean female;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public byte[] getSecretData() {
        return secretData;
    }

    public void setSecretData(byte[] secretData) {
        this.secretData = secretData;
    }

    public boolean isFemale() {
        return female;
    }

    public void setFemale(boolean female) {
        this.female = female;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TSPerson{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
