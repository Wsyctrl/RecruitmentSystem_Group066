# RecruitmentSystem

### Team Members
| GitHub Username | QMID (Student ID) |
|-----------------|-------------------|
| Wsyctrl         | 231226576         |
| Season-art      | 231226912         |
| YUNSHI111       | 231226646 |
| KKkkKKkk501     | 231226635         |
| bibilabu-gugugaga | 231226602       |
| Mellow-zhu | 231226901 |

### Commit Message Standards
All commit messages must strictly follow the format: `[Type]: [Concise Description]`. Refer to the table below for valid types and usage:

| Type       | Description                                                                 | Example                                                |
| :--------- | :-------------------------------------------------------------------------- | :----------------------------------------------------- |
| `feat`     | New feature development                                                     | `feat: add TA profile creation function`               |
| `fix`      | Bug fix                                                                     | `fix: resolve CV upload file format error`             |
| `doc`      | Document modification (all updates in `/doc` folder)                        | `doc: update product backlog in doc folder`            |
| `refactor` | Code refactoring (no new features or bug fixes, only optimization)          | `refactor: optimize TA application status query logic` |
| `test`     | Add or modify test code                                                     | `test: write unit test for MO job posting function`    |
| `chore`    | Routine maintenance tasks (no code/logic changes)                           | `chore: update README.md`                              |

---

# RecruitmentSystem Project Run Guide

## 1. Environment Setup
- JDK 21
- Apache Maven

## 2. Clone the Repository
```bash
git clone https://github.com/Wsyctrl/RecruitmentSystem_Group066.git
```

## 3. Load Maven Dependencies
Navigate to the project root directory and run:
```bash
mvn clean install
```

## 4. Run the Project
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

# Package and Run Executable JAR

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