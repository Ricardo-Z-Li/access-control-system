package acs.log;

import acs.domain.LogEntry;

public interface LogService {

    void record(LogEntry entry);
}
