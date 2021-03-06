package me.bartosz1.monitoring.services;

import com.influxdb.client.domain.DeletePredicateRequest;
import me.bartosz1.monitoring.Monitoring;
import me.bartosz1.monitoring.models.Agent;
import me.bartosz1.monitoring.models.ContactList;
import me.bartosz1.monitoring.models.User;
import me.bartosz1.monitoring.models.monitor.Monitor;   
import me.bartosz1.monitoring.models.monitor.MonitorCDO;
import me.bartosz1.monitoring.models.monitor.MonitorType;
import me.bartosz1.monitoring.repos.AgentRepository;
import me.bartosz1.monitoring.repos.ContactListRepository;
import me.bartosz1.monitoring.repos.IncidentRepository;
import me.bartosz1.monitoring.repos.MonitorRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class MonitorService {

    @Autowired
    private MonitorRepository monitorRepository;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private ContactListRepository contactListRepository;
    @Autowired
    private AgentRepository agentRepository;

    @Value("${monitoring.influxdb.enabled}")
    private boolean influxEnabled;
    @Value("${monitoring.influxdb.organization}")
    private String influxOrg;
    @Value("${monitoring.influxdb.bucket}")
    private String influxBucket;
    @Autowired
    private ZoneId zoneId;

    public Monitor addMonitor(User user, MonitorCDO cdo) {
        if (cdo.getType() == MonitorType.AGENT) {
            String id;
            do {
                id = RandomStringUtils.random(16, true, true);
            } while (agentRepository.existsById(id));
            Agent agent = new Agent().setId(id);
            return monitorRepository.save(new Monitor(cdo, user, agent, Instant.now()));
        } else return monitorRepository.save(new Monitor(cdo, user, Instant.now()));
    }

    public Monitor deleteMonitor(long id, User user) {
        Optional<Monitor> optionalMonitor = monitorRepository.findById(id);
        if (optionalMonitor.isPresent()) {
            Monitor monitor = optionalMonitor.get();
            if (monitor.getUser().getId() == user.getId()) {
                incidentRepository.deleteAllByMonitorId(monitor.getId());
                monitorRepository.delete(monitor);
                if (influxEnabled) {
                    OffsetDateTime createdAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(monitor.getCreatedAt()), zoneId);
                    DeletePredicateRequest deletePredicateRequest = new DeletePredicateRequest().start(createdAt);
                    if (monitor.getType() == MonitorType.AGENT) {
                        String agentId = monitor.getAgent().getId();
                        deletePredicateRequest.predicate("_measurement=\"" + agentId + "\"");
                    } else deletePredicateRequest.predicate("_measurement=\"" + monitor.getId() + "\"");
                    deletePredicateRequest.stop(OffsetDateTime.now(zoneId));
                    Monitoring.getInfluxClient().getDeleteApi().delete(deletePredicateRequest, influxBucket, influxOrg);
                }
                return monitor;
            }
        }
        return null;
    }

    //Maybe I shouldn't proxy this?
    public Iterable<Monitor> findAllByUser(User user) {
        return monitorRepository.findAllByUserId(user.getId());
    }

    public Monitor findByIdAndUser(long id, User user) {
        Optional<Monitor> optionalMonitor = monitorRepository.findById(id);
        if (optionalMonitor.isPresent()) {
            Monitor monitor = optionalMonitor.get();
            if (monitor.getUser().getId() == user.getId()) return monitor;
        }
        return null;
    }

    public Monitor renameMonitor(long id, String name, User user) {
        Optional<Monitor> optionalMonitor = monitorRepository.findById(id);
        if (optionalMonitor.isPresent()) {
            Monitor monitor = optionalMonitor.get();
            if (monitor.getUser().getId() == user.getId()) {
                monitor.setName(name);
                return monitorRepository.save(monitor);
            }
        }
        return null;
    }

    public Monitor pauseMonitor(long id, boolean pause, User user) {
        Optional<Monitor> optionalMonitor = monitorRepository.findById(id);
        if (optionalMonitor.isPresent()) {
            Monitor monitor = optionalMonitor.get();
            if (monitor.getUser().getId() == user.getId()) {
                monitor.setPaused(pause);
                return monitorRepository.save(monitor);
            }
        }
        return null;
    }

    public Monitor assignContactList(User user, long monitorId, long contactListId) {
        Optional<ContactList> optionalContactList = contactListRepository.findById(contactListId);
        Optional<Monitor> optionalMonitor = monitorRepository.findById(monitorId);
        if (optionalContactList.isPresent() && optionalMonitor.isPresent()) {
            ContactList contactList = optionalContactList.get();
            Monitor monitor = optionalMonitor.get();
            if (contactList.getUser().getId() == user.getId() && monitor.getUser().getId() == user.getId()) {
                contactList.getMonitors().add(monitor);
                monitor.setContactList(contactList);
                contactListRepository.save(contactList);
                return monitorRepository.save(monitor);
            }
        }
        return null;
    }

    public Monitor unassignContactList(User user, long monitorId) {
        Optional<Monitor> optionalMonitor = monitorRepository.findById(monitorId);
        if (optionalMonitor.isPresent()) {
            Monitor monitor = optionalMonitor.get();
            if (monitor.getUser().getId() == user.getId()) {
                ContactList contactList = monitor.getContactList();
                if (contactList != null) {
                    monitor.getContactList().getMonitors().remove(monitor);
                    contactListRepository.save(monitor.getContactList());
                }
                monitor.setContactList(null);
                return monitorRepository.save(monitor);
            }
        }
        return null;
    }
}
