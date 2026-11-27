package com.aarogya.model;
import java.time.LocalDate;
public class Patient {
 private int id; private String patientId,fullName,guardianName,mobile,email,address,city,state,pinCode,emergencyContact,bloodGroup,gender,status;
 private LocalDate dob;
 public int getId(){return id;} public void setId(int id){this.id=id;}
 public String getPatientId(){return patientId;} public void setPatientId(String patientId){this.patientId=patientId;}
 public String getFullName(){return fullName;} public void setFullName(String fullName){this.fullName=fullName;}
 public String getGuardianName(){return guardianName;} public void setGuardianName(String guardianName){this.guardianName=guardianName;}
 public String getMobile(){return mobile;} public void setMobile(String mobile){this.mobile=mobile;}
 public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
 public String getAddress(){return address;} public void setAddress(String address){this.address=address;}
 public String getCity(){return city;} public void setCity(String city){this.city=city;}
 public String getState(){return state;} public void setState(String state){this.state=state;}
 public String getPinCode(){return pinCode;} public void setPinCode(String pinCode){this.pinCode=pinCode;}
 public String getEmergencyContact(){return emergencyContact;} public void setEmergencyContact(String emergencyContact){this.emergencyContact=emergencyContact;}
 public String getBloodGroup(){return bloodGroup;} public void setBloodGroup(String bloodGroup){this.bloodGroup=bloodGroup;}
 public String getGender(){return gender;} public void setGender(String gender){this.gender=gender;}
 public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
 public LocalDate getDob(){return dob;} public void setDob(LocalDate dob){this.dob=dob;}
}
