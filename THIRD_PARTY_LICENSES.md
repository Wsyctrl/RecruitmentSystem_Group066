# Third-Party Software Notices

**BUPT TA Recruitment System** (`tarecruit`) version 4.0.0

Project license: [MIT](LICENSE)

This document lists open-source and third-party components used by this project. Versions match `pom.xml` at the time of this notice. Transitive dependencies are included where they appear in the packaged application (`jar-with-dependencies`).

For the current dependency tree, run:

```bash
mvn dependency:tree
```

## 1. Runtime dependencies (bundled in release JARs)

| Component | Version | License | Notice |
| --- | --- | --- | --- |
| OpenJFX (`javafx-base`, `javafx-controls`, `javafx-fxml`, `javafx-graphics`) | 21.0.2 | GPL-2.0 with Classpath-exception-2.0 | https://openjfx.io/ |
| ControlsFX | 11.1.2 | BSD-3-Clause | https://github.com/controlsfx/controlsfx |
| Ikonli (`ikonli-javafx`, `ikonli-core`) | 12.3.1 | Apache-2.0 | https://github.com/kordamp/ikonli |
| BootstrapFX (`bootstrapfx-core`) | 0.4.0 | Apache-2.0 | https://github.com/kordamp/bootstrapfx |
| OpenCSV | 5.9 | Apache-2.0 | https://opencsv.sourceforge.net/ |
| Apache Commons Lang3 | 3.13.0 | Apache-2.0 | https://commons.apache.org/proper/commons-lang/ |
| Apache Commons Text | 1.11.0 | Apache-2.0 | https://commons.apache.org/proper/commons-text/ |
| Apache Commons BeanUtils | 1.9.4 | Apache-2.0 | https://commons.apache.org/proper/commons-beanutils/ |
| Apache Commons Collections4 | 4.4 | Apache-2.0 | https://commons.apache.org/proper/commons-collections/ |
| Apache Commons Collections (legacy) | 3.2.2 | Apache-2.0 | https://commons.apache.org/proper/commons-collections/ |
| `org.json:json` | 20240303 | JSON License | https://github.com/stleary/JSON-java |
| Apache PDFBox (`pdfbox`, `pdfbox-io`, `fontbox`) | 3.0.3 | Apache-2.0 | https://pdfbox.apache.org/ |
| Apache Commons Logging | 1.3.3 | Apache-2.0 | https://commons.apache.org/proper/commons-logging/ |

OpenJFX platform classifiers (for example `javafx-*-win`) are resolved at build time for the host platform and are subject to the same OpenJFX license terms.

## 2. Test-only dependencies (not shipped in release JARs)

| Component | Version | License | Notice |
| --- | --- | --- | --- |
| JUnit Jupiter (api, engine, params) | 5.10.2 | EPL-2.0 | https://junit.org/junit5/ |
| JUnit Platform (commons, engine) | 1.10.2 | EPL-2.0 | https://junit.org/junit5/ |
| OpenTest4J | 1.3.0 | Apache-2.0 | https://github.com/ota4j-team/opentest4j |
| Apiguardian API | 1.1.2 | Apache-2.0 | https://github.com/apiguardian-team/apiguardian |
| TestFX (`testfx-core`, `testfx-junit5`) | 4.0.18 | Apache-2.0 | https://github.com/TestFX/TestFX |
| Hamcrest | 2.1 | BSD-3-Clause | https://github.com/hamcrest/JavaHamcrest |
| AssertJ Core | 3.13.2 | Apache-2.0 | https://assertj.github.io/doc/ |
| OSGi Core | 6.0.0 | Apache-2.0 | https://www.osgi.org/ |

## 3. Special license terms

### 3.1 OpenJFX (GPL-2.0 with Classpath Exception)

OpenJFX is licensed under GPL-2.0 with the Classpath Exception. Linking this application with OpenJFX and distributing the resulting JAR is intended to fall under the Classpath Exception when used as a library dependency.

- https://github.com/openjdk/jfx/blob/master/LICENSE
- https://openjdk.org/legal/gplv2+ce.html

### 3.2 JSON-java (The JSON License)

`org.json:json` is distributed under the JSON License (not OSI-approved). Full text:

- https://github.com/stleary/JSON-java/blob/master/LICENSE

### 3.3 Eclipse Public License 2.0 (JUnit)

JUnit 5 is used only at test scope. Source and binaries are available at https://junit.org/junit5/ under EPL-2.0.

## 4. Standard license references

| License | URL |
| --- | --- |
| Apache-2.0 | https://www.apache.org/licenses/LICENSE-2.0 |
| BSD-3-Clause | https://opensource.org/licenses/BSD-3-Clause |
| MIT | https://opensource.org/licenses/MIT |
| EPL-2.0 | https://www.eclipse.org/legal/epl-2.0/ |
| GPL-2.0 with Classpath Exception | https://openjdk.org/legal/gplv2+ce.html |

## 5. Optional external services

When configured, the application may call third-party AI APIs (for example Qwen). Those services are not Maven dependencies; their terms are governed by the provider. API keys are supplied by the deployer (`QWEN_API_KEY` or `ai-config.properties`).

## 6. Build tools (not redistributed)

Apache Maven, the Maven Wrapper (`mvnw`), and Maven plugins in `pom.xml` are used only to build and test the project. The Maven Wrapper is licensed under Apache-2.0 (see headers in `mvnw` and `mvnw.cmd`).

---

If you redistribute binaries built from this project, retain this file together with `LICENSE` and ensure third-party license conditions (especially OpenJFX and `org.json`) remain satisfied for your distribution model.
