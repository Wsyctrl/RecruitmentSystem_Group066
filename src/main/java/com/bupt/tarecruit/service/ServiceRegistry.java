package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.*;
import com.bupt.tarecruit.util.DataBootstrapper;
import com.bupt.tarecruit.util.FileStorageHelper;

import java.nio.file.Path;

public class ServiceRegistry {

    private final Path dataDir;

    private final TaDao taDao;
    private final MoDao moDao;
    private final JobDao jobDao;
    private final ApplicationDao applicationDao;
    private final AccountLogDao accountLogDao;
    private final JobLogDao jobLogDao;

    private final AuthService authService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final ProfileService profileService;
    private final AdminService adminService;

    private final FileStorageHelper fileStorageHelper;

    public ServiceRegistry(Path dataDir) {
        this.dataDir = dataDir;
        new DataBootstrapper(dataDir).initialize();
        this.taDao = new CsvTaDao(dataDir.resolve("TA.csv"));
        this.moDao = new CsvMoDao(dataDir.resolve("MO.csv"));
        this.jobDao = new CsvJobDao(dataDir.resolve("Jobs.csv"));
        this.applicationDao = new CsvApplicationDao(dataDir.resolve("Applications.csv"));
        this.accountLogDao = new CsvAccountLogDao(dataDir.resolve("AccountLogs.csv"));
        this.jobLogDao = new CsvJobLogDao(dataDir.resolve("JobLogs.csv"));

        this.authService = new AuthService(taDao, moDao);
        this.jobService = new JobService(jobDao);
        this.applicationService = new ApplicationService(applicationDao, jobDao);
        // Ensure Applications.csv is consistent with Jobs.csv (no Pending applications for closed jobs).
        this.applicationService.normalizePendingApplicationsForClosedJobs();
        this.profileService = new ProfileService(taDao, moDao);
        this.adminService = new AdminService(taDao, moDao, jobDao, accountLogDao);
        this.fileStorageHelper = new FileStorageHelper(dataDir);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public AuthService authService() {
        return authService;
    }

    public JobService jobService() {
        return jobService;
    }

    public ApplicationService applicationService() {
        return applicationService;
    }

    public ProfileService profileService() {
        return profileService;
    }

    public AdminService adminService() {
        return adminService;
    }

    public FileStorageHelper fileStorageHelper() {
        return fileStorageHelper;
    }

    public AccountLogDao accountLogDao() {
        return accountLogDao;
    }

    public JobLogDao jobLogDao() {
        return jobLogDao;
    }
}
