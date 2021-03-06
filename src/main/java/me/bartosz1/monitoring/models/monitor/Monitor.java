package me.bartosz1.monitoring.models.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import me.bartosz1.monitoring.models.Agent;
import me.bartosz1.monitoring.models.ContactList;
import me.bartosz1.monitoring.models.Incident;
import me.bartosz1.monitoring.models.User;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private MonitorType type;
    private String host;
    private int timeout;
    private int retries;
    private boolean verifySSLCerts;
    private MonitorStatus lastStatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIncludeProperties({"id"})
    @JsonUnwrapped(prefix = "contactlist")
    private ContactList contactList;
    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SELECT)
    private List<Incident> incidents;
    @ManyToOne
    @JsonIgnore
    private User user;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "agent_id")
    private Agent agent;
    //easier to store as string
    private String allowedHttpCodes;
    private long createdAt;
    private boolean paused;
    public Monitor() {
    }

    public Monitor(MonitorCDO cdo, User user, Instant createdAt) {
        this.name = cdo.getName();
        this.host = cdo.getHost();
        this.timeout = cdo.getTimeout();
        this.type = cdo.getType();
        this.verifySSLCerts = cdo.getVerifySSL();
        this.retries = cdo.getRetries();
        this.allowedHttpCodes = cdo.getAllowedHttpCodes();
        this.user = user;
        this.createdAt = createdAt.getEpochSecond();
        this.paused = false;
    }

    public Monitor(MonitorCDO cdo, User user, Agent agent, Instant createdAt) {
        this.name = cdo.getName();
        this.host = cdo.getHost();
        this.timeout = cdo.getTimeout();
        this.type = cdo.getType();
        this.verifySSLCerts = cdo.getVerifySSL();
        this.retries = cdo.getRetries();
        this.allowedHttpCodes = cdo.getAllowedHttpCodes();
        this.user = user;
        this.agent = agent;
        this.createdAt = createdAt.getEpochSecond();
        this.paused = false;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Monitor setName(String name) {
        this.name = name;
        return this;
    }


    public String getHost() {
        return host;
    }

    public Monitor setHost(String host) {
        this.host = host;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public Monitor setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public Monitor setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public boolean getVerifySSL() {
        return verifySSLCerts;
    }

    public Monitor setVerifySSLCerts(boolean verifySSLCerts) {
        this.verifySSLCerts = verifySSLCerts;
        return this;
    }

    public MonitorStatus getLastStatus() {
        return lastStatus;
    }

    public Monitor setLastStatus(MonitorStatus lastStatus) {
        this.lastStatus = lastStatus;
        return this;
    }

    public ContactList getContactList() {
        return contactList;

    }

    public Monitor setContactList(ContactList contactList) {
        this.contactList = contactList;
        return this;
    }

    public String getAllowedHttpCodes() {
        return allowedHttpCodes;
    }

    @JsonIgnore
    public List<Integer> getAllowedHttpCodesAsList() {
        return Arrays.stream(allowedHttpCodes.split(",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    public Monitor setAllowedHttpCodes(String allowedHttpCodes) {
        this.allowedHttpCodes = allowedHttpCodes;
        return this;
    }

    public Monitor setAllowedHttpCodes(List<Integer> allowedHttpCodes) {
        StringBuilder sb = new StringBuilder();
        allowedHttpCodes.forEach(code -> sb.append(code).append(","));
        this.allowedHttpCodes = sb.toString();
        return this;
    }

    public Agent getAgent() {
        return agent;
    }

    public Monitor setAgent(Agent agent) {
        this.agent = agent;
        return this;
    }

    public List<Incident> getIncidents() {
        return incidents;
    }

    public Monitor setIncidents(List<Incident> incidents) {
        this.incidents = incidents;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Monitor setUser(User user) {
        this.user = user;
        return this;
    }

    public Monitor addIncident(Incident incident) {
        incidents.add(incident);
        return this;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public MonitorType getType() {
        return type;
    }

    public Monitor getType(MonitorType monitorType) {
        this.type = monitorType;
        return this;
    }
}
