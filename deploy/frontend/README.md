# Frontend deployment

The React app is served as static files by host nginx on EC2
(`smartserve.anshuyadav.me`, `root /var/www/smartserve`). The API stays on
`api.smartserve.anshuyadav.me`.

```
GitHub Actions ──build──▶ ECR image (FROM scratch, only /dist, ~4 MB)
      │
      └──SSM──▶ /opt/smartserve/deploy-frontend.sh <sha>
                  pulls image, copies /dist to /var/www/smartserve-releases/<sha>/
                  swaps symlink /var/www/smartserve -> that release
```

nginx config does not change: `/var/www/smartserve` simply becomes a symlink.
The first run keeps the hand-copied build as `releases/legacy-<timestamp>`.

## One-time setup

1. Create the ECR repository and attach the lifecycle rule:

   ```bash
   aws ecr create-repository --repository-name smartserve-frontend --region ap-south-1
   aws ecr put-lifecycle-policy --repository-name smartserve-frontend \
       --lifecycle-policy-text file://deploy/frontend/ecr-lifecycle.json --region ap-south-1
   ```

2. IAM: allow the GitHub OIDC role to push to `smartserve-frontend`, and the
   EC2 instance role to pull from it (same actions as the backend repository).

3. On EC2, install the script:

   ```bash
   sudo install -m 755 deploy-frontend.sh /opt/smartserve/deploy-frontend.sh
   ```

   Optional overrides go in `/opt/smartserve/frontend.env`
   (`ECR_REGISTRY`, `ECR_REPOSITORY`, `LIVE_ROOT`, `RELEASES_DIR`, `KEEP_RELEASES`).

4. Optional GitHub repository variables: `FRONTEND_ECR_REPOSITORY`
   (default `smartserve-frontend`) and `FRONTEND_API_BASE_URL`
   (default `https://api.smartserve.anshuyadav.me/api`).

## Deploy and rollback

- Deploy: Actions → *SmartServe Frontend CI/CD* → Run workflow on `main`.
- Rollback, on EC2: `sudo /opt/smartserve/deploy-frontend.sh <older-sha>`.
  The last 5 releases stay on disk and switch instantly; older ones are
  pulled from ECR again.
