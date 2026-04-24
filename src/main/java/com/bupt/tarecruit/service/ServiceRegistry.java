package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.*;
import com.bupt.tarecruit.util.DataBootstrapper;
import com.bupt.tarecruit.util.FileStorageHelper;

import java.nio.file.Path;

/**
 * Central registry for application data access objects and business services.
 * Responsible for initializing persistent storage, bootstrapping default data,
 * and wiring service-layer dependencies.
 */
public class ServiceRegistry {

    /**
     * Root directory that stores application CSV data files.
     */
    private final Path dataDir;

    /**
     * Data access object for TA records.
     */
    private final TaDao taDao;

    /**
     * Data access object for MO records.
     */
    private final MoDao moDao;

    /**
     * Data access object for job records.
     */
    private final JobDao jobDao;

    /**
     * Data access object for application records.
     */
    private final ApplicationDao applicationDao;

    /**
     * Data access object for account log records.
     */
    private final AccountLogDao accountLogDao;

    /**
     * Data access object for job log records.
     */
    private final JobLogDao jobLogDao;

    /**
     * Authentication service.
     */
    private final AuthService authService;

    /**
     * Job management service.
     */
    private final JobService jobService;

    /**
     * Job application service.
     */
    private final ApplicationService applicationService;

    /**
     * User profile service.
     */
    private final ProfileService profileService;

    /**
     * Administrator operation service.
     */
    private final AdminService adminService;
    private final AiService aiService;

    /**
     * Helper for file storage operations such as CV upload and download.
     */
    private final FileStorageHelper fileStorageHelper;

    /**
     * Creates a service registry and initializes all data access objects,
     * services, storage helpers, and startup normalization logic.
     *
     * @param dataDir root directory containing application data files
     */
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
        this.applicationService.normalizePendingApplicationsForClosedJobs();
        this.profileService = new ProfileService(taDao, moDao);
        this.adminService = new AdminService(taDao, moDao, jobDao, accountLogDao);
        this.aiService = new AiService();
        this.fileStorageHelper = new FileStorageHelper(dataDir);
    }

    /**
     * Returns the root data directory used by the application.
     *
     * @return application data directory
     */
    public Path getDataDir() {
        return dataDir;
    }

    /**
     * Returns the authentication service.
     *
     * @return authentication service instance
     */
    public AuthService authService() {
        return authService;
    }

    /**
     * Returns the job service.
     *
     * @return job service instance
     */
    public JobService jobService() {
        return jobService;
    }

    /**
     * Returns the application service.
     *
     * @return application service instance
     */
    public ApplicationService applicationService() {
        return applicationService;
    }

    /**
     * Returns the profile service.
     *
     * @return profile service instance
     */
    public ProfileService profileService() {
        return profileService;
    }

    /**
     * Returns the administrator service.
     *
     * @return administrator service instance
     */
    public AdminService adminService() {
        return adminService;
    }

    public AiService aiService() {
        return aiService;
    }

    /**
     * Returns the file storage helper.
     *
     * @return file storage helper instance
     */
    public FileStorageHelper fileStorageHelper() {
        return fileStorageHelper;
    }

    /**
     * Returns the account log data access object.
     *
     * @return account log DAO instance
     */
    public AccountLogDao accountLogDao() {
        return accountLogDao;
    }

    /**
     * Returns the job log data access object.
     *
     * @return job log DAO instance
     */
    public JobLogDao jobLogDao() {
        return jobLogDao;
    }
}