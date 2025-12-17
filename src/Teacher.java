import java.sql.*;
import java.util.*;

class Teacher {

    Scanner sc;

    Teacher(Scanner sc) {
        this.sc = sc;
    }

    public void add() {
        try {
            System.out.print("Enter Teacher Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Subject ID: ");
            int id = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO teacher VALUES (?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.setString(2, name);

            ps.executeUpdate();
            con.close();

            System.out.println("Teacher stored in database!\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void view() {
        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM teacher");

            System.out.println("SubjectID\tName");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + "\t\t" + rs.getString(2));
            }

            con.close();
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void delete(){
        try {
            System.out.print("Enter Subject ID to delete: ");
            int id = sc.nextInt();
            sc.nextLine();
            Connection con = DBConnection.getConnection();
            String sql = "delete from teacher where subject_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1,id);
            int rows = ps.executeUpdate();
            con.close();

            if(rows>0){
                System.out.println("Teacher deleted succesfully!\n");
            }
            else{
                System.out.println("Teacher not found\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
