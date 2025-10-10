# Publishing Guide

This document explains how to publish KBehave to Maven Central.

## Prerequisites

1. **Sonatype Account**: Create an account at https://s01.oss.sonatype.org/
2. **Claim Namespace**: Request access to `io.github.iamkoch` namespace
3. **GPG Key**: Generate a GPG key for signing artifacts

## Setup GPG Key

```bash
# Generate key
gpg --gen-key

# Export private key (you'll need this for GitHub secrets)
gpg --armor --export-secret-keys YOUR_KEY_ID

# Export public key to upload to key server
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## Configure GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets → Actions):

- `MAVEN_USERNAME`: Your Sonatype username
- `MAVEN_PASSWORD`: Your Sonatype password
- `GPG_PRIVATE_KEY`: Your GPG private key (full armored output)
- `GPG_PASSPHRASE`: Your GPG key passphrase

## Publishing Process

### Automatic Publishing (Recommended)

1. Create a new release on GitHub
2. The publish workflow will automatically trigger
3. Visit https://s01.oss.sonatype.org/ to close and release the staging repository

### Manual Publishing

```bash
# Set environment variables
export OSSRH_USERNAME=your_username
export OSSRH_PASSWORD=your_password
export SIGNING_KEY="$(cat ~/.gnupg/private-key.asc)"
export SIGNING_PASSWORD=your_gpg_passphrase

# Publish to staging
./gradlew publishMavenPublicationToOSSRHRepository
```

## Version Management

Update the version in `build.gradle.kts`:

```kotlin
version = "1.0.0" // Remove -SNAPSHOT for releases
```

## Post-Release

1. Close the staging repository at https://s01.oss.sonatype.org/
2. Release the staging repository
3. Wait ~10 minutes for sync to Maven Central
4. Verify at https://search.maven.org/
5. Update version to next SNAPSHOT (e.g., `1.1.0-SNAPSHOT`)

## Troubleshooting

- **401 Unauthorized**: Check Maven credentials
- **Signature failed**: Verify GPG key is properly configured
- **Validation errors**: Ensure all POM fields are filled correctly
