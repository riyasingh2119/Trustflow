# Jenkins setup (labs 6 and 7)

1. On the CI instance (Ubuntu 22.04, 2 GB RAM or more): install Jenkins, `openjdk-17-jdk`, `maven`, `docker.io`, `kubectl`
   and Trivy. Add the `jenkins` user to the `docker` group and restart Jenkins.
2. Run SonarQube: `docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community`. Create a project `trustflow`
   and a token. In Jenkins > Manage Jenkins > System add a SonarQube server named `sonar` with that token.
   In SonarQube add a webhook to `http://<jenkins>:8080/sonarqube-webhook/` so `waitForQualityGate` returns.
3. Credentials (Jenkins > Credentials > Global):
   - `dockerhub`: username and Docker Hub access token
   - `kubeconfig`: secret file built from the `jenkins-deployer-token` secret (see `k8s/60-rbac.yaml`)
4. Create a Pipeline job "trustflow", definition "Pipeline script from SCM", script path `jenkins/Jenkinsfile`,
   and tick "GitHub hook trigger for GITScm polling".
5. In GitHub: Settings > Webhooks > `http://<jenkins-ip>:8080/github-webhook/`, content type `application/json`, event "push".
   Restrict port 8080 to GitHub's webhook IP ranges or use a reverse proxy, instead of leaving it open to everyone.
6. Quality gate: the default "Sonar way" gate fails on new bugs, vulnerabilities and low coverage on new code.
   For lab 7 evidence run once with a deliberate flaw (failed build), then fix it (green build).
7. Cache the OWASP Dependency-Check database (`~/.m2` and `~/.dependency-check`) and set an NVD API key with
   `-DnvdApiKey=...` from a Jenkins credential to avoid slow or rate-limited updates.
