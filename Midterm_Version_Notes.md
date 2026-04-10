# RecruitmentSystem Midterm Version Notes

## 1. Project Overview
RecruitmentSystem is a recruitment management system built with JavaFX and Maven. It supports TA job applications, MO job posting and management, and administrator-level account and job administration.

## 2. Technology Stack
This project uses the following technologies:

- Java 21
- JavaFX 21 for the desktop user interface
- Maven for build management and application launch
- FXML for UI layout definition
- CSV file storage for local data persistence
- JUnit 5 for unit testing
- ControlsFX, Ikonli, and BootstrapFX for UI enhancement and styling
- OpenCSV for CSV file read and write operations

## 3. System Roles
The system currently contains three roles: TA, MO, and ADMIN.

- TA: job applicant
- MO: job poster and recruitment manager
- ADMIN: system administrator with higher-level global management privileges

## 4. TA Features
TA users are mainly responsible for browsing jobs, applying for jobs, and maintaining their personal profile.

- Browse the list of currently open jobs
- Search jobs by job title, module name, or job requirements
- View job details, including job period, number of positions, current applicants, and hired count
- Apply for a job
- Withdraw applications that are still pending
- View personal application records and status
- Maintain profile information, including name, phone, email, major, skills, experience, and self-evaluation
- Upload and download CV files in TXT format
- Change login password

## 5. MO Features
MO users are mainly responsible for posting jobs, handling applicants, and maintaining their own account information.

- View the list of jobs they posted
- Create new jobs
- Edit posted jobs
- Close jobs
- Reopen closed jobs
- View the applicant list for each job
- View applicant profile information and CV status
- Hire applicants
- Mark applicants as not hired
- Undo hiring and return the applicant to pending status
- Maintain personal profile information
- Change login password

## 6. ADMIN Features
ADMIN is the global administrator of the system. In addition to the normal MO job management capabilities, ADMIN can perform account-level and system-wide management.

- View all TA account information
- View all MO account information
- Reset TA account passwords
- Reset MO account passwords
- Enable or disable TA accounts
- Enable or disable MO accounts
- View the global job list
- Close global jobs
- View account operation logs
- View job operation logs
- Inspect jobs applied for and hired records by TA
- Inspect jobs managed by each MO

## 7. Data and Storage
The system uses CSV files as its local data storage format. The main files include:

- TA.csv: TA account and profile data
- MO.csv: MO account and profile data
- Jobs.csv: job data
- Applications.csv: application records
- AccountLogs.csv: account operation logs
- JobLogs.csv: job operation logs

## 8. Login and Registration
- Users can log in with a student ID or staff ID
- New users can register as TA or MO accounts
- The account format is validated during registration
- Disabled accounts cannot log in

## 9. Version Summary
This midterm version has completed the following core modules:

- Login and registration
- TA job application functionality
- MO job posting and applicant management functionality
- ADMIN global account management and log viewing functionality
- Local persistence based on CSV files

## 10. Future Improvements
- Add more complete job filtering conditions
- Add clearer application status transition prompts
- Improve log querying and export features
- Strengthen input validation and exception handling
