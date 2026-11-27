package com.aarogya.controller;
import com.aarogya.util.DBUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
 protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
  try(Connection c=DBUtil.getConnection()){
   req.setAttribute("totalPatients",count(c,"SELECT COUNT(*) FROM patients"));
   req.setAttribute("todayAppointments",count(c,"SELECT COUNT(*) FROM appointments WHERE appointment_date=CURRENT_DATE"));
   req.setAttribute("waiting",count(c,"SELECT COUNT(*) FROM tokens WHERE status='Waiting'"));
   req.setAttribute("completed",count(c,"SELECT COUNT(*) FROM consultations WHERE status='Completed'"));
   req.setAttribute("doctors",count(c,"SELECT COUNT(*) FROM doctors WHERE status='Available'"));
   req.setAttribute("revenue",sum(c,"SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='Paid'"));
  }catch(SQLException e){req.setAttribute("dbError","Dashboard data is temporarily unavailable.");}
  req.getRequestDispatcher("/admin/dashboard.jsp").forward(req,res);
 }
 private int count(Connection c,String sql)throws SQLException{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getInt(1);}}
 private int sum(Connection c,String sql)throws SQLException{return count(c,sql);}
}
