# Recruitment System

**BUPT TA Recruitment System** — desktop application for Teaching Assistant recruitment at BUPT.

Two separate JavaFX portals:

| Portal | Users |
|--------|--------|
| **TA Portal** | TA applicants |
| **MO/Admin Portal** | Module Officers and system administrators |

For login, workflows, and screenshots, see the end-user guide: [`doc/User_Manual.md`](doc/User_Manual.md).

---

## Setup

### Environment Setup

- JDK 21
- Apache Maven

### Clone the Repository

```bash
git clone https://github.com/Wsyctrl/RecruitmentSystem_Group066.git
```

### Load Maven Dependencies

Navigate to the project root directory and run:

```bash
mvn clean install
```

---

## Configure

- **Data:** Keep the `data/` folder in the project root (CSV accounts, jobs, applications, CV files). The app reads/writes it from the working directory when you run via Maven or from the folder that contains the JAR in a release package.
- **AI (optional):** Set `QWEN_API_KEY`, or edit `src/main/resources/ai-config.properties` (`qwen.api.key=...`). Without a key, the app runs normally; AI features will error when used.

---

## Run

Run different portals with dedicated execution IDs:

- TA Portal：
```bash
mvn javafx:run@ta-portal
```
- MO/Admin Portal：
```bash
mvn javafx:run@mo-portal
```

---

## Package and Run Executable JAR

(replace "your-version" with the actual version)

### 1. Update top-level project version in pom.xml:
```xml
<version>your-version</version>
```

### 2. Build two standalone JARs:
```bash
mvn clean package
```

- After build completes, the JAR files will be generated in the `target/` folder:
    - `tarecruit-ta-your-version.jar` (TA Portal)
    - `tarecruit-mo-your-version.jar` (MO/Admin Portal)


### 3. Prepare release folder
Create a folder (for example `release/`) and copy these files:
- Executable JAR files: `tarecruit-ta-your-version.jar` and `tarecruit-mo-your-version.jar`
- Application data: prebuilt `data/` folder (copy the entire data folder from the project root directory)
- Startup scripts: `run-ta.bat` and `run-mo.bat`

`run-ta.bat` content:
```bat
java -jar tarecruit-ta-your-version.jar
pause
```

`run-mo.bat` content:
```bat
java -jar tarecruit-mo-your-version.jar
pause
```

### 4. Distribute
Zip the `release/` folder and send it to users.

### 5. Run on user side
After unzip, run by double-clicking:
- `run-ta.bat` (TA Portal)
- `run-mo.bat` (MO/Admin Portal)

---

## License

This project is licensed under the [MIT License](LICENSE).

Third-party libraries and their licenses are documented in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

---

## Team Members

| GitHub Username | QMID (Student ID) |
|-----------------|-------------------|
| Wsyctrl | 231226576 |
| Season-art | 231226912 |
| YUNSHI111 | 231226646 |
| KKkkKKkk501 | 231226635 |
| bibilabu-gugugaga | 231226602 |
| Mellow-zhu | 231226901 |

---

## Commit Message Standards

All commit messages must follow: `[Type]: [Concise Description]`

| Type | Description | Example |
| :--- | :---------- | :------ |
| `feat` | New feature | `feat: add TA profile creation function` |
| `fix` | Bug fix | `fix: resolve CV upload file format error` |
| `doc` | Document updates (especially `/doc`) | `doc: update product backlog in doc folder` |
| `refactor` | Refactor without feature/fix | `refactor: optimize TA application status query logic` |
| `test` | Add or change tests | `test: write unit test for MO job posting function` |
| `chore` | Maintenance (no logic change) | `chore: update README.md` |
