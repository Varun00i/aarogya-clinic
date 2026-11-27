<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Login | Aarogya Clinic</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<main class="login-page">
  <div class="login-shell">
    <aside class="login-aside">
      <div class="brand">
        <img class="brand-mark" src="${pageContext.request.contextPath}/assets/css/logo.svg" alt="">
        <div class="brand-name">Aarogya<small>CLINIC</small></div>
      </div>
      <div>
        <div class="eyebrow" style="color:#78cbc4">SECURE PATIENT PORTAL</div>
        <h1>Good health begins with clear care.</h1>
        <p>Access appointments, tokens and prescriptions through one secure hospital account.</p>
      </div>
      <small>© 2026 Aarogya Clinic · Sector 62, Noida</small>
    </aside>
    <section class="login-main">
      <form class="login-card" method="post" action="${pageContext.request.contextPath}/login">
        <div class="brand" style="margin-bottom:28px">
          <img class="brand-mark" src="${pageContext.request.contextPath}/assets/css/logo.svg" alt="">
          <div class="brand-name">Aarogya<small>CLINIC</small></div>
        </div>
        <h2>Welcome back</h2>
        <p style="color:var(--muted);font-size:14px">Sign in to continue</p>
        <c:if test="${not empty error}">
          <div class="alert-error" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <div class="form-group">
          <label for="email">Email address</label>
          <input class="form-control" id="email" type="email" name="email" value="<c:out value='${param.email}'/>"
                 autocomplete="email" placeholder="name@example.in" required>
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input class="form-control" id="password" type="password" name="password"
                 autocomplete="current-password" placeholder="Enter your password" required>
        </div>
        <button class="btn btn-primary" style="width:100%;margin-top:6px" type="submit">Sign in</button>
        <p style="text-align:center;font-size:13px;margin:18px 0 0">
          New patient? <a href="${pageContext.request.contextPath}/register" style="color:var(--teal);font-weight:700">Create your account</a>
        </p>
        <div class="demo-box">
          <b>Demo sign-in:</b> admin@aarogya.in · doctor@aarogya.in · reception@aarogya.in · patient@aarogya.in<br>
          Password: <b>password</b>
        </div>
      </form>
    </section>
  </div>
</main>
</body>
</html>
