<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Book Appointment | Aarogya Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="app">
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand">
      <img class="brand-mark" src="${pageContext.request.contextPath}/assets/css/logo.svg" alt="">
      <div class="brand-name">Aarogya Clinic<small>PATIENT PORTAL</small></div>
    </div>
    <a class="side-link" href="${pageContext.request.contextPath}/patient/dashboard">▦ <span>Overview</span></a>
    <a class="side-link active" href="${pageContext.request.contextPath}/patient/book-appointment">+ <span>Book Appointment</span></a>
  </aside>
  <main class="main">
    <header class="app-header">
      <b>Patient Portal</b>
      <a href="${pageContext.request.contextPath}/logout" style="font-size:13px;color:var(--muted)">Sign out</a>
    </header>
    <div class="app-content">
      <c:if test="${not empty bookingAppointmentId}">
        <div class="alert-error" style="background:#edf7f2;border-color:#b8dac8;color:#246b4c" role="status">
          Appointment <b><c:out value="${bookingAppointmentId}"/></b> is confirmed.
          Your token is <b><c:out value="${bookingTokenNumber}"/></b>. Pay the displayed amount at the reception counter.
        </div>
        <c:remove var="bookingAppointmentId" scope="session"/>
        <c:remove var="bookingTokenNumber" scope="session"/>
      </c:if>
      <c:if test="${not empty error}">
        <div class="alert-error" role="alert"><c:out value="${error}"/></div>
      </c:if>
      <c:if test="${not empty dbError}">
        <div class="alert-error" role="alert"><c:out value="${dbError}"/></div>
      </c:if>
      <div class="page-heading">
        <div><div class="eyebrow">NEW APPOINTMENT</div><h1>Find a doctor</h1></div>
      </div>
      <form class="panel" style="padding:22px" method="post" action="${pageContext.request.contextPath}/patient/book-appointment"
            data-availability-url="${pageContext.request.contextPath}/patient/appointment-availability">
        <div class="form-grid">
          <div class="form-group">
            <label for="departmentId">Department</label>
            <select class="form-control" id="departmentId" name="departmentId" required>
              <option value="">Select a department</option>
              <c:forEach items="${departments}" var="department">
                <option value="${department.id}" <c:if test="${param.departmentId == department.id}">selected</c:if>>
                  <c:out value="${department.name}"/>
                </option>
              </c:forEach>
            </select>
          </div>
          <div class="form-group">
            <label for="doctorId">Doctor</label>
            <select class="form-control" id="doctorId" name="doctorId" required>
              <option value="">Select an available doctor</option>
              <c:forEach items="${doctors}" var="doctor">
                <option value="${doctor.id}" data-department="${doctor.departmentId}"
                        data-fee="${doctor.fee}" <c:if test="${param.doctorId == doctor.id}">selected</c:if>>
                  <c:out value="${doctor.name}"/> — ₹<fmt:formatNumber value="${doctor.fee}" minFractionDigits="0" maxFractionDigits="2"/>
                </option>
              </c:forEach>
            </select>
          </div>
          <div class="form-group">
            <label for="appointmentDate">Appointment date</label>
            <input class="form-control" id="appointmentDate" name="appointmentDate" type="date"
                   min="${today}" value="${param.appointmentDate}" required>
          </div>
          <div class="form-group">
            <label for="appointmentTime">Available time</label>
            <select class="form-control" id="appointmentTime" name="appointmentTime" required disabled>
              <option value="">Select a time</option>
              <option value="09:30 AM" <c:if test="${param.appointmentTime == '09:30 AM'}">selected</c:if>>09:30 AM</option>
              <option value="10:30 AM" <c:if test="${param.appointmentTime == '10:30 AM'}">selected</c:if>>10:30 AM</option>
              <option value="01:00 PM" <c:if test="${param.appointmentTime == '01:00 PM'}">selected</c:if>>01:00 PM</option>
              <option value="02:30 PM" <c:if test="${param.appointmentTime == '02:30 PM'}">selected</c:if>>02:30 PM</option>
              <option value="05:00 PM" <c:if test="${param.appointmentTime == '05:00 PM'}">selected</c:if>>05:00 PM</option>
            </select>
          </div>
        </div>
        <p id="availabilityMessage" role="status" aria-live="polite" style="font-size:13px;color:var(--muted)">
          Select a doctor and date to check current availability.
        </p>
        <div class="demo-box" style="margin:0 0 18px">
          Registration fee: ₹100 · Consultation fee: <span id="consultationFee">select a doctor</span>
          · <b>Total due at reception: <span id="totalFee">—</span></b>
        </div>
        <button class="btn btn-primary" id="confirmAppointment" type="submit" disabled>Confirm Appointment</button>
      </form>
    </div>
  </main>
