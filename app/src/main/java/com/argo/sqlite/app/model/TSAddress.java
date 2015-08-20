package com.argo.sqlite.app.model;

import com.argo.sqlite.annotations.Column;
import com.argo.sqlite.annotations.RefLink;
import com.argo.sqlite.annotations.Table;

/**
 * Created by user on 8/20/15.
 */
@Table(value = "ts_address")
public class TSAddress {

    @Column(pk = true)
    private int id;

    @Column
    private int personId;

    @Column
    private String name;

    @RefLink(on = "personId")
    private TSPerson person;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TSPerson getPerson() {
        return person;
    }

    public void setPerson(TSPerson person) {
        this.person = person;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TSAddress{");
        sb.append("id=").append(id);
        sb.append(", personId=").append(personId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", person=").append(person);
        sb.append('}');
        return sb.toString();
    }
}
