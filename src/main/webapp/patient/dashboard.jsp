<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Patient Dashboard | Aarogya Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="app">
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand">
      <img class="brand-mark" src="${pageContext.request.contextPath}/assets/css/logo.svg" alt="">
      <div class="brand-name">Aarogya Clinic<small>PATIENT PORTAL</small></div>
    </div>
    <a class="side-link active" href="${pageContext.request.contextPath}/patient/dashboard">▦ <span>Overview</span></a>
    <a class="side-link" href="${pageContext.request.contextPath}/patient/book-appointment">+ <span>Book Appointment</span></a>
  </aside>
  <main class="main">
    <header class="app-header">
      <div>
        <b><c:out value="${patientName}"/></b>
        <div style="font-size:12px;color:var(--muted)">Patient ID: <c:out value="${patientId}"/></div>
      </div>
      <a href="${pageContext.request.contextPath}/logout" style="font-size:13px;color:var(--muted)">Sign out</a>
    </header>
    <div class="app-content">
      <c:if test="${not empty success}">
        <div class="alert-error" style="background:#edf7f2;border-color:#b8dac8;color:#246b4c" role="status">
          <c:out value="${success}"/>
        </div>
        <c:remove var="success" scope="session"/>
      </c:if>
      <c:if test="${not empty dbError}">
        <div class="alert-error" role="alert"><c:out value="${dbError}"/></div>
      </c:if>
      <div class="page-heading">
        <div><div class="eyebrow">PATIENT OVERVIEW</div><h1>Your care at a glance</h1></div>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/patient/book-appointment">+ Book Appointment</a>
      </div>
      <div class="stats-grid">
        <div class="stat">
          <div class="stat-label">NEXT APPOINTMENT</div>
          <c:choose>
            <c:when test="${not empty nextAppointment}">
              <div class="stat-value" style="font-size:20px"><fmt:formatDate value="${nextAppointment.appointmentDate}" pattern="dd MMM yyyy"/></div>
              <div class="stat-note"><c:out value="${nextAppointment.appointmentTime}"/> · <c:out value="${nextAppointment.departmentName}"/></div>
            </c:when>
            <c:otherwise><div class="stat-value" style="font-size:20px">None booked</div><div class="stat-note">Choose a doctor to book a visit</div></c:otherwise>
          </c:choose>
        </div>
        <div class="stat">
          <div class="stat-label">YOUR TOKEN</div>
          <div class="stat-value"><c:out value="${empty nextAppointment.tokenNumber ? '—' : nextAppointment.tokenNumber}"/></div>
          <div class="stat-note"><c:out value="${empty nextAppointment.tokenStatus ? 'No active token' : nextAppointment.tokenStatus}"/></div>
        </div>
        <div class="stat">
          <div class="stat-label">PATIENTS AHEAD</div>
          <div class="stat-value"><c:out value="${empty patientsAhead ? '—' : patientsAhead}"/></div>
          <div class="stat-note">
            <c:choose>
              <c:when test="${not empty estimatedWaitMinutes}">Estimated wait: about <c:out value="${estimatedWaitMinutes}"/> min</c:when>
              <c:otherwise>Queue estimate available on appointment day</c:otherwise>
            </c:choose>
          </div>
        </div>
        <div class="stat"><div class="stat-label">NOTIFICATIONS</div><div class="stat-value"><c:out value="${notificationCount}"/></div></div>
      </div>
      <div class="panel">
        <div class="panel-head"><h3>Upcoming appointment</h3></div>
        <c:choose>
          <c:when test="${not empty nextAppointment}">
            <div style="padding:18px">
              <b><c:out value="${nextAppointment.doctorName}"/></b>
              <div style="font-size:13px;color:var(--muted);margin:6px 0">
                <c:out value="${nextAppointment.departmentName}"/> · <c:out value="${nextAppointment.appointmentTime}"/>
              </div>
              <div style="font-size:13px">Appointment: <c:out value="${nextAppointment.appointmentId}"/>
                · Status: <c:out value="${nextAppointment.status}"/></div>
            </div>
          </c:when>
          <c:otherwise>
            <div style="padding:18px;color:var(--muted);font-size:14px">You do not have an upcoming appointment.</div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </main>
</div>
</body>
</html>