</div>
<script>
  (function () {
    const department = document.getElementById("departmentId");
    const doctor = document.getElementById("doctorId");
    const appointmentDate = document.getElementById("appointmentDate");
    const appointmentTime = document.getElementById("appointmentTime");
    const consultationFee = document.getElementById("consultationFee");
    const totalFee = document.getElementById("totalFee");
    const availabilityMessage = document.getElementById("availabilityMessage");
    const confirmButton = document.getElementById("confirmAppointment");
    const availabilityUrl = document.querySelector("form[data-availability-url]").dataset.availabilityUrl;
    let availabilityRequest = 0;

    function updateDoctors() {
      Array.from(doctor.options).forEach(function (option) {
        option.hidden = option.value !== "" && option.dataset.department !== department.value;
      });
      if (doctor.selectedOptions.length && doctor.selectedOptions[0].hidden) {
        doctor.value = "";
      }
      updateFee();
      updateAvailability();
    }

    function updateFee() {
      const selectedDoctor = doctor.selectedOptions[0];
      const fee = selectedDoctor && selectedDoctor.dataset.fee;
      consultationFee.textContent = fee ? "₹" + Number(fee).toLocaleString("en-IN") : "select a doctor";
      totalFee.textContent = fee ? "₹" + (Number(fee) + 100).toLocaleString("en-IN") : "—";
    }

    async function updateAvailability() {
      const requestNumber = ++availabilityRequest;
      const selectedDoctor = doctor.value;
      const selectedDate = appointmentDate.value;
      confirmButton.disabled = true;
      appointmentTime.disabled = true;

      if (!selectedDoctor || !selectedDate) {
        availabilityMessage.textContent = "Select a doctor and date to check current availability.";
        return;
      }

      availabilityMessage.textContent = "Checking available times…";
      const query = new URLSearchParams({doctorId: selectedDoctor, appointmentDate: selectedDate});
      try {
        const response = await fetch(availabilityUrl + "?" + query.toString(), {
          headers: {Accept: "application/json"}
        });
        if (!response.ok) {
          throw new Error("Availability check failed with HTTP " + response.status);
        }
        const result = await response.json();
        if (requestNumber !== availabilityRequest) {
          return;
        }

        let openSlots = 0;
        Array.from(appointmentTime.options).forEach(function (option) {
          if (!option.value) {
            return;
          }
          const label = option.dataset.label || option.textContent.trim();
          option.dataset.label = label.replace(" — Not available", "");
          const unavailable = result.unavailable.includes(option.value);
          option.disabled = unavailable;
          option.textContent = option.dataset.label + (unavailable ? " — Not available" : "");
          if (!unavailable) {
            openSlots++;
          }
        });

        if (appointmentTime.selectedOptions[0] && appointmentTime.selectedOptions[0].disabled) {
          appointmentTime.value = "";
        }
        appointmentTime.disabled = openSlots === 0;
        confirmButton.disabled = openSlots === 0;
        availabilityMessage.textContent = openSlots === 0
          ? "No appointment times are available for this doctor on that date."
          : "Unavailable times are marked. Availability is checked again when you confirm.";
      } catch (error) {
        if (requestNumber !== availabilityRequest) {
          return;
        }
        availabilityMessage.textContent = "Could not check availability. Please change the date or try again.";
        console.error(error);
      }
    }

    department.addEventListener("change", updateDoctors);
    doctor.addEventListener("change", function () {
      updateFee();
      updateAvailability();
    });
    appointmentDate.addEventListener("change", updateAvailability);
    updateDoctors();
  }());
</script>
</body>
</html>
