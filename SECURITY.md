# Security Vulnerability Report

This document lists the security vulnerabilities found during a security scan of this repository, along with their severity, affected files, and remediation status.

---

## Vulnerabilities Found

### 1. SQL Injection (CWE-89) — **CRITICAL** ✅ Fixed

| Field | Detail |
|---|---|
| **Severity** | Critical |
| **CWE** | [CWE-89: Improper Neutralization of Special Elements used in an SQL Command](https://cwe.mitre.org/data/definitions/89.html) |
| **Affected File** | `src/main/java/com/github/hackathon/advancedsecurityjava/Controllers/IndexController.java` |
| **Affected Lines** | Lines 42–49 (original) |
| **Status** | ✅ Fixed |

#### Description

The `getBooks` endpoint directly concatenated user-supplied HTTP query parameters (`name`, `author`, `read`) into SQL query strings without any sanitization or parameterization:

```java
// VULNERABLE (original code)
query = "SELECT * FROM Books WHERE name LIKE '%" + bookname + "%'";
query = "SELECT * FROM Books WHERE author LIKE '%" + bookauthor + "%'";
query = "SELECT * FROM Books WHERE read = '" + read.toString() + "'";
```

An attacker could craft a request like `/?name=' OR '1'='1` to manipulate the query and extract, modify, or delete data.

#### Fix

Replaced raw `Statement` with `PreparedStatement` using parameterized queries. User input is now passed as bind parameters, never interpolated directly into SQL:

```java
// FIXED
query = "SELECT * FROM Books WHERE name LIKE ?";
parameters.add("%" + bookname + "%");
// ...
statement = connection.prepareStatement(query);
statement.setString(index, parameter);
```

---

### 2. SQLite JDBC Remote Code Execution (CVE-2023-32697) — **CRITICAL** ✅ Fixed

| Field | Detail |
|---|---|
| **Severity** | Critical |
| **CVE** | [CVE-2023-32697](https://github.com/advisories/GHSA-6phf-73q6-gh87) |
| **Affected Dependency** | `org.xerial:sqlite-jdbc` version `3.32.3.2` |
| **Affected File** | `pom.xml` |
| **Status** | ✅ Fixed |

#### Description

`sqlite-jdbc` versions `>= 3.6.14.1` and `< 3.41.2.2` allow Remote Code Execution when the JDBC connection URL is controlled by an attacker. The vulnerability exists because the library supports a `org.sqlite.tmpdir` property that allows loading native libraries from attacker-specified paths.

#### Fix

Updated `sqlite-jdbc` from `3.32.3.2` to `3.41.2.2` in `pom.xml`.

---

### 3. Outdated and Mismatched Log4j Versions — **HIGH** ✅ Fixed

| Field | Detail |
|---|---|
| **Severity** | High |
| **Related CVEs** | CVE-2021-44228 (Log4Shell), CVE-2021-45046, CVE-2021-45105, CVE-2021-44832 |
| **Affected Dependencies** | `org.apache.logging.log4j:log4j-api` version `2.1`, `org.apache.logging.log4j:log4j-core` version `2.12.4` |
| **Affected File** | `pom.xml` |
| **Status** | ✅ Fixed |

#### Description

The project declared `log4j-api` at version `2.1` (released 2014) and `log4j-core` at `2.12.4`. This version mismatch is dangerous:

- **`log4j-api 2.1`** is from 2014 and predates all Log4Shell patches. While the JNDI lookup vulnerability lives in `log4j-core`, running mismatched API/core versions is unsupported and may introduce unpredictable behavior.
- **`log4j-core 2.12.4`** addresses the Log4Shell family of CVEs for the Java 8 branch, but using a pinned version that diverges from the API version creates maintenance risk.
- Spring Boot 2.4.x (without an explicit override) defaults to `log4j2` version `2.13.3`, which **is** vulnerable to CVE-2021-44228 (Log4Shell).

#### Fix

Updated both `log4j-api` and `log4j-core` to `2.17.2` in `pom.xml`. Version `2.17.2` patches all known Log4Shell variants (CVE-2021-44228, CVE-2021-45046, CVE-2021-45105, CVE-2021-44832).

---

### 4. Thread-Safety Issue — Shared Static Database Connection — **MEDIUM** ✅ Fixed

| Field | Detail |
|---|---|
| **Severity** | Medium |
| **CWE** | [CWE-362: Race Condition](https://cwe.mitre.org/data/definitions/362.html) |
| **Affected File** | `src/main/java/com/github/hackathon/advancedsecurityjava/Controllers/IndexController.java` |
| **Status** | ✅ Fixed |

#### Description

The original `IndexController` declared the database connection as a `private static` field:

```java
private static Connection connection;
```

In a multi-threaded Spring web application, multiple concurrent HTTP requests share this single static field. One request can close the connection while another is still using it, leading to race conditions, data corruption, and potential denial-of-service.

#### Fix

Removed the static field. The `Connection` is now declared as a local variable within the `getBooks` method, giving each request its own independent connection lifecycle.

---

### 5. Stack Trace Exposure — **LOW** ✅ Fixed

| Field | Detail |
|---|---|
| **Severity** | Low |
| **CWE** | [CWE-209: Generation of Error Message Containing Sensitive Information](https://cwe.mitre.org/data/definitions/209.html) |
| **Affected Files** | `src/main/java/com/github/hackathon/advancedsecurityjava/Controllers/IndexController.java` |
| **Status** | ✅ Fixed |

#### Description

The original code used `error.printStackTrace()` in catch blocks:

```java
} catch (SQLException error) {
    error.printStackTrace();
}
```

While `printStackTrace()` writes to `stderr` (not to the HTTP response), it bypasses the application's logging framework, making it impossible to control log verbosity, redact sensitive information, or correlate errors with request context.

#### Fix

Replaced `error.printStackTrace()` calls with `Application.logger.error(...)` to route all error output through the application's configured Log4j logger.

---

## Summary

| # | Vulnerability | Severity | Status |
|---|---|---|---|
| 1 | SQL Injection (CWE-89) in `IndexController.java` | Critical | ✅ Fixed |
| 2 | SQLite JDBC RCE (CVE-2023-32697) | Critical | ✅ Fixed |
| 3 | Outdated / mismatched Log4j versions (Log4Shell risk) | High | ✅ Fixed |
| 4 | Static shared database connection (race condition) | Medium | ✅ Fixed |
| 5 | Stack trace exposure via `printStackTrace()` | Low | ✅ Fixed |
