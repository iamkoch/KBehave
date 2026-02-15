# Publishing to Maven Central

This document explains how to publish KBehave to Maven Central via the Sonatype Central Portal.

## Prerequisites

1. **Central Portal Account**: Sign in at https://central.sonatype.com with your GitHub account. This auto-verifies the `io.github.iamkoch` namespace.
2. **GPG Key**: A GPG key for signing artifacts.
3. **Central Portal Token**: A user token for authentication.

## Setup GPG Key

```bash
# Generate a 4096-bit RSA key
gpg --full-generate-key

# List keys to find your KEY_ID
gpg --list-keys --keyid-format long

# Distribute public key to keyservers (Central Portal checks these)
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID

# Export private key (needed for GitHub secrets)
gpg --armor --export-secret-keys YOUR_KEY_ID
```

## Generate Central Portal Token

1. Go to https://central.sonatype.com/account/token (or navigate to Account > User Token)
2. Click "Generate User Token"
3. Save the username and password values -- these are **not** your login credentials but a separate token pair

## Configure GitHub Secrets

Add these secrets to your GitHub repository (Settings > Secrets and variables > Actions):

| Secret | Description |
|--------|-------------|
| `MAVEN_USERNAME` | Central Portal token username |
| `MAVEN_PASSWORD` | Central Portal token password |
| `GPG_PRIVATE_KEY` | Full armored GPG private key output |
| `GPG_PASSPHRASE` | GPG key passphrase |

## Publishing Process

### Automatic Publishing (Recommended)

1. Ensure `version` in `build.gradle.kts` is set to the release version (e.g., `1.0.0`)
2. Create a new release on GitHub
3. The `publish.yml` workflow triggers automatically
4. After the workflow completes, go to https://central.sonatype.com/publishing/deployments
5. Find the staged deployment, verify it, and click **Publish**
6. Wait ~10 minutes for sync to Maven Central
7. Verify at https://search.maven.org/artifact/io.github.iamkoch/kbehave

### Manual Publishing

```bash
# Set environment variables
export MAVEN_USERNAME=your_token_username
export MAVEN_PASSWORD=your_token_password
export SIGNING_KEY="$(gpg --armor --export-secret-keys YOUR_KEY_ID)"
export SIGNING_PASSWORD=your_gpg_passphrase

# Publish to Central Portal staging
./gradlew publishMavenPublicationToCentralPortalRepository
```

Then log into Central Portal to verify and publish the staged deployment.

### Local Validation

Before publishing, validate the artifacts locally:

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Verify artifacts exist
ls ~/.m2/repository/io/github/iamkoch/kbehave/1.0.0/

# Expected files:
# kbehave-1.0.0.jar          (main library)
# kbehave-1.0.0-sources.jar  (source code)
# kbehave-1.0.0-javadoc.jar  (API docs)
# kbehave-1.0.0.pom          (POM metadata)
```

## Post-Release

1. Verify the artifact is available on Maven Central:
   - Check https://search.maven.org/artifact/io.github.iamkoch/kbehave (may take 10-30 minutes)
   - Check https://repo1.maven.org/maven2/io/github/iamkoch/kbehave/ for the version directory
2. Update `version` in `build.gradle.kts` to next snapshot (e.g., `1.1.0-SNAPSHOT`)
3. Commit the version bump: `git commit -am "chore: bump version to 1.1.0-SNAPSHOT"`

## Troubleshooting

- **401 Unauthorized**: Verify your Central Portal token is valid and correctly set in GitHub secrets
- **Signature failed**: Ensure GPG key is exported correctly and passphrase matches
- **Validation errors**: Check POM has all required fields (name, description, url, license, developer with email, scm)
- **Deployment not visible**: Check https://central.sonatype.com/publishing/deployments - it may take a few minutes to appear
