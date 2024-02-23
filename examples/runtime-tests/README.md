## Runtime tests
This docker compose example is used to test several scenario's that are impossible (or difficult) to test within Java Unit Tests.

### Incorrect `KE_RUNTIME_EXPOSED_URL`
The `docker-compose.yml` contains the exact same scenario as the `multiple-runtimes` example, but additionally it contains several broken runtimes where the exposed URL is incorrect. With this docker project we can test whether these different types of exposed urls are breaking their own KER, other KERs or the Knowledge Directory.