import java.sql.*;
import java.util.*;

class Student {

    Scanner sc;

    Student(Scanner sc) {
        this.sc = sc;
    }

    public void add() {
        try {
            System.out.print("Enter Student Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Roll No: ");
            int roll = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO student VALUES (?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roll);
            ps.setString(2, name);

            ps.executeUpdate();
            con.close();

            System.out.println("Student stored in database!\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void view() {
        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM student");

            System.out.println("RollNo\tName");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + "\t" + rs.getString(2));
            }

            con.close();
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void delete(){
        try{
            System.out.print("Enter Roll No to delete: ");
            int roll = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            String sql = "Delete from student where rollno=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,roll);
            int rows = ps.executeUpdate();
            con.close();
            if(rows>0){
                System.out.println("Student deleted Successfully!\n");
            }
            else{
                System.out.println("Student Not Found!\n");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
