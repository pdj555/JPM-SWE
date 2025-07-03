# GitHub Pipeline Debug Report

## Issues Identified

### 1. Maven Wrapper JAR Missing (Primary Issue)

**Error:**
```
Error: Could not find or load main class org.apache.maven.wrapper.MavenWrapperMain
Caused by: java.lang.ClassNotFoundException: org.apache.maven.wrapper.MavenWrapperMain
```

**Root Cause:**
- The `.mvn/wrapper/maven-wrapper.jar` file is missing from the repository
- The `.gitignore` file (line 31) explicitly excludes `maven-wrapper.jar` 
- While this is intentional to keep the repository smaller, the CI pipeline doesn't handle downloading the wrapper JAR

**Current State:**
- ✅ `mvnw` and `mvnw.cmd` wrapper scripts exist
- ✅ `.mvn/wrapper/maven-wrapper.properties` exists with correct configuration
- ❌ `.mvn/wrapper/maven-wrapper.jar` is missing (gitignored)

### 2. Test Report Files Not Found (Secondary Issue)

**Error:**
```
Error: No test report files were found
```

**Root Cause:**
- The dorny/test-reporter@v1 action looks for test reports at `ingest-service/target/surefire-reports/*.xml`
- Since the Maven build fails due to the missing wrapper, tests never run
- No test reports are generated, causing the test reporter to fail

## Solutions

### Option 1: Download Maven Wrapper JAR in CI (Recommended)

Add a step before the Maven verify command in `.github/workflows/ci.yml`:

```yaml
- name: Download Maven Wrapper JAR
  run: |
    if [ ! -f .mvn/wrapper/maven-wrapper.jar ]; then
      mkdir -p .mvn/wrapper
      curl -o .mvn/wrapper/maven-wrapper.jar https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    fi

- name: Run Maven verify
  run: ./mvnw -B verify -Dspring.profiles.active=test
```

### Option 2: Use Maven Wrapper Download Action

Use a pre-built action to handle the wrapper:

```yaml
- name: Set up Maven Wrapper
  run: |
    mvn wrapper:wrapper -Dmaven=3.9.6
    
- name: Run Maven verify
  run: ./mvnw -B verify -Dspring.profiles.active=test
```

### Option 3: Commit the Maven Wrapper JAR

Remove the exclusion from `.gitignore` and commit the JAR:

```bash
# Remove the line from .gitignore
sed -i '/\.mvn\/wrapper\/maven-wrapper\.jar/d' .gitignore

# Download and commit the wrapper JAR
mvn wrapper:wrapper
git add .mvn/wrapper/maven-wrapper.jar
git commit -m "Add maven-wrapper.jar for CI compatibility"
```

### Option 4: Use System Maven Instead

Replace `./mvnw` with `mvn` in the CI workflow, but this loses version consistency benefits.

## Recommended Implementation

**Step 1:** Update `.github/workflows/ci.yml` to download the wrapper JAR:

```yaml
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: ${{ env.JAVA_DISTRIBUTION }}

    - name: Download Maven Wrapper JAR
      run: |
        if [ ! -f .mvn/wrapper/maven-wrapper.jar ]; then
          echo "Downloading Maven Wrapper JAR..."
          mkdir -p .mvn/wrapper
          curl -L -o .mvn/wrapper/maven-wrapper.jar https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
          chmod +x ./mvnw
        fi

    - name: Cache Maven dependencies
      uses: actions/cache@v4
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
```

**Step 2:** Ensure the test reporter paths are correct (they already are):
- Tests exist in `ingest-service/src/test/`
- Test reports will be generated at `ingest-service/target/surefire-reports/*.xml`
- Current test-reporter configuration is correct

## Project Structure Confirmed

```
├── .mvn/wrapper/
│   ├── maven-wrapper.properties ✅
│   └── maven-wrapper.jar        ❌ (missing, gitignored)
├── mvnw                         ✅
├── mvnw.cmd                     ✅
├── pom.xml                      ✅ (parent with ingest-service module)
├── ingest-service/
│   ├── pom.xml                  ✅
│   └── src/test/                ✅ (tests exist)
└── .github/workflows/ci.yml     ✅
```

## Testing the Fix

After implementing the fix, the pipeline should:
1. ✅ Download the Maven wrapper JAR
2. ✅ Successfully run `./mvnw -B verify -Dspring.profiles.active=test`
3. ✅ Generate test reports in `ingest-service/target/surefire-reports/`
4. ✅ Process test reports with dorny/test-reporter@v1

## Additional Considerations

- The Maven wrapper version (3.2.0) matches what's specified in `maven-wrapper.properties`
- Java 21 is correctly configured in both CI and Maven compiler plugin
- All required services (Kafka, Zookeeper, Cassandra) are properly configured in the CI workflow
- The fix maintains the benefit of version-locked Maven while solving the CI issue