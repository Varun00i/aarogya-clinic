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

The repository includes a Dockerfile and Render Blueprint configured for Render's free web-service plan. It deploys Tomcat 9, which is compatible with this `javax.servlet` application.

1. Push the complete project folder to a GitHub repository. Include `Dockerfile`, `docker/render-entrypoint.sh`, and `render.yaml`.
2. In Render, choose **New + → Blueprint** and connect the GitHub repository and branch.
3. Review the `aarogya-clinic` web service and apply the Blueprint.
4. Wait for the Docker build and deploy to finish. Open the `onrender.com` URL shown on the service page.
5. Create a patient account and test an appointment. The demo accounts below are available on a fresh database.

Pushes to the connected branch trigger automatic deploys. **Render's free filesystem is ephemeral:** H2 accounts and appointments may be erased when the service restarts, sleeps, or redeploys. This free setup is suitable for a demo, not reliable patient data storage. To preserve data, use a paid Render service with a persistent disk or configure a separately hosted database.

### Deploy without a Blueprint

Create a Render **Web Service**, connect the repository, select **Docker** as the runtime, and set the Dockerfile path to `./Dockerfile`. Choose the free plan for a demo. If you choose a paid plan with persistent storage, mount the disk at `/var/lib/aarogya` and set `JAVA_OPTS` to `-Duser.home=/var/lib/aarogya`.

## MySQL

Create the database using `database/schema.sql`, populate it using `database/seed.sql`, then provide JDBC properties when starting Tomcat:

```sh
mvn -Ddb.url='jdbc:mysql://localhost:3306/aarogya_care?serverTimezone=Asia/Kolkata' \
    -Ddb.user=root -Ddb.password='your-password' tomcat7:run
```

Patient passwords are stored as BCrypt hashes. Appointment booking creates the appointment, waiting token, pending counter payment and patient notification together in a database transaction.
