<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Create a Patient Account | Aarogya Clinic</title>
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
        <div class="eyebrow" style="color:#78cbc4">PATIENT PORTAL</div>
        <h1>Your care, connected.</h1>
        <p>Create a secure account to manage appointments, tokens and follow-ups at Aarogya Clinic.</p>
      </div>
      <small>© 2026 Aarogya Clinic · Sector 62, Noida</small>
    </aside>
    <section class="login-main">
      <form class="login-card" method="post" action="${pageContext.request.contextPath}/register">
        <div class="brand" style="margin-bottom:24px">
          <img class="brand-mark" src="${pageContext.request.contextPath}/assets/css/logo.svg" alt="">
          <div class="brand-name">Aarogya<small>CLINIC</small></div>
        </div>
        <h2>Create your account</h2>
        <p style="color:var(--muted);font-size:14px">Patient accounts are for patients. Staff accounts are managed by the hospital.</p>
        <c:if test="${not empty error}">
          <div class="alert-error" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <div class="form-group">
          <label for="fullName">Full name</label>
          <input class="form-control" id="fullName" name="fullName" maxlength="100" autocomplete="name"
                 value="<c:out value='${param.fullName}'/>" required>
        </div>
        <div class="form-group">
          <label for="email">Email address</label>
          <input class="form-control" id="email" type="email" name="email" maxlength="150"
                 autocomplete="email" value="<c:out value='${param.email}'/>" required>
        </div>
        <div class="form-group">
          <label for="mobile">Indian mobile number</label>
          <input class="form-control" id="mobile" type="tel" name="mobile" inputmode="numeric"
                 pattern="[6-9][0-9]{9}" maxlength="10" autocomplete="tel-national"
                 value="<c:out value='${param.mobile}'/>" placeholder="9876543210" required>
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input class="form-control" id="password" type="password" name="password"
                 minlength="8" maxlength="72" autocomplete="new-password" required>
          <small style="color:var(--muted)">Use 8 to 72 characters.</small>
        </div>
        <div class="form-group">
          <label for="confirmPassword">Confirm password</label>
          <input class="form-control" id="confirmPassword" type="password" name="confirmPassword"
                 minlength="8" maxlength="72" autocomplete="new-password" required>
        </div>
        <button class="btn btn-primary" style="width:100%;margin-top:6px" type="submit">Create account</button>
        <p style="text-align:center;font-size:13px;margin:18px 0 0">
          Already registered? <a href="${pageContext.request.contextPath}/login" style="color:var(--teal);font-weight:700">Sign in</a>
        </p>
      </form>
    </section>
  </div>
</main>
</body>
</html>
