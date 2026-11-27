# Aarogya Clinic

## Run locally

The application uses a persistent H2 database by default, so it can start without a MySQL server. Database files are stored under `~/.aarogya-care/`.

```sh
mvn test
mvn package
mvn tomcat7:run
```

Open `http://localhost:9090/aarogya/`. New patient accounts can be created from **Create Patient Account**. Staff roles cannot be self-registered.

The seeded demo accounts all use the password `password`:

- `admin@aarogya.in`
- `doctor@aarogya.in`
- `reception@aarogya.in`
- `patient@aarogya.in`

## Deploy to Render

The repository includes a Dockerfile and Render Blueprint. The Blueprint deploys Tomcat 9 (compatible with this `javax.servlet` application) and attaches a 1 GB persistent disk for the H2 database. Persistent disks require a paid Render web-service plan; check Render's current pricing before deploying. Without persistent storage, accounts and appointments can be lost when the service restarts or redeploys.

1. Push the complete project folder to a GitHub repository. Include `Dockerfile`, `docker/render-entrypoint.sh`, and `render.yaml`.
2. In Render, choose **New + → Blueprint** and connect the GitHub repository and branch.
3. Review the `aarogya-clinic` web service and its paid plan/disk, then apply the Blueprint.
4. Wait for the Docker build and deploy to finish. Open the `onrender.com` URL shown on the service page.
5. Create a patient account and test an appointment. The demo accounts below are available on a fresh database.

Pushes to the connected branch trigger automatic deploys. The H2 database is stored under the mounted disk at `/var/lib/aarogya`; the container configures the app to use that path.

If you don't want a paid persistent disk, you can remove the `disk` block from `render.yaml` and select a free plan, but treat that deployment as a temporary demo because its database won't persist across restarts/redeploys.

### Deploy without a Blueprint

Create a Render **Web Service**, connect the repository, select **Docker** as the runtime, set the Dockerfile path to `./Dockerfile`, and add a persistent disk mounted at `/var/lib/aarogya`. The disk requires a paid plan. Set the environment variable `JAVA_OPTS` to `-Duser.home=/var/lib/aarogya`.

## MySQL

Create the database using `database/schema.sql`, populate it using `database/seed.sql`, then provide JDBC properties when starting Tomcat:

```sh
mvn -Ddb.url='jdbc:mysql://localhost:3306/aarogya_care?serverTimezone=Asia/Kolkata' \
    -Ddb.user=root -Ddb.password='your-password' tomcat7:run
```

Patient passwords are stored as BCrypt hashes. Appointment booking creates the appointment, waiting token, pending counter payment and patient notification together in a database transaction.
