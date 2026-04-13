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
### Option 1: Command Line
Navigate to the project root directory and run:
```bash
mvn clean install
```

### Option 2: IntelliJ IDEA
- Open the **Maven** tool window on the right side
- Click the **refresh button (🔄)** at the top to automatically download all dependencies

## 4. Run the Project
### Option 1: Command Line
```bash
mvn javafx:run
```

### Option 2: IntelliJ IDEA
- Open the **Maven** tool window on the right side
- Expand `rec` → `Plugins` → `javafx`
- Double-click **javafx:run** to start the application

---

# Package and Run Executable JAR

### 1. Update version in pom.xml:
```xml
<version>your-version</version>
```

### 2. Build the package:
```bash
mvn clean package
```
### 3. Go to the target directory:
```bash
cd target
```

### 4. Run the JAR (replace "your-version" with the actual version):
```bash
java -jar tarecruit-your-version.jar
```
Example:
```bash
java -jar tarecruit-1.0.0.jar
```
