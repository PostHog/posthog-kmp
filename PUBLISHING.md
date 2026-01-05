# Publishing Guide

This guide explains how to set up and publish posthog-kmp to Maven Central.

## Prerequisites

1. GPG key for signing artifacts
2. Sonatype/Maven Central account
3. GitHub repository with secrets configured

---

## Step 1: Create GPG Key

### Generate the key

```bash
gpg --full-generate-key
```

When prompted:
- **Key type**: `1` (RSA and RSA)
- **Key size**: `4096`
- **Expiration**: `0` (never) or `2y` (2 years)
- **Real name**: Your name
- **Email**: Your email
- **Passphrase**: Strong passphrase (remember this!)

### Get your key ID

```bash
gpg --list-secret-keys --keyid-format LONG
```

Output looks like:
```
sec   rsa4096/ABC123DEF4567890 2025-01-03 [SC]
      FULL_FINGERPRINT_HERE
uid                 [ultimate] Your Name <your@email.com>
ssb   rsa4096/SUB_KEY_ID 2025-01-03 [E]
```

The **Key ID** is `ABC123DEF4567890` (last 16 characters after `rsa4096/`).
For signing, use the **last 8 characters**: `F4567890`

### Export keys for GitHub Actions

```bash
# Export private key (armor format for GitHub secret)
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# Export as base64 (alternative format)
gpg --export-secret-keys YOUR_KEY_ID | base64 > private-key-base64.txt
```

### Upload to keyserver (required for Maven Central)

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

---

## Step 2: Create Maven Central Account

### Option A: Central Portal (New - Recommended)

1. Go to [central.sonatype.com](https://central.sonatype.com)
2. Sign in with GitHub
3. Go to **Account** → **Generate User Token**
4. Save the **username** and **password** (these are your Maven Central credentials)

### Option B: OSSRH (Legacy)

1. Create account at [issues.sonatype.org](https://issues.sonatype.org)
2. Create a new issue to claim your namespace (e.g., `io.github.yourusername`)
3. Wait for approval (usually 1-2 business days)

### Verify namespace ownership

For `io.github.yourusername`:
- Create a public repo named `OSSRH-XXXXX` (ticket number) OR
- Use the Central Portal automatic GitHub verification

---

## Step 3: Set Up GitHub Repository

### Create the repository

```bash
cd posthog-kmp
git init
git add .
git commit -m "Initial commit"

# Create repo on GitHub, then:
git remote add origin https://github.com/YOUR_USERNAME/posthog-kmp.git
git branch -M main
git push -u origin main
```

### Add GitHub Secrets

Go to **Repository Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `MAVEN_CENTRAL_USERNAME` | Your Maven Central token username |
| `MAVEN_CENTRAL_PASSWORD` | Your Maven Central token password |
| `GPG_KEY_ID` | Last 8 characters of your key ID (e.g., `F4567890`) |
| `GPG_KEY_PASSWORD` | Your GPG key passphrase |
| `GPG_KEY_ARMOR` | Contents of `private-key.asc` (full armored key) |
| `GPG_KEY_BASE64` | Contents of `private-key-base64.txt` |

---

## Step 4: Publishing

### Manual Release

1. Update version in `gradle.properties`:
   ```properties
   VERSION_NAME=0.1.0
   ```

2. Commit and push:
   ```bash
   git add gradle.properties
   git commit -m "Bump version to 0.1.0"
   git push
   ```

3. Create a GitHub Release:
   - Go to **Releases** → **Create new release**
   - Tag: `v0.1.0`
   - Title: `v0.1.0`
   - Description: Release notes
   - Click **Publish release**

4. The `publish.yml` workflow will automatically publish to Maven Central.

### Manual Workflow Trigger

1. Go to **Actions** → **Publish to Maven Central**
2. Click **Run workflow**
3. Enter the version (e.g., `0.1.0`)
4. Click **Run workflow**

---

## Step 5: Verify Publication

### Check Maven Central

After publishing, artifacts appear at:
- **Central Portal**: [central.sonatype.com](https://central.sonatype.com) → Search for `posthog-kmp`
- **Maven Central**: `https://repo1.maven.org/maven2/io/github/YOUR_USERNAME/posthog-kmp/`

Note: It can take 10-30 minutes for artifacts to sync to Maven Central after publishing.

### Test in a project

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.YOUR_USERNAME:posthog-kmp:0.1.0")
}
```

---

## Local Publishing (Testing)

### Publish to local Maven

```bash
./gradlew publishToMavenLocal
```

Artifacts go to `~/.m2/repository/io/github/YOUR_USERNAME/posthog-kmp/`

### Use local artifacts

```kotlin
// settings.gradle.kts
repositories {
    mavenLocal()
    mavenCentral()
}
```

---

## Troubleshooting

### "Could not find key"
- Ensure you uploaded to keyservers
- Wait a few minutes for propagation
- Try different keyserver: `keyserver.ubuntu.com` or `keys.openpgp.org`

### "Forbidden" error on publish
- Verify your Maven Central credentials
- Check namespace ownership is verified
- Ensure group ID matches your namespace

### iOS build fails
- macOS runner required for iOS/macOS targets
- Check Xcode version compatibility
- Verify SPM4KMP plugin configuration

### Signing fails
- Verify GPG_KEY_ID is exactly 8 characters
- Check GPG_KEY_ARMOR is the complete key (including headers)
- Ensure GPG_KEY_PASSWORD is correct

---

## Security Best Practices

1. **Never commit** credentials or private keys
2. **Use GitHub Secrets** for all sensitive values
3. **Rotate keys** periodically
4. **Use 2FA** on all accounts (GitHub, Sonatype)
5. **Review** GitHub Actions logs for exposed secrets
