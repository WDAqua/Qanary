# Security

## Reporting a vulnerability

Please report security issues privately to the maintainers
(andreas.both@htwk-leipzig.de) rather than via public issues.

## Secrets handling

- **No real secrets are committed.** The pipeline's Virtuoso credential is supplied
  at deploy/CI time: `service_config/files/pipeline` contains the placeholder
  `VIRTUOSO_PASSWORD=SECRETS_VIRTUOSO_PASSWORD`, which `service_config/build_images.sh`
  substitutes from the `$VIRTUOSO_PASSWORD` CI secret (task 1.6 verified the secret
  never reaches a Docker image layer; the deploy injects it as a runtime env var).
- `application.properties` ships only commented-out, non-secret placeholders
  (e.g. `#virtuoso.password=dba`).
- Automated **secret scanning** runs in CI (`.github/workflows/secret-scan.yml`,
  gitleaks) over the working tree and full history.

## Rotating the Virtuoso read/write credential

The project's improvement backlog (task 1.6) flagged that a Virtuoso read/write
password may have been committed historically. Treat any credential that was ever
committed as compromised and rotate it. **These steps require infrastructure access
and history rewriting and must be performed by a maintainer — they are intentionally
not automated here.**

1. **Rotate at the source.** On the Virtuoso server, change the `readWrite` user's
   password (e.g. `set_user_password('readWrite', '<new>')` via isql, or the
   Management UI → System Admin → User Accounts).
2. **Update the secret store.** Set the new value in the CI/CD secret
   `VIRTUOSO_PASSWORD` (GitHub Actions repo secret) and any deploy secret store. Do
   **not** put it in a tracked file — the `SECRETS_VIRTUOSO_PASSWORD` placeholder
   stays in git.
3. **Confirm propagation.** Re-deploy; verify the pipeline connects with the new
   credential and the old one is rejected.
4. **Scrub history (optional, coordinated).** If a real secret is found in history,
   rewrite it out with [git-filter-repo](https://github.com/newren/git-filter-repo)
   or the [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/), e.g.:
   ```bash
   # back up the repo first; this rewrites history and requires a coordinated
   # force-push + everyone re-cloning
   git filter-repo --replace-text <(echo 'LITERAL_OLD_SECRET==>SECRETS_VIRTUOSO_PASSWORD')
   ```
   Rotation (step 1) is the real fix; scrubbing only removes the now-dead value.

## Verifying the repository for leaked secrets

```bash
# one-off local scan of the full history
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest detect --source=/repo -v
```
